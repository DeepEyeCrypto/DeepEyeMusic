// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.components

/**
 * The single data model every neon card surface feeds into [NeonMediaCard].
 *
 * All existing domain models (`HomeVideoItem`, `PersonalizedFeedItem`,
 * `HomeMusicItem`, `LibraryItem`, `VideoItem`, `PlaybackHistoryEntity`,
 * `VideoHistoryEntity`) are mapped to this class via extension functions in
 * `UnifiedCardMappers.kt`. Having one input type lets the composable apply the
 * blank-data hiding rules in a single place instead of reimplementing them per
 * surface.
 *
 * ## Nullability contract
 *
 * - [viewCount] — `null` hides the field; `0` is also treated as unavailable and
 *   hidden, satisfying the unified rule (never render "0 views").
 * - [uploadDate] — `null` or blank hides the field; the literal `"—"` is also
 *   hidden (never render "—").
 * - [channelName] — blank, `"."`, or `"·"` triggers the "DeepEye" fallback text
 *   **and** the avatar fallback thumbnail derived from the video ID.
 * - [thumbnailUrl] / [channelAvatarUrl] — blank triggers the ytimg fallback
 *   URLs derived from [id].
 * - [qualityLabel] — `null` or blank hides the quality badge. Real quality data
 *   only; never derived from an id hash.
 * - [durationMs] — `0` or unknown hides the duration badge (never "0:00").
 */
data class NeonCardData(
    val id: String,
    val title: String,
    val channelName: String? = null,
    val thumbnailUrl: String? = null,
    val channelAvatarUrl: String? = null,
    val viewCount: Long? = null,
    val uploadDate: String? = null,
    val durationMs: Long? = null,
    val qualityLabel: String? = null,
    val isLive: Boolean = false,
    val progressFraction: Float? = null,
    val cardType: NeonCardType = NeonCardType.VIDEO,
    val contentType: String = "video",
)

/**
 * Card shape discriminator. Drives geometry selection inside [NeonMediaCard]
 * so a single composable can serve video rails, music rails, and compact
 * library history rows without a branching `when` at every call site.
 */
enum class NeonCardType {
    VIDEO,
    MUSIC,
    COMPACT,
}