package com.example.dotoday

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import android.content.Intent
import android.util.Log

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val adapter = setupTimeline()
        setupCalendar()
        setupBottomNav()

        findViewById<FloatingActionButton>(R.id.fab_add_task).setOnClickListener {
            showAddTaskDialog(adapter)
        }

        findViewById<View>(R.id.btn_new_inbox_task).setOnClickListener {
            Snackbar.make(it, "New Inbox Task functionality coming soon!", Snackbar.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.btn_logout).setOnClickListener {
            Log.d("MainActivity", "User logged out, returning to LoginActivity")
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun showAddTaskDialog(adapter: TimelineAdapter) {
        val builder = android.app.AlertDialog.Builder(this)
        val input = android.widget.EditText(this)
        input.hint = "Task Title"
        builder.setTitle("Add New Task")
        builder.setView(input)
        builder.setPositiveButton("Add") { _, _ ->
            val title = input.text.toString()
            if (title.isNotEmpty()) {
                adapter.addItem(TimelineItem("Now", "Now", title, "New task", R.drawable.ic_alarm))
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }


    private fun setupCalendar() {
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

    private fun setupTimeline(): TimelineAdapter {
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

    private fun setupBottomNav() {
        val nav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        nav.selectedItemId = R.id.nav_timeline
        nav.setOnItemSelectedListener { item ->
            val rvTimeline = findViewById<RecyclerView>(R.id.rv_timeline)
            val layoutNotes = findViewById<View>(R.id.layout_notes)
            val layoutInbox = findViewById<View>(R.id.layout_inbox)
            val layoutSettings = findViewById<View>(R.id.layout_settings)
            
            // Hide all
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

    data class TimelineItem(val time: String, val endTime: String, val title: String, val subtitle: String, val iconRes: Int)

    class TimelineAdapter(private val items: MutableList<TimelineItem>) : RecyclerView.Adapter<TimelineAdapter.ViewHolder>() {

        fun addItem(item: TimelineItem) {
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
            holder.tvTime.text = item.time
            holder.tvTitle.text = item.title
            holder.tvSubtitle.text = item.subtitle
            holder.tvSubtitle.visibility = if (item.subtitle.isEmpty()) View.GONE else View.VISIBLE
            holder.ivIcon.setImageResource(item.iconRes)
        }

        override fun getItemCount() = items.size
    }
}