package com.example.dotoday

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.util.Locale

/**
 * Settings screen for configuring user profile, app theme, default task times,
 * completed task behaviors, haptic feedback, and user session management (Logout).
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

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

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Root vertical layout
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#121212")) // Dark background to match app
        }

        // 1. Top Toolbar with Navigation Back Arrow
        val toolbar = MaterialToolbar(this).apply {
            title = "Settings"
            setTitleTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#1F1F1F"))
            navigationIcon = androidx.core.content.ContextCompat.getDrawable(
                this@SettingsActivity,
                android.R.drawable.ic_menu_revert
            )
            setNavigationOnClickListener { finish() }
        }
        rootLayout.addView(toolbar)

        // Scrollable content view for settings items
        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 48)
        }


        // FEATURE 1: User Profile & Username Management

        val profileLabel = createSectionLabel("User Profile")
        val profileCard = MaterialCardView(this).apply {
            setCardBackgroundColor(Color.parseColor("#1F1F1F"))
            radius = 16f
            strokeWidth = 0
            val cardPadding = 32
            setPadding(cardPadding, cardPadding, cardPadding, cardPadding)
        }

        val profileInnerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }

        val currentUsername = prefs.getString(KEY_USERNAME, "DoToday User") ?: "DoToday User"
        val tvUsername = TextView(this).apply {
            text = "Username: $currentUsername"
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(0, 0, 0, 16)
        }
        profileInnerLayout.addView(tvUsername)

        val btnEditUsername = MaterialButton(this).apply {
            text = "Update Username"
            setBackgroundColor(Color.parseColor("#2C2C2E"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                showUpdateUsernameDialog(tvUsername)
            }
        }
        profileInnerLayout.addView(btnEditUsername)

        profileCard.addView(profileInnerLayout)
        contentLayout.addView(profileLabel)
        contentLayout.addView(profileCard)

        addSpacer(contentLayout)


        // FEATURE 2: App Theme Selection (Light / Dark Mode)

        val themeLabel = createSectionLabel("App Theme")
        val themeInputLayout = createDropdownContainer("Select Theme")

        val themeOptions = arrayOf("System Default", "Light Mode", "Dark Mode")
        val themeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, themeOptions)

        val themeDropdown = AutoCompleteTextView(themeInputLayout.context).apply {
            inputType = android.text.InputType.TYPE_NULL
            setTextColor(Color.WHITE)
            setAdapter(themeAdapter)

            val currentTheme = prefs.getString(KEY_THEME, "System Default")
            setText(currentTheme, false)

            setOnClickListener { showDropDown() }
            setOnFocusChangeListener { _, hasFocus -> if (hasFocus) showDropDown() }

            setOnItemClickListener { _, _, position, _ ->
                val selected = themeOptions[position]
                prefs.edit().putString(KEY_THEME, selected).apply()
                applyAppTheme(selected)
            }
        }
        themeInputLayout.addView(themeDropdown)

        contentLayout.addView(themeLabel)
        contentLayout.addView(themeInputLayout)

        addSpacer(contentLayout)


        // FEATURE 3: Default Due Time

        val timeLabel = createSectionLabel("Default Task Time")
        val savedTime = prefs.getString(KEY_DEFAULT_DUE_TIME, "09:00") ?: "09:00"

        val timePickerButton = MaterialButton(this).apply {
            text = "Default Time: $savedTime"
            setBackgroundColor(Color.parseColor("#2C2C2E"))
            setTextColor(Color.WHITE)
            setOnClickListener {
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
                    text = "Default Time: $newTime"
                    Toast.makeText(context, "Default time saved", Toast.LENGTH_SHORT).show()
                }
                picker.show(supportFragmentManager, "DEFAULT_TIME_PICKER")
            }
        }

        contentLayout.addView(timeLabel)
        contentLayout.addView(timePickerButton)

        addSpacer(contentLayout)


        // FEATURE 4: Completed Task Action

        val behaviorLabel = createSectionLabel("Completed Task Action")
        val behaviorInputLayout = createDropdownContainer("Select Action")

        val behaviorOptions = arrayOf("Keep in Place", "Move to Bottom", "Hide Immediately")
        val behaviorAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, behaviorOptions)

        val behaviorDropdown = AutoCompleteTextView(behaviorInputLayout.context).apply {
            inputType = android.text.InputType.TYPE_NULL
            setTextColor(Color.WHITE)
            setAdapter(behaviorAdapter)

            val currentBehavior = prefs.getString(KEY_COMPLETED_BEHAVIOR, "Move to Bottom")
            setText(currentBehavior, false)

            setOnClickListener { showDropDown() }
            setOnFocusChangeListener { _, hasFocus -> if (hasFocus) showDropDown() }

            setOnItemClickListener { _, _, position, _ ->
                val selected = behaviorOptions[position]
                prefs.edit().putString(KEY_COMPLETED_BEHAVIOR, selected).apply()
                Toast.makeText(context, "Preference saved", Toast.LENGTH_SHORT).show()
            }
        }
        behaviorInputLayout.addView(behaviorDropdown)

        contentLayout.addView(behaviorLabel)
        contentLayout.addView(behaviorInputLayout)

        addSpacer(contentLayout)


        // FEATURE 5: Haptic Feedback Toggle

        val hapticCard = MaterialCardView(this).apply {
            setCardBackgroundColor(Color.parseColor("#1F1F1F"))
            radius = 16f
            strokeWidth = 0
            val cardPadding = 32
            setPadding(cardPadding, cardPadding, cardPadding, cardPadding)
        }

        val hapticSwitch = SwitchMaterial(this).apply {
            text = "Haptic Feedback on Completion"
            setTextColor(Color.WHITE)
            isChecked = prefs.getBoolean(KEY_HAPTIC_FEEDBACK, true)

            setOnCheckedChangeListener { _, isChecked ->
                prefs.edit().putBoolean(KEY_HAPTIC_FEEDBACK, isChecked).apply()
                if (isChecked) {
                    triggerHapticFeedback(context)
                }
            }
        }
        hapticCard.addView(hapticSwitch)
        contentLayout.addView(hapticCard)

        addSpacer(contentLayout)


        // FEATURE 6: Logout Button

        val logoutButton = MaterialButton(this).apply {
            text = "Log Out"
            setBackgroundColor(Color.parseColor("#CF6679")) // Error/Red tint
            setTextColor(Color.WHITE)
            setOnClickListener {
                MaterialAlertDialogBuilder(this@SettingsActivity)
                    .setTitle("Log Out")
                    .setMessage("Are you sure you want to log out of DoToday?")
                    .setPositiveButton("Log Out") { _, _ ->
                        Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@SettingsActivity, LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                        finish()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
        contentLayout.addView(logoutButton)


        scrollView.addView(contentLayout)
        rootLayout.addView(scrollView)
        setContentView(rootLayout)
    }

    private fun showUpdateUsernameDialog(tvUsername: TextView) {
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
                    tvUsername.text = "Username: $newName"
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

    private fun createSectionLabel(title: String): TextView {
        return TextView(this).apply {
            text = title
            textSize = 14f
            setTextColor(Color.parseColor("#A0A0A0"))
            setPadding(0, 0, 0, 12)
        }
    }

    private fun createDropdownContainer(hintText: String): TextInputLayout {
        return TextInputLayout(
            this,
            null,
            com.google.android.material.R.style.Widget_MaterialComponents_TextInputLayout_OutlinedBox_ExposedDropdownMenu
        ).apply {
            hint = hintText
            setHintTextColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#A0A0A0")))
            boxStrokeColor = Color.parseColor("#2F80ED")
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun addSpacer(container: LinearLayout) {
        val spacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                32
            )
        }
        container.addView(spacer)
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
