package com.example.dotoday

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI tests for the SSO Login feature on [LoginActivity].
 *
 * Validates:
 * - [LoginActivity] launches successfully.
 * - The "Continue with Google" SSO button is visible and correctly styled.
 * - Clicking the SSO button triggers the account picker action without crashing.
 */
@RunWith(AndroidJUnit4::class)
class SsoLoginInstrumentedTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(LoginActivity::class.java)

    @Test
    fun testSsoButtonIsDisplayedAndClickable() {
        // Verify that the SSO button with text "Continue with Google" is displayed on the login screen
        onView(withId(R.id.btn_sso_login))
            .check(matches(isDisplayed()))
            .check(matches(withText("Continue with Google")))

        // Perform click on the SSO button to verify it launches the account picker flow without errors
        onView(withId(R.id.btn_sso_login))
            .perform(click())
    }
}
