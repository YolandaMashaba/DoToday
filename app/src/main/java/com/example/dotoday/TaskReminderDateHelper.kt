package com.example.dotoday

import android.app.DatePickerDialog
import android.content.Context
import android.util.Log
import android.widget.Toast
import java.text.DateFormat
import java.util.Calendar
import java.util.UUID

object TaskReminderDateHelper {

    private const val TAG = "DoTodayScheduler"

    /**
     * Opens a calendar picker.
     * Returns the selected date through onDateSelected.
     */
    fun selectDate(
        context: Context,
        onDateSelected: (Calendar) -> Unit
    ) {

        val today = Calendar.getInstance()

        val dialog = DatePickerDialog(
            context,
            { _, year, month, day ->

                val selectedDate = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, day)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                onDateSelected(selectedDate)
            },
            today.get(Calendar.YEAR),
            today.get(Calendar.MONTH),
            today.get(Calendar.DAY_OF_MONTH)
        )

        dialog.datePicker.minDate = today.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        dialog.show()
    }

    /**
     * Combines a calendar date with a 24-hour time.
     */
    fun buildTimestamp(
        date: Calendar,
        hour: Int,
        minute: Int
    ): Long {

        return (date.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    /**
     * Schedule a reminder for an actual task.
     *
     * taskId should be a stable, unique task ID.
     * Replace the generated ID with the RoomDB task ID
     * once database integration is complete.
     */
    fun scheduleTask(
        context: Context,
        taskTitle: String,
        date: Calendar,
        hour: Int,
        minute: Int,
        taskId: Int = UUID.randomUUID().hashCode()
    ): Boolean {

        val timestamp = buildTimestamp(
            date,
            hour,
            minute
        )

        if (taskTitle.isBlank()) {
            return false
        }

        if (timestamp <= System.currentTimeMillis()) {

            Toast.makeText(
                context,
                "Please select a future date and time",
                Toast.LENGTH_LONG
            ).show()

            return false
        }

        val success = TaskReminderScheduler.scheduleReminder(
            context = context,
            taskId = taskId,
            taskTitle = taskTitle,
            triggerTimeMillis = timestamp
        )

        if (success) {

            val formattedDate = DateFormat
                .getDateTimeInstance(
                    DateFormat.MEDIUM,
                    DateFormat.SHORT,
                    context.resources.configuration.locales[0]
                )
                .format(timestamp)

            Log.d(
                TAG,
                "Reminder scheduled for $taskTitle at $formattedDate"
            )

        } else {

            Log.e(
                TAG,
                "Failed to schedule reminder for $taskTitle"
            )
        }

        return success
    }
}