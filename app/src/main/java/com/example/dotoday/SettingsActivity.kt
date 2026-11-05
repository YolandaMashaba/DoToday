package com.example.dotoday

import android.content.Context
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
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.util.Locale

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    companion object {
        const val PREFS_NAME = "DoTodaySettings"
        const val KEY_THEME = "key_theme"
        const val KEY_DEFAULT_DUE_TIME = "key_default_due_time"
        const val KEY_COMPLETED_BEHAVIOR = "key_completed_behavior"
        const val KEY_HAPTIC_FEEDBACK = "key_haptic_feedback"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        // ROOT LAYOUT
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )

            setBackgroundColor(
                Color.parseColor("#121212")
            )
        }

        // TOP TOOLBAR
        val toolbar = MaterialToolbar(this).apply {
            title = getString(R.string.nav_settings)

            setTitleTextColor(Color.WHITE)

            setBackgroundColor(
                Color.parseColor("#1F1F1F")
            )

            navigationIcon = ContextCompat.getDrawable(
                this@SettingsActivity,
                android.R.drawable.ic_menu_revert
            )

            setNavigationOnClickListener {
                finish()
            }
        }

        rootLayout.addView(toolbar)

        // SCROLLABLE SETTINGS CONTENT
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

        // ==========================================
        // FEATURE 1: APP THEME SELECTION
        // ==========================================

        val themeLabel = createSectionLabel(
            getString(R.string.settings_theme)
        )

        val themeInputLayout = createDropdownContainer(
            "Select Theme"
        )

        val themeOptions = arrayOf(
            "System Default",
            "Light Mode",
            "Dark Mode"
        )

        val themeAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            themeOptions
        )

        val themeDropdown = AutoCompleteTextView(
            themeInputLayout.context
        ).apply {

            inputType = android.text.InputType.TYPE_NULL
            setTextColor(Color.WHITE)
            setAdapter(themeAdapter)

            val currentTheme = prefs.getString(
                KEY_THEME,
                "System Default"
            )

            setText(currentTheme, false)

            setOnClickListener {
                showDropDown()
            }

            setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    showDropDown()
                }
            }

            setOnItemClickListener { _, _, position, _ ->

                val selected = themeOptions[position]

                prefs.edit()
                    .putString(KEY_THEME, selected)
                    .apply()

                applyAppTheme(selected)
            }
        }

        themeInputLayout.addView(themeDropdown)

        contentLayout.addView(themeLabel)
        contentLayout.addView(themeInputLayout)

        addSpacer(contentLayout)

        // ==========================================
        // FEATURE 2: MULTI-LANGUAGE SUPPORT
        // ==========================================

        val languageLabel = createSectionLabel(
            getString(R.string.settings_language)
        )

        val languageInputLayout = createDropdownContainer(
            getString(R.string.settings_select_language)
        )

        // Languages available in DoToday
        val languageNames = arrayOf(
            getString(R.string.settings_follow_system),
            "English",
            "isiZulu",
            "Afrikaans"
        )

        // Language codes recognized by Android
        val languageCodes = arrayOf(
            "",
            "en",
            "zu",
            "af"
        )

        // Read the currently selected application language
        val currentLanguage =
            AppCompatDelegate
                .getApplicationLocales()[0]
                ?.language ?: ""

        // Determine the selected language
        val selectedLanguageIndex =
            languageCodes.indexOf(currentLanguage)
                .let { index ->
                    if (index >= 0) index else 0
                }

        val languageAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            languageNames
        )

        val languageDropdown = AutoCompleteTextView(
            languageInputLayout.context
        ).apply {

            inputType = android.text.InputType.TYPE_NULL

            setTextColor(Color.WHITE)

            setAdapter(languageAdapter)

            // Display currently selected language
            setText(
                languageNames[selectedLanguageIndex],
                false
            )

            setOnClickListener {
                showDropDown()
            }

            setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    showDropDown()
                }
            }

            setOnItemClickListener { _, _, position, _ ->

                val selectedCode = languageCodes[position]

                // Do not recreate unnecessarily
                if (selectedCode != currentLanguage) {

                    val localeList =
                        LocaleListCompat.forLanguageTags(
                            selectedCode
                        )

                    // Apply selected language to the app
                    AppCompatDelegate.setApplicationLocales(
                        localeList
                    )
                }
            }
        }

        languageInputLayout.addView(languageDropdown)

        contentLayout.addView(languageLabel)
        contentLayout.addView(languageInputLayout)

        addSpacer(contentLayout)

        // ==========================================
        // FEATURE 3: DEFAULT TASK TIME
        // ==========================================

        val timeLabel = createSectionLabel(
            getString(R.string.settings_default_time)
        )

        val savedTime = prefs.getString(
            KEY_DEFAULT_DUE_TIME,
            "09:00"
        ) ?: "09:00"

        val timePickerButton = MaterialButton(this).apply {

            text = "Default Time: $savedTime"

            setBackgroundColor(
                Color.parseColor("#2C2C2E")
            )

            setTextColor(Color.WHITE)

            setOnClickListener {

                val currentSavedTime = prefs.getString(
                    KEY_DEFAULT_DUE_TIME,
                    "09:00"
                ) ?: "09:00"

                val timeParts = currentSavedTime.split(":")

                val hour = timeParts
                    .getOrNull(0)
                    ?.toIntOrNull() ?: 9

                val minute = timeParts
                    .getOrNull(1)
                    ?.toIntOrNull() ?: 0

                val picker = MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setHour(hour)
                    .setMinute(minute)
                    .setTitleText("Select Default Task Time")
                    .build()

                picker.addOnPositiveButtonClickListener {

                    val newTime = String.format(
                        Locale.ROOT,
                        "%02d:%02d",
                        picker.hour,
                        picker.minute
                    )

                    prefs.edit()
                        .putString(KEY_DEFAULT_DUE_TIME, newTime)
                        .apply()

                    text = "Default Time: $newTime"

                    Toast.makeText(
                        context,
                        "Default time saved",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                picker.show(
                    supportFragmentManager,
                    "DEFAULT_TIME_PICKER"
                )
            }
        }

        contentLayout.addView(timeLabel)
        contentLayout.addView(timePickerButton)

        addSpacer(contentLayout)

        // ==========================================
        // FEATURE 4: COMPLETED TASK ACTION
        // ==========================================

        val behaviorLabel = createSectionLabel(
            getString(R.string.settings_completed_action)
        )

        val behaviorInputLayout = createDropdownContainer(
            "Select Action"
        )

        val behaviorOptions = arrayOf(
            "Keep in Place",
            "Move to Bottom",
            "Hide Immediately"
        )

        val behaviorAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            behaviorOptions
        )

        val behaviorDropdown = AutoCompleteTextView(
            behaviorInputLayout.context
        ).apply {

            inputType = android.text.InputType.TYPE_NULL

            setTextColor(Color.WHITE)

            setAdapter(behaviorAdapter)

            val currentBehavior = prefs.getString(
                KEY_COMPLETED_BEHAVIOR,
                "Move to Bottom"
            )

            setText(currentBehavior, false)

            setOnClickListener {
                showDropDown()
            }

            setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    showDropDown()
                }
            }

            setOnItemClickListener { _, _, position, _ ->

                val selected = behaviorOptions[position]

                prefs.edit()
                    .putString(
                        KEY_COMPLETED_BEHAVIOR,
                        selected
                    )
                    .apply()

                Toast.makeText(
                    context,
                    "Preference saved",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        behaviorInputLayout.addView(behaviorDropdown)

        contentLayout.addView(behaviorLabel)
        contentLayout.addView(behaviorInputLayout)

        addSpacer(contentLayout)

        // ==========================================
        // FEATURE 5: HAPTIC FEEDBACK
        // ==========================================

        val hapticCard = MaterialCardView(this).apply {

            setCardBackgroundColor(
                Color.parseColor("#1F1F1F")
            )

            radius = 16f
            strokeWidth = 0

            val cardPadding = 32

            setPadding(
                cardPadding,
                cardPadding,
                cardPadding,
                cardPadding
            )
        }

        val hapticSwitch = SwitchMaterial(this).apply {

            text = getString(
                R.string.settings_haptic_feedback
            )

            setTextColor(Color.WHITE)

            isChecked = prefs.getBoolean(
                KEY_HAPTIC_FEEDBACK,
                true
            )

            setOnCheckedChangeListener { _, isChecked ->

                prefs.edit()
                    .putBoolean(
                        KEY_HAPTIC_FEEDBACK,
                        isChecked
                    )
                    .apply()

                if (isChecked) {
                    triggerHapticFeedback(context)
                }
            }
        }

        hapticCard.addView(hapticSwitch)

        contentLayout.addView(hapticCard)

        // ==========================================
        // FINALIZE SETTINGS SCREEN
        // ==========================================

        scrollView.addView(contentLayout)

        rootLayout.addView(scrollView)

        setContentView(rootLayout)
    }

    // ==========================================
    // APPLY APP THEME
    // ==========================================

    private fun applyAppTheme(themeChoice: String) {

        val mode = when (themeChoice) {

            "Light Mode" ->
                AppCompatDelegate.MODE_NIGHT_NO

            "Dark Mode" ->
                AppCompatDelegate.MODE_NIGHT_YES

            else ->
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }

        AppCompatDelegate.setDefaultNightMode(mode)
    }

    // ==========================================
    // CREATE SECTION LABEL
    // ==========================================

    private fun createSectionLabel(
        title: String
    ): TextView {

        return TextView(this).apply {

            text = title
            textSize = 14f

            setTextColor(
                Color.parseColor("#A0A0A0")
            )

            setPadding(0, 0, 0, 12)
        }
    }

    // ==========================================
    // CREATE DROPDOWN CONTAINER
    // ==========================================

    private fun createDropdownContainer(
        hintText: String
    ): TextInputLayout {

        return TextInputLayout(
            this,
            null,
            com.google.android.material.R.style
                .Widget_MaterialComponents_TextInputLayout_OutlinedBox_ExposedDropdownMenu
        ).apply {

            hint = hintText

            setHintTextColor(
                android.content.res.ColorStateList.valueOf(
                    Color.parseColor("#A0A0A0")
                )
            )

            boxStrokeColor = Color.parseColor(
                "#2F80ED"
            )

            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    // ==========================================
    // ADD SPACING BETWEEN SETTINGS
    // ==========================================

    private fun addSpacer(
        container: LinearLayout
    ) {

        val spacer = View(this).apply {

            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                32
            )
        }

        container.addView(spacer)
    }

    // ==========================================
    // HAPTIC FEEDBACK
    // ==========================================

    private fun triggerHapticFeedback(
        context: Context
    ) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            val vibratorManager =
                context.getSystemService(
                    Context.VIBRATOR_MANAGER_SERVICE
                ) as VibratorManager

            vibratorManager.defaultVibrator.vibrate(
                VibrationEffect.createOneShot(
                    50,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )

        } else {

            @Suppress("DEPRECATION")
            val vibrator =
                context.getSystemService(
                    Context.VIBRATOR_SERVICE
                ) as Vibrator

            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
        }
    }
}