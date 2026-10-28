package com.deepeye.musicpro.ui.navigation

import com.deepeye.musicpro.account.AccountSession

/** The only app-level authentication destinations. */
enum class AppRoute {
    YOUTUBE_LOGIN,
    HOME,
}

/**
 * Keeps root navigation bound to the primary YouTube account session only.
 * Firebase/Google identity must never decide whether MusicPro is signed in.
 */
object AccountRouteDecider {
    fun routeFor(session: AccountSession): AppRoute =
        if (session is AccountSession.Connected) AppRoute.HOME else AppRoute.YOUTUBE_LOGIN
}
