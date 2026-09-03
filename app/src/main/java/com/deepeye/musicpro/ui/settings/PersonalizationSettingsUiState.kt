// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.settings

import com.deepeye.musicpro.account.AccountSession
import com.deepeye.musicpro.data.prefs.PersonalizationPreferences

/**
 * Summary of cached personalized sections and their storage health.
 */
data class CacheSummary(
    val cachedSectionCount: Int = 0,
    val totalCachedItems: Int = 0,
    val lastRefreshMillis: Long = 0L,
    val isStale: Boolean = false,
)

/**
 * UI State for active confirmation dialogs across destructive personalization actions.
 */
data class ConfirmationDialogState(
    val title: String,
    val message: String,
    val confirmLabel: String = "Confirm",
    val isDestructive: Boolean = true,
    val onConfirm: () -> Unit,
)

/**
 * UI State for the Music Personalization settings screen.
 */
data class PersonalizationSettingsUiState(
    val preferences: PersonalizationPreferences = PersonalizationPreferences(),
    val accountState: AccountSession = AccountSession.LoggedOut,
    val hiddenItemCount: Int = 0,
    val hiddenArtistCount: Int = 0,
    val preferredLanguages: Set<String> = emptySet(),
    val preferredGenres: Set<String> = emptySet(),
    val cacheSummary: CacheSummary? = null,
    val isClearingCache: Boolean = false,
    val isClearingHistory: Boolean = false,
    val isClearingHidden: Boolean = false,
    val isRefreshing: Boolean = false,
    val activeConfirmation: ConfirmationDialogState? = null,
    val message: String? = null,
)
