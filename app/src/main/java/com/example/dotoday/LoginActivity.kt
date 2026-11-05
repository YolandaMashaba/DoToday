package com.example.dotoday

import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/** Login screen with credential validation and Android's Google account picker. */
class LoginActivity : AppCompatActivity() {

    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnSsoLogin: MaterialButton
    private lateinit var btnGoToRegister: MaterialButton

    private val ssoAccountPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val accountName = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            if (accountName != null) {
                Log.i(TAG, "Signed in with Google device account: $accountName")
                Toast.makeText(this, "Signed in as $accountName", Toast.LENGTH_SHORT).show()
                saveLoginStateAndGoToMain()
            } else {
                Log.w(TAG, "Account picker returned no account name")
                Toast.makeText(this, "Sign-in cancelled or failed", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.i(TAG, "Google account picker cancelled")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySavedTheme()

        // Check if user is already logged in (e.g. after language locale restart)
        val prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(SettingsActivity.KEY_IS_LOGGED_IN, false)) {
            Log.i(TAG, "User session active - bypassing LoginActivity and jumping to MainActivity")
            goToMain()
            return
        }

        Log.d(TAG, "Initializing LoginActivity")
        setContentView(R.layout.activity_login)

        tilEmail = findViewById(R.id.til_email)
        tilPassword = findViewById(R.id.til_password)
        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        btnLogin = findViewById(R.id.btn_login)
        btnSsoLogin = findViewById(R.id.btn_sso_login)
        btnGoToRegister = findViewById(R.id.btn_go_to_register)

        btnLogin.setOnClickListener { attemptLogin() }
        btnSsoLogin.setOnClickListener { attemptSsoLogin() }
        btnGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun applySavedTheme() {
        val prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val currentTheme = prefs.getString(SettingsActivity.KEY_THEME, "System Default")
        val mode = when (currentTheme) {
            "Light Mode" -> AppCompatDelegate.MODE_NIGHT_NO
            "Dark Mode" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    private fun attemptLogin() {
        val email = etEmail.text?.toString()?.trim().orEmpty()
        val password = etPassword.text?.toString().orEmpty()
        tilEmail.error = null
        tilPassword.error = null

        if (email.isEmpty()) {
            tilEmail.error = getString(R.string.auth_email_required)
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = getString(R.string.auth_invalid_email)
            return
        }
        if (password.isEmpty()) {
            tilPassword.error = getString(R.string.auth_password_required)
            return
        }
        if (password.length < 8) {
            tilPassword.error = getString(R.string.auth_password_minimum)
            return
        }

        val registeredUsers = getRegisteredUsers()
        val match = registeredUsers.find { it.email.equals(email, ignoreCase = true) && it.password == password }
        if (match == null) {
            tilEmail.error = "No account found for this email"
            tilPassword.error = "Check your email and password"
            Toast.makeText(this, "Please register before logging in", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, getString(R.string.auth_login_success), Toast.LENGTH_SHORT).show()
        saveLoginStateAndGoToMain()
    }

    private fun getRegisteredUsers(): List<RegisteredUser> {
        val prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_REGISTERED_USERS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<RegisteredUser>>() {}.type
            Gson().fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read registered users", e)
            emptyList()
        }
    }

    data class RegisteredUser(
        val email: String,
        val password: String
    )

    @Suppress("DEPRECATION")
    private fun attemptSsoLogin() {
        try {
            val intent = AccountManager.newChooseAccountIntent(
                null, null, arrayOf("com.google"), false, null, null, null, null
            )
            ssoAccountPickerLauncher.launch(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch account picker", e)
            Toast.makeText(this, "Unable to open Google account picker", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveLoginStateAndGoToMain() {
        val prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(SettingsActivity.KEY_IS_LOGGED_IN, true).apply()
        goToMain()
    }

    private fun goToMain() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    companion object {
        private const val TAG = "LoginActivity"
        private const val KEY_REGISTERED_USERS = "key_registered_users"
    }
}
