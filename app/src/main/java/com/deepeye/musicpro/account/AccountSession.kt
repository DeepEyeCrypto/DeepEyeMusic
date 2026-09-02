// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.account

/**
 * Privacy-safe, lifecycle-safe public representation of the user's connected account state.
 *
 * This model contains NO access tokens, refresh tokens, cookies, email addresses,
 * authorization headers, or private user credentials.
 */
sealed interface AccountSession {
    data object Loading : AccountSession

    data object LoggedOut : AccountSession

    data class Connected(
        val accountKey: String,
        val displayName: String? = null,
        val avatarUrl: String? = null,
        val channelKey: String? = null,
        val lastUpdatedAtMs: Long? = null
    ) : AccountSession

    data class Error(
        val userMessage: String,
        val isRecoverable: Boolean = true
    ) : AccountSession
}
