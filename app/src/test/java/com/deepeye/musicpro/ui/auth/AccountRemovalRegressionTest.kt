package com.deepeye.musicpro.ui.auth

import com.deepeye.musicpro.ui.navigation.Routes
import com.deepeye.musicpro.ui.navigation.AccountRouteDecider
import com.deepeye.musicpro.ui.navigation.AppRoute
import com.deepeye.musicpro.account.AccountSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression contract for the forced Google/Firebase account removal:
 * - the duplicate Google login route is gone from the navigation surface,
 * - root routing is driven only by the YouTube AccountSession,
 * - no Firebase identity decides app sign-in state.
 */
class AccountRemovalRegressionTest {

    @Test
    fun `duplicate Google login route no longer exists`() {
        val routeStrings = Routes::class.nestedClasses.map { it.simpleName }
        assertFalse("Routes.Login must be removed", "Login" in routeStrings)
    }

    @Test
    fun `Firebase-backed chat routes no longer exist`() {
        val routeStrings = Routes::class.nestedClasses.map { it.simpleName }
        assertFalse("Routes.ChatList must be removed", "ChatList" in routeStrings)
        assertFalse("Routes.ChatRoom must be removed", "ChatRoom" in routeStrings)
    }

    @Test
    fun `Firebase-backed gamification route no longer exists`() {
        val routeStrings = Routes::class.nestedClasses.map { it.simpleName }
        assertFalse("Routes.Gamification must be removed", "Gamification" in routeStrings)
    }

    @Test
    fun `YouTube login remains the only auth route`() {
        val routeStrings = Routes::class.nestedClasses.map { it.simpleName }
        assertTrue("Routes.YouTubeLogin must remain", "YouTubeLogin" in routeStrings)
    }

    @Test
    fun `route decider never emits a Firebase auth destination`() {
        val loggedIn = AccountRouteDecider.routeFor(
            AccountSession.Connected(accountKey = "opaque-youtube-key")
        )
        assertEquals(AppRoute.HOME, loggedIn)

        val loggedOut = AccountRouteDecider.routeFor(AccountSession.LoggedOut)
        assertEquals(AppRoute.YOUTUBE_LOGIN, loggedOut)

        // AppRoute is a closed enum of two values: YOUTUBE_LOGIN, HOME.
        // There is no Firebase/Google auth destination it can produce.
        val all = AppRoute.entries
        assertEquals(setOf(AppRoute.YOUTUBE_LOGIN, AppRoute.HOME), all.toSet())
    }
}