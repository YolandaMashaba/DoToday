package com.example.dotoday

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class TaskReminderReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "DoTodayReminder"

        const val CHANNEL_ID = "dotoday_task_reminders"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"

        fun createNotificationChannel(context: Context) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "DoToday Task Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications for upcoming tasks"
                    enableVibration(true)
                }

                val manager = context.getSystemService(
                    Context.NOTIFICATION_SERVICE
                ) as NotificationManager

                manager.createNotificationChannel(channel)
            }
        }
    }

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        val taskId = intent.getIntExtra(
            EXTRA_TASK_ID,
            0
        )

        val taskTitle = intent.getStringExtra(
            EXTRA_TASK_TITLE
        ) ?: return

        createNotificationChannel(context)

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Notification permission not granted")
            return
        }

        val openAppIntent = Intent(
            context,
            MainActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            context,
            CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle("DoToday Task Reminder")
            .setContentText(taskTitle)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(taskTitle)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {

            NotificationManagerCompat.from(context).notify(
                taskId,
                notification
            )

            Log.d(
                TAG,
                "Reminder displayed for task: $taskTitle"
            )

        } catch (e: SecurityException) {

            Log.e(TAG, "Notification permission error", e)
        }
    }
}
