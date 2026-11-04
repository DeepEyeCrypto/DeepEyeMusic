// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.components

/**
 * The single data model every card surface feeds into [UnifiedMediaCard].
 *
 * All existing domain models (`HomeVideoItem`, `PersonalizedFeedItem`,
 * `VideoItem`, `HomeMusicItem`, `LibraryItem`, `PlaybackHistoryEntity`,
 * `VideoHistoryEntity`) are mapped to this class via extension functions in
 * `UnifiedCardMappers.kt`. Having one input type lets the composable apply the
 * blank-data hiding rules in a single place instead of reimplementing them per
 * surface.
 *
 * ## Nullability contract
 *
 * [viewCount] and [uploadDate] are nullable so the card can distinguish "data
 * exists" from "data unavailable" and hide the field when unavailable:
 *
 *  - [viewCount] — `null` hides the field; `0` is also treated as unavailable
 *    and hidden, satisfying requirement 2.7 (never render "0 views").
 *  - [uploadDate] — `null` or blank hides the field; the literal `"—"` is also
 *    hidden, satisfying requirement 2.8 (never render "—").
 *  - [channelName] — blank, `"."`, or `"·"` triggers the "DeepEye" fallback text
 *    **and** the avatar fallback thumbnail derived from the video ID, per
 *    requirement 2.9.
 *  - [thumbnailUrl] / [channelAvatarUrl] — blank triggers the ytimg fallback
 *    URLs derived from [id] (requirements 2.10 and 3.3).
 *
 * [durationSeconds] is **not** nullable: `0` is a valid value and renders
 * "0:00", preserving the existing For You card behavior.
 *
 * [progressFraction] is optional (`null` = no progress bar). The Continue
 * Watching / History sections set it from completion-percent data
 * (requirement 3.8).
 *
 * @param id video or media identifier; drives ytimg fallback URLs when a
 *   thumbnail or avatar URL is missing.
 * @param title card title, up to 2 lines.
 * @param channelName channel name; blank/`.`/`·` → "DeepEye" fallback.
 * @param thumbnailUrl thumbnail URL; blank → [ytimgThumbnail] fallback.
 * @param channelAvatarUrl avatar URL; blank → [ytimgAvatar] fallback.
 * @param viewCount view count; `null` or `0` → hidden.
 * @param uploadDate upload date text; `null`, blank, or `"—"` → hidden.
 * @param durationSeconds duration in seconds; `0` renders "0:00".
 * @param isLive whether the item is a live stream (drives the LIVE badge).
 * @param progressFraction watch-progress fraction in `[0, 1]`; `null` = no bar.
 */
data class UnifiedCardData(
    val id: String,
    val title: String,
    val channelName: String,
    val thumbnailUrl: String,
    val channelAvatarUrl: String,
    val viewCount: Long?,
    val uploadDate: String?,
    val durationSeconds: Long,
    val isLive: Boolean,
    val progressFraction: Float? = null,
) {
    companion object {
        /**
         * Derives a YouTube thumbnail URL from a video ID.
         *
         * Uses the `hqdefault` size to match the For You reference fallback.
         * Used when [thumbnailUrl] is blank (requirement 2.10).
         */
        fun ytimgThumbnail(id: String): String =
            "https://i.ytimg.com/vi/$id/hqdefault.jpg"

        /**
         * Derives a YouTube avatar URL from a video ID.
         *
         * Uses the `default` (small square) size to match the For You reference
         * fallback. Used when [channelAvatarUrl] is blank (requirement 3.3) and
         * as part of the channel fallback for blank/`.`/`·` names
         * (requirement 2.9).
         */
        fun ytimgAvatar(id: String): String =
            "https://i.ytimg.com/vi/$id/default.jpg"
    }
}
