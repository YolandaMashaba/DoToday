package com.example.dotoday

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
 * - Timeline view (scheduled daily tasks)
 * - Calendar header week picker
 * - Inbox view (synced with Todoist REST API)
 * - Notes section
 * - Settings section
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private val todoistRepository = TodoistRepository()
    private lateinit var inboxAdapter: InboxAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate() called - initializing Edge-to-Edge and view hierarchy")
        
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Handle System Bar Insets gracefully for Edge-to-Edge display without double-padding BottomNavigationView
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            Log.v(TAG, "WindowInsets received: top=${systemBars.top}, left=${systemBars.left}, right=${systemBars.right}")
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        val adapter = setupTimeline()
        setupCalendar()
        setupInbox()
        setupBottomNav()

        findViewById<FloatingActionButton>(R.id.fab_add_task).setOnClickListener {
            Log.d(TAG, "FAB clicked - opening add scheduled task dialog")
            showAddTaskDialog(adapter)
        }

        findViewById<View>(R.id.btn_new_inbox_task).setOnClickListener {
            Log.d(TAG, "New Inbox Task button clicked")
            showAddTodoistTaskDialog()
        }

        findViewById<View>(R.id.btn_new_inbox_task_bottom).setOnClickListener {
            Log.d(TAG, "New Inbox Task bottom button clicked")
            showAddTodoistTaskDialog()
        }

        findViewById<View>(R.id.btn_logout).setOnClickListener {
            Log.d(TAG, "Logout button clicked")
            Snackbar.make(it, "Logged out successfully!", Snackbar.LENGTH_SHORT).show()
        }
    }

    /**
     * Displays a dialog allowing the user to create and add a new scheduled task to the timeline.
     */
    private fun showAddTaskDialog(adapter: TimelineAdapter) {
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
            Log.v(TAG, "Updated task duration: $diff minutes")
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
                    Log.i(TAG, "Adding new timeline item: title='$title', time=$startTime-$endTime")
                    adapter.addItem(TimelineItem(startTime, endTime, title, description, R.drawable.ic_alarm))
                } else {
                    Log.w(TAG, "Task creation skipped - title was empty")
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                Log.d(TAG, "Add task dialog cancelled")
            }
            .show()

        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(R.color.app_secondary))
        dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(getColor(R.color.text_secondary_color))
    }

    /**
     * Populates the top horizontal scrollable week calendar view with days.
     */
    private fun setupCalendar() {
        Log.d(TAG, "setupCalendar() - inflating week days into calendar container")
        val container = findViewById<LinearLayout>(R.id.calendar_container)
        val days = listOf("Mon" to "17", "Tue" to "18", "Wed" to "19", "Thu" to "20", "Fri" to "21", "Sat" to "22", "Sun" to "23")
        
        val inflater = LayoutInflater.from(this)
        days.forEach { (name, number) ->
            val view = inflater.inflate(R.layout.item_calendar_day, container, false)
            view.findViewById<TextView>(R.id.tv_day_name).text = name
            val tvNumber = view.findViewById<TextView>(R.id.tv_day_number)
            tvNumber.text = number
            if (number == "20") {
                tvNumber.setBackgroundResource(R.drawable.shape_circle_small)
                tvNumber.backgroundTintList = getColorStateList(R.color.app_secondary)
                tvNumber.setTextColor(getColor(R.color.white))
            }
            container.addView(view)
        }
    }

    /**
     * Initializes the RecyclerView for the main daily timeline view with default sample tasks.
     */
    private fun setupTimeline(): TimelineAdapter {
        Log.d(TAG, "setupTimeline() - initializing timeline RecyclerView")
        val rv = findViewById<RecyclerView>(R.id.rv_timeline)
        rv.layoutManager = LinearLayoutManager(this)
        val adapter = TimelineAdapter(mutableListOf(
            TimelineItem("08:00", "08:00", getString(R.string.task_wake_up), "", R.drawable.ic_alarm),
            TimelineItem("18:36", "18:45", getString(R.string.task_start_structured), getString(R.string.desc_structured), R.drawable.ic_timeline),
            TimelineItem("18:50", "19:00", getString(R.string.task_first_task), getString(R.string.desc_first_task), R.drawable.ic_alarm),
            TimelineItem("19:05", "19:15", getString(R.string.task_inbox), getString(R.string.desc_inbox), R.drawable.ic_inbox),
            TimelineItem("19:20", "19:30", getString(R.string.task_own_it), getString(R.string.desc_own_it), R.drawable.ic_settings)
        ))
        rv.adapter = adapter
        return adapter
    }

    /**
     * Configures bottom navigation bar click behavior and section visibility.
     */
    private fun setupBottomNav() {
        Log.d(TAG, "setupBottomNav() - configuring navigation listener")
        val nav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        nav.selectedItemId = R.id.nav_timeline
        nav.setOnItemSelectedListener { item ->
            val rvTimeline = findViewById<RecyclerView>(R.id.rv_timeline)
            val layoutNotes = findViewById<View>(R.id.layout_notes)
            val layoutInbox = findViewById<View>(R.id.layout_inbox)
            val layoutSettings = findViewById<View>(R.id.layout_settings)
            
            Log.d(TAG, "Navigation item selected: ${item.title}")

            // Hide all sections first
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

    /**
     * Initializes the RecyclerView adapter for the Todoist Inbox section.
     */
    private fun setupInbox() {
        Log.d(TAG, "setupInbox() - configuring Inbox RecyclerView")
        val rv = findViewById<RecyclerView>(R.id.rv_inbox)
        rv.layoutManager = LinearLayoutManager(this)
        inboxAdapter = InboxAdapter(mutableListOf())
        rv.adapter = inboxAdapter
    }

    /**
     * Asynchronously fetches Todoist tasks using [lifecycleScope] and updates UI states.
     */
    private fun fetchTodoistTasks() {
        Log.d(TAG, "fetchTodoistTasks() called - initiating network request")
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
                Log.i(TAG, "fetchTodoistTasks() successful - retrieved ${tasks.size} task(s)")
                if (tasks.isEmpty()) {
                    emptyState.visibility = View.VISIBLE
                } else {
                    inboxAdapter.updateTasks(tasks)
                    rv.visibility = View.VISIBLE
                    btnBottom.visibility = View.VISIBLE
                }
            }.onFailure { error ->
                Log.e(TAG, "fetchTodoistTasks() failed: ${error.localizedMessage}", error)
                emptyState.visibility = View.VISIBLE
                Snackbar.make(rv, "Failed to fetch tasks: ${error.message}", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Displays a dialog tailored for creating a new unscheduled Todoist Inbox task.
     */
    private fun showAddTodoistTaskDialog() {
        Log.d(TAG, "showAddTodoistTaskDialog() displayed")
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null)
        val etTitle = dialogView.findViewById<TextInputEditText>(R.id.et_task_title)
        val etDescription = dialogView.findViewById<TextInputEditText>(R.id.et_task_description)
        
        // Hide time and duration layout for Todoist Inbox tasks
        dialogView.findViewById<View>(R.id.layout_time_duration)?.visibility = View.GONE
        dialogView.findViewById<TextView>(R.id.tv_dialog_title)?.text = "New Inbox Task"

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("New Inbox Task")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val title = etTitle.text.toString()
                val description = etDescription.text.toString()
                if (title.isNotEmpty()) {
                    Log.i(TAG, "Creating new Todoist task: title='$title'")
                    addNewTodoistTask(title, description)
                } else {
                    Log.w(TAG, "Todoist task creation skipped - title was empty")
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                Log.d(TAG, "New Inbox task dialog cancelled")
            }
            .show()

        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(R.color.app_secondary))
        dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(getColor(R.color.text_secondary_color))
    }

    /**
     * Sends a network request to create a new task in Todoist.
     */
    private fun addNewTodoistTask(title: String, description: String? = null) {
        Log.d(TAG, "addNewTodoistTask() - sending task creation request")
        lifecycleScope.launch {
            val result = todoistRepository.addTask(title, description)
            result.onSuccess { newTask ->
                Log.i(TAG, "addNewTodoistTask() succeeded - ID='${newTask.id}'")
                fetchTodoistTasks() // Refresh list
                Snackbar.make(findViewById(R.id.main), "Task added to Todoist", Snackbar.LENGTH_SHORT).show()
            }.onFailure { error ->
                Log.e(TAG, "addNewTodoistTask() failed: ${error.localizedMessage}", error)
                Snackbar.make(findViewById(R.id.main), "Failed to add task: ${error.message}", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    data class TimelineItem(val time: String, val endTime: String, val title: String, val subtitle: String, val iconRes: Int)

    /**
     * RecyclerView adapter for displaying list of tasks retrieved from Todoist Inbox.
     */
    class InboxAdapter(private val tasks: MutableList<TodoistTask>) : RecyclerView.Adapter<InboxAdapter.ViewHolder>() {

        fun updateTasks(newTasks: List<TodoistTask>) {
            Log.d("InboxAdapter", "updateTasks() - updating data set with ${newTasks.size} items")
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

    /**
     * RecyclerView adapter for displaying timeline task entries.
     */
    class TimelineAdapter(private val items: MutableList<TimelineItem>) : RecyclerView.Adapter<TimelineAdapter.ViewHolder>() {

        fun addItem(item: TimelineItem) {
            Log.d("TimelineAdapter", "addItem() - adding timeline item: '${item.title}'")
            items.add(item)
            notifyItemInserted(items.size - 1)
        }

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvTime: TextView = view.findViewById(R.id.tv_time)
            val tvTitle: TextView = view.findViewById(R.id.tv_title)
            val tvSubtitle: TextView = view.findViewById(R.id.tv_subtitle)
            val ivIcon: android.widget.ImageView = view.findViewById(R.id.iv_icon)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_timeline, parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvTime.text = "${item.time}\n${item.endTime}"
            holder.tvTitle.text = item.title
            holder.tvSubtitle.text = item.subtitle
            holder.tvSubtitle.visibility = if (item.subtitle.isEmpty()) View.GONE else View.VISIBLE
            holder.ivIcon.setImageResource(item.iconRes)
        }

        override fun getItemCount() = items.size
    }
}