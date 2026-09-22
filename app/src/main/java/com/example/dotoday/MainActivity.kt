package com.example.dotoday

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CalendarView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

/**
 * Main activity of the DoToday app.
 *
 * Manages the primary navigation sections:
 * - Timeline view (scheduled daily tasks with expandable calendar date selection, calendar pop-up dialog, and interval features)
 * - Calendar header week picker & calendar edit pop-up
 * - Inbox view (synced with Todoist REST API)
 * - Notes section
 * - Settings section
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    data class DayInfo(val name: String, val fullName: String, val number: String)

    sealed class TimelineEntry {
        data class Task(
            val id: String = java.util.UUID.randomUUID().toString(),
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

    private val daysList = listOf(
        DayInfo("Sun", "20 September 2026 >", "20"),
        DayInfo("Mon", "21 September 2026 >", "21"),
        DayInfo("Tue", "22 September 2026 >", "22"),
        DayInfo("Wed", "23 September 2026 >", "23"),
        DayInfo("Thu", "24 September 2026 >", "24"),
        DayInfo("Fri", "25 September 2026 >", "25"),
        DayInfo("Sat", "26 September 2026 >", "26")
    )

    private var selectedDayNumber = "22"
    private lateinit var timelineAdapter: TimelineAdapter
    private val dayViewsMap = mutableMapOf<String, Pair<View, TextView>>()

    private val tasksByDay = mutableMapOf<String, MutableList<TimelineEntry>>(
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

    private val todoistRepository = TodoistRepository()
    private lateinit var inboxAdapter: InboxAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate() called - initializing Edge-to-Edge and view hierarchy")
        
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            if (v.paddingLeft != systemBars.left || v.paddingTop != systemBars.top || v.paddingRight != systemBars.right) {
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            }
            insets
        }

        setupTimeline()
        setupCalendar()
        setupInbox()
        setupBottomNav()

        findViewById<View>(R.id.btn_calendar_picker)?.setOnClickListener {
            Log.d(TAG, "Calendar icon clicked - opening calendar dialog overview")
            showCalendarPickerDialog()
        }

        findViewById<FloatingActionButton>(R.id.fab_add_task).setOnClickListener {
            Log.d(TAG, "FAB clicked - opening add scheduled task dialog")
            showAddTaskDialog()
        }

        findViewById<View>(R.id.btn_new_inbox_task).setOnClickListener {
            showAddTodoistTaskDialog()
        }

        findViewById<View>(R.id.btn_new_inbox_task_bottom).setOnClickListener {
            showAddTodoistTaskDialog()
        }

        findViewById<View>(R.id.btn_logout).setOnClickListener {
            Snackbar.make(it, "Logged out successfully!", Snackbar.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun getCurrentTimeFormatted(): String {
        val cal = Calendar.getInstance()
        return String.format(Locale.getDefault(), "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
    }

    private fun setupTimeline(): TimelineAdapter {
        Log.d(TAG, "setupTimeline() - initializing timeline RecyclerView")
        val rv = findViewById<RecyclerView>(R.id.rv_timeline)
        rv.layoutManager = LinearLayoutManager(this)
        
        val initialEntries = tasksByDay[selectedDayNumber] ?: mutableListOf()
        timelineAdapter = TimelineAdapter(
            items = initialEntries.toMutableList(),
            onAddTaskClicked = { showAddTaskDialog() },
            onTaskToggleCompleted = { task ->
                Log.d(TAG, "Task '${task.title}' toggled completed: ${task.isCompleted}")
                timelineAdapter.notifyDataSetChanged()
            }
        )
        rv.adapter = timelineAdapter
        return timelineAdapter
    }

    private fun setupCalendar() {
        Log.d(TAG, "setupCalendar() - inflating week days into calendar container")
        val container = findViewById<LinearLayout>(R.id.calendar_container)
        container.removeAllViews()
        dayViewsMap.clear()

        val inflater = LayoutInflater.from(this)
        daysList.forEach { dayInfo ->
            val view = inflater.inflate(R.layout.item_calendar_day, container, false)
            view.findViewById<TextView>(R.id.tv_day_name).text = dayInfo.name
            val tvNumber = view.findViewById<TextView>(R.id.tv_day_number)
            tvNumber.text = dayInfo.number

            dayViewsMap[dayInfo.number] = Pair(view, tvNumber)

            updateDayDots(dayInfo.number, view)

            view.setOnClickListener {
                selectCalendarDay(dayInfo, animate = true)
            }

            container.addView(view)
        }

        daysList.find { it.number == selectedDayNumber }?.let { defaultDay ->
            selectCalendarDay(defaultDay, animate = false)
        }
    }

    private fun updateDayDots(dayNumber: String, dayView: View) {
        val entries = tasksByDay[dayNumber] ?: emptyList()
        val tasksCount = entries.count { it is TimelineEntry.Task }
        val dot1 = dayView.findViewById<View>(R.id.dot1)
        val dot2 = dayView.findViewById<View>(R.id.dot2)

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

    private fun selectCalendarDay(dayInfo: DayInfo, animate: Boolean) {
        selectedDayNumber = dayInfo.number
        Log.d(TAG, "selectCalendarDay() - selected day: ${dayInfo.fullName}")

        findViewById<TextView>(R.id.tv_month_year)?.text = dayInfo.fullName

        dayViewsMap.forEach { (number, pair) ->
            val (view, tvNumber) = pair
            if (number == dayInfo.number) {
                tvNumber.setBackgroundResource(R.drawable.shape_circle_small)
                tvNumber.backgroundTintList = getColorStateList(R.color.app_secondary)
                tvNumber.setTextColor(getColor(R.color.white))

                if (animate) {
                    view.animate()
                        .scaleX(1.15f)
                        .scaleY(1.15f)
                        .setDuration(120)
                        .withEndAction {
                            view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start()
                        }
                        .start()
                }
            } else {
                tvNumber.background = null
                tvNumber.setTextColor(getThemeColor(com.google.android.material.R.attr.colorOnSurface))
            }
        }

        val entries = tasksByDay[dayInfo.number] ?: mutableListOf()
        timelineAdapter.updateEntries(entries)

        if (entries.none { it is TimelineEntry.Task } && animate) {
            Snackbar.make(findViewById(R.id.main), "No tasks for ${dayInfo.fullName}. Tap + to add!", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun getThemeColor(attr: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }

    /**
     * Displays a calendar pop-up dialog showing a full calendar and tasks preview for any selected date.
     */
    private fun showCalendarPickerDialog() {
        Log.d(TAG, "showCalendarPickerDialog() displayed")
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_calendar_picker, null)
        val calendarView = dialogView.findViewById<CalendarView>(R.id.calendar_view_pop)
        val tvHeader = dialogView.findViewById<TextView>(R.id.tv_tasks_summary_header)
        val tvNoTasks = dialogView.findViewById<TextView>(R.id.tv_no_tasks_preview)
        val rvPreview = dialogView.findViewById<RecyclerView>(R.id.rv_dialog_tasks_preview)

        rvPreview.layoutManager = LinearLayoutManager(this)

        var dialogSelectedDayNumber = selectedDayNumber
        var dialogSelectedFullName = daysList.find { it.number == selectedDayNumber }?.fullName ?: "$selectedDayNumber September 2026 >"

        fun updateDialogTaskPreview(dayNumber: String, fullName: String) {
            tvHeader.text = "Tasks for $fullName:"
            val entries = tasksByDay[dayNumber] ?: emptyList()
            val tasksOnly = entries.filterIsInstance<TimelineEntry.Task>()

            if (tasksOnly.isEmpty()) {
                tvNoTasks.visibility = View.VISIBLE
                rvPreview.visibility = View.GONE
            } else {
                tvNoTasks.visibility = View.GONE
                rvPreview.visibility = View.VISIBLE
                rvPreview.adapter = DialogTaskPreviewAdapter(tasksOnly)
            }
        }

        try {
            val cal = Calendar.getInstance()
            cal.set(2026, Calendar.SEPTEMBER, dialogSelectedDayNumber.toIntOrNull() ?: 22)
            calendarView.date = cal.timeInMillis
        } catch (e: Exception) {
            Log.w(TAG, "Error setting calendar date: ${e.message}")
        }

        updateDialogTaskPreview(dialogSelectedDayNumber, dialogSelectedFullName)

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            dialogSelectedDayNumber = dayOfMonth.toString()
            val monthNames = arrayOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
            val monthName = monthNames.getOrElse(month) { "September" }
            dialogSelectedFullName = "$dayOfMonth $monthName $year >"
            updateDialogTaskPreview(dialogSelectedDayNumber, dialogSelectedFullName)
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setPositiveButton("View Timeline") { _, _ ->
                val dayInfo = daysList.find { it.number == dialogSelectedDayNumber }
                    ?: DayInfo("Day", dialogSelectedFullName, dialogSelectedDayNumber)

                selectCalendarDay(dayInfo, animate = true)
                Snackbar.make(findViewById(R.id.main), "Switched to $dialogSelectedFullName", Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton("Close", null)
            .show()

        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(getColor(R.color.app_secondary))
        dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(getColor(R.color.text_secondary_color))
    }

    private fun showAddTaskDialog() {
        Log.d(TAG, "showAddTaskDialog() displayed")
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null)
        val etTitle = dialogView.findViewById<TextInputEditText>(R.id.et_task_title)
        val etDescription = dialogView.findViewById<TextInputEditText>(R.id.et_task_description)
        val etDuration = dialogView.findViewById<TextInputEditText>(R.id.et_task_duration)
        val btnPickTime = dialogView.findViewById<MaterialButton>(R.id.btn_pick_time)
        val btnPickEndTime = dialogView.findViewById<MaterialButton>(R.id.btn_pick_end_time)

        var selectedHour = 8
        var selectedMinute = 0
        var endHour = 8
        var endMinute = 30

        fun updateDuration() {
            val startCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, selectedHour)
                set(Calendar.MINUTE, selectedMinute)
            }
            val endCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, endHour)
                set(Calendar.MINUTE, endMinute)
            }
            if (endCal.before(startCal)) endCal.add(Calendar.DAY_OF_YEAR, 1)
            val diff = (endCal.timeInMillis - startCal.timeInMillis) / (60 * 1000)
            etDuration.setText(diff.toString())
        }

        btnPickTime.setOnClickListener {
            android.app.TimePickerDialog(this, { _, hourOfDay, minute ->
                selectedHour = hourOfDay
                selectedMinute = minute
                btnPickTime.text = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
                updateDuration()
            }, selectedHour, selectedMinute, true).show()
        }

        btnPickEndTime.setOnClickListener {
            android.app.TimePickerDialog(this, { _, hourOfDay, minute ->
                endHour = hourOfDay
                endMinute = minute
                btnPickEndTime.text = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
                updateDuration()
            }, endHour, endMinute, true).show()
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val title = etTitle.text.toString()
                val description = etDescription.text.toString()
                
                if (title.isNotEmpty()) {
                    val startTime = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute)
                    val endTime = String.format(Locale.getDefault(), "%02d:%02d", endHour, endMinute)
                    val newTask = TimelineEntry.Task(
                        time = startTime,
                        endTime = endTime,
                        title = title,
                        subtitle = description,
                        iconRes = R.drawable.ic_alarm,
                        isRepeat = true
                    )
                    
                    val currentEntries = tasksByDay.getOrPut(selectedDayNumber) { mutableListOf() }
                    timelineAdapter.addTask(newTask, currentEntries)

                    dayViewsMap[selectedDayNumber]?.first?.let { dayView ->
                        updateDayDots(selectedDayNumber, dayView)
                    }

                    Snackbar.make(findViewById(R.id.main), "Task added to $selectedDayNumber", Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()

        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(getColor(R.color.app_secondary))
        dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(getColor(R.color.text_secondary_color))
    }

    private fun setupBottomNav() {
        val nav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        nav.selectedItemId = R.id.nav_timeline
        nav.setOnItemSelectedListener { item ->
            val rvTimeline = findViewById<RecyclerView>(R.id.rv_timeline)
            val layoutNotes = findViewById<View>(R.id.layout_notes)
            val layoutInbox = findViewById<View>(R.id.layout_inbox)
            val layoutSettings = findViewById<View>(R.id.layout_settings)

            rvTimeline.visibility = View.GONE
            layoutNotes.visibility = View.GONE
            layoutInbox.visibility = View.GONE
            layoutSettings.visibility = View.GONE

            when(item.itemId) {
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
                    layoutSettings.visibility = View.VISIBLE
                    true
                }
                else -> {
                    Snackbar.make(nav, "${item.title} section coming soon!", Snackbar.LENGTH_SHORT).show()
                    true
                }
            }
        }
    }

    private fun setupInbox() {
        val rv = findViewById<RecyclerView>(R.id.rv_inbox)
        rv.layoutManager = LinearLayoutManager(this)
        inboxAdapter = InboxAdapter(mutableListOf())
        rv.adapter = inboxAdapter
    }

    private fun fetchTodoistTasks() {
        val pb = findViewById<ProgressBar>(R.id.pb_inbox_loading)
        val rv = findViewById<RecyclerView>(R.id.rv_inbox)
        val emptyState = findViewById<View>(R.id.layout_inbox_empty)
        val btnBottom = findViewById<View>(R.id.btn_new_inbox_task_bottom)

        pb.visibility = View.VISIBLE
        rv.visibility = View.GONE
        emptyState.visibility = View.GONE
        btnBottom.visibility = View.GONE

        lifecycleScope.launch {
            val result = todoistRepository.getTasks()
            pb.visibility = View.GONE
            
            result.onSuccess { tasks ->
                if (tasks.isEmpty()) {
                    emptyState.visibility = View.VISIBLE
                } else {
                    inboxAdapter.updateTasks(tasks)
                    rv.visibility = View.VISIBLE
                    btnBottom.visibility = View.VISIBLE
                }
            }.onFailure { error ->
                emptyState.visibility = View.VISIBLE
                Snackbar.make(rv, "Failed to fetch tasks: ${error.message}", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun showAddTodoistTaskDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null)
        val etTitle = dialogView.findViewById<TextInputEditText>(R.id.et_task_title)
        val etDescription = dialogView.findViewById<TextInputEditText>(R.id.et_task_description)
        
        dialogView.findViewById<View>(R.id.layout_time_duration)?.visibility = View.GONE
        dialogView.findViewById<TextView>(R.id.tv_dialog_title)?.text = "New Inbox Task"

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("New Inbox Task")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val title = etTitle.text.toString()
                val description = etDescription.text.toString()
                if (title.isNotEmpty()) {
                    addNewTodoistTask(title, description)
                }
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
                Snackbar.make(findViewById(R.id.main), "Failed to add task: ${error.message}", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    class InboxAdapter(private val tasks: MutableList<TodoistTask>) : RecyclerView.Adapter<InboxAdapter.ViewHolder>() {

        fun updateTasks(newTasks: List<TodoistTask>) {
            tasks.clear()
            tasks.addAll(newTasks)
            notifyDataSetChanged()
        }

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvContent: TextView = view.findViewById(R.id.tv_task_content)
            val tvDescription: TextView = view.findViewById(R.id.tv_task_description)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_inbox_task, parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val task = tasks[position]
            holder.tvContent.text = task.content
            holder.tvDescription.text = task.description
            holder.tvDescription.visibility = if (task.description.isNullOrEmpty()) View.GONE else View.VISIBLE
        }

        override fun getItemCount() = tasks.size
    }

    class DialogTaskPreviewAdapter(private val tasks: List<TimelineEntry.Task>) : RecyclerView.Adapter<DialogTaskPreviewAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvContent: TextView = view.findViewById(R.id.tv_task_content)
            val tvDescription: TextView = view.findViewById(R.id.tv_task_description)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_inbox_task, parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val task = tasks[position]
            holder.tvContent.text = "${task.time} - ${task.title}"
            holder.tvDescription.text = task.subtitle
            holder.tvDescription.visibility = if (task.subtitle.isEmpty()) View.GONE else View.VISIBLE
        }

        override fun getItemCount() = tasks.size
    }

    class TimelineAdapter(
        private val items: MutableList<TimelineEntry>,
        private val onAddTaskClicked: () -> Unit,
        private val onTaskToggleCompleted: (TimelineEntry.Task) -> Unit
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        companion object {
            private const val TYPE_TASK = 0
            private const val TYPE_INTERVAL = 1
        }

        fun updateEntries(newEntries: List<TimelineEntry>) {
            items.clear()
            items.addAll(newEntries)
            notifyDataSetChanged()
        }

        fun addTask(task: TimelineEntry.Task, fullList: MutableList<TimelineEntry>) {
            val lastIndex = fullList.indexOfLast { it is TimelineEntry.Task }
            val insertIndex = if (lastIndex >= 0) lastIndex + 1 else fullList.size
            fullList.add(insertIndex, task)
            items.clear()
            items.addAll(fullList)
            notifyDataSetChanged()
        }

        override fun getItemViewType(position: Int): Int {
            return when (items[position]) {
                is TimelineEntry.Task -> TYPE_TASK
                is TimelineEntry.Interval -> TYPE_INTERVAL
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return if (viewType == TYPE_TASK) {
                TaskViewHolder(inflater.inflate(R.layout.item_timeline_task, parent, false))
            } else {
                IntervalViewHolder(inflater.inflate(R.layout.item_timeline_interval, parent, false))
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (val entry = items[position]) {
                is TimelineEntry.Task -> (holder as TaskViewHolder).bind(entry, position == 0, position == items.size - 1, onTaskToggleCompleted)
                is TimelineEntry.Interval -> (holder as IntervalViewHolder).bind(entry, onAddTaskClicked)
            }
        }

        override fun getItemCount() = items.size

        class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val tvTimeLeft: TextView = view.findViewById(R.id.tv_time_left)
            private val tvTaskTime: TextView = view.findViewById(R.id.tv_task_time)
            private val tvTaskTitle: TextView = view.findViewById(R.id.tv_task_title)
            private val tvTaskSubtitle: TextView = view.findViewById(R.id.tv_task_subtitle)
            private val ivIcon: android.widget.ImageView = view.findViewById(R.id.iv_icon)
            private val ivRepeat: android.widget.ImageView = view.findViewById(R.id.iv_repeat)
            private val btnComplete: android.widget.ImageView = view.findViewById(R.id.btn_complete)
            private val lineTop: View = view.findViewById(R.id.line_top)
            private val lineBottom: View = view.findViewById(R.id.line_bottom)

            fun bind(task: TimelineEntry.Task, isFirst: Boolean, isLast: Boolean, onToggle: (TimelineEntry.Task) -> Unit) {
                tvTimeLeft.text = task.time
                tvTaskTime.text = task.time
                tvTaskTitle.text = task.title

                if (task.subtitle.isNotEmpty()) {
                    tvTaskSubtitle.visibility = View.VISIBLE
                    tvTaskSubtitle.text = task.subtitle
                } else {
                    tvTaskSubtitle.visibility = View.GONE
                }

                ivIcon.setImageResource(task.iconRes)
                ivRepeat.visibility = if (task.isRepeat) View.VISIBLE else View.GONE

                lineTop.visibility = if (isFirst) View.INVISIBLE else View.VISIBLE
                lineBottom.visibility = if (isLast) View.INVISIBLE else View.VISIBLE

                if (task.isCompleted) {
                    btnComplete.setImageResource(R.drawable.ic_check_ring_filled)
                    tvTaskTitle.paintFlags = tvTaskTitle.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                    tvTaskTitle.alpha = 0.6f
                } else {
                    btnComplete.setImageResource(R.drawable.ic_check_ring)
                    tvTaskTitle.paintFlags = tvTaskTitle.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    tvTaskTitle.alpha = 1.0f
                }

                btnComplete.setOnClickListener {
                    task.isCompleted = !task.isCompleted
                    onToggle(task)
                }
            }
        }

        class IntervalViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val tvRuler1: TextView = view.findViewById(R.id.tv_ruler_time1)
            private val tvRuler2: TextView = view.findViewById(R.id.tv_ruler_time2)
            private val tvRulerCurrent: TextView = view.findViewById(R.id.tv_ruler_current_time)
            private val tvStatus: TextView = view.findViewById(R.id.tv_interval_status)
            private val btnAddInline: View = view.findViewById(R.id.btn_inline_add_task)

            fun bind(interval: TimelineEntry.Interval, onAddClicked: () -> Unit) {
                tvRuler1.text = interval.times.getOrNull(0) ?: "12:00"
                tvRuler2.text = interval.times.getOrNull(1) ?: "16:00"
                tvRulerCurrent.text = interval.currentTime
                tvStatus.text = interval.remainingText

                btnAddInline.setOnClickListener { onAddClicked() }
            }
        }
    }
}