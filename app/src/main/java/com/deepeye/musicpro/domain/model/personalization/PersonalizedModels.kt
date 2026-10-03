// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.domain.model.personalization

import com.deepeye.musicpro.account.AccountSession
import com.deepeye.musicpro.domain.model.MediaItem
import com.deepeye.musicpro.domain.personalization.SectionDiagnostics

/**
 * Types of personalized music feed sections rendered on the Music tab.
 */
enum class PersonalizedSectionType {
    CONTINUE_LISTENING,
    YOUR_QUEUE,
    RECENTLY_PLAYED,
    LIKED_MUSIC,
    YOUR_PLAYLISTS,
    NEW_FROM_SUBSCRIPTIONS,
    BASED_ON_LISTENING,
    TRENDING_MUSIC,
}

/**
 * Types of individual items within a personalized section.
 */
enum class PersonalizedItemType {
    SONG,
    PLAYLIST,
    VIDEO,
}

/**
 * Clean, UI-ready item representing a track, playlist, or video in a personalized section.
 * Encapsulates playback metadata without exposing tokens, cookies, or internal API internals.
 */
data class PersonalizedFeedItem(
    val id: String,
    val title: String,
    val artist: String,
    val channelId: String? = null,
    val artworkUrl: String? = null,
    val durationMs: Long = 0L,
    val itemType: PersonalizedItemType = PersonalizedItemType.SONG,
    val sourceBadge: String? = null,
    val isExplicit: Boolean = false,
    val mediaItem: MediaItem? = null,
    val playCount: Long = 0L,
    val lastPlayedAt: Long = 0L,
    val explanation: String? = null,
)

/**
 * A single structured section within the personalized Music tab.
 * Contains explicit source labels, caching flags, and isolated error/retry state.
 */
data class PersonalizedSection(
    val id: String,
    val type: PersonalizedSectionType,
    val title: String,
    val subtitle: String? = null,
    val sourceLabel: String,
    val explanation: String? = null,
    val items: List<PersonalizedFeedItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAccountRequired: Boolean = false,
    val canRetry: Boolean = false,
    val isFromCache: Boolean = false,
    val lastUpdatedMillis: Long = 0L,
)

/**
 * Full state of the personalized Music tab feed.
 */
data class PersonalizedFeedState(
    val sections: List<PersonalizedSection> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val accountSession: AccountSession = AccountSession.Loading,
    val globalError: String? = null,
    val lastUpdatedMillis: Long = 0L,
    /** Debug-only diagnostics for each section. Null when diagnostics are disabled (release). */
    val diagnostics: List<SectionDiagnostics> = emptyList(),
)
