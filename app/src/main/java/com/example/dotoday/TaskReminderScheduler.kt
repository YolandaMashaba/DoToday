package com.example.dotoday

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

object TaskReminderScheduler {

    private const val TAG = "DoTodayScheduler"

    fun scheduleReminder(
        context: Context,
        taskId: Int,
        taskTitle: String,
        triggerTimeMillis: Long
    ): Boolean {

        if (taskTitle.isBlank()) {
            Log.w(TAG, "Cannot schedule a task without a title")
            return false
        }

        if (triggerTimeMillis <= System.currentTimeMillis()) {
            Log.w(TAG, "Cannot schedule a reminder in the past")
            return false
        }

        val alarmManager = context.getSystemService(
            Context.ALARM_SERVICE
        ) as AlarmManager

        val intent = Intent(
            context,
            TaskReminderReceiver::class.java
        ).apply {

            putExtra(
                TaskReminderReceiver.EXTRA_TASK_ID,
                taskId
            )

            putExtra(
                TaskReminderReceiver.EXTRA_TASK_TITLE,
                taskTitle
            )
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        return try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )

            } else {

                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
            }

            Log.d(
                TAG,
                "Reminder scheduled for $taskTitle"
            )

            true

        } catch (e: Exception) {

            Log.e(TAG, "Failed to schedule reminder", e)

            false
        }
    }

    fun cancelReminder(
        context: Context,
        taskId: Int
    ) {

        val intent = Intent(
            context,
            TaskReminderReceiver::class.java
        )

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId,
            intent,
            PendingIntent.FLAG_NO_CREATE or
                    PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {

            val alarmManager = context.getSystemService(
                Context.ALARM_SERVICE
            ) as AlarmManager

            alarmManager.cancel(pendingIntent)

            pendingIntent.cancel()

            Log.d(
                TAG,
                "Reminder cancelled for task ID: $taskId"
            )
        }
    }
}
