package com.example.dotoday

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * Registration screen for new users creating an account in DoToday.
 *
 * Key Responsibilities:
 * - Edge-to-Edge window inset handling targeting root ScrollView ([R.id.main]).
 * - Thorough client-side input validation (email structure, min 8-char password, password confirmation match).
 * - Navigation back to [LoginActivity] or advancing directly to [MainActivity].
 */
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
        Log.d(TAG, "onCreate() - Initializing RegisterActivity UI and Edge-to-Edge insets")

        // Do not force edge-to-edge rendering here; the app theme already handles bars
        // and the previous insets callback caused layout loops that triggered ANR state.
        setContentView(R.layout.activity_register)

        // Bind layout views
        tilEmail = findViewById(R.id.til_email)
        tilPassword = findViewById(R.id.til_password)
        tilConfirmPassword = findViewById(R.id.til_confirm_password)
        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        etConfirmPassword = findViewById(R.id.et_confirm_password)
        btnRegister = findViewById(R.id.btn_register)
        btnGoToLogin = findViewById(R.id.btn_go_to_login)

        btnRegister.setOnClickListener {
            Log.d(TAG, "Register button clicked - executing attemptRegister()")
            attemptRegister()
        }

        btnGoToLogin.setOnClickListener {
            Log.i(TAG, "Returning to LoginActivity from RegisterActivity")
            finish()
        }
    }

    /**
     * Performs multi-step form validation:
     * 1. Email is required and must match valid email syntax.
     * 2. Password must contain at least 8 characters.
     * 3. Confirm password input must match the password input.
     */
    private fun attemptRegister() {
        val email = etEmail.text?.toString()?.trim() ?: ""
        val password = etPassword.text?.toString() ?: ""
        val confirm = etConfirmPassword.text?.toString() ?: ""

        // Reset previous form field errors
        tilEmail.error = null
        tilPassword.error = null
        tilConfirmPassword.error = null

        // Email validation
        if (email.isEmpty()) {
            tilEmail.error = "Email is required"
            Log.w(TAG, "Register validation failed: email is empty")
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Enter a valid email address"
            Log.w(TAG, "Register validation failed: invalid email format '$email'")
            return
        }

        // Password length validation
        if (password.length < 8) {
            tilPassword.error = "Password must be at least 8 characters"
            Log.w(TAG, "Register validation failed: password length < 8")
            return
        }

        // Password match validation
        if (password != confirm) {
            tilConfirmPassword.error = "Passwords do not match"
            Log.w(TAG, "Register validation failed: passwords do not match")
            return
        }

        Log.i(TAG, "Registration validation succeeded for account: '$email'")

        // Simulation placeholder for backend registration logic
        Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
        goToMain()
    }

    /**
     * Navigates to [MainActivity] and clears the task back stack.
     */
    private fun goToMain() {
        Log.i(TAG, "Transitioning from RegisterActivity to MainActivity")
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    companion object {
        private const val TAG = "RegisterActivity"
    }
}