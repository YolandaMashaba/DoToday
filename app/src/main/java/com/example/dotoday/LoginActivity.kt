package com.example.dotoday

import android.accounts.AccountManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

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
                goToMain()
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

        Toast.makeText(this, getString(R.string.auth_login_success), Toast.LENGTH_SHORT).show()
        goToMain()
    }

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

    private fun goToMain() {
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
