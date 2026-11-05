package com.example.dotoday

import android.content.Context
import android.content.Intent
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

/** Settings screen backed by the controls in layout_settings.xml. */
class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var tvProfileName: TextView
    private lateinit var tvProfileEmail: TextView
    private lateinit var btnEditUsername: MaterialButton
    private lateinit var dropdownTheme: AutoCompleteTextView
    private lateinit var btnDefaultTime: MaterialButton
    private lateinit var dropdownBehavior: AutoCompleteTextView
    private lateinit var switchHaptic: SwitchMaterial
    private lateinit var btnLogout: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_settings)
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        tvProfileName = findViewById(R.id.tv_profile_name)
        tvProfileEmail = findViewById(R.id.tv_profile_email)
        btnEditUsername = findViewById(R.id.btn_edit_username)
        dropdownTheme = findViewById(R.id.dropdown_theme)
        btnDefaultTime = findViewById(R.id.btn_default_time)
        dropdownBehavior = findViewById(R.id.dropdown_completed_behavior)
        switchHaptic = findViewById(R.id.switch_haptic)
        btnLogout = findViewById(R.id.btn_logout)

        tvProfileName.text = prefs.getString(KEY_USERNAME, "DoToday User")
        tvProfileEmail.text = "Active Account"
        btnEditUsername.setOnClickListener { showUpdateUsernameDialog() }

        val themeOptions = arrayOf("System Default", "Light Mode", "Dark Mode")
        dropdownTheme.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, themeOptions))
        dropdownTheme.setText(prefs.getString(KEY_THEME, themeOptions[0]), false)
        dropdownTheme.setOnItemClickListener { _, _, position, _ ->
            val selected = themeOptions[position]
            prefs.edit().putString(KEY_THEME, selected).apply()
            applyAppTheme(selected)
        }

        updateDefaultTimeLabel()
        btnDefaultTime.setOnClickListener { showTimePicker() }

        val behaviorOptions = arrayOf("Keep in Place", "Move to Bottom", "Hide Immediately")
        dropdownBehavior.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, behaviorOptions))
        dropdownBehavior.setText(prefs.getString(KEY_COMPLETED_BEHAVIOR, behaviorOptions[1]), false)
        dropdownBehavior.setOnItemClickListener { _, _, position, _ ->
            prefs.edit().putString(KEY_COMPLETED_BEHAVIOR, behaviorOptions[position]).apply()
            Toast.makeText(this, "Preference saved", Toast.LENGTH_SHORT).show()
        }

        switchHaptic.isChecked = prefs.getBoolean(KEY_HAPTIC_FEEDBACK, true)
        switchHaptic.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(KEY_HAPTIC_FEEDBACK, checked).apply()
            if (checked) triggerHapticFeedback(this)
        }

        btnLogout.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out of DoToday?")
                .setPositiveButton("Log Out") { _, _ ->
                    startActivity(Intent(this, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun showUpdateUsernameDialog() {
        val inputLayout = TextInputLayout(this).apply { hint = "New Username" }
        val editText = TextInputEditText(inputLayout.context).apply {
            setText(prefs.getString(KEY_USERNAME, "DoToday User"))
            maxLines = 1
        }
        inputLayout.addView(editText)
        MaterialAlertDialogBuilder(this)
            .setTitle("Update Username")
            .setView(inputLayout)
            .setPositiveButton("Save") { _, _ ->
                val name = editText.text?.toString()?.trim().orEmpty()
                if (name.isNotEmpty()) {
                    prefs.edit().putString(KEY_USERNAME, name).apply()
                    tvProfileName.text = name
                } else {
                    Toast.makeText(this, "Username cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showTimePicker() {
        val time = prefs.getString(KEY_DEFAULT_DUE_TIME, "09:00") ?: "09:00"
        val parts = time.split(":")
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(parts.getOrNull(0)?.toIntOrNull() ?: 9)
            .setMinute(parts.getOrNull(1)?.toIntOrNull() ?: 0)
            .setTitleText("Select Default Task Time")
            .build()
        picker.addOnPositiveButtonClickListener {
            val newTime = String.format(Locale.ROOT, "%02d:%02d", picker.hour, picker.minute)
            prefs.edit().putString(KEY_DEFAULT_DUE_TIME, newTime).apply()
            updateDefaultTimeLabel()
        }
        picker.show(supportFragmentManager, "DEFAULT_TIME_PICKER")
    }

    private fun updateDefaultTimeLabel() {
        btnDefaultTime.text = "Default Task Time: ${prefs.getString(KEY_DEFAULT_DUE_TIME, "09:00") ?: "09:00"}"
    }

    private fun applyAppTheme(choice: String) {
        val mode = when (choice) {
            "Light Mode" -> AppCompatDelegate.MODE_NIGHT_NO
            "Dark Mode" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    private fun triggerHapticFeedback(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
        }
    }

    companion object {
        const val PREFS_NAME = "DoTodaySettings"
        const val KEY_THEME = "key_theme"
        const val KEY_DEFAULT_DUE_TIME = "key_default_due_time"
        const val KEY_COMPLETED_BEHAVIOR = "key_completed_behavior"
        const val KEY_HAPTIC_FEEDBACK = "key_haptic_feedback"
        const val KEY_USERNAME = "key_username"
    }
}
