// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.domain.model.profile

/**
 * Lifecycle phase of the profile dashboard, derived from the account session.
 * Drives the UI's loading / signed-out / ready / error states.
 */
enum class ProfilePhase { LOADING, SIGNED_OUT, READY, ERROR }

/**
 * Read-only profile dashboard state for P1.
 *
 * Everything here is derived from sources already wired into the app —
 * [com.deepeye.musicpro.account.AccountSession] for identity,
 * [com.deepeye.musicpro.data.prefs.SettingsDataStore] for avatar/email, and
 * [com.deepeye.musicpro.domain.repository.library.LibraryRepository] for the
 * library counters. No token, cookie, header or raw account identifier is
 * carried in this type; only the human-readable display fields the UI renders.
 */
data class ProfileState(
    val phase: ProfilePhase = ProfilePhase.LOADING,
    val isConnected: Boolean = false,
    val accountKey: String? = null,
    val avatarUrl: String? = null,
    val displayName: String? = null,
    val email: String? = null,
    val subscribedCount: Int = 0,
    val likedCount: Int = 0,
    val playlistCount: Int = 0,
    val downloadCount: Int = 0,
    val errorMessage: String? = null,
    val isFromCache: Boolean = false,
    val lastSyncedAt: Long = 0L,
)
