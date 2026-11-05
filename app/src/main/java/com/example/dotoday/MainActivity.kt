package com.example.dotoday

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dotoday.data.TodoistRepository
import com.example.dotoday.data.TodoistTask
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val SCHEDULER_TAG = "DoTodayScheduler"
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

    private val dateKeyFormat = SimpleDateFormat(
        "yyyy-MM-dd",
        Locale.US
    )

    private val dateHeaderFormat = SimpleDateFormat(
        "d MMMM yyyy",
        Locale.getDefault()
    )

    private val weekdayFormat = SimpleDateFormat(
        "EEE",
        Locale.getDefault()
    )

    private var selectedDate: Calendar =
        Calendar.getInstance()

    private var selectedDayNumber: String =
        formatDateKey(selectedDate)

    private var daysList = emptyList<DayInfo>()

    private val dayViewsMap =
        mutableMapOf<String, Pair<View, TextView>>()

    private val tasksByDay =
        mutableMapOf<String, MutableList<TimelineEntry>>()

    // ==========================================
    // ADAPTERS AND REPOSITORIES
    // ==========================================

    private lateinit var timelineAdapter: TimelineAdapter
    private lateinit var inboxAdapter: InboxAdapter

    private val todoistRepository = TodoistRepository()

    // ==========================================
    // ACTIVITY LIFECYCLE
    // ==========================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        // Firebase Cloud Messaging setup
        FirebaseNotificationSetup.initialize(this)

        // Local reminder notification channel
        TaskReminderReceiver.createNotificationChannel(this)

        // Initialize existing screens
        setupTimeline()
        setupCalendar()
        setupInbox()
        setupBottomNav()

        // Calendar picker
        findViewById<View>(
            R.id.btn_calendar_picker
        )?.setOnClickListener {
            showCalendarPickerDialog()
        }

        // Timeline add task
        findViewById<FloatingActionButton>(
            R.id.fab_add_task
        ).setOnClickListener {
            showAddTaskDialog()
        }

        // Inbox task creation
        findViewById<View>(
            R.id.btn_new_inbox_task
        )?.setOnClickListener {
            showAddTodoistTaskDialog()
        }

        findViewById<View>(
            R.id.btn_new_inbox_task_bottom
        )?.setOnClickListener {
            showAddTodoistTaskDialog()
        }

        // Logout
        findViewById<View>(
            R.id.btn_logout
        )?.setOnClickListener {

            val intent = Intent(
                this,
                LoginActivity::class.java
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            startActivity(intent)
            finish()
        }

        Log.d(TAG, "DoToday initialized successfully")
    }

    // ==========================================
    // DATE HELPERS
    // ==========================================

    private fun formatDateKey(
        calendar: Calendar
    ): String {
        return dateKeyFormat.format(calendar.time)
    }

    private fun displayDate(
        calendar: Calendar
    ): String {
        return dateHeaderFormat.format(calendar.time)
    }

    private fun calendarCopy(
        calendar: Calendar
    ): Calendar {
        return calendar.clone() as Calendar
    }

    private fun calendarFromKey(
        key: String
    ): Calendar {
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

    private fun createWeekDays(
        calendar: Calendar
    ): List<DayInfo> {

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
                number = day.get(
                    Calendar.DAY_OF_MONTH
                ).toString(),
                dateKey = formatDateKey(day)
            )
        }
    }

    private fun getCurrentTimeFormatted(): String {
        val now = Calendar.getInstance()

        return String.format(
            Locale.getDefault(),
            "%02d:%02d",
            now.get(Calendar.HOUR_OF_DAY),
            now.get(Calendar.MINUTE)
        )
    }

    private fun getThemeColor(attr: Int): Int {
        val typedValue = TypedValue()

        theme.resolveAttribute(
            attr,
            typedValue,
            true
        )

        return typedValue.data
    }

    private fun showMessage(message: String) {
        Snackbar.make(
            findViewById(R.id.main),
            message,
            Snackbar.LENGTH_SHORT
        ).show()
    }

    // ==========================================
    // TIMELINE SETUP
    // ==========================================

    private fun setupTimeline() {

        val recyclerView =
            findViewById<RecyclerView>(
                R.id.rv_timeline
            )

        recyclerView.layoutManager =
            LinearLayoutManager(this)

        timelineAdapter = TimelineAdapter(
            items = mutableListOf(),
            onAddTaskClicked = {
                showAddTaskDialog()
            },
            onTaskToggleCompleted = { task ->

                if (task.isCompleted) {

                    TaskReminderScheduler.cancelReminder(
                        context = this,
                        taskId = task.id.hashCode()
                    )

                    Log.d(
                        SCHEDULER_TAG,
                        "Cancelled completed task reminder: ${task.title}"
                    )

                } else {

                    // Re-enabling a completed task requires
                    // its original due timestamp. That will
                    // be restored through persistent storage.
                    Log.d(
                        SCHEDULER_TAG,
                        "Task reopened: ${task.title}"
                    )
                }

                timelineAdapter.notifyDataSetChanged()
            }
        )

        recyclerView.adapter = timelineAdapter
    }

    // ==========================================
    // HORIZONTAL WEEK CALENDAR
    // ==========================================

    private fun setupCalendar() {

        val container = findViewById<LinearLayout>(
            R.id.calendar_container
        )

        container.removeAllViews()
        dayViewsMap.clear()

        daysList = createWeekDays(selectedDate)

        val inflater = LayoutInflater.from(this)

        daysList.forEach { dayInfo ->

            val dayView = inflater.inflate(
                R.layout.item_calendar_day,
                container,
                false
            )

            val tvDayName =
                dayView.findViewById<TextView>(
                    R.id.tv_day_name
                )

            val tvDayNumber =
                dayView.findViewById<TextView>(
                    R.id.tv_day_number
                )

            tvDayName.text = dayInfo.name
            tvDayNumber.text = dayInfo.number

            dayViewsMap[dayInfo.dateKey] =
                Pair(dayView, tvDayNumber)

            updateDayDots(
                dayInfo.dateKey,
                dayView
            )

            dayView.setOnClickListener {
                selectCalendarDay(
                    dayInfo,
                    animate = true
                )
            }

            container.addView(dayView)
        }

        daysList.find {
            it.dateKey == selectedDayNumber
        }?.let {
            selectCalendarDay(
                it,
                animate = false
            )
        }
    }

    // ==========================================
    // CALENDAR TASK INDICATOR DOTS
    // ==========================================

    private fun updateDayDots(
        dateKey: String,
        dayView: View
    ) {

        val entries =
            tasksByDay[dateKey] ?: emptyList()

        val tasksCount = entries.count {
            it is TimelineEntry.Task
        }

        val dot1 =
            dayView.findViewById<View>(R.id.dot1)

        val dot2 =
            dayView.findViewById<View>(R.id.dot2)

        when {

            tasksCount >= 2 -> {
                dot1?.visibility = View.VISIBLE
                dot2?.visibility = View.VISIBLE
            }

            tasksCount == 1 -> {
                dot1?.visibility = View.VISIBLE
                dot2?.visibility = View.GONE
            }

            else -> {
                dot1?.visibility = View.GONE
                dot2?.visibility = View.GONE
            }
        }
    }

    // ==========================================
    // SELECT CALENDAR DAY
    // ==========================================

    private fun selectCalendarDay(
        dayInfo: DayInfo,
        animate: Boolean
    ) {

        selectedDayNumber = dayInfo.dateKey

        selectedDate =
            calendarFromKey(dayInfo.dateKey)

        findViewById<TextView>(
            R.id.tv_month_year
        )?.text = "${dayInfo.fullName} >"

        dayViewsMap.forEach { (key, pair) ->

            val dayView = pair.first
            val tvNumber = pair.second

            if (key == selectedDayNumber) {

                tvNumber.setBackgroundResource(
                    R.drawable.shape_circle_small
                )

                tvNumber.backgroundTintList =
                    getColorStateList(
                        R.color.app_secondary
                    )

                tvNumber.setTextColor(
                    getColor(R.color.white)
                )

                if (animate) {

                    dayView.animate()
                        .scaleX(1.15f)
                        .scaleY(1.15f)
                        .setDuration(120)
                        .withEndAction {

                            dayView.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(120)
                                .start()
                        }
                        .start()
                }

            } else {

                tvNumber.background = null

                tvNumber.setTextColor(
                    getThemeColor(
                        com.google.android.material.R.attr
                            .colorOnSurface
                    )
                )
            }
        }

        val entries =
            tasksByDay[selectedDayNumber]
                ?: mutableListOf()

        timelineAdapter.updateEntries(entries)

        if (
            animate &&
            entries.none {
                it is TimelineEntry.Task
            }
        ) {

            showMessage(
                "No tasks for ${dayInfo.fullName}. Tap + to add!"
            )
        }
    }

    // ==========================================
    // FULL CALENDAR PICKER
    // ==========================================

    private fun showCalendarPickerDialog() {

        val dialogView = LayoutInflater.from(this)
            .inflate(
                R.layout.dialog_calendar_picker,
                null
            )

        val calendarView =
            dialogView.findViewById<CalendarView>(
                R.id.calendar_view_pop
            )

        val tvHeader =
            dialogView.findViewById<TextView>(
                R.id.tv_tasks_summary_header
            )

        val tvNoTasks =
            dialogView.findViewById<TextView>(
                R.id.tv_no_tasks_preview
            )

        val rvPreview =
            dialogView.findViewById<RecyclerView>(
                R.id.rv_dialog_tasks_preview
            )

        rvPreview.layoutManager =
            LinearLayoutManager(this)

        var dialogSelectedDate =
            calendarCopy(selectedDate)

        fun updatePreview() {

            val key =
                formatDateKey(dialogSelectedDate)

            val tasks = (
                    tasksByDay[key] ?: emptyList()
                    ).filterIsInstance<TimelineEntry.Task>()

            tvHeader.text =
                "Tasks for ${displayDate(dialogSelectedDate)}"

            if (tasks.isEmpty()) {

                tvNoTasks.visibility = View.VISIBLE
                rvPreview.visibility = View.GONE

            } else {

                tvNoTasks.visibility = View.GONE
                rvPreview.visibility = View.VISIBLE

                rvPreview.adapter =
                    DialogTaskPreviewAdapter(tasks)
            }
        }

        calendarView.date =
            dialogSelectedDate.timeInMillis

        updatePreview()

        calendarView.setOnDateChangeListener {
                _, year, month, dayOfMonth ->

            dialogSelectedDate =
                Calendar.getInstance().apply {
                    set(
                        year,
                        month,
                        dayOfMonth
                    )
                }

            updatePreview()
        }

        val dialog =
            MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setPositiveButton(
                    "View Timeline"
                ) { _, _ ->

                    selectedDate =
                        calendarCopy(dialogSelectedDate)

                    selectedDayNumber =
                        formatDateKey(selectedDate)

                    setupCalendar()

                    showMessage(
                        "Switched to ${displayDate(selectedDate)}"
                    )
                }
                .setNegativeButton(
                    "Close",
                    null
                )
                .show()

        dialog.getButton(
            android.app.AlertDialog.BUTTON_POSITIVE
        )?.setTextColor(
            getColor(R.color.app_secondary)
        )

        dialog.getButton(
            android.app.AlertDialog.BUTTON_NEGATIVE
        )?.setTextColor(
            getColor(R.color.text_secondary_color)
        )
    }

    // ==========================================
    // CREATE A NEW TIMELINE TASK
    // ==========================================

    private fun showAddTaskDialog() {

        val dialogView = LayoutInflater.from(this)
            .inflate(
                R.layout.dialog_add_task,
                null
            )

        val etTitle =
            dialogView.findViewById<TextInputEditText>(
                R.id.et_task_title
            )

        val etDescription =
            dialogView.findViewById<TextInputEditText>(
                R.id.et_task_description
            )

        val etDuration =
            dialogView.findViewById<TextInputEditText>(
                R.id.et_task_duration
            )

        val btnPickTime =
            dialogView.findViewById<MaterialButton>(
                R.id.btn_pick_time
            )

        val btnPickEndTime =
            dialogView.findViewById<MaterialButton>(
                R.id.btn_pick_end_time
            )

        val switchRepeat =
            dialogView.findViewById<MaterialSwitch>(
                R.id.switch_repeat_task
            )

        val layoutRepeatDetails =
            dialogView.findViewById<View>(
                R.id.layout_repeat_details
            )

        val spinnerFrequency =
            dialogView.findViewById<AutoCompleteTextView>(
                R.id.spinner_repeat_frequency
            )

        val etRepeatDuration =
            dialogView.findViewById<TextInputEditText>(
                R.id.et_repeat_duration
            )

        // ======================================
        // DATE SELECTION
        // ======================================

        var selectedReminderDate =
            calendarCopy(selectedDate)

        val dateButton = MaterialButton(this).apply {

            text = DateFormat.getDateInstance(
                DateFormat.LONG
            ).format(selectedReminderDate.time)

            setOnClickListener {

                val date = selectedReminderDate

                DatePickerDialog(
                    this@MainActivity,
                    { _, year, month, day ->

                        selectedReminderDate =
                            Calendar.getInstance().apply {
                                set(
                                    year,
                                    month,
                                    day
                                )
                            }

                        text = DateFormat.getDateInstance(
                            DateFormat.LONG
                        ).format(
                            selectedReminderDate.time
                        )
                    },
                    date.get(Calendar.YEAR),
                    date.get(Calendar.MONTH),
                    date.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
        }

        (dialogView as LinearLayout).addView(
            dateButton,
            1
        )

        // ======================================
        // REPEAT OPTIONS
        // ======================================

        val frequencies = arrayOf(
            "Daily",
            "Weekly",
            "Monthly"
        )

        val frequencyAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            frequencies
        )

        spinnerFrequency.setAdapter(
            frequencyAdapter
        )

        spinnerFrequency.setText(
            frequencies[0],
            false
        )

        switchRepeat.setOnCheckedChangeListener {
                _, isChecked ->

            layoutRepeatDetails.visibility =
                if (isChecked) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
        }

        // ======================================
        // TIME SELECTION
        // ======================================

        val now = Calendar.getInstance()

        var selectedHour =
            now.get(Calendar.HOUR_OF_DAY)

        var selectedMinute =
            now.get(Calendar.MINUTE)

        var endHour = (selectedHour + 1) % 24
        var endMinute = selectedMinute

        fun updateDuration() {

            val startMinutes =
                selectedHour * 60 + selectedMinute

            val finishMinutes =
                endHour * 60 + endMinute

            var difference =
                finishMinutes - startMinutes

            if (difference < 0) {
                difference += 24 * 60
            }

            etDuration.setText(
                difference.toString()
            )
        }

        btnPickTime.text = String.format(
            Locale.getDefault(),
            "%02d:%02d",
            selectedHour,
            selectedMinute
        )

        btnPickEndTime.text = String.format(
            Locale.getDefault(),
            "%02d:%02d",
            endHour,
            endMinute
        )

        updateDuration()

        btnPickTime.setOnClickListener {

            TimePickerDialog(
                this,
                { _, hour, minute ->

                    selectedHour = hour
                    selectedMinute = minute

                    btnPickTime.text = String.format(
                        Locale.getDefault(),
                        "%02d:%02d",
                        hour,
                        minute
                    )

                    updateDuration()
                },
                selectedHour,
                selectedMinute,
                true
            ).show()
        }

        btnPickEndTime.setOnClickListener {

            TimePickerDialog(
                this,
                { _, hour, minute ->

                    endHour = hour
                    endMinute = minute

                    btnPickEndTime.text = String.format(
                        Locale.getDefault(),
                        "%02d:%02d",
                        hour,
                        minute
                    )

                    updateDuration()
                },
                endHour,
                endMinute,
                true
            ).show()
        }

        // ======================================
        // ADD TASK DIALOG
        // ======================================

        val dialog =
            MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setPositiveButton("Add", null)
                .setNegativeButton(
                    "Cancel",
                    null
                )
                .create()

        dialog.setOnShowListener {

            dialog.getButton(
                android.app.AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener {

                val title =
                    etTitle.text.toString().trim()

                val description =
                    etDescription.text.toString().trim()

                if (title.isEmpty()) {

                    etTitle.error =
                        "Task title is required"

                    return@setOnClickListener
                }

                val startTime = String.format(
                    Locale.getDefault(),
                    "%02d:%02d",
                    selectedHour,
                    selectedMinute
                )

                val endTime = String.format(
                    Locale.getDefault(),
                    "%02d:%02d",
                    endHour,
                    endMinute
                )

                val isRepeat =
                    switchRepeat.isChecked

                val repeatCount = if (isRepeat) {

                    (
                            etRepeatDuration.text.toString()
                                .toIntOrNull() ?: 1
                            ).coerceIn(1, 365)

                } else {
                    1
                }

                val frequency =
                    spinnerFrequency.text.toString()
                        .trim()
                        .lowercase(Locale.ROOT)

                val baseDate =
                    calendarCopy(selectedReminderDate)

                var createdCount = 0
                var scheduledCount = 0

                // ==================================
                // CREATE TASK OCCURRENCES
                // ==================================

                for (index in 0 until repeatCount) {

                    val occurrence =
                        calendarCopy(baseDate)

                    if (isRepeat) {

                        when (frequency) {

                            "weekly" -> occurrence.add(
                                Calendar.WEEK_OF_YEAR,
                                index
                            )

                            "monthly" -> occurrence.add(
                                Calendar.MONTH,
                                index
                            )

                            else -> occurrence.add(
                                Calendar.DAY_OF_YEAR,
                                index
                            )
                        }
                    }

                    val occurrenceKey =
                        formatDateKey(occurrence)

                    val newTask =
                        TimelineEntry.Task(
                            time = startTime,
                            endTime = endTime,
                            title = title,
                            subtitle = description,
                            iconRes = R.drawable.ic_alarm,
                            isRepeat = isRepeat
                        )

                    val entries =
                        tasksByDay.getOrPut(
                            occurrenceKey
                        ) {
                            mutableListOf()
                        }

                    entries.add(newTask)

                    createdCount++

                    // ==================================
                    // SCHEDULE REMINDER
                    // ==================================

                    val reminderScheduled =
                        TaskReminderDateHelper.scheduleTask(
                            context = this@MainActivity,
                            taskTitle = title,
                            date = occurrence,
                            hour = selectedHour,
                            minute = selectedMinute,
                            taskId = newTask.id.hashCode()
                        )

                    if (reminderScheduled) {
                        scheduledCount++
                    }

                    Log.d(
                        SCHEDULER_TAG,
                        "Task: $title | Date: $occurrenceKey | Reminder: $reminderScheduled"
                    )
                }

                // ==================================
                // REFRESH TIMELINE
                // ==================================

                selectedDate =
                    calendarCopy(baseDate)

                selectedDayNumber =
                    formatDateKey(selectedDate)

                setupCalendar()

                showMessage(
                    "$createdCount task occurrence(s) added. " +
                            "$scheduledCount reminder(s) scheduled."
                )

                dialog.dismiss()
            }

            dialog.getButton(
                android.app.AlertDialog.BUTTON_POSITIVE
            )?.setTextColor(
                getColor(R.color.app_secondary)
            )

            dialog.getButton(
                android.app.AlertDialog.BUTTON_NEGATIVE
            )?.setTextColor(
                getColor(R.color.text_secondary_color)
            )
        }

        dialog.show()
    }

    // ==========================================
    // BOTTOM NAVIGATION
    // ==========================================

    private fun setupBottomNav() {

        val nav =
            findViewById<BottomNavigationView>(
                R.id.bottom_navigation
            )

        nav.selectedItemId = R.id.nav_timeline

        nav.setOnItemSelectedListener { item ->

            val rvTimeline =
                findViewById<RecyclerView>(
                    R.id.rv_timeline
                )

            val layoutNotes =
                findViewById<View>(
                    R.id.layout_notes
                )

            val layoutInbox =
                findViewById<View>(
                    R.id.layout_inbox
                )

            val layoutSettings =
                findViewById<View>(
                    R.id.layout_settings
                )

            rvTimeline.visibility = View.GONE
            layoutNotes.visibility = View.GONE
            layoutInbox.visibility = View.GONE
            layoutSettings.visibility = View.GONE

            when (item.itemId) {

                R.id.nav_timeline -> {
                    rvTimeline.visibility = View.VISIBLE
                    true
                }

                R.id.nav_notes -> {
                    layoutNotes.visibility = View.VISIBLE
                    true
                }

                R.id.nav_inbox -> {
                    layoutInbox.visibility = View.VISIBLE
                    fetchTodoistTasks()
                    true
                }

                R.id.nav_settings -> {

                    startActivity(
                        Intent(
                            this,
                            SettingsActivity::class.java
                        )
                    )

                    true
                }

                else -> {
                    showMessage(
                        "${item.title} coming soon"
                    )
                    true
                }
            }
        }
    }

    // ==========================================
    // INBOX SETUP
    // ==========================================

    private fun setupInbox() {

        val recyclerView =
            findViewById<RecyclerView>(
                R.id.rv_inbox
            )

        recyclerView.layoutManager =
            LinearLayoutManager(this)

        inboxAdapter =
            InboxAdapter(mutableListOf())

        recyclerView.adapter = inboxAdapter
    }

    private fun fetchTodoistTasks() {

        val progressBar =
            findViewById<ProgressBar>(
                R.id.pb_inbox_loading
            )

        val recyclerView =
            findViewById<RecyclerView>(
                R.id.rv_inbox
            )

        val emptyState =
            findViewById<View>(
                R.id.layout_inbox_empty
            )

        val btnBottom =
            findViewById<View>(
                R.id.btn_new_inbox_task_bottom
            )

        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        emptyState.visibility = View.GONE
        btnBottom.visibility = View.GONE

        lifecycleScope.launch {

            val result =
                todoistRepository.getTasks()

            progressBar.visibility = View.GONE

            result.onSuccess { tasks ->

                if (tasks.isEmpty()) {

                    emptyState.visibility = View.VISIBLE

                } else {

                    inboxAdapter.updateTasks(tasks)
                    recyclerView.visibility = View.VISIBLE
                    btnBottom.visibility = View.VISIBLE
                }

            }.onFailure { error ->

                Log.e(
                    TAG,
                    "Failed to fetch Inbox tasks",
                    error
                )

                emptyState.visibility = View.VISIBLE

                showMessage(
                    "Failed to fetch tasks: ${error.message}"
                )
            }
        }
    }

    // ==========================================
    // ADD INBOX TASK
    // ==========================================

    private fun showAddTodoistTaskDialog() {

        val dialogView = LayoutInflater.from(this)
            .inflate(
                R.layout.dialog_add_task,
                null
            )

        val etTitle =
            dialogView.findViewById<TextInputEditText>(
                R.id.et_task_title
            )

        val etDescription =
            dialogView.findViewById<TextInputEditText>(
                R.id.et_task_description
            )

        dialogView.findViewById<View>(
            R.id.layout_time_duration
        )?.visibility = View.GONE

        dialogView.findViewById<View>(
            R.id.layout_repeat_section
        )?.visibility = View.GONE

        dialogView.findViewById<TextView>(
            R.id.tv_dialog_title
        )?.text = "New Inbox Task"

        MaterialAlertDialogBuilder(this)
            .setTitle("New Inbox Task")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->

                val title =
                    etTitle.text.toString().trim()

                val description =
                    etDescription.text.toString().trim()

                if (title.isNotEmpty()) {

                    addNewTodoistTask(
                        title,
                        description
                    )
                }
            }
            .setNegativeButton(
                "Cancel",
                null
            )
            .show()
    }

    private fun addNewTodoistTask(
        title: String,
        description: String? = null
    ) {

        lifecycleScope.launch {

            val result =
                todoistRepository.addTask(
                    title,
                    description
                )

            result.onSuccess {

                fetchTodoistTasks()

                showMessage(
                    "Task added to Todoist"
                )

            }.onFailure { error ->

                Log.e(
                    TAG,
                    "Failed to create Inbox task",
                    error
                )

                showMessage(
                    "Failed to add task: ${error.message}"
                )
            }
        }
    }

    // ==========================================
    // INBOX ADAPTER
    // ==========================================

    class InboxAdapter(
        private val tasks: MutableList<TodoistTask>
    ) : RecyclerView.Adapter<InboxAdapter.ViewHolder>() {

        fun updateTasks(
            newTasks: List<TodoistTask>
        ) {

            tasks.clear()
            tasks.addAll(newTasks)

            notifyDataSetChanged()
        }

        class ViewHolder(
            view: View
        ) : RecyclerView.ViewHolder(view) {

            val tvContent: TextView =
                view.findViewById(
                    R.id.tv_task_content
                )

            val tvDescription: TextView =
                view.findViewById(
                    R.id.tv_task_description
                )
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): ViewHolder {

            val view = LayoutInflater.from(
                parent.context
            ).inflate(
                R.layout.item_inbox_task,
                parent,
                false
            )

            return ViewHolder(view)
        }

        override fun onBindViewHolder(
            holder: ViewHolder,
            position: Int
        ) {

            val task = tasks[position]

            holder.tvContent.text = task.content
            holder.tvDescription.text = task.description

            holder.tvDescription.visibility =
                if (task.description.isNullOrEmpty()) {
                    View.GONE
                } else {
                    View.VISIBLE
                }
        }

        override fun getItemCount(): Int =
            tasks.size
    }

    // ==========================================
    // CALENDAR PREVIEW ADAPTER
    // ==========================================

    class DialogTaskPreviewAdapter(
        private val tasks: List<TimelineEntry.Task>
    ) : RecyclerView.Adapter<
            DialogTaskPreviewAdapter.ViewHolder
            >() {

        class ViewHolder(
            view: View
        ) : RecyclerView.ViewHolder(view) {

            val tvContent: TextView =
                view.findViewById(
                    R.id.tv_task_content
                )

            val tvDescription: TextView =
                view.findViewById(
                    R.id.tv_task_description
                )
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): ViewHolder {

            return ViewHolder(
                LayoutInflater.from(
                    parent.context
                ).inflate(
                    R.layout.item_inbox_task,
                    parent,
                    false
                )
            )
        }

        override fun onBindViewHolder(
            holder: ViewHolder,
            position: Int
        ) {

            val task = tasks[position]

            holder.tvContent.text =
                "${task.time} - ${task.title}"

            holder.tvDescription.text =
                task.subtitle

            holder.tvDescription.visibility =
                if (task.subtitle.isEmpty()) {
                    View.GONE
                } else {
                    View.VISIBLE
                }
        }

        override fun getItemCount(): Int =
            tasks.size
    }

    // ==========================================
    // TIMELINE ADAPTER
    // ==========================================

    class TimelineAdapter(
        private val items: MutableList<TimelineEntry>,
        private val onAddTaskClicked: () -> Unit,
        private val onTaskToggleCompleted:
            (TimelineEntry.Task) -> Unit
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        companion object {
            private const val TYPE_TASK = 0
            private const val TYPE_INTERVAL = 1
        }

        fun updateEntries(
            newEntries: List<TimelineEntry>
        ) {

            items.clear()
            items.addAll(newEntries)

            notifyDataSetChanged()
        }

        override fun getItemViewType(
            position: Int
        ): Int {

            return when (items[position]) {

                is TimelineEntry.Task -> TYPE_TASK
                is TimelineEntry.Interval -> TYPE_INTERVAL
            }
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): RecyclerView.ViewHolder {

            val inflater =
                LayoutInflater.from(parent.context)

            return if (viewType == TYPE_TASK) {

                TaskViewHolder(
                    inflater.inflate(
                        R.layout.item_timeline_task,
                        parent,
                        false
                    )
                )

            } else {

                IntervalViewHolder(
                    inflater.inflate(
                        R.layout.item_timeline_interval,
                        parent,
                        false
                    )
                )
            }
        }

        override fun onBindViewHolder(
            holder: RecyclerView.ViewHolder,
            position: Int
        ) {

            when (val entry = items[position]) {

                is TimelineEntry.Task -> {

                    (holder as TaskViewHolder).bind(
                        task = entry,
                        isFirst = position == 0,
                        isLast = position == items.size - 1,
                        onToggle = onTaskToggleCompleted
                    )
                }

                is TimelineEntry.Interval -> {

                    (holder as IntervalViewHolder).bind(
                        interval = entry,
                        onAddClicked = onAddTaskClicked
                    )
                }
            }
        }

        override fun getItemCount(): Int =
            items.size

        // ======================================
        // TASK VIEW HOLDER
        // ======================================

        class TaskViewHolder(
            view: View
        ) : RecyclerView.ViewHolder(view) {

            private val tvTimeLeft: TextView =
                view.findViewById(
                    R.id.tv_time_left
                )

            private val tvTaskTime: TextView =
                view.findViewById(
                    R.id.tv_task_time
                )

            private val tvTaskTitle: TextView =
                view.findViewById(
                    R.id.tv_task_title
                )

            private val tvTaskSubtitle: TextView =
                view.findViewById(
                    R.id.tv_task_subtitle
                )

            private val ivIcon: ImageView =
                view.findViewById(
                    R.id.iv_icon
                )

            private val ivRepeat: ImageView =
                view.findViewById(
                    R.id.iv_repeat
                )

            private val btnComplete: ImageView =
                view.findViewById(
                    R.id.btn_complete
                )

            private val lineTop: View =
                view.findViewById(
                    R.id.line_top
                )

            private val lineBottom: View =
                view.findViewById(
                    R.id.line_bottom
                )

            fun bind(
                task: TimelineEntry.Task,
                isFirst: Boolean,
                isLast: Boolean,
                onToggle: (TimelineEntry.Task) -> Unit
            ) {

                tvTimeLeft.text = task.time
                tvTaskTime.text = task.time
                tvTaskTitle.text = task.title

                if (task.subtitle.isNotEmpty()) {

                    tvTaskSubtitle.visibility =
                        View.VISIBLE

                    tvTaskSubtitle.text =
                        task.subtitle

                } else {

                    tvTaskSubtitle.visibility =
                        View.GONE
                }

                ivIcon.setImageResource(
                    task.iconRes
                )

                ivRepeat.visibility =
                    if (task.isRepeat) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }

                lineTop.visibility =
                    if (isFirst) {
                        View.INVISIBLE
                    } else {
                        View.VISIBLE
                    }

                lineBottom.visibility =
                    if (isLast) {
                        View.INVISIBLE
                    } else {
                        View.VISIBLE
                    }

                if (task.isCompleted) {

                    btnComplete.setImageResource(
                        R.drawable.ic_check_ring_filled
                    )

                    tvTaskTitle.paintFlags =
                        tvTaskTitle.paintFlags or
                                android.graphics.Paint
                                    .STRIKE_THRU_TEXT_FLAG

                    tvTaskTitle.alpha = 0.6f

                } else {

                    btnComplete.setImageResource(
                        R.drawable.ic_check_ring
                    )

                    tvTaskTitle.paintFlags =
                        tvTaskTitle.paintFlags and
                                android.graphics.Paint
                                    .STRIKE_THRU_TEXT_FLAG.inv()

                    tvTaskTitle.alpha = 1f
                }

                btnComplete.setOnClickListener {

                    task.isCompleted =
                        !task.isCompleted

                    onToggle(task)
                }
            }
        }

        // ======================================
        // INTERVAL VIEW HOLDER
        // ======================================

        class IntervalViewHolder(
            view: View
        ) : RecyclerView.ViewHolder(view) {

            private val tvRuler1: TextView =
                view.findViewById(
                    R.id.tv_ruler_time1
                )

            private val tvRuler2: TextView =
                view.findViewById(
                    R.id.tv_ruler_time2
                )

            private val tvRulerCurrent: TextView =
                view.findViewById(
                    R.id.tv_ruler_current_time
                )

            private val tvStatus: TextView =
                view.findViewById(
                    R.id.tv_interval_status
                )

            private val btnAddInline: View =
                view.findViewById(
                    R.id.btn_inline_add_task
                )

            fun bind(
                interval: TimelineEntry.Interval,
                onAddClicked: () -> Unit
            ) {

                tvRuler1.text =
                    interval.times.getOrNull(0)
                        ?: "12:00"

                tvRuler2.text =
                    interval.times.getOrNull(1)
                        ?: "16:00"

                tvRulerCurrent.text =
                    interval.currentTime

                tvStatus.text =
                    interval.remainingText

                btnAddInline.setOnClickListener {
                    onAddClicked()
                }
            }
        }
    }
}
