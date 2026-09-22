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
 * Authentication screen for existing users to sign into DoToday.
 *
 * Key Responsibilities:
 * - Enables Edge-to-Edge window rendering while safely applying system bar window insets to [R.id.main].
 * - Validates email format using Android [Patterns.EMAIL_ADDRESS] regex and enforces non-empty passwords.
 * - Handles navigation to [RegisterActivity] and clears the back stack when advancing to [MainActivity].
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnGoToRegister: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate() - Initializing LoginActivity UI and Edge-to-Edge window insets")

        // Do not force edge-to-edge rendering here; the app theme already handles bars
        // and the previous insets callback caused layout loops that triggered ANR state.
        setContentView(R.layout.activity_login)

        // Bind layout view components
        tilEmail = findViewById(R.id.til_email)
        tilPassword = findViewById(R.id.til_password)
        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        btnLogin = findViewById(R.id.btn_login)
        btnGoToRegister = findViewById(R.id.btn_go_to_register)

        // Set up action click listeners
        btnLogin.setOnClickListener {
            Log.d(TAG, "Login button clicked - executing attemptLogin()")
            attemptLogin()
        }

        btnGoToRegister.setOnClickListener {
            Log.i(TAG, "Navigating from LoginActivity to RegisterActivity")
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    /**
     * Validates email address format and password inputs.
     * Displays error messages directly on [TextInputLayout] components if validation fails.
     */
    private fun attemptLogin() {
        val email = etEmail.text?.toString()?.trim() ?: ""
        val password = etPassword.text?.toString() ?: ""

        // Clear any previous error indicators on text input fields
        tilEmail.error = null
        tilPassword.error = null

        // Validate email presence
        if (email.isEmpty()) {
            tilEmail.error = "Email is required"
            Log.w(TAG, "Login validation failed: email field is empty")
            return
        }

        // Validate email syntax structure
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Enter a valid email address"
            Log.w(TAG, "Login validation failed: invalid email format '$email'")
            return
        }

        // Validate password presence
        if (password.isEmpty()) {
            tilPassword.error = "Password is required"
            Log.w(TAG, "Login validation failed: password field is empty")
            return
        }

        Log.i(TAG, "Login inputs validated successfully for user: '$email'")

        // Simulation placeholder for backend / Firebase authentication
        Toast.makeText(this, "Logged in successfully!", Toast.LENGTH_SHORT).show()
        goToMain()
    }

    /**
     * Navigates to [MainActivity] and purges the activity task back stack
     * so pressing the system back button cannot return to the login screen.
     */
    private fun goToMain() {
        Log.i(TAG, "Transitioning to MainActivity - clearing task back stack")
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    companion object {
        private const val TAG = "LoginActivity"
    }
}