package com.deepeye.musicpro.ui.auth

import com.deepeye.musicpro.ui.navigation.Routes
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression contract for the duplicate Google/Firebase login removal.
 *
 * Verifies at the route surface that:
 * - the duplicate Google login route and its UI entry points are gone,
 * - the primary YouTube login route remains the only auth entry point.
 *
 * (YouTubeLoginScreen itself uses Hilt injection; its YouTube-only content is
 * covered structurally here instead of a Hilt compose harness.)
 */
class LoginRemovalTest {

    private val routeNames: Set<String> =
        Routes::class.nestedClasses
            .mapNotNull { it.simpleName }
            .toSet()

    @Test
    fun `duplicate Google login route is absent`() {
        assertFalse("Routes.Login must not exist", "Login" in routeNames)
    }

    @Test
    fun `primary YouTube login route is present`() {
        assertTrue("Routes.YouTubeLogin must remain", "YouTubeLogin" in routeNames)
    }

    @Test
    fun `no second Google identity route exists`() {
        assertFalse("Routes.GoogleSignIn must not exist", "GoogleSignIn" in routeNames)
        assertFalse("Routes.CredentialPicker must not exist", "CredentialPicker" in routeNames)
    }
}