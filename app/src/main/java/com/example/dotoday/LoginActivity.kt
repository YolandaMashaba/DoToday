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

/**
 * Authentication screen for existing users to sign into DoToday.
 *
 * Key Responsibilities:
 * - Credentials Login: Validates email format using Android [Patterns.EMAIL_ADDRESS] and password.
 * - Single Sign-On (SSO): Launches the system Google Account Picker intent ([AccountManager.newChooseAccountIntent])
 *   connected directly to phone settings and Google accounts registered on the device.
 *   This asynchronous system intent prevents main-thread blocking and eliminates ANRs ("System UI not responding").
 * - Navigation: Purges the task back stack upon successful login to transition to [MainActivity].
 *
 * Citation: Android AccountManager & System Account Picker Intent Guidelines.
 * See: https://developer.android.com/reference/android/accounts/AccountManager#newChooseAccountIntent
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnSsoLogin: MaterialButton
    private lateinit var btnGoToRegister: MaterialButton

    /**
     * Modern Activity Result Launcher for the system Google Account Picker intent.
     * Executes off the main thread in the OS system process, preventing ANRs.
     */
    private val ssoAccountPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val accountName = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            if (accountName != null) {
                Log.i(TAG, "Successfully signed in with Google device account: $accountName")
                Toast.makeText(this, "Signed in as $accountName", Toast.LENGTH_SHORT).show()
                goToMain()
            } else {
                Log.w(TAG, "Google account picker returned null account name")
                Toast.makeText(this, "Sign-in cancelled or failed", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.i(TAG, "Google account picker cancelled by user or no account selected")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate() - Initializing LoginActivity UI and authentication listeners")

        setContentView(R.layout.activity_login)

        // Bind layout view components
        tilEmail = findViewById(R.id.til_email)
        tilPassword = findViewById(R.id.til_password)
        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        btnLogin = findViewById(R.id.btn_login)
        btnSsoLogin = findViewById(R.id.btn_sso_login)
        btnGoToRegister = findViewById(R.id.btn_go_to_register)

        // Set up action click listeners
        btnLogin.setOnClickListener {
            Log.d(TAG, "Login button clicked - executing attemptLogin()")
            attemptLogin()
        }

        btnSsoLogin.setOnClickListener {
            Log.d(TAG, "SSO Login button clicked - launching system Google Account Picker")
            attemptSsoLogin()
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

        Toast.makeText(this, "Logged in successfully!", Toast.LENGTH_SHORT).show()
        goToMain()
    }

    /**
     * Launches the native Android system Google Account Picker intent ([AccountManager.newChooseAccountIntent]).
     * This connects directly to phone settings and Google accounts registered on the device
     * while running asynchronously to prevent ANR / "System UI not responding" errors.
     */
    private fun attemptSsoLogin() {
        Log.i(TAG, "Initiating system Google Account Picker intent")
        try {
            @Suppress("DEPRECATION")
            val intent = AccountManager.newChooseAccountIntent(
                null,
                null,
                arrayOf("com.google"),
                false,
                null,
                null,
                null,
                null
            )
            ssoAccountPickerLauncher.launch(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch account picker intent: ${e.localizedMessage}", e)
            Toast.makeText(this, "Unable to open Google account picker", Toast.LENGTH_SHORT).show()
        }
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
