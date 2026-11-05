package com.example.dotoday

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
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.TimePicker
import android.app.TimePickerDialog
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
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.materialswitch.MaterialSwitch
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    data class DayInfo(
        val name: String,
        val fullName: String,
        val number: String
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
            val isRepeat: Boolean = true
        ) : TimelineEntry()

        data class Interval(
            val times: List<String> = listOf("12:00", "16:00"),
            val currentTime: String = "19:23",
            val remainingText: String =
                "2h 37m left — then, let's dive in!"
        ) : TimelineEntry()
    }

    // Existing sample calendar dates
    private val daysList: List<DayInfo>
        get() {
            val locale = resources.configuration.locales[0]

            return (20..26).map { day ->
                val cal = Calendar.getInstance().apply {
                    set(2026, Calendar.SEPTEMBER, day)
                }

                val shortName = SimpleDateFormat(
                    "EEE",
                    locale
                ).format(cal.time)

                val fullName = SimpleDateFormat(
                    "d MMMM yyyy",
                    locale
                ).format(cal.time) + " >"

                DayInfo(
                    name = shortName,
                    fullName = fullName,
                    number = day.toString()
                )
            }
        }

    private var selectedDayNumber = "22"

    private lateinit var timelineAdapter: TimelineAdapter
    private lateinit var inboxAdapter: InboxAdapter

    private val dayViewsMap =
        mutableMapOf<String, Pair<View, TextView>>()

    // Original in-memory timeline tasks
    private val tasksByDay =
        mutableMapOf<String, MutableList<TimelineEntry>>(

            "20" to mutableListOf(
                TimelineEntry.Task(
                    "1", "07:30", "08:00",
                    "Morning Meditation", "15 mins focus",
                    R.drawable.ic_alarm
                ),
                TimelineEntry.Interval(
                    listOf("11:00", "15:00"),
                    getCurrentTimeFormatted(),
                    "3h 15m left — time to focus!"
                ),
                TimelineEntry.Task(
                    "2", "21:30", "22:00",
                    "Read Book", "20 pages",
                    R.drawable.ic_moon
                )
            ),

            "21" to mutableListOf(
                TimelineEntry.Task(
                    "1", "08:00", "09:00",
                    "Morning Jog", "Park run",
                    R.drawable.ic_alarm
                ),
                TimelineEntry.Interval(
                    listOf("12:00", "16:00"),
                    getCurrentTimeFormatted(),
                    "1h 45m left — stay energized!"
                ),
                TimelineEntry.Task(
                    "2", "22:00", "22:30",
                    "Wind Down", "Sleep preparation",
                    R.drawable.ic_moon
                )
            ),

            "22" to mutableListOf(
                TimelineEntry.Task(
                    "1", "08:00", "08:30",
                    "Rise and Shine", "",
                    R.drawable.ic_alarm,
                    isRepeat = true
                ),
                TimelineEntry.Interval(
                    listOf("12:00", "16:00"),
                    "19:23",
                    "2h 37m left — then, let's dive in!"
                ),
                TimelineEntry.Task(
                    "2", "22:00", "22:30",
                    "Wind Down", "",
                    R.drawable.ic_moon,
                    isRepeat = true
                )
            ),

            "23" to mutableListOf(
                TimelineEntry.Task(
                    "1", "09:00", "10:00",
                    "Team Standup", "Project sync",
                    R.drawable.ic_timeline
                ),
                TimelineEntry.Interval(
                    listOf("13:00", "17:00"),
                    getCurrentTimeFormatted(),
                    "4h 00m left — ready for next task"
                ),
                TimelineEntry.Task(
                    "2", "21:45", "22:15",
                    "Journal & Plan", "",
                    R.drawable.ic_settings
                )
            ),

            "24" to mutableListOf(
                TimelineEntry.Task(
                    "1", "08:15", "08:45",
                    "Morning Stretch", "Flexibility routine",
                    R.drawable.ic_alarm
                ),
                TimelineEntry.Interval(
                    listOf("12:30", "16:30"),
                    getCurrentTimeFormatted(),
                    "2h 10m left — deep work time"
                ),
                TimelineEntry.Task(
                    "2", "22:00", "22:30",
                    "Wind Down", "",
                    R.drawable.ic_moon
                )
            ),

            "25" to mutableListOf(
                TimelineEntry.Task(
                    "1", "08:00", "09:00",
                    "Workout & Breakfast", "",
                    R.drawable.ic_alarm
                ),
                TimelineEntry.Interval(
                    listOf("13:00", "17:00"),
                    getCurrentTimeFormatted(),
                    "3h 30m left — finish the week strong!"
                ),
                TimelineEntry.Task(
                    "2", "22:30", "23:00",
                    "Night Relaxation", "",
                    R.drawable.ic_moon
                )
            ),

            "26" to mutableListOf(
                TimelineEntry.Task(
                    "1", "09:30", "10:30",
                    "Weekend Breakfast", "Family time",
                    R.drawable.ic_alarm
                ),
                TimelineEntry.Interval(
                    listOf("14:00", "18:00"),
                    getCurrentTimeFormatted(),
                    "5h 00m left — enjoy weekend!"
                ),
                TimelineEntry.Task(
                    "2", "23:00", "23:30",
                    "Late Rest", "",
                    R.drawable.ic_moon
                )
            )
        )

    private val todoistRepository by lazy { TodoistRepository(applicationContext) }
    private lateinit var inboxAdapter: InboxAdapter
    private val todoistRepository = TodoistRepository()

    // ==========================================
    // APPLICATION STARTUP
    // ==========================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        setupTimeline()
        setupCalendar()
        setupInbox()
        setupBottomNav()

        findViewById<View>(
            R.id.btn_calendar_picker
        )?.setOnClickListener {
            showCalendarPickerDialog()
        }

        findViewById<FloatingActionButton>(
            R.id.fab_add_task
        ).setOnClickListener {
            showAddTaskDialog()
        }

        findViewById<View>(
            R.id.btn_new_inbox_task
        ).setOnClickListener {
            showAddTodoistTaskDialog()
        }

        findViewById<View>(
            R.id.btn_new_inbox_task_bottom
        ).setOnClickListener {
            showAddTodoistTaskDialog()
        }

        findViewById<View>(
            R.id.btn_logout
        ).setOnClickListener {

            Snackbar.make(
                it,
                getString(R.string.general_logout_success),
                Snackbar.LENGTH_SHORT
            ).show()

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
    }

    // ==========================================
    // TIME FORMAT
    // ==========================================

    private fun getCurrentTimeFormatted(): String {
        val cal = Calendar.getInstance()

        return String.format(
            Locale.ROOT,
            "%02d:%02d",
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE)
        )
    }

    // ==========================================
    // TIMELINE SETUP
    // ==========================================

    private fun setupTimeline(): TimelineAdapter {

        val rv = findViewById<RecyclerView>(
            R.id.rv_timeline
        )

        rv.layoutManager = LinearLayoutManager(this)

        val initialEntries =
            tasksByDay[selectedDayNumber] ?: mutableListOf()

        timelineAdapter = TimelineAdapter(
            items = initialEntries.toMutableList(),

            onAddTaskClicked = {
                showAddTaskDialog()
            },

            onTaskToggleCompleted = { task ->
                Log.i(
                    TAG,
                    "Task completion updated: ${task.isCompleted}"
                )

                timelineAdapter.notifyDataSetChanged()
            }
        )

        rv.adapter = timelineAdapter

        return timelineAdapter
    }

    // ==========================================
    // CALENDAR SETUP
    // ==========================================

    private fun setupCalendar() {

        val container = findViewById<LinearLayout>(
            R.id.calendar_container
        )

        container.removeAllViews()
        dayViewsMap.clear()

        val inflater = LayoutInflater.from(this)

        daysList.forEach { dayInfo ->

            val view = inflater.inflate(
                R.layout.item_calendar_day,
                container,
                false
            )

            view.findViewById<TextView>(
                R.id.tv_day_name
            ).text = dayInfo.name

            val tvNumber = view.findViewById<TextView>(
                R.id.tv_day_number
            )

            tvNumber.text = dayInfo.number

            dayViewsMap[dayInfo.number] =
                Pair(view, tvNumber)

            updateDayDots(dayInfo.number, view)

            view.setOnClickListener {
                selectCalendarDay(
                    dayInfo,
                    animate = true
                )
            }

            container.addView(view)
        }

        daysList.find {
            it.number == selectedDayNumber
        }?.let {
            selectCalendarDay(
                it,
                animate = false
            )
        }
    }

    // ==========================================
    // CALENDAR TASK INDICATORS
    // ==========================================

    private fun updateDayDots(
        dayNumber: String,
        dayView: View
    ) {

        val entries =
            tasksByDay[dayNumber] ?: emptyList()

        val taskCount = entries.count {
            it is TimelineEntry.Task
        }

        val dot1 = dayView.findViewById<View>(
            R.id.dot1
        )

        val dot2 = dayView.findViewById<View>(
            R.id.dot2
        )

        when {

            taskCount >= 2 -> {
                dot1.visibility = View.VISIBLE
                dot2.visibility = View.VISIBLE
            }

            taskCount == 1 -> {
                dot1.visibility = View.VISIBLE
                dot2.visibility = View.GONE
            }

            else -> {
                dot1.visibility = View.GONE
                dot2.visibility = View.GONE
            }
        }
    }

    // ==========================================
    // CALENDAR DAY SELECTION
    // ==========================================

    private fun selectCalendarDay(
        dayInfo: DayInfo,
        animate: Boolean
    ) {

        selectedDayNumber = dayInfo.number

        findViewById<TextView>(
            R.id.tv_month_year
        )?.text = dayInfo.fullName

        dayViewsMap.forEach { (number, pair) ->

            val (view, tvNumber) = pair

            if (number == dayInfo.number) {

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

                    view.animate()
                        .scaleX(1.15f)
                        .scaleY(1.15f)
                        .setDuration(120)
                        .withEndAction {
                            view.animate()
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
                        com.google.android.material.R.attr.colorOnSurface
                    )
                )
            }
        }

        val entries =
            tasksByDay[dayInfo.number] ?: mutableListOf()

        timelineAdapter.updateEntries(entries)

        if (
            entries.none { it is TimelineEntry.Task } &&
            animate
        ) {

            Snackbar.make(
                findViewById(R.id.main),
                getString(
                    R.string.calendar_empty_day,
                    dayInfo.fullName
                ),
                Snackbar.LENGTH_SHORT
            ).show()
        }
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

    // ==========================================
    // FULL CALENDAR DIALOG
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

        var dialogSelectedDayNumber =
            selectedDayNumber

        var dialogSelectedFullName =
            daysList.find {
                it.number == selectedDayNumber
            }?.fullName ?: selectedDayNumber

        fun updateDialogTaskPreview(
            dayNumber: String,
            fullName: String
        ) {

            tvHeader.text = getString(
                R.string.calendar_tasks_for,
                fullName
            )

            val entries =
                tasksByDay[dayNumber] ?: emptyList()

            val tasksOnly =
                entries.filterIsInstance<TimelineEntry.Task>()

            if (tasksOnly.isEmpty()) {

                tvNoTasks.visibility = View.VISIBLE
                rvPreview.visibility = View.GONE

            } else {

                tvNoTasks.visibility = View.GONE
                rvPreview.visibility = View.VISIBLE

                rvPreview.adapter =
                    DialogTaskPreviewAdapter(tasksOnly)
            }
        }

        try {

            val cal = Calendar.getInstance().apply {
                set(
                    2026,
                    Calendar.SEPTEMBER,
                    dialogSelectedDayNumber.toIntOrNull() ?: 22
                )
            }

            calendarView.date = cal.timeInMillis

        } catch (e: Exception) {

            Log.w(
                TAG,
                "Calendar initialization failed",
                e
            )
        }

        updateDialogTaskPreview(
            dialogSelectedDayNumber,
            dialogSelectedFullName
        )

        calendarView.setOnDateChangeListener {
                _, year, month, dayOfMonth ->

            dialogSelectedDayNumber =
                dayOfMonth.toString()

            val cal = Calendar.getInstance().apply {
                set(year, month, dayOfMonth)
            }

            val locale =
                resources.configuration.locales[0]

            dialogSelectedFullName =
                SimpleDateFormat(
                    "d MMMM yyyy",
                    locale
                ).format(cal.time) + " >"

            updateDialogTaskPreview(
                dialogSelectedDayNumber,
                dialogSelectedFullName
            )
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setPositiveButton(
                R.string.calendar_view_timeline
            ) { _, _ ->

                val dayInfo =
                    daysList.find {
                        it.number == dialogSelectedDayNumber
                    } ?: DayInfo(
                        "",
                        dialogSelectedFullName,
                        dialogSelectedDayNumber
                    )

                selectCalendarDay(
                    dayInfo,
                    animate = true
                )

                Snackbar.make(
                    findViewById(R.id.main),
                    getString(
                        R.string.calendar_switched,
                        dialogSelectedFullName
                    ),
                    Snackbar.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton(
                R.string.calendar_close,
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
    // ADD SCHEDULED TASK DIALOG
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

        // Translated frequency options
        val frequencies = arrayOf(
            getString(R.string.task_daily),
            getString(R.string.task_weekly),
            getString(R.string.task_monthly)
        )

        var selectedFrequencyIndex = 0

        val frequencyAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            frequencies
        )

        spinnerFrequency.setAdapter(frequencyAdapter)

        spinnerFrequency.setText(
            frequencies[0],
            false
        )

        spinnerFrequency.setOnClickListener {
            spinnerFrequency.showDropDown()
        }

        spinnerFrequency.setOnItemClickListener {
                _, _, position, _ ->

            selectedFrequencyIndex = position
        }

        switchRepeat.setOnCheckedChangeListener {
                _, isChecked ->

            layoutRepeatDetails.visibility =
                if (isChecked) View.VISIBLE else View.GONE
        }

        var selectedHour = 8
        var selectedMinute = 0

        var endHour = 8
        var endMinute = 30

        fun updateDuration() {

            val startCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, selectedHour)
                set(Calendar.MINUTE, selectedMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val endCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, endHour)
                set(Calendar.MINUTE, endMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (endCal.before(startCal)) {
                endCal.add(Calendar.DAY_OF_YEAR, 1)
            }

            val difference =
                (endCal.timeInMillis - startCal.timeInMillis) /
                        (60 * 1000)

            etDuration.setText(difference.toString())
        }

        btnPickTime.setOnClickListener {

            TimePickerDialog(
                this,
                { _: TimePicker, hour: Int, minute: Int ->

                    selectedHour = hour
                    selectedMinute = minute

                    btnPickTime.text = String.format(
                        Locale.ROOT,
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
                { _: TimePicker, hour: Int, minute: Int ->

                    endHour = hour
                    endMinute = minute

                    btnPickEndTime.text = String.format(
                        Locale.ROOT,
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

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setPositiveButton(
                R.string.task_add
            ) { _, _ ->

                val title =
                    etTitle.text?.toString()?.trim().orEmpty()

                val description =
                    etDescription.text?.toString().orEmpty()

                if (title.isBlank()) {

                    Snackbar.make(
                        findViewById(R.id.main),
                        getString(R.string.task_required),
                        Snackbar.LENGTH_SHORT
                    ).show()

                } else {

                    val startTime = String.format(
                        Locale.ROOT,
                        "%02d:%02d",
                        selectedHour,
                        selectedMinute
                    )

                    val endTime = String.format(
                        Locale.ROOT,
                        "%02d:%02d",
                        endHour,
                        endMinute
                    )

                    val isRepeat = switchRepeat.isChecked

                    val startDay =
                        selectedDayNumber.toIntOrNull() ?: 22

                    val repeatCount =
                        if (isRepeat) {
                            (
                                    etRepeatDuration.text
                                        ?.toString()
                                        ?.toIntOrNull() ?: 7
                                    ).coerceIn(1, 365)
                        } else {
                            1
                        }

                    val step =
                        if (selectedFrequencyIndex == 1) 7
                        else 1

                    val targetDays = mutableListOf<String>()

                    for (i in 0 until repeatCount) {

                        val dayNumber =
                            startDay + (i * step)

                        targetDays.add(
                            dayNumber.toString()
                        )
                    }

                    targetDays.forEach { dayNumber ->

                        val newTask = TimelineEntry.Task(
                            time = startTime,
                            endTime = endTime,
                            title = title,
                            subtitle = description,
                            iconRes = R.drawable.ic_alarm,
                            isRepeat = isRepeat
                        )

                        val currentEntries =
                            tasksByDay.getOrPut(dayNumber) {
                                mutableListOf()
                            }

                        val lastTaskIndex =
                            currentEntries.indexOfLast {
                                it is TimelineEntry.Task
                            }

                        val insertIndex =
                            if (lastTaskIndex >= 0) {
                                lastTaskIndex + 1
                            } else {
                                currentEntries.size
                            }

                        currentEntries.add(
                            insertIndex,
                            newTask
                        )

                        dayViewsMap[dayNumber]
                            ?.first
                            ?.let { dayView ->
                                updateDayDots(
                                    dayNumber,
                                    dayView
                                )
                            }
                    }

                    val currentEntries =
                        tasksByDay[selectedDayNumber]
                            ?: mutableListOf()

                    timelineAdapter.updateEntries(
                        currentEntries
                    )

                    // Translated completion message
                    val snackbarMessage =
                        if (isRepeat && repeatCount > 1) {

                            val unit =
                                if (selectedFrequencyIndex == 1) {
                                    getString(R.string.task_weeks)
                                } else {
                                    getString(R.string.task_days)
                                }

                            getString(
                                R.string.task_repeated,
                                title,
                                repeatCount,
                                unit
                            )

                        } else {

                            getString(
                                R.string.task_added_day,
                                title,
                                selectedDayNumber
                            )
                        }

                    Snackbar.make(
                        findViewById(R.id.main),
                        snackbarMessage,
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton(
                R.string.task_cancel,
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
    // BOTTOM NAVIGATION
    // ==========================================

    private fun setupBottomNav() {

        val nav = findViewById<BottomNavigationView>(
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

                    Snackbar.make(
                        nav,
                        getString(
                            R.string.general_section_soon,
                            item.title.toString()
                        ),
                        Snackbar.LENGTH_SHORT
                    ).show()

                    true
                }
            }
        }
    }

    // ==========================================
    // INBOX INITIALIZATION
    // ==========================================

    private fun setupInbox() {

        val rv = findViewById<RecyclerView>(
            R.id.rv_inbox
        )

        rv.layoutManager = LinearLayoutManager(this)

        inboxAdapter = InboxAdapter(
            mutableListOf()
        )

        rv.adapter = inboxAdapter
    }

    // ==========================================
    // FETCH TODOIST TASKS
    // ==========================================

    private fun fetchTodoistTasks() {

        val loading = findViewById<ProgressBar>(
            R.id.pb_inbox_loading
        )

        val rv = findViewById<RecyclerView>(
            R.id.rv_inbox
        )

        val emptyState = findViewById<View>(
            R.id.layout_inbox_empty
        )

        val bottomButton = findViewById<View>(
            R.id.btn_new_inbox_task_bottom
        )

        loading.visibility = View.VISIBLE
        rv.visibility = View.GONE
        emptyState.visibility = View.GONE
        bottomButton.visibility = View.GONE

        lifecycleScope.launch {

            val result = todoistRepository.getTasks()

            loading.visibility = View.GONE

            result.onSuccess { tasks ->

                if (tasks.isEmpty()) {

                    emptyState.visibility = View.VISIBLE

                } else {

                    inboxAdapter.updateTasks(tasks)

                    rv.visibility = View.VISIBLE
                    bottomButton.visibility = View.VISIBLE
                }

            }.onFailure { error ->

                Log.e(
                    TAG,
                    "Failed to fetch Todoist tasks",
                    error
                )

                emptyState.visibility = View.VISIBLE

                Snackbar.make(
                    rv,
                    getString(
                        R.string.inbox_fetch_error,
                        error.message ?: ""
                    ),
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    // ==========================================
    // ADD TODOIST TASK DIALOG
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
        )?.text = getString(
            R.string.inbox_dialog_title
        )

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(
                R.string.inbox_dialog_title
            )
            .setView(dialogView)
            .setPositiveButton(
                R.string.task_add
            ) { _, _ ->

                val title =
                    etTitle.text?.toString()?.trim().orEmpty()

                val description =
                    etDescription.text?.toString().orEmpty()

                if (title.isNotBlank()) {

                    addNewTodoistTask(
                        title,
                        description
                    )

                } else {

                    Snackbar.make(
                        findViewById(R.id.main),
                        getString(R.string.task_required),
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton(
                R.string.task_cancel,
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
    // CREATE TODOIST TASK
    // ==========================================

    private fun addNewTodoistTask(
        title: String,
        description: String? = null
    ) {

        lifecycleScope.launch {

            val result = todoistRepository.addTask(
                title,
                description
            )

            result.onSuccess {

                fetchTodoistTasks()

                Snackbar.make(
                    findViewById(R.id.main),
                    getString(R.string.inbox_add_success),
                    Snackbar.LENGTH_SHORT
                ).show()

            }.onFailure { error ->

                Log.e(
                    TAG,
                    "Failed to add Todoist task",
                    error
                )

                Snackbar.make(
                    findViewById(R.id.main),
                    getString(
                        R.string.inbox_add_error,
                        error.message ?: ""
                    ),
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    // ==========================================
    // INBOX ADAPTER
    // ==========================================

    class InboxAdapter(
        private val tasks: MutableList<TodoistTask>
    ) : RecyclerView.Adapter<InboxAdapter.ViewHolder>() {

        fun updateTasks(newTasks: List<TodoistTask>) {

            tasks.clear()
            tasks.addAll(newTasks)
            notifyDataSetChanged()
        }

        class ViewHolder(view: View) :
            RecyclerView.ViewHolder(view) {

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
    // CALENDAR TASK PREVIEW ADAPTER
    // ==========================================

    class DialogTaskPreviewAdapter(
        private val tasks: List<TimelineEntry.Task>
    ) : RecyclerView.Adapter<
            DialogTaskPreviewAdapter.ViewHolder>() {

        class ViewHolder(view: View) :
            RecyclerView.ViewHolder(view) {

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

            holder.tvDescription.text = task.subtitle

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

                is TimelineEntry.Task ->
                    TYPE_TASK

                is TimelineEntry.Interval ->
                    TYPE_INTERVAL
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
                        entry,
                        position == 0,
                        position == items.size - 1,
                        onTaskToggleCompleted
                    )
                }

                is TimelineEntry.Interval -> {

                    (holder as IntervalViewHolder).bind(
                        entry,
                        onAddTaskClicked
                    )
                }
            }
        }

        override fun getItemCount(): Int =
            items.size

        // ==========================================
        // TASK VIEW HOLDER
        // ==========================================

        class TaskViewHolder(view: View) :
            RecyclerView.ViewHolder(view) {

            private val tvTimeLeft: TextView =
                view.findViewById(R.id.tv_time_left)

            private val tvTaskTime: TextView =
                view.findViewById(R.id.tv_task_time)

            private val tvTaskTitle: TextView =
                view.findViewById(R.id.tv_task_title)

            private val tvTaskSubtitle: TextView =
                view.findViewById(R.id.tv_task_subtitle)

            private val ivIcon: android.widget.ImageView =
                view.findViewById(R.id.iv_icon)

            private val ivRepeat: android.widget.ImageView =
                view.findViewById(R.id.iv_repeat)

            private val btnComplete: android.widget.ImageView =
                view.findViewById(R.id.btn_complete)

            private val lineTop: View =
                view.findViewById(R.id.line_top)

            private val lineBottom: View =
                view.findViewById(R.id.line_bottom)

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

                    tvTaskTitle.alpha = 1.0f
                }

                btnComplete.setOnClickListener {

                    task.isCompleted =
                        !task.isCompleted

                    onToggle(task)
                }
            }
        }

        // ==========================================
        // INTERVAL VIEW HOLDER
        // ==========================================

        class IntervalViewHolder(view: View) :
            RecyclerView.ViewHolder(view) {

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