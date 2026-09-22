package com.example.dotoday

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class LoginActivity : AppCompatActivity() {

    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnGoToRegister: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        Log.d(TAG, "LoginActivity created")

        // Bind views
        tilEmail = findViewById(R.id.til_email)
        tilPassword = findViewById(R.id.til_password)
        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        btnLogin = findViewById(R.id.btn_login)
        btnGoToRegister = findViewById(R.id.btn_go_to_register)

        btnLogin.setOnClickListener { attemptLogin() }

        btnGoToRegister.setOnClickListener {
            Log.d(TAG, "Navigating to RegisterActivity")
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    /**
     * Validates input fields. If valid, proceeds to login logic.
     * Firebase call will be added in the next step.
     */
    private fun attemptLogin() {
        val email = etEmail.text?.toString()?.trim() ?: ""
        val password = etPassword.text?.toString() ?: ""

        // Reset previous errors
        tilEmail.error = null
        tilPassword.error = null

        // --- Validation ---
        if (email.isEmpty()) {
            tilEmail.error = "Email is required"
            Log.w(TAG, "Login validation failed: empty email")
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Enter a valid email address"
            Log.w(TAG, "Login validation failed: invalid email format")
            return
        }
        if (password.isEmpty()) {
            tilPassword.error = "Password is required"
            Log.w(TAG, "Login validation failed: empty password")
            return
        }

        Log.d(TAG, "Input valid. Proceeding to authenticate $email")

        // TODO: Replace with Firebase Auth call in the next step.
        // For now, simulate success and navigate to MainActivity.
        Toast.makeText(this, "Logged in successfully!", Toast.LENGTH_SHORT).show()
        goToMain()
    }

    private fun goToMain() {
        val intent = Intent(this, MainActivity::class.java)
        // Clear back stack so user can't press "back" to return to Login
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    companion object {
        private const val TAG = "LoginActivity"
    }
}