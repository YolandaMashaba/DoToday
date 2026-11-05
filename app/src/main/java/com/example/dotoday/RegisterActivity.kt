package com.example.dotoday

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

        val email = etEmail.text
            ?.toString()
            ?.trim() ?: ""

        val password = etPassword.text
            ?.toString() ?: ""

        val confirm = etConfirmPassword.text
            ?.toString() ?: ""

        tilEmail.error = null
        tilPassword.error = null
        tilConfirmPassword.error = null

        // Check email is entered
        if (email.isEmpty()) {
            tilEmail.error = getString(
                R.string.auth_email_required
            )
            return
        }

        // Validate email format
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = getString(
                R.string.auth_invalid_email
            )
            return
        }

        // Minimum password length
        if (password.length < 8) {
            tilPassword.error = getString(
                R.string.auth_password_minimum
            )
            return
        }

        // Confirm passwords match
        if (password != confirm) {
            tilConfirmPassword.error = getString(
                R.string.auth_password_mismatch
            )
            return
        }

        // Existing simulated registration
        Toast.makeText(
            this,
            getString(R.string.auth_register_success),
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
