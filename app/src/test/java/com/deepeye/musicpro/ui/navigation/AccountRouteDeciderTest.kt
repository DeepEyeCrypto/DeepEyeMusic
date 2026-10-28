package com.deepeye.musicpro.ui.navigation

import com.deepeye.musicpro.account.AccountSession
import org.junit.Assert.assertEquals
import org.junit.Test

class AccountRouteDeciderTest {
    @Test
    fun `logged out routes to primary YouTube login`() {
        assertEquals(
            AppRoute.YOUTUBE_LOGIN,
            AccountRouteDecider.routeFor(AccountSession.LoggedOut),
        )
    }

    @Test
    fun `loading routes to primary YouTube login until session truth arrives`() {
        assertEquals(
            AppRoute.YOUTUBE_LOGIN,
            AccountRouteDecider.routeFor(AccountSession.Loading),
        )
    }

    @Test
    fun `connected routes to home`() {
        assertEquals(
            AppRoute.HOME,
            AccountRouteDecider.routeFor(
                AccountSession.Connected(accountKey = "opaque-account-key"),
            ),
        )
    }

    @Test
    fun `session error routes to primary YouTube login`() {
        assertEquals(
            AppRoute.YOUTUBE_LOGIN,
            AccountRouteDecider.routeFor(AccountSession.Error("session unavailable")),
        )
    }
}
