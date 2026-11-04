package com.example.dotoday

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.util.Locale

/**
 * Settings screen matching the app's Material 3 design system (`layout_settings.xml`).
 * Configures user profile, app theme, default task time, completed task actions,
 * haptic feedback, and user logout.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    private lateinit var tvProfileName: TextView
    private lateinit var tvProfileEmail: TextView
    private lateinit var btnEditUsername: MaterialButton
    private lateinit var dropdownTheme: AutoCompleteTextView
    private lateinit var btnDefaultTime: MaterialButton
    private lateinit var dropdownBehavior: AutoCompleteTextView
    private lateinit var switchHaptic: SwitchMaterial
    private lateinit var btnLogout: MaterialButton

    companion object {
        const val PREFS_NAME = "DoTodaySettings"
        const val KEY_THEME = "key_theme"
        const val KEY_DEFAULT_DUE_TIME = "key_default_due_time"
        const val KEY_COMPLETED_BEHAVIOR = "key_completed_behavior"
        const val KEY_HAPTIC_FEEDBACK = "key_haptic_feedback"
        const val KEY_USERNAME = "key_username"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_settings)

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Bind views from layout_settings.xml
        tvProfileName = findViewById(R.id.tv_profile_name)
        tvProfileEmail = findViewById(R.id.tv_profile_email)
        btnEditUsername = findViewById(R.id.btn_edit_username)
        dropdownTheme = findViewById(R.id.dropdown_theme)
        btnDefaultTime = findViewById(R.id.btn_default_time)
        dropdownBehavior = findViewById(R.id.dropdown_completed_behavior)
        switchHaptic = findViewById(R.id.switch_haptic)
        btnLogout = findViewById(R.id.btn_logout)

        // 1. User Profile Setup
        val currentUsername = prefs.getString(KEY_USERNAME, "DoToday User") ?: "DoToday User"
        tvProfileName.text = currentUsername
        tvProfileEmail.text = "Active Account"

        btnEditUsername.setOnClickListener {
            showUpdateUsernameDialog()
        }

        // 2. App Theme Selection
        val themeOptions = arrayOf("System Default", "Light Mode", "Dark Mode")
        val themeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, themeOptions)
        dropdownTheme.setAdapter(themeAdapter)
        val currentTheme = prefs.getString(KEY_THEME, "System Default")
        dropdownTheme.setText(currentTheme, false)

        dropdownTheme.setOnItemClickListener { _, _, position, _ ->
            val selected = themeOptions[position]
            prefs.edit().putString(KEY_THEME, selected).apply()
            applyAppTheme(selected)
            Toast.makeText(this, "Theme updated", Toast.LENGTH_SHORT).show()
        }

        // 3. Default Due Time
        val savedTime = prefs.getString(KEY_DEFAULT_DUE_TIME, "09:00") ?: "09:00"
        btnDefaultTime.text = "Default Task Time: $savedTime"
        btnDefaultTime.setOnClickListener {
            val timeParts = savedTime.split(":")
            val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 9
            val minute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0

            val picker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(hour)
                .setMinute(minute)
                .setTitleText("Select Default Task Time")
                .build()

            picker.addOnPositiveButtonClickListener {
                val newTime = String.format(Locale.getDefault(), "%02d:%02d", picker.hour, picker.minute)
                prefs.edit().putString(KEY_DEFAULT_DUE_TIME, newTime).apply()
                btnDefaultTime.text = "Default Task Time: $newTime"
                Toast.makeText(this, "Default time saved", Toast.LENGTH_SHORT).show()
            }
            picker.show(supportFragmentManager, "DEFAULT_TIME_PICKER")
        }

        // 4. Completed Task Action
        val behaviorOptions = arrayOf("Keep in Place", "Move to Bottom", "Hide Immediately")
        val behaviorAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, behaviorOptions)
        dropdownBehavior.setAdapter(behaviorAdapter)
        val currentBehavior = prefs.getString(KEY_COMPLETED_BEHAVIOR, "Move to Bottom")
        dropdownBehavior.setText(currentBehavior, false)

        dropdownBehavior.setOnItemClickListener { _, _, position, _ ->
            val selected = behaviorOptions[position]
            prefs.edit().putString(KEY_COMPLETED_BEHAVIOR, selected).apply()
            Toast.makeText(this, "Preference saved", Toast.LENGTH_SHORT).show()
        }

        // 5. Haptic Feedback Toggle
        switchHaptic.isChecked = prefs.getBoolean(KEY_HAPTIC_FEEDBACK, true)
        switchHaptic.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_HAPTIC_FEEDBACK, isChecked).apply()
            if (isChecked) {
                triggerHapticFeedback(this)
            }
        }

        // 6. Logout Button
        btnLogout.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out of DoToday?")
                .setPositiveButton("Log Out") { _, _ ->
                    Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun showUpdateUsernameDialog() {
        val inputLayout = TextInputLayout(this).apply {
            hint = "New Username"
            setPadding(32, 16, 32, 16)
        }
        val editText = TextInputEditText(inputLayout.context).apply {
            setText(prefs.getString(KEY_USERNAME, "DoToday User"))
            maxLines = 1
        }
        inputLayout.addView(editText)

        MaterialAlertDialogBuilder(this)
            .setTitle("Update Username")
            .setView(inputLayout)
            .setPositiveButton("Save") { _, _ ->
                val newName = editText.text?.toString()?.trim() ?: ""
                if (newName.isNotEmpty()) {
                    prefs.edit().putString(KEY_USERNAME, newName).apply()
                    tvProfileName.text = newName
                    Toast.makeText(this, "Username updated", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Username cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun applyAppTheme(themeChoice: String) {
        val mode = when (themeChoice) {
            "Light Mode" -> AppCompatDelegate.MODE_NIGHT_NO
            "Dark Mode" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    private fun triggerHapticFeedback(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator.vibrate(
                VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
        }
    }
}
