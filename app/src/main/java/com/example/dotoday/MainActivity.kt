package com.example.dotoday

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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

/** Main DoToday dashboard: timeline, calendar, Todoist inbox, notes, and settings with bottom navigation. */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val YEAR = 2026
    }

    data class DayInfo(val name: String, val fullName: String, val number: String)

    sealed class TimelineEntry {
        data class Task(
            val id: String = UUID.randomUUID().toString(),
            val time: String,
            val endTime: String = "",
            val title: String,
            val subtitle: String = "",
            val iconRes: Int = R.drawable.ic_alarm,
            var isCompleted: Boolean = false,
            val isRepeat: Boolean = true
        ) : TimelineEntry()

        data class Interval(
            val times: List<String> = listOf("12:00", "16:00"),
            val currentTime: String = "19:23",
            val remainingText: String = "2h 37m left — then, let's dive in!"
        ) : TimelineEntry()
    }

    private val daysList: List<DayInfo>
        get() {
            val locale = resources.configuration.locales[0]
            return (20..26).map { day ->
                val date = Calendar.getInstance().apply { set(YEAR, Calendar.SEPTEMBER, day) }
                DayInfo(
                    SimpleDateFormat("EEE", locale).format(date.time),
                    SimpleDateFormat("d MMMM yyyy", locale).format(date.time) + " >",
                    day.toString()
                )
            }
        }

    private var selectedDayNumber = "22"
    private lateinit var timelineAdapter: TimelineAdapter
    private lateinit var inboxAdapter: InboxAdapter
    private val dayViewsMap = mutableMapOf<String, Pair<View, TextView>>()
    private val todoistRepository by lazy { TodoistRepository(applicationContext) }
    private val taskStorageKey = "dotoday_timeline_tasks_v1"
    private val tasksByDay = mutableMapOf<String, MutableList<TimelineEntry>>()

    private fun defaultTasksByDay(): Map<String, MutableList<TimelineEntry>> = mapOf(
        "20" to mutableListOf(
            TimelineEntry.Task("1", "07:30", "08:00", "Morning Meditation", "15 mins focus", R.drawable.ic_alarm),
            TimelineEntry.Interval(listOf("11:00", "15:00"), getCurrentTimeFormatted(), "3h 15m left — time to focus!"),
            TimelineEntry.Task("2", "21:30", "22:00", "Read Book", "20 pages", R.drawable.ic_moon)
        ),
        "21" to mutableListOf(
            TimelineEntry.Task("1", "08:00", "09:00", "Morning Jog", "Park run", R.drawable.ic_alarm),
            TimelineEntry.Interval(listOf("12:00", "16:00"), getCurrentTimeFormatted(), "1h 45m left — stay energized!"),
            TimelineEntry.Task("2", "22:00", "22:30", "Wind Down", "Sleep preparation", R.drawable.ic_moon)
        ),
        "22" to mutableListOf(
            TimelineEntry.Task("1", "08:00", "08:30", "Rise and Shine", "", R.drawable.ic_alarm, isRepeat = true),
            TimelineEntry.Interval(listOf("12:00", "16:00"), "19:23", "2h 37m left — then, let's dive in!"),
            TimelineEntry.Task("2", "22:00", "22:30", "Wind Down", "", R.drawable.ic_moon, isRepeat = true)
        ),
        "23" to mutableListOf(
            TimelineEntry.Task("1", "09:00", "10:00", "Team Standup", "Project sync", R.drawable.ic_timeline),
            TimelineEntry.Interval(listOf("13:00", "17:00"), getCurrentTimeFormatted(), "4h 00m left — ready for next task"),
            TimelineEntry.Task("2", "21:45", "22:15", "Journal & Plan", "", R.drawable.ic_settings)
        ),
        "24" to mutableListOf(
            TimelineEntry.Task("1", "08:15", "08:45", "Morning Stretch", "Flexibility routine", R.drawable.ic_alarm),
            TimelineEntry.Interval(listOf("12:30", "16:30"), getCurrentTimeFormatted(), "2h 10m left — deep work time"),
            TimelineEntry.Task("2", "22:00", "22:30", "Wind Down", "", R.drawable.ic_moon)
        ),
        "25" to mutableListOf(
            TimelineEntry.Task("1", "08:00", "09:00", "Workout & Breakfast", "", R.drawable.ic_alarm),
            TimelineEntry.Interval(listOf("13:00", "17:00"), getCurrentTimeFormatted(), "3h 30m left — finish the week strong!"),
            TimelineEntry.Task("2", "22:30", "23:00", "Night Relaxation", "", R.drawable.ic_moon)
        ),
        "26" to mutableListOf(
            TimelineEntry.Task("1", "09:30", "10:30", "Weekend Breakfast", "Family time", R.drawable.ic_alarm),
            TimelineEntry.Interval(listOf("14:00", "18:00"), getCurrentTimeFormatted(), "5h 00m left — enjoy weekend!"),
            TimelineEntry.Task("2", "23:00", "23:30", "Late Rest", "", R.drawable.ic_moon)
        )
    )

    private fun loadTasksFromPrefs(): Map<String, MutableList<TimelineEntry>> {
        val prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(taskStorageKey, null) ?: return emptyMap()
        return try {
            val type = object : TypeToken<Map<String, List<TaskSnapshot>>>() {}.type
            val snapshots = Gson().fromJson<Map<String, List<TaskSnapshot>>>(json, type) ?: emptyMap()
            snapshots.mapValues { (_, value) -> value.map { it.toTimelineEntry() }.toMutableList() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load saved tasks", e)
            emptyMap()
        }
    }

    private fun saveTasksToPrefs() {
        val prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val snapshots = tasksByDay.mapValues { (_, entries) ->
            entries.filterIsInstance<TimelineEntry.Task>().map { TaskSnapshot.from(it) }
        }
        prefs.edit().putString(taskStorageKey, Gson().toJson(snapshots)).apply()
    }

    private data class TaskSnapshot(
        val id: String,
        val time: String,
        val endTime: String,
        val title: String,
        val subtitle: String,
        val iconRes: Int,
        val isCompleted: Boolean,
        val isRepeat: Boolean
    ) {
        fun toTimelineEntry(): TimelineEntry.Task = TimelineEntry.Task(
            id = id,
            time = time,
            endTime = endTime,
            title = title,
            subtitle = subtitle,
            iconRes = iconRes,
            isCompleted = isCompleted,
            isRepeat = isRepeat
        )

        companion object {
            fun from(entry: TimelineEntry.Task): TaskSnapshot = TaskSnapshot(
                id = entry.id,
                time = entry.time,
                endTime = entry.endTime,
                title = entry.title,
                subtitle = entry.subtitle,
                iconRes = entry.iconRes,
                isCompleted = entry.isCompleted,
                isRepeat = entry.isRepeat
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySavedTheme()
        tasksByDay.putAll(defaultTasksByDay())

        // Initialize Firebase Cloud Messaging notifications and local task reminders
        FirebaseNotificationSetup.initialize(this)
        TaskReminderReceiver.createNotificationChannel(this)

        setContentView(R.layout.activity_main)
        setupTimeline()
        setupCalendar()
        setupInbox()
        setupSettings()
        setupBottomNav()

        lifecycleScope.launch {
            val savedTasks = withContext(Dispatchers.IO) { loadTasksFromPrefs() }
            if (savedTasks.isNotEmpty()) {
                tasksByDay.clear()
                tasksByDay.putAll(savedTasks)
                runOnUiThread {
                    setupCalendar()
                    setupTimeline()
                    selectCalendarDay(daysList.firstOrNull { it.number == selectedDayNumber } ?: daysList.first(), animate = false)
                }
            }
        }

        findViewById<View>(R.id.btn_calendar_picker)?.setOnClickListener { showCalendarPickerDialog() }
        findViewById<FloatingActionButton>(R.id.fab_add_task).setOnClickListener { showAddTaskDialog() }
        findViewById<View>(R.id.btn_new_inbox_task).setOnClickListener { showAddTodoistTaskDialog() }
        findViewById<View>(R.id.btn_new_inbox_task_bottom).setOnClickListener { showAddTodoistTaskDialog() }
        findViewById<View>(R.id.btn_logout)?.setOnClickListener { view ->
            val prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(SettingsActivity.KEY_IS_LOGGED_IN, false).apply()
            Snackbar.make(view, getString(R.string.general_logout_success), Snackbar.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
    }

    private fun getCurrentTimeFormatted(): String {
        val now = Calendar.getInstance()
        return String.format(Locale.ROOT, "%02d:%02d", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))
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

    private fun setupTimeline() {
        val recycler = findViewById<RecyclerView>(R.id.rv_timeline)
        recycler.layoutManager = LinearLayoutManager(this)
        timelineAdapter = TimelineAdapter(
            (tasksByDay[selectedDayNumber] ?: mutableListOf()).toMutableList(),
            onAddTaskClicked = { showAddTaskDialog() },
            onTaskToggleCompleted = { task ->
                Log.i(TAG, "Task completion updated: ${task.isCompleted}")
                timelineAdapter.notifyDataSetChanged()
            }
        )
        recycler.adapter = timelineAdapter

        // Swipe task to side to open task actions bottom sheet with Edit and Delete icons
        val touchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val entries = tasksByDay[selectedDayNumber] ?: return
                if (position !in entries.indices) return
                val entry = entries[position]

                // Reset swiped item view state immediately
                timelineAdapter.notifyItemChanged(position)

                if (entry !is TimelineEntry.Task) return

                // Show Bottom Sheet Dialog with Edit and Delete icons
                val bottomSheet = com.google.android.material.bottomsheet.BottomSheetDialog(this@MainActivity)
                val sheetView = layoutInflater.inflate(R.layout.dialog_task_actions, findViewById(android.R.id.content), false)
                bottomSheet.setContentView(sheetView)

                sheetView.findViewById<View>(R.id.btn_sheet_edit)?.setOnClickListener {
                    bottomSheet.dismiss()
                    showEditTaskDialog(entry, position)
                }

                sheetView.findViewById<View>(R.id.btn_sheet_delete)?.setOnClickListener {
                    bottomSheet.dismiss()
                    entries.removeAt(position)
                    saveTasksToPrefs()
                    timelineAdapter.updateEntries(entries)
                    Snackbar.make(recycler, "Task deleted", Snackbar.LENGTH_LONG)
                        .setAction("Undo") {
                            entries.add(position, entry)
                            saveTasksToPrefs()
                            timelineAdapter.updateEntries(entries)
                        }.show()
                }

                bottomSheet.show()
            }
        }

        ItemTouchHelper(touchHelperCallback).attachToRecyclerView(recycler)
    }

    private fun showEditTaskDialog(task: TimelineEntry.Task, position: Int) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null)
        val etTitle = view.findViewById<TextInputEditText>(R.id.et_task_title)
        val etDescription = view.findViewById<TextInputEditText>(R.id.et_task_description)
        val btnPickTime = view.findViewById<MaterialButton>(R.id.btn_pick_time)
        val btnPickEndTime = view.findViewById<MaterialButton>(R.id.btn_pick_end_time)

        var selectedStartTime = task.time.ifEmpty { "08:00" }
        var selectedEndTime = task.endTime.ifEmpty { "09:00" }

        btnPickTime?.text = selectedStartTime
        btnPickEndTime?.text = selectedEndTime

        btnPickTime?.setOnClickListener {
            val parts = selectedStartTime.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
            TimePickerDialog(this, { _, h, m ->
                selectedStartTime = String.format(Locale.ROOT, "%02d:%02d", h, m)
                btnPickTime?.text = selectedStartTime
            }, hour, minute, true).show()
        }

        btnPickEndTime?.setOnClickListener {
            val parts = selectedEndTime.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: 9
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
            TimePickerDialog(this, { _, h, m ->
                selectedEndTime = String.format(Locale.ROOT, "%02d:%02d", h, m)
                btnPickEndTime?.text = selectedEndTime
            }, hour, minute, true).show()
        }

        etTitle?.setText(task.title)
        etDescription?.setText(task.subtitle)
        view.findViewById<TextView>(R.id.tv_dialog_title)?.text = "Edit Task"

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Edit Task")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val newTitle = etTitle?.text?.toString()?.trim().orEmpty()
                val newDesc = etDescription?.text?.toString()?.trim()
                if (newTitle.isNotEmpty()) {
                    task.copy(
                        title = newTitle,
                        subtitle = newDesc ?: "",
                        time = selectedStartTime,
                        endTime = selectedEndTime
                    ).let { updated ->
                        val entries = tasksByDay[selectedDayNumber] ?: mutableListOf()
                        if (position in entries.indices) {
                            entries[position] = updated
                            saveTasksToPrefs()
                            timelineAdapter.updateEntries(entries)
                            Toast.makeText(this, "Task updated", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                val entries = tasksByDay[selectedDayNumber] ?: mutableListOf()
                timelineAdapter.updateEntries(entries)
            }
            .setOnCancelListener {
                val entries = tasksByDay[selectedDayNumber] ?: mutableListOf()
                timelineAdapter.updateEntries(entries)
            }
            .show()

        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(getColor(R.color.app_secondary))
        dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(getColor(R.color.text_secondary_color))
    }

    private fun setupCalendar() {
        val container = findViewById<LinearLayout>(R.id.calendar_container)
        container.removeAllViews()
        dayViewsMap.clear()
        val inflater = LayoutInflater.from(this)
        daysList.forEach { day ->
            val item = inflater.inflate(R.layout.item_calendar_day, container, false)
            item.findViewById<TextView>(R.id.tv_day_name).text = day.name
            val number = item.findViewById<TextView>(R.id.tv_day_number)
            number.text = day.number
            dayViewsMap[day.number] = item to number
            updateDayDots(day.number, item)
            item.setOnClickListener { selectCalendarDay(day, animate = true) }
            container.addView(item)
        }
        daysList.firstOrNull { it.number == selectedDayNumber }?.let { selectCalendarDay(it, animate = false) }
    }

    private fun updateDayDots(dayNumber: String, dayView: View) {
        val count = (tasksByDay[dayNumber] ?: emptyList()).count { it is TimelineEntry.Task }
        dayView.findViewById<View>(R.id.dot1)?.visibility = if (count > 0) View.VISIBLE else View.GONE
        dayView.findViewById<View>(R.id.dot2)?.visibility = if (count > 1) View.VISIBLE else View.GONE
    }

    private fun selectCalendarDay(day: DayInfo, animate: Boolean) {
        selectedDayNumber = day.number
        findViewById<TextView>(R.id.tv_month_year)?.text = day.fullName
        dayViewsMap.forEach { (number, pair) ->
            val (view, label) = pair
            if (number == day.number) {
                label.setBackgroundResource(R.drawable.shape_circle_small)
                label.backgroundTintList = getColorStateList(R.color.app_secondary)
                label.setTextColor(getColor(R.color.white))
                if (animate) view.animate().scaleX(1.15f).scaleY(1.15f).setDuration(120)
                    .withEndAction { view.animate().scaleX(1f).scaleY(1f).setDuration(120).start() }.start()
            } else {
                label.background = null
                label.setTextColor(getThemeColor(com.google.android.material.R.attr.colorOnSurface))
            }
        }
        val entries = tasksByDay[day.number] ?: mutableListOf()
        timelineAdapter.updateEntries(entries)
        if (entries.none { it is TimelineEntry.Task } && animate) {
            Snackbar.make(findViewById(R.id.main), "No tasks for ${day.fullName}. Tap + to add!", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun getThemeColor(attribute: Int): Int {
        val value = TypedValue()
        theme.resolveAttribute(attribute, value, true)
        return value.data
    }

    private fun showCalendarPickerDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_calendar_picker, null)
        val calendar = dialogView.findViewById<CalendarView>(R.id.calendar_view_pop)
        val header = dialogView.findViewById<TextView>(R.id.tv_tasks_summary_header)
        val empty = dialogView.findViewById<TextView>(R.id.tv_no_tasks_preview)
        val preview = dialogView.findViewById<RecyclerView>(R.id.rv_dialog_tasks_preview)
        preview.layoutManager = LinearLayoutManager(this)
        var selectedDay = selectedDayNumber
        var selectedFullName = daysList.firstOrNull { it.number == selectedDay }?.fullName ?: selectedDay

        fun updatePreview(day: String, fullName: String) {
            header.text = "Tasks for $fullName:"
            val tasks = (tasksByDay[day] ?: emptyList()).filterIsInstance<TimelineEntry.Task>()
            empty.visibility = if (tasks.isEmpty()) View.VISIBLE else View.GONE
            preview.visibility = if (tasks.isEmpty()) View.GONE else View.VISIBLE
            if (tasks.isNotEmpty()) preview.adapter = DialogTaskPreviewAdapter(tasks)
        }

        Calendar.getInstance().apply { set(YEAR, Calendar.SEPTEMBER, selectedDay.toIntOrNull() ?: 22) }
            .also { calendar.date = it.timeInMillis }
        updatePreview(selectedDay, selectedFullName)
        calendar.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedDay = dayOfMonth.toString()
            val date = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
            selectedFullName = SimpleDateFormat("d MMMM yyyy", resources.configuration.locales[0]).format(date.time) + " >"
            updatePreview(selectedDay, selectedFullName)
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setPositiveButton("View Timeline") { _, _ ->
                val day = daysList.firstOrNull { it.number == selectedDay }
                    ?: DayInfo("", selectedFullName, selectedDay)
                selectCalendarDay(day, animate = true)
            }
            .setNegativeButton("Close", null)
            .show()
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(getColor(R.color.app_secondary))
        dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(getColor(R.color.text_secondary_color))
    }

    private fun showAddTaskDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null)
        val title = view.findViewById<TextInputEditText>(R.id.et_task_title)
        val description = view.findViewById<TextInputEditText>(R.id.et_task_description)
        val duration = view.findViewById<TextInputEditText>(R.id.et_task_duration)
        val startButton = view.findViewById<MaterialButton>(R.id.btn_pick_time)
        val endButton = view.findViewById<MaterialButton>(R.id.btn_pick_end_time)
        val repeat = view.findViewById<MaterialSwitch>(R.id.switch_repeat_task)
        val repeatDetails = view.findViewById<View>(R.id.layout_repeat_details)
        val frequency = view.findViewById<AutoCompleteTextView>(R.id.spinner_repeat_frequency)
        val repeatDuration = view.findViewById<TextInputEditText>(R.id.et_repeat_duration)
        val frequencies = arrayOf("Daily", "Weekly", "Monthly")
        frequency.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, frequencies))
        frequency.setText(frequencies[0], false)
        repeat.setOnCheckedChangeListener { _, checked -> repeatDetails.visibility = if (checked) View.VISIBLE else View.GONE }

        var startHour = 8
        var startMinute = 0
        var endHour = 8
        var endMinute = 30
        fun updateDuration() {
            val start = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, startHour); set(Calendar.MINUTE, startMinute); set(Calendar.SECOND, 0) }
            val end = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, endHour); set(Calendar.MINUTE, endMinute); set(Calendar.SECOND, 0) }
            if (end.before(start)) end.add(Calendar.DAY_OF_YEAR, 1)
            duration.setText(((end.timeInMillis - start.timeInMillis) / 60000).toString())
        }
        startButton.setOnClickListener {
            TimePickerDialog(this, { _: TimePicker, hour: Int, minute: Int ->
                startHour = hour; startMinute = minute
                startButton.text = String.format(Locale.ROOT, "%02d:%02d", hour, minute)
                updateDuration()
            }, startHour, startMinute, true).show()
        }
        endButton.setOnClickListener {
            TimePickerDialog(this, { _: TimePicker, hour: Int, minute: Int ->
                endHour = hour; endMinute = minute
                endButton.text = String.format(Locale.ROOT, "%02d:%02d", hour, minute)
                updateDuration()
            }, endHour, endMinute, true).show()
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(view)
            .setPositiveButton("Add") { _, _ ->
                val taskTitle = title.text?.toString()?.trim().orEmpty()
                if (taskTitle.isBlank()) return@setPositiveButton
                val isRepeat = repeat.isChecked
                val count = if (isRepeat) (repeatDuration.text?.toString()?.toIntOrNull() ?: 7).coerceIn(1, 365) else 1
                val step = if (frequency.text.toString().equals("Weekly", true)) 7 else 1
                repeat(count) { index ->
                    val dayNumber = ((selectedDayNumber.toIntOrNull() ?: 22) + index * step).toString()
                    val entries = tasksByDay.getOrPut(dayNumber) { mutableListOf() }
                    val newTask = TimelineEntry.Task(
                        time = String.format(Locale.ROOT, "%02d:%02d", startHour, startMinute),
                        endTime = String.format(Locale.ROOT, "%02d:%02d", endHour, endMinute),
                        title = taskTitle,
                        subtitle = description.text?.toString().orEmpty(),
                        isRepeat = isRepeat
                    )
                    val lastTask = entries.indexOfLast { it is TimelineEntry.Task }
                    entries.add(if (lastTask >= 0) lastTask + 1 else entries.size, newTask)
                    dayViewsMap[dayNumber]?.first?.let { updateDayDots(dayNumber, it) }
                }
                saveTasksToPrefs()
                timelineAdapter.updateEntries(tasksByDay[selectedDayNumber] ?: mutableListOf())
            }
            .setNegativeButton("Cancel", null)
            .show()
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(getColor(R.color.app_secondary))
        dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(getColor(R.color.text_secondary_color))
    }

    private fun selectTab(itemId: Int) {
        val timeline = findViewById<RecyclerView>(R.id.rv_timeline)
        val notes = findViewById<View>(R.id.layout_notes)
        val inbox = findViewById<View>(R.id.layout_inbox)
        val settings = findViewById<View>(R.id.layout_settings)
        timeline?.visibility = View.GONE
        notes?.visibility = View.GONE
        inbox?.visibility = View.GONE
        settings?.visibility = View.GONE

        when (itemId) {
            R.id.nav_timeline -> timeline?.visibility = View.VISIBLE
            R.id.nav_notes -> notes?.visibility = View.VISIBLE
            R.id.nav_inbox -> { inbox?.visibility = View.VISIBLE; fetchTodoistTasks() }
            R.id.nav_settings -> { settings?.visibility = View.VISIBLE; setupSettings() }
        }
    }

    private fun setupBottomNav() {
        val nav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        val prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val savedTabId = prefs.getInt("key_selected_tab", R.id.nav_timeline)

        nav.setOnItemSelectedListener { item ->
            prefs.edit().putInt("key_selected_tab", item.itemId).apply()
            selectTab(item.itemId)
            true
        }
        nav.selectedItemId = savedTabId
    }

    private fun setupSettings() {
        val settingsView = findViewById<View>(R.id.layout_settings) ?: return
        val prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)

        val tvProfileName = settingsView.findViewById<TextView>(R.id.tv_profile_name)
        val tvProfileEmail = settingsView.findViewById<TextView>(R.id.tv_profile_email)
        val btnEditUsername = settingsView.findViewById<MaterialButton>(R.id.btn_edit_username)
        val dropdownLanguage = settingsView.findViewById<AutoCompleteTextView>(R.id.dropdown_language)
        val dropdownTheme = settingsView.findViewById<AutoCompleteTextView>(R.id.dropdown_theme)
        val btnDefaultTime = settingsView.findViewById<MaterialButton>(R.id.btn_default_time)
        val dropdownBehavior = settingsView.findViewById<AutoCompleteTextView>(R.id.dropdown_completed_behavior)
        val switchHaptic = settingsView.findViewById<SwitchMaterial>(R.id.switch_haptic)
        val btnLogout = settingsView.findViewById<MaterialButton>(R.id.btn_logout)

        val currentUsername = prefs.getString(SettingsActivity.KEY_USERNAME, "DoToday User") ?: "DoToday User"
        tvProfileName?.text = currentUsername
        tvProfileEmail?.text = "Active Account"
        btnEditUsername?.setOnClickListener {
            showUpdateUsernameDialog(tvProfileName)
        }

        val languageLabels = arrayOf("English", "Afrikaans", "isiZulu", "System Default")
        val languageCodes = arrayOf("en", "af", "zu", "")
        val languageAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, languageLabels)
        dropdownLanguage?.setAdapter(languageAdapter)
        dropdownLanguage?.threshold = 0
        dropdownLanguage?.setOnClickListener {
            languageAdapter.filter.filter(null)
            dropdownLanguage.post { dropdownLanguage.showDropDown() }
        }
        dropdownLanguage?.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                languageAdapter.filter.filter(null)
                dropdownLanguage.post { dropdownLanguage.showDropDown() }
            }
        }

        val currentLangCode = prefs.getString(SettingsActivity.KEY_LANGUAGE, "") ?: ""
        val currentLangLabel = when (currentLangCode) {
            "af" -> "Afrikaans"
            "zu" -> "isiZulu"
            "en" -> "English"
            else -> "System Default"
        }
        dropdownLanguage?.setText(currentLangLabel, false)
        dropdownLanguage?.setOnItemClickListener { _, _, position, _ ->
            val selectedCode = languageCodes[position]
            prefs.edit().putString(SettingsActivity.KEY_LANGUAGE, selectedCode).apply()
            val appLocale = if (selectedCode.isEmpty()) LocaleListCompat.getEmptyLocaleList()
                            else LocaleListCompat.forLanguageTags(selectedCode)
            AppCompatDelegate.setApplicationLocales(appLocale)
            Toast.makeText(this, "Language updated", Toast.LENGTH_SHORT).show()
        }

        val themeOptions = arrayOf("System Default", "Light Mode", "Dark Mode")
        val themeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, themeOptions)
        dropdownTheme?.setAdapter(themeAdapter)
        dropdownTheme?.threshold = 0
        dropdownTheme?.setOnClickListener { dropdownTheme.post { dropdownTheme.showDropDown() } }
        dropdownTheme?.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) dropdownTheme.post { dropdownTheme.showDropDown() } }
        val currentTheme = prefs.getString(SettingsActivity.KEY_THEME, "System Default")
        dropdownTheme?.setText(currentTheme, false)
        dropdownTheme?.setOnItemClickListener { _, _, position, _ ->
            val selected = themeOptions[position]
            prefs.edit().putString(SettingsActivity.KEY_THEME, selected).apply()
            val mode = when (selected) {
                "Light Mode" -> AppCompatDelegate.MODE_NIGHT_NO
                "Dark Mode" -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            AppCompatDelegate.setDefaultNightMode(mode)
            Toast.makeText(this, "Theme updated", Toast.LENGTH_SHORT).show()
        }

        val savedTime = prefs.getString(SettingsActivity.KEY_DEFAULT_DUE_TIME, "09:00") ?: "09:00"
        btnDefaultTime?.text = "Default Task Time: $savedTime"
        btnDefaultTime?.setOnClickListener {
            val timeParts = savedTime.split(":")
            val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 9
            val minute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0

            val picker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(hour)
                .setMinute(minute)
                .setTitleText("Select Default Task Time")
                .build()

            picker.addOnPositiveButtonClickListener {
                val newTime = String.format(Locale.getDefault(), "%02d:%02d", picker.hour, picker.minute)
                prefs.edit().putString(SettingsActivity.KEY_DEFAULT_DUE_TIME, newTime).apply()
                btnDefaultTime?.text = "Default Task Time: $newTime"
                Toast.makeText(this, "Default time saved", Toast.LENGTH_SHORT).show()
            }
            picker.show(supportFragmentManager, "DEFAULT_TIME_PICKER")
        }

        val behaviorOptions = arrayOf("Keep in Place", "Move to Bottom", "Hide Immediately")
        val behaviorAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, behaviorOptions)
        dropdownBehavior?.setAdapter(behaviorAdapter)
        dropdownBehavior?.threshold = 0
        dropdownBehavior?.setOnClickListener { dropdownBehavior.post { dropdownBehavior.showDropDown() } }
        dropdownBehavior?.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) dropdownBehavior.post { dropdownBehavior.showDropDown() } }
        val currentBehavior = prefs.getString(SettingsActivity.KEY_COMPLETED_BEHAVIOR, "Move to Bottom")
        dropdownBehavior?.setText(currentBehavior, false)
        dropdownBehavior?.setOnItemClickListener { _, _, position, _ ->
            val selected = behaviorOptions[position]
            prefs.edit().putString(SettingsActivity.KEY_COMPLETED_BEHAVIOR, selected).apply()
            Toast.makeText(this, "Preference saved", Toast.LENGTH_SHORT).show()
        }

        switchHaptic?.isChecked = prefs.getBoolean(SettingsActivity.KEY_HAPTIC_FEEDBACK, true)
        switchHaptic?.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(SettingsActivity.KEY_HAPTIC_FEEDBACK, isChecked).apply()
        }

        btnLogout?.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out of DoToday?")
                .setPositiveButton("Log Out") { _, _ ->
                    val prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
                    prefs.edit().putBoolean(SettingsActivity.KEY_IS_LOGGED_IN, false).apply()
                    Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun showUpdateUsernameDialog(tvProfileName: TextView) {
        val prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val inputLayout = TextInputLayout(this).apply {
            hint = "New Username"
            setPadding(32, 16, 32, 16)
        }
        val editText = TextInputEditText(inputLayout.context).apply {
            setText(prefs.getString(SettingsActivity.KEY_USERNAME, "DoToday User"))
            maxLines = 1
        }
        inputLayout.addView(editText)

        MaterialAlertDialogBuilder(this)
            .setTitle("Update Username")
            .setView(inputLayout)
            .setPositiveButton("Save") { _, _ ->
                val newName = editText.text?.toString()?.trim() ?: ""
                if (newName.isNotEmpty()) {
                    prefs.edit().putString(SettingsActivity.KEY_USERNAME, newName).apply()
                    tvProfileName.text = newName
                    Toast.makeText(this, "Username updated", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Username cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupInbox() {
        val recycler = findViewById<RecyclerView>(R.id.rv_inbox)
        recycler.layoutManager = LinearLayoutManager(this)
        inboxAdapter = InboxAdapter(mutableListOf())
        recycler.adapter = inboxAdapter
    }

    private fun fetchTodoistTasks() {
        val progress = findViewById<ProgressBar>(R.id.pb_inbox_loading)
        val recycler = findViewById<RecyclerView>(R.id.rv_inbox)
        val empty = findViewById<View>(R.id.layout_inbox_empty)
        val bottomButton = findViewById<View>(R.id.btn_new_inbox_task_bottom)
        progress.visibility = View.VISIBLE
        recycler.visibility = View.GONE
        empty.visibility = View.GONE
        bottomButton.visibility = View.GONE
        lifecycleScope.launch {
            val result = todoistRepository.getTasks()
            progress.visibility = View.GONE
            result.onSuccess { tasks ->
                if (tasks.isEmpty()) empty.visibility = View.VISIBLE
                else {
                    inboxAdapter.updateTasks(tasks)
                    recycler.visibility = View.VISIBLE
                    bottomButton.visibility = View.VISIBLE
                }
            }.onFailure { error ->
                Log.e(TAG, "Unable to fetch Todoist tasks", error)
                empty.visibility = View.VISIBLE
                Snackbar.make(recycler, "Failed to fetch tasks: ${error.message}", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun showAddTodoistTaskDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null)
        val title = view.findViewById<TextInputEditText>(R.id.et_task_title)
        val description = view.findViewById<TextInputEditText>(R.id.et_task_description)
        view.findViewById<View>(R.id.layout_time_duration)?.visibility = View.GONE
        view.findViewById<View>(R.id.layout_repeat_section)?.visibility = View.GONE
        view.findViewById<TextView>(R.id.tv_dialog_title)?.text = "New Inbox Task"
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("New Inbox Task")
            .setView(view)
            .setPositiveButton("Add") { _, _ ->
                val taskTitle = title.text?.toString()?.trim().orEmpty()
                if (taskTitle.isNotEmpty()) addNewTodoistTask(taskTitle, description.text?.toString())
            }
            .setNegativeButton("Cancel", null)
            .show()
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(getColor(R.color.app_secondary))
        dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(getColor(R.color.text_secondary_color))
    }

    private fun addNewTodoistTask(title: String, description: String? = null) {
        lifecycleScope.launch {
            val result = todoistRepository.addTask(title, description)
            result.onSuccess {
                fetchTodoistTasks()
                Snackbar.make(findViewById(R.id.main), "Task added to Todoist", Snackbar.LENGTH_SHORT).show()
            }.onFailure { error ->
                Log.e(TAG, "Unable to add Todoist task", error)
                Snackbar.make(findViewById(R.id.main), "Failed to add task: ${error.message}", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    class InboxAdapter(private val tasks: MutableList<TodoistTask>) : RecyclerView.Adapter<InboxAdapter.ViewHolder>() {
        fun updateTasks(updated: List<TodoistTask>) {
            tasks.clear(); tasks.addAll(updated); notifyDataSetChanged()
        }
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val content: TextView = view.findViewById(R.id.tv_task_content)
            val description: TextView = view.findViewById(R.id.tv_task_description)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_inbox_task, parent, false))
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val task = tasks[position]
            holder.content.text = task.content
            holder.description.text = task.description
            holder.description.visibility = if (task.description.isNullOrEmpty()) View.GONE else View.VISIBLE
        }
        override fun getItemCount() = tasks.size
    }

    class DialogTaskPreviewAdapter(private val tasks: List<TimelineEntry.Task>) : RecyclerView.Adapter<DialogTaskPreviewAdapter.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val content: TextView = view.findViewById(R.id.tv_task_content)
            val description: TextView = view.findViewById(R.id.tv_task_description)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_inbox_task, parent, false))
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val task = tasks[position]
            holder.content.text = "${task.time} - ${task.title}"
            holder.description.text = task.subtitle
            holder.description.visibility = if (task.subtitle.isEmpty()) View.GONE else View.VISIBLE
        }
        override fun getItemCount() = tasks.size
    }

    class TimelineAdapter(
        private val items: MutableList<TimelineEntry>,
        private val onAddTaskClicked: () -> Unit,
        private val onTaskToggleCompleted: (TimelineEntry.Task) -> Unit
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        companion object { private const val TYPE_TASK = 0; private const val TYPE_INTERVAL = 1 }
        fun updateEntries(entries: List<TimelineEntry>) {
            items.clear(); items.addAll(entries); notifyDataSetChanged()
        }
        override fun getItemViewType(position: Int) = when (items[position]) {
            is TimelineEntry.Task -> TYPE_TASK
            is TimelineEntry.Interval -> TYPE_INTERVAL
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return if (viewType == TYPE_TASK) TaskViewHolder(inflater.inflate(R.layout.item_timeline_task, parent, false))
            else IntervalViewHolder(inflater.inflate(R.layout.item_timeline_interval, parent, false))
        }
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (val entry = items[position]) {
                is TimelineEntry.Task -> (holder as TaskViewHolder).bind(entry, position == 0, position == items.lastIndex, onTaskToggleCompleted)
                is TimelineEntry.Interval -> (holder as IntervalViewHolder).bind(entry, onAddTaskClicked)
            }
        }
        override fun getItemCount() = items.size

        class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val timeLeft: TextView = view.findViewById(R.id.tv_time_left)
            private val taskTime: TextView = view.findViewById(R.id.tv_task_time)
            private val title: TextView = view.findViewById(R.id.tv_task_title)
            private val subtitle: TextView = view.findViewById(R.id.tv_task_subtitle)
            private val icon: android.widget.ImageView = view.findViewById(R.id.iv_icon)
            private val repeat: android.widget.ImageView = view.findViewById(R.id.iv_repeat)
            private val complete: android.widget.ImageView = view.findViewById(R.id.btn_complete)
            private val lineTop: View = view.findViewById(R.id.line_top)
            private val lineBottom: View = view.findViewById(R.id.line_bottom)
            fun bind(task: TimelineEntry.Task, first: Boolean, last: Boolean, toggle: (TimelineEntry.Task) -> Unit) {
                timeLeft.text = task.time
                taskTime.text = task.time
                title.text = task.title
                subtitle.text = task.subtitle
                subtitle.visibility = if (task.subtitle.isEmpty()) View.GONE else View.VISIBLE
                icon.setImageResource(task.iconRes)
                repeat.visibility = if (task.isRepeat) View.VISIBLE else View.GONE
                lineTop.visibility = if (first) View.INVISIBLE else View.VISIBLE
                lineBottom.visibility = if (last) View.INVISIBLE else View.VISIBLE
                if (task.isCompleted) {
                    complete.setImageResource(R.drawable.ic_check_ring_filled)
                    title.paintFlags = title.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                    title.alpha = 0.6f
                } else {
                    complete.setImageResource(R.drawable.ic_check_ring)
                    title.paintFlags = title.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    title.alpha = 1f
                }
                complete.setOnClickListener { task.isCompleted = !task.isCompleted; toggle(task) }
            }
        }

        class IntervalViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val firstTime: TextView = view.findViewById(R.id.tv_ruler_time1)
            private val secondTime: TextView = view.findViewById(R.id.tv_ruler_time2)
            private val currentTime: TextView = view.findViewById(R.id.tv_ruler_current_time)
            private val status: TextView = view.findViewById(R.id.tv_interval_status)
            private val addButton: View = view.findViewById(R.id.btn_inline_add_task)
            fun bind(interval: TimelineEntry.Interval, add: () -> Unit) {
                firstTime.text = interval.times.getOrNull(0) ?: "12:00"
                secondTime.text = interval.times.getOrNull(1) ?: "16:00"
                currentTime.text = interval.currentTime
                status.text = interval.remainingText
                addButton.setOnClickListener { add() }
            }
        }
    }
}
