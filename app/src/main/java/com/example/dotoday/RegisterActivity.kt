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
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)
        Log.d(TAG, "RegisterActivity created")

        findViewById<View>(android.R.id.content)?.let { root ->
            ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                if (v.paddingLeft != systemBars.left || v.paddingTop != systemBars.top || v.paddingRight != systemBars.right || v.paddingBottom != systemBars.bottom) {
                    v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                }
                insets
            }
        }

        // Bind views
        tilEmail = findViewById(R.id.til_email)
        tilPassword = findViewById(R.id.til_password)
        tilConfirmPassword = findViewById(R.id.til_confirm_password)
        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        etConfirmPassword = findViewById(R.id.et_confirm_password)
        btnRegister = findViewById(R.id.btn_register)
        btnGoToLogin = findViewById(R.id.btn_go_to_login)

        btnRegister.setOnClickListener { attemptRegister() }

        btnGoToLogin.setOnClickListener {
            Log.d(TAG, "Returning to LoginActivity")
            finish()
        }
    }

    /**
     * Validates all fields, then simulates registration.
     * Firebase call will be added in the next step.
     */
    private fun attemptRegister() {
        val email = etEmail.text?.toString()?.trim() ?: ""
        val password = etPassword.text?.toString() ?: ""
        val confirm = etConfirmPassword.text?.toString() ?: ""

        // Reset errors
        tilEmail.error = null
        tilPassword.error = null
        tilConfirmPassword.error = null

        // --- Validation ---
        if (email.isEmpty()) {
            tilEmail.error = "Email is required"
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Enter a valid email address"
            return
        }
        if (password.length < 8) {
            tilPassword.error = "Password must be at least 8 characters"
            return
        }
        if (password != confirm) {
            tilConfirmPassword.error = "Passwords do not match"
            return
        }

        Log.d(TAG, "Input valid. Registering $email")

        // TODO: Replace with Firebase Auth call in the next step.
        Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
        goToMain()
    }

    private fun goToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    companion object {
        private const val TAG = "RegisterActivity"
    }
}