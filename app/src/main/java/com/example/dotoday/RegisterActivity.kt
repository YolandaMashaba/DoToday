package com.example.dotoday

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class RegisterActivity : AppCompatActivity() {

    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var tilConfirmPassword: TextInputLayout

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText

    private lateinit var btnRegister: MaterialButton
    private lateinit var btnGoToLogin: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_register)

        tilEmail = findViewById(R.id.til_email)
        tilPassword = findViewById(R.id.til_password)
        tilConfirmPassword = findViewById(
            R.id.til_confirm_password
        )

        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        etConfirmPassword = findViewById(
            R.id.et_confirm_password
        )

        btnRegister = findViewById(R.id.btn_register)
        btnGoToLogin = findViewById(R.id.btn_go_to_login)

        btnRegister.setOnClickListener {
            attemptRegister()
        }

        btnGoToLogin.setOnClickListener {
            finish()
        }
    }

    private fun attemptRegister() {
        val email = etEmail.text?.toString()?.trim().orEmpty()
        val password = etPassword.text?.toString().orEmpty()
        val confirm = etConfirmPassword.text?.toString().orEmpty()

        tilEmail.error = null
        tilPassword.error = null
        tilConfirmPassword.error = null

        if (email.isEmpty()) {
            tilEmail.error = getString(R.string.auth_email_required)
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = getString(R.string.auth_invalid_email)
            return
        }

        if (password.length < 8) {
            tilPassword.error = getString(R.string.auth_password_minimum)
            return
        }

        if (password != confirm) {
            tilConfirmPassword.error = getString(R.string.auth_password_mismatch)
            return
        }

        val prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val existingUsers = getRegisteredUsers()
        if (existingUsers.any { it.email.equals(email, ignoreCase = true) }) {
            tilEmail.error = "An account with this email already exists"
            Toast.makeText(this, "User already registered", Toast.LENGTH_SHORT).show()
            return
        }

        val updatedUsers = existingUsers + RegisteredUser(email = email, password = password)
        prefs.edit().putString(KEY_REGISTERED_USERS, Gson().toJson(updatedUsers)).apply()

        prefs.edit().putBoolean(SettingsActivity.KEY_IS_LOGGED_IN, true).apply()

        Toast.makeText(this, getString(R.string.auth_register_success), Toast.LENGTH_SHORT).show()
        goToMain()
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

    private fun goToMain() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intent)
        finish()
    }

    companion object {
        private const val TAG = "RegisterActivity"
        private const val KEY_REGISTERED_USERS = "key_registered_users"
    }
}
