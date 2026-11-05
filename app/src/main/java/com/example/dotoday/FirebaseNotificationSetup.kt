package com.example.dotoday

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging

/**
 * Firebase Cloud Messaging notification setup.
 *
 * Responsible for:
 * 1. Creating a notification channel.
 * 2. Requesting notification permission.
 * 3. Retrieving the Firebase FCM token.
 *
 * DoToday Android Application.
 */
object FirebaseNotificationSetup {

    private const val TAG = "DoTodayFCM"

    private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001

    const val CHANNEL_ID = "dotoday_notifications"

    /**
     * Initialize notifications.
     *
     * Call this function from MainActivity.onCreate().
     */
    fun initialize(activity: AppCompatActivity) {

        Log.d(TAG, "Initializing Firebase notifications")

        createNotificationChannel(activity)

        requestNotificationPermission(activity)

        retrieveFcmToken()
    }

    /**
     * Create the Android notification channel.
     *
     * Required for Android 8.0 and newer.
     */
    private fun createNotificationChannel(
        context: Context
    ) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                "DoToday Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {

                description =
                    "Task reminders, assignments and daily summaries"

                enableVibration(true)
                enableLights(true)
            }

            val notificationManager =
                context.getSystemService(
                    Context.NOTIFICATION_SERVICE
                ) as NotificationManager

            notificationManager.createNotificationChannel(
                channel
            )

            Log.d(
                TAG,
                "Notification channel created successfully"
            )
        }
    }

    /**
     * Request notification permission.
     *
     * Android 13+ requires POST_NOTIFICATIONS.
     * Older Android versions don't require this
     * runtime permission.
     */
    private fun requestNotificationPermission(
        activity: AppCompatActivity
    ) {

        if (Build.VERSION.SDK_INT >= 33) {

            val permissionStatus =
                ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS
                )

            if (
                permissionStatus ==
                PackageManager.PERMISSION_GRANTED
            ) {

                Log.d(
                    TAG,
                    "Notification permission already granted"
                )

            } else {

                Log.d(
                    TAG,
                    "Requesting notification permission"
                )

                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(
                        Manifest.permission.POST_NOTIFICATIONS
                    ),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }

        } else {

            Log.d(
                TAG,
                "Notification runtime permission not required"
            )
        }
    }

    /**
     * Retrieve Firebase registration token.
     *
     * The token identifies this app installation
     * when sending Firebase test messages.
     *
     * Do not commit registration tokens to GitHub.
     */
    private fun retrieveFcmToken() {

        FirebaseMessaging.getInstance()
            .token
            .addOnCompleteListener { task ->

                if (!task.isSuccessful) {

                    Log.e(
                        TAG,
                        "Failed to retrieve FCM token",
                        task.exception
                    )

                    return@addOnCompleteListener
                }

                val token = task.result

                if (token.isNullOrBlank()) {

                    Log.e(
                        TAG,
                        "Firebase returned an empty token"
                    )

                    return@addOnCompleteListener
                }

                Log.d(
                    TAG,
                    "FCM token retrieved successfully"
                )

                // Development testing only.
                // Remove token logging before release.
                Log.d(
                    TAG,
                    "FCM_TOKEN: $token"
                )
            }
    }
}