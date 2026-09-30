package com.deepeye.musicpro.ui.auth

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented coverage for the sign-in form's fit on short viewports.
 *
 * The form stacks a header, two fields, the primary CTA, a mode toggle, a
 * divider and two provider buttons, which needs more height than a landscape
 * phone offers. Before this was fixed the column was measured with
 * `fillMaxWidth` inside a centring Box, so the trailing nodes were laid out
 * past the bottom edge and could not be reached at all — the a11y tree put the
 * last one flush against the 720px viewport floor. These tests pin the two
 * properties that prevent a regression: the form scrolls, and the controls that
 * used to be clipped are reachable by scrolling to them.
 */
@RunWith(AndroidJUnit4::class)
class LoginScreenLayoutTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Renders the form's scrollable body. [LoginScreen] itself needs a Hilt
     * [AuthViewModel], so the assertions target the layout contract rather than
     * the auth wiring.
     */
    private fun setContent(isSignUpMode: Boolean = false) {
        composeTestRule.setContent {
            var email by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }
            LoginFormBody(
                email = email,
                onEmailChange = { email = it },
                password = password,
                onPasswordChange = { password = it },
                isSignUpMode = isSignUpMode,
                onToggleMode = {},
                primaryAction = if (isSignUpMode) "Create Account" else "Sign In",
                onPrimaryAction = {},
                onYouTubeLoginClick = {},
                onGoogleLogin = {},
                onSkipAsGuest = {},
                isLoading = false,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    @Test
    fun formBodyIsScrollable() {
        setContent()

        composeTestRule
            .onNode(hasScrollAction())
            .assertExists()
    }

    @Test
    fun trailingControlsAreReachableByScrolling() {
        setContent()

        // Each of these sat below the fold on a 1604x720 viewport. Reaching
        // them is the actual user-facing fix.
        listOf(
            "Don't have an account? ",
            "OR",
            "Login with YouTube (TV)",
            "Continue with Google",
            "Skip & Continue as Guest →",
        ).forEach { label ->
            composeTestRule.onNodeWithText(label).performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun signUpModeSwapsPrimaryActionAndToggleCopy() {
        setContent(isSignUpMode = true)

        composeTestRule.onNodeWithText("Create Account").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Already have an account? ")
            .performScrollTo()
            .assertIsDisplayed()
    }

    /**
     * Pins that the form is a controlled component: what the user types must be
     * the value the caller's submit action sees. The form previously mirrored
     * the input into its own local state, so the callback fired with the
     * caller's initial empty strings and email sign-in could never succeed.
     */
    @Test
    fun typedCredentialsArePropagatedToTheSubmitAction() {
        var submittedEmail: String? = null
        var submittedPassword: String? = null

        composeTestRule.setContent {
            var email by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }
            LoginFormBody(
                email = email,
                onEmailChange = { email = it },
                password = password,
                onPasswordChange = { password = it },
                isSignUpMode = false,
                onToggleMode = {},
                primaryAction = "Sign In",
                onPrimaryAction = {
                    submittedEmail = email
                    submittedPassword = password
                },
                onYouTubeLoginClick = {},
                onGoogleLogin = {},
                onSkipAsGuest = {},
                isLoading = false,
                modifier = Modifier.fillMaxSize(),
            )
        }

        composeTestRule
            .onNodeWithText("Email Address")
            .performTextInput("user@example.com")
        composeTestRule
            .onNodeWithText("Password")
            .performTextInput("s3cret-pass")
        composeTestRule
            .onNodeWithText("Sign In")
            .performScrollTo()
            .performClick()

        assertEquals("user@example.com", submittedEmail)
        assertEquals("s3cret-pass", submittedPassword)
    }
}