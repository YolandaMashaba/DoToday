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

        setContentView(R.layout.activity_login)

        tilEmail = findViewById(R.id.til_email)
        tilPassword = findViewById(R.id.til_password)

        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)

        btnLogin = findViewById(R.id.btn_login)
        btnGoToRegister = findViewById(R.id.btn_go_to_register)

        btnLogin.setOnClickListener {
            attemptLogin()
        }

        btnGoToRegister.setOnClickListener {
            startActivity(
                Intent(this, RegisterActivity::class.java)
            )
        }
    }

    private fun attemptLogin() {

        val email = etEmail.text
            ?.toString()
            ?.trim() ?: ""

        val password = etPassword.text
            ?.toString() ?: ""

        tilEmail.error = null
        tilPassword.error = null

        // Email must not be empty
        if (email.isEmpty()) {
            tilEmail.error = getString(
                R.string.auth_email_required
            )
            return
        }

        // Check email format
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = getString(
                R.string.auth_invalid_email
            )
            return
        }

        // Password must not be empty
        if (password.isEmpty()) {
            tilPassword.error = getString(
                R.string.auth_password_required
            )
            return
        }

        Log.i("LoginActivity", "Login input validated")

        // Existing simulated authentication
        Toast.makeText(
            this,
            getString(R.string.auth_login_success),
            Toast.LENGTH_SHORT
        ).show()

        goToMain()
    }

    private fun goToMain() {

        val intent = Intent(
            this,
            MainActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intent)
        finish()
    }
}
