package com.example.dotoday

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.CalendarView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dotoday.data.TodoistRepository
import com.example.dotoday.data.TodoistTask
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

/** Main DoToday dashboard: timeline, calendar, Todoist inbox and navigation. */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val SCHEDULER_TAG = "DoTodayScheduler"
        private const val YEAR = 2026
    }

    // ==========================================
    // DATA MODELS
    // ==========================================

    data class DayInfo(
        val name: String,
        val fullName: String,
        val number: String,
        val dateKey: String
    )

    sealed class TimelineEntry {
        data class Task(
            val id: String = UUID.randomUUID().toString(),
            val time: String,
            val endTime: String = "",
            val title: String,
            val subtitle: String = "",
            val iconRes: Int = R.drawable.ic_alarm,
            var isCompleted: Boolean = false,
            val isRepeat: Boolean = false
        ) : TimelineEntry()

        data class Interval(
            val times: List<String> = listOf("12:00", "16:00"),
            val currentTime: String = "",
            val remainingText: String = ""
        ) : TimelineEntry()
    }

    // ==========================================
    // REAL CALENDAR DATA
    // ==========================================

    private val dateKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val dateHeaderFormat = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
    private val weekdayFormat = SimpleDateFormat("EEE", Locale.getDefault())

    private var selectedDate: Calendar = Calendar.getInstance()
    private var selectedDayNumber: String = formatDateKey(selectedDate)
    private var daysList = emptyList<DayInfo>()
    private val dayViewsMap = mutableMapOf<String, Pair<View, TextView>>()
    private val tasksByDay = mutableMapOf<String, MutableList<TimelineEntry>>()

    // ==========================================
    // ADAPTERS AND REPOSITORIES
    // ==========================================

    private lateinit var timelineAdapter: TimelineAdapter
    private lateinit var inboxAdapter: InboxAdapter
    private val todoistRepository by lazy { TodoistRepository(applicationContext) }

    // ==========================================
    // ACTIVITY LIFECYCLE
    // ==========================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySavedTheme()
        setContentView(R.layout.activity_main)

        // Firebase Cloud Messaging & Local Notifications
        FirebaseNotificationSetup.initialize(this)
        TaskReminderReceiver.createNotificationChannel(this)

        // Initialize screens & components
        setupTimeline()
        setupCalendar()
        setupInbox()
        setupSettings()
        setupBottomNav()

        // Calendar picker
        findViewById<View>(R.id.btn_calendar_picker)?.setOnClickListener {
            showCalendarPickerDialog()
        }

        // Timeline add task
        findViewById<FloatingActionButton>(R.id.fab_add_task).setOnClickListener {
            showAddTaskDialog()
        }

        // Inbox task creation
        findViewById<View>(R.id.btn_new_inbox_task)?.setOnClickListener {
            showAddTodoistTaskDialog()
        }

        findViewById<View>(R.id.btn_new_inbox_task_bottom)?.setOnClickListener {
            showAddTodoistTaskDialog()
        }

        // Logout
        findViewById<View>(R.id.btn_logout)?.setOnClickListener { view ->
            val prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(SettingsActivity.KEY_IS_LOGGED_IN, false).apply()
            Snackbar.make(view, getString(R.string.general_logout_success), Snackbar.LENGTH_SHORT).show()
            
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }

        Log.d(TAG, "DoToday initialized successfully")
    }

    // ==========================================
    // DATE & THEME HELPERS
    // ==========================================

    private fun formatDateKey(calendar: Calendar): String = dateKeyFormat.format(calendar.time)

    private fun displayDate(calendar: Calendar): String = dateHeaderFormat.format(calendar.time)

    private fun calendarCopy(calendar: Calendar): Calendar = calendar.clone() as Calendar

    private fun calendarFromKey(key: String): Calendar {
        val result = Calendar.getInstance()
        try {
            val parsed = dateKeyFormat.parse(key)
            if (parsed != null) {
                result.time = parsed
            }
        } catch (e: Exception) {
            Log.e(TAG, "Invalid date key: $key", e)
        }
        return result
    }

    private fun createWeekDays(calendar: Calendar): List<DayInfo> {
        val monday = calendarCopy(calendar).apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }

        return (0 until 7).map { index ->
            val day = calendarCopy(monday).apply {
                add(Calendar.DAY_OF_MONTH, index)
            }
            DayInfo(
                name = weekdayFormat.format(day.time),
                fullName = displayDate(day),
                number = day.get(Calendar.DAY_OF_MONTH).toString(),
                dateKey = formatDateKey(day)
            )
        }
    }

    private fun getCurrentTimeFormatted(): String {
        val now = Calendar.getInstance()
        return String.format(Locale.ROOT, "%02d:%02d", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))
    }

    private fun getThemeColor(attr: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }

    private fun showMessage(message: String) {
        Snackbar.make(findViewById(R.id.main), message, Snackbar.LENGTH_SHORT).show()
    }

    private fun applySavedTheme() {
        val prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val currentTheme = prefs.getString(SettingsActivity.KEY_THEME, "System Default")
        val mode = when (currentTheme) {
            "Light Mode" -> AppCompatDelegate.MODE_NIGHT_NO
            "Dark Mode" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    // ==========================================
    // TIMELINE SETUP
    // ==========================================

    private fun setupTimeline() {
        val recycler = findViewById<RecyclerView>(R.id.rv_timeline)
        recycler.layoutManager = LinearLayoutManager(this)

        timelineAdapter = TimelineAdapter(
            items = (tasksByDay[selectedDayNumber] ?: mutableListOf()).toMutableList(),
            onAddTaskClicked = { showAddTaskDialog() },
            onTaskToggleCompleted = { task ->
                if (task.isCompleted) {
                    TaskReminderScheduler.cancelReminder(
                        context = this,
                        taskId = task.id.hashCode()
                    )
                    Log.d(SCHEDULER_TAG, "Cancelled completed task reminder: ${task.title}")
                } else {
                    Log.d(SCHEDULER_TAG, "Task reopened: ${task.title}")
                }
                timelineAdapter.notifyDataSetChanged()
            }
        )
        recycler.adapter = timelineAdapter

        // Swipe task to open task actions bottom sheet with Edit and Delete icons
        val touchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false

            override fun onSwiped(viewHolder: