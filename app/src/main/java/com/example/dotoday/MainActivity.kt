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
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

/** Main DoToday dashboard for timeline, calendar and Todoist inbox. */
class MainActivity : AppCompatActivity() {
    companion object { private const val TAG = "MainActivity" }

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

    // TodoistRepository requires an Android Context; use the application context.
    private val todoistRepository by lazy { TodoistRepository(applicationContext) }
    private lateinit var inboxAdapter: InboxAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate() - Initializing MainActivity view hierarchy")
        setContentView(R.layout.activity_main)
        setupTimeline()
        setupCalendar()
        setupInbox()
        setupBottomNav()

        findViewById<View>(R.id.btn_calendar_picker)?.setOnClickListener { showCalendarPickerDialog() }
        findViewById<FloatingActionButton>(R.id.fab_add_task).setOnClickListener { showAddTaskDialog() }
        findViewById<View>(R.id.btn_new_inbox_task).setOnClickListener { showAddTodoistTaskDialog() }
        findViewById<View>(R.id.btn_new_inbox_task_bottom).setOnClickListener { showAddTodoistTaskDialog() }
        findViewById<View>(R.id.btn_logout).setOnClickListener {
            Snackbar.make(it, "Logged out successfully!", Snackbar.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
    }

    private fun getCurrentTimeFormatted(): String {
        val cal = Calendar.getInstance()
        return String.format(Locale.getDefault(), "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
    }

    private fun setupTimeline(): TimelineAdapter {
        val rv = findViewById<RecyclerView>(R.id.rv_timeline)
        rv.layoutManager = LinearLayoutManager(this)
        timelineAdapter = TimelineAdapter(
            items = (tasksByDay[selectedDayNumber] ?: mutableListOf()).toMutableList(),
            onAddTaskClicked = { showAddTaskDialog() },
            onTaskToggleCompleted = { timelineAdapter.notifyDataSetChanged() }
        )
        rv.adapter = timelineAdapter
        return timelineAdapter
    }

    private fun setupCalendar() {
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
            view.setOnClickListener { selectCalendarDay(dayInfo, animate = true) }
            container.addView(view)
        }
        daysList.find { it.number == selectedDayNumber }?.let { selectCalendarDay(it, animate = false) }
    }

    private fun updateDayDots(dayNumber: String, dayView: View) {
        val count = (tasksByDay[dayNumber] ?: emptyList()).count { it is TimelineEntry.Task }
        dayView.findViewById<View>(R.id.dot1)?.visibility = if (count >= 1) View.VISIBLE else View.GONE
        dayView.findViewById<View>(R.id.dot2)?.visibility = if (count >= 2) View.VISIBLE else View.GONE
    }

    private fun selectCalendarDay(dayInfo: DayInfo, animate: Boolean) {
        selectedDayNumber = dayInfo.number
        findViewById<TextView>(R.id.tv_month_year)?.text = dayInfo.fullName
        dayViewsMap.forEach { (number, pair) ->
            val (view, tvNumber) = pair
            if (number == dayInfo.number) {
                tvNumber.setBackgroundResource(R.drawable.shape_circle_small)
                tvNumber.backgroundTintList = getColorStateList(R.color.app_secondary)
                tvNumber.setTextColor(getColor(R.color.white))
                if (animate) view.animate().scaleX(1.15f).scaleY(1.15f).setDuration(120).withEndAction {
                    view.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                }.start()
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
        val value = TypedValue()
        theme.resolveAttribute(attr, value, true)
        return value.data
    }

    private fun showCalendarPickerDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_calendar_picker, null)
        val calendarView = dialogView.findViewById<CalendarView>(R.id.calendar_view_pop)
        val tvHeader = dialogView.findViewById<TextView>(R.id.tv_tasks_summary_header)
        val tvNoTasks = dialogView.findViewById<TextView>(R.id.tv_no_tasks_preview)
        val rvPreview = dialogView.findViewById<RecyclerView>(R.id.rv_dialog_tasks_preview)
        rvPreview.layoutManager = LinearLayoutManager(this)
        var selectedDay = selectedDayNumber
        var selectedName = daysList.find { it.number == selectedDay }?.fullName ?: "$selectedDay September 2026 >"

        fun updatePreview(day: String, name: String) {
            tvHeader.text = "Tasks for $name:"
            val tasks = (tasksByDay[day] ?: emptyList()).filterIsInstance<TimelineEntry.Task>()
            if (tasks.isEmpty()) {
                tvNoTasks.visibility = View.VISIBLE
                rvPreview.visibility = View.GONE
            } else {
                tvNoTasks.visibility = View.GONE
                rvPreview.visibility = View.VISIBLE
                rvPreview.adapter = DialogTaskPreviewAdapter(tasks)
            }
        }
        val cal = Calendar.getInstance().apply { set(2026, Calendar.SEPTEMBER, selectedDay.toIntOrNull() ?: 22) }
        calendarView.date = cal.timeInMillis
        updatePreview(selectedDay, selectedName)
        calendarView.setOnDateChangeListener { _, year, month, day ->
            selectedDay = day.toString()
            val months = arrayOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
            selectedName = "$day ${months.getOrElse(month) { "September" }} $year >"
            updatePreview(selectedDay, selectedName)
        }
        MaterialAlertDialogBuilder(this).setView(dialogView)
            .setPositiveButton("View Timeline") { _, _ ->
                val info = daysList.find { it.number == selectedDay } ?: DayInfo("Day", selectedName, selectedDay)
                selectCalendarDay(info, animate = true)
                Snackbar.make(findViewById(R.id.main), "Switched to $selectedName", Snackbar.LENGTH_SHORT).show()
            }.setNegativeButton("Close", null).show()
    }

    private fun showAddTaskDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null)
        val etTitle = dialogView.findViewById<TextInputEditText>(R.id.et_task_title)
        val etDescription = dialogView.findViewById<TextInputEditText>(R.id.et_task_description)
        val etDuration = dialogView.findViewById<TextInputEditText>(R.id.et_task_duration)
        val btnStart = dialogView.findViewById<MaterialButton>(R.id.btn_pick_time)
        val btnEnd = dialogView.findViewById<MaterialButton>(R.id.btn_pick_end_time)
        val switchRepeat = dialogView.findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.switch_repeat_task)
        val repeatDetails = dialogView.findViewById<View>(R.id.layout_repeat_details)
        val frequency = dialogView.findViewById<android.widget.AutoCompleteTextView>(R.id.spinner_repeat_frequency)
        val repeatDuration = dialogView.findViewById<TextInputEditText>(R.id.et_repeat_duration)
        frequency.setAdapter(android.widget.ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, arrayOf("Daily", "Weekly", "Monthly")))
        switchRepeat.setOnCheckedChangeListener { _, checked -> repeatDetails.visibility = if (checked) View.VISIBLE else View.GONE }
        var startHour = 8; var startMinute = 0; var endHour = 8; var endMinute = 30
        fun updateDuration() {
            val start = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, startHour); set(Calendar.MINUTE, startMinute) }
            val end = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, endHour); set(Calendar.MINUTE, endMinute) }
            if (end.before(start)) end.add(Calendar.DAY_OF_YEAR, 1)
            etDuration.setText(((end.timeInMillis - start.timeInMillis) / 60000).toString())
        }
        btnStart.setOnClickListener { android.app.TimePickerDialog(this, { _, h, m ->
            startHour = h; startMinute = m; btnStart.text = String.format(Locale.getDefault(), "%02d:%02d", h, m); updateDuration()
        }, startHour, startMinute, true).show() }
        btnEnd.setOnClickListener { android.app.TimePickerDialog(this, { _, h, m ->
            endHour = h; endMinute = m; btnEnd.text = String.format(Locale.getDefault(), "%02d:%02d", h, m); updateDuration()
        }, endHour, endMinute, true).show() }
        MaterialAlertDialogBuilder(this).setView(dialogView).setPositiveButton("Add") { _, _ ->
            val title = etTitle.text.toString()
            if (title.isNotEmpty()) {
                val startTime = String.format(Locale.getDefault(), "%02d:%02d", startHour, startMinute)
                val endTime = String.format(Locale.getDefault(), "%02d:%02d", endHour, endMinute)
                val isRepeat = switchRepeat.isChecked
                val count = if (isRepeat) (repeatDuration.text.toString().toIntOrNull() ?: 7) else 1
                val step = if (frequency.text.toString().equals("Weekly", true)) 7 else 1
                repeat(count) { index ->
                    val day = ((selectedDayNumber.toIntOrNull() ?: 22) + index * step).toString()
                    val entries = tasksByDay.getOrPut(day) { mutableListOf() }
                    val task = TimelineEntry.Task(time = startTime, endTime = endTime, title = title,
                        subtitle = etDescription.text.toString(), iconRes = R.drawable.ic_alarm, isRepeat = isRepeat)
                    val lastTask = entries.indexOfLast { it is TimelineEntry.Task }
                    entries.add(if (lastTask >= 0) lastTask + 1 else entries.size, task)
                    dayViewsMap[day]?.first?.let { updateDayDots(day, it) }
                }
                timelineAdapter.updateEntries(tasksByDay[selectedDayNumber] ?: mutableListOf())
                Snackbar.make(findViewById(R.id.main), "Task '$title' added", Snackbar.LENGTH_SHORT).show()
            }
        }.setNegativeButton("Cancel", null).show()
    }

    private fun setupBottomNav() {
        val nav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        nav.selectedItemId = R.id.nav_timeline
        nav.setOnItemSelectedListener { item ->
            val timeline = findViewById<RecyclerView>(R.id.rv_timeline)
            val notes = findViewById<View>(R.id.layout_notes)
            val inbox = findViewById<View>(R.id.layout_inbox)
            val settings = findViewById<View>(R.id.layout_settings)
            timeline.visibility = View.GONE; notes.visibility = View.GONE; inbox.visibility = View.GONE; settings.visibility = View.GONE
            when (item.itemId) {
                R.id.nav_timeline -> { timeline.visibility = View.VISIBLE; true }
                R.id.nav_notes -> { notes.visibility = View.VISIBLE; true }
                R.id.nav_inbox -> { inbox.visibility = View.VISIBLE; fetchTodoistTasks(); true }
                R.id.nav_settings -> { startActivity(Intent(this, SettingsActivity::class.java)); true }
                else -> { Snackbar.make(nav, "${item.title} section coming soon!", Snackbar.LENGTH_SHORT).show(); true }
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
        val progress = findViewById<ProgressBar>(R.id.pb_inbox_loading)
        val rv = findViewById<RecyclerView>(R.id.rv_inbox)
        val empty = findViewById<View>(R.id.layout_inbox_empty)
        val bottomButton = findViewById<View>(R.id.btn_new_inbox_task_bottom)
        progress.visibility = View.VISIBLE; rv.visibility = View.GONE; empty.visibility = View.GONE; bottomButton.visibility = View.GONE
        lifecycleScope.launch {
            val result = todoistRepository.getTasks()
            progress.visibility = View.GONE
            result.onSuccess { tasks ->
                if (tasks.isEmpty()) empty.visibility = View.VISIBLE else {
                    inboxAdapter.updateTasks(tasks); rv.visibility = View.VISIBLE; bottomButton.visibility = View.VISIBLE
                }
            }.onFailure { error ->
                empty.visibility = View.VISIBLE
                Snackbar.make(rv, "Failed to fetch tasks: ${error.message}", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun showAddTodoistTaskDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null)
        val title = dialogView.findViewById<TextInputEditText>(R.id.et_task_title)
        val description = dialogView.findViewById<TextInputEditText>(R.id.et_task_description)
        dialogView.findViewById<View>(R.id.layout_time_duration)?.visibility = View.GONE
        dialogView.findViewById<View>(R.id.layout_repeat_section)?.visibility = View.GONE
        dialogView.findViewById<TextView>(R.id.tv_dialog_title)?.text = "New Inbox Task"
        MaterialAlertDialogBuilder(this).setTitle("New Inbox Task").setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                if (title.text.toString().isNotEmpty()) addNewTodoistTask(title.text.toString(), description.text.toString())
            }.setNegativeButton("Cancel", null).show()
    }

    private fun addNewTodoistTask(title: String, description: String? = null) {
        lifecycleScope.launch {
            val result = todoistRepository.addTask(title, description)
            result.onSuccess { fetchTodoistTasks(); Snackbar.make(findViewById(R.id.main), "Task added to Todoist", Snackbar.LENGTH_SHORT).show() }
                .onFailure { error -> Snackbar.make(findViewById(R.id.main), "Failed to add task: ${error.message}", Snackbar.LENGTH_LONG).show() }
        }
    }

    class InboxAdapter(private val tasks: MutableList<TodoistTask>) : RecyclerView.Adapter<InboxAdapter.ViewHolder>() {
        fun updateTasks(newTasks: List<TodoistTask>) { tasks.clear(); tasks.addAll(newTasks); notifyDataSetChanged() }
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val content: TextView = view.findViewById(R.id.tv_task_content)
            val description: TextView = view.findViewById(R.id.tv_task_description)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_inbox_task, parent, false))
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
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_inbox_task, parent, false))
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
        fun updateEntries(newEntries: List<TimelineEntry>) { items.clear(); items.addAll(newEntries); notifyDataSetChanged() }
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
                is TimelineEntry.Task -> (holder as TaskViewHolder).bind(entry, position == 0, position == items.size - 1, onTaskToggleCompleted)
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
            fun bind(task: TimelineEntry.Task, first: Boolean, last: Boolean, onToggle: (TimelineEntry.Task) -> Unit) {
                timeLeft.text = task.time; taskTime.text = task.time; title.text = task.title
                subtitle.text = task.subtitle; subtitle.visibility = if (task.subtitle.isNotEmpty()) View.VISIBLE else View.GONE
                icon.setImageResource(task.iconRes); repeat.visibility = if (task.isRepeat) View.VISIBLE else View.GONE
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
                complete.setOnClickListener { task.isCompleted = !task.isCompleted; onToggle(task) }
            }
        }

        class IntervalViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val ruler1: TextView = view.findViewById(R.id.tv_ruler_time1)
            private val ruler2: TextView = view.findViewById(R.id.tv_ruler_time2)
            private val current: TextView = view.findViewById(R.id.tv_ruler_current_time)
            private val status: TextView = view.findViewById(R.id.tv_interval_status)
            private val addButton: View = view.findViewById(R.id.btn_inline_add_task)
            fun bind(interval: TimelineEntry.Interval, onAdd: () -> Unit) {
                ruler1.text = interval.times.getOrNull(0) ?: "12:00"
                ruler2.text = interval.times.getOrNull(1) ?: "16:00"
                current.text = interval.currentTime; status.text = interval.remainingText
                addButton.setOnClickListener { onAdd() }
            }
        }
    }
}
