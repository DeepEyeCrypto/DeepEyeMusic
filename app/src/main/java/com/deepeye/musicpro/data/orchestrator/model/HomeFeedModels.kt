// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.orchestrator.model

import com.deepeye.musicpro.account.AccountSession

/**
 * Unified feed state emitted by [HomeFeedOrchestrator] to the UI.
 *
 * Replaces the two parallel feed systems (legacy `HomeFeedRepository` and
 * `PersonalizationRepositoryImpl`) with a single, generation-fenced,
 * account-isolated, thermal-aware state machine. Music and Video share one
 * pipeline; playback routing is decided by [FeedItem.contentType], not by
 * which repository produced the item.
 */
data class HomeFeedState(
    val sections: List<FeedSection> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val accountSession: AccountSession = AccountSession.Loading,
    val globalError: String? = null,
    val lastUpdatedMillis: Long = 0L,
    val generation: Long = 0L,
    val diagnostics: List<SectionDiagnostics> = emptyList(),
)

/** Per-section lifecycle status. */
enum class SectionState {
    /** Not yet requested (feature-flagged off or deferred). */
    Idle,

    /** Fetch in flight; UI shows skeleton. */
    Loading,

    /** Fetch succeeded; items are present. */
    Ready,

    /** Source returned zero items legitimately (e.g. no history yet). */
    Empty,

    /** Network/parse/timeout failure; UI shows partial-error banner + retry chip. */
    Error,

    /** No network and cache is stale or absent; UI shows offline state. */
    Offline,

    /** Auth client returned 401/403; section is hidden until re-auth. */
    Unauthorized,

    /** Thermal tier is HOT/CRITICAL; fetch deferred to next thermal window. */
    ThermalDeferred,
}

/** Content-type classification for every feed item. */
enum class ContentType {
    MUSIC,
    VIDEO,
    LIVE,
    SHORT,
    PODCAST,
    MIXED,
}

/** One rail on the home feed. */
data class FeedSection(
    val id: String,
    val contentType: ContentType,
    val title: String,
    val subtitle: String? = null,
    val sourceLabel: String,
    val explanation: String? = null,
    val items: List<FeedItem> = emptyList(),
    val state: SectionState = SectionState.Loading,
    val error: String? = null,
    val canRetry: Boolean = false,
    val isAccountRequired: Boolean = false,
    val isFromCache: Boolean = false,
    val lastUpdatedMillis: Long = 0L,
    val failureReason: String? = null,
)

/** A single feed item with enough metadata to route playback correctly. */
data class FeedItem(
    val stableId: String,
    val contentType: ContentType,
    val source: String,
    val title: String,
    val subtitle: String? = null,
    val artworkUrl: String? = null,
    val durationMs: Long = 0L,
    val channelId: String? = null,
    val artistName: String? = null,
    val availability: ItemAvailability = ItemAvailability(),
    val playCount: Long = 0L,
    val lastPlayedAt: Long = 0L,
    val completionRatio: Float = 0f,
    val cacheKey: String = "",
    val ttlMillis: Long = 0L,
)

/** Availability flags that affect routing and UI. */
data class ItemAvailability(
    val isLive: Boolean = false,
    val isShort: Boolean = false,
    val isExplicit: Boolean = false,
    val ageRestricted: Boolean = false,
)

/** Per-section diagnostics emitted only when diagnostics are enabled (release: off). */
data class SectionDiagnostics(
    val sectionId: String,
    val state: SectionState,
    val attemptCount: Int,
    val lastError: String?,
    val fromCache: Boolean,
    val latencyMs: Long,
)