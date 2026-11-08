// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.components

import com.deepeye.musicpro.data.db.PlaybackHistoryEntity
import com.deepeye.musicpro.data.db.VideoHistoryEntity
import com.deepeye.musicpro.domain.model.home.HomeMusicItem
import com.deepeye.musicpro.domain.model.home.HomeVideoItem
import com.deepeye.musicpro.domain.model.library.LibraryItem
import com.deepeye.musicpro.domain.model.personalization.PersonalizedFeedItem
import com.deepeye.musicpro.domain.recommendation.VideoItem

/**
 * Maps every domain model in the app to [NeonCardData] so [NeonMediaCard] can
 * apply the blank-data hiding rules in one place.
 *
 * ## Unit conventions
 *
 * - [HomeVideoItem.duration] and [HomeMusicItem.duration] are **seconds**
 *   (see `AuthenticatedYouTubeClient` — `duration = durationSeconds`). They are
 *   multiplied by 1000 to reach [NeonCardData.durationMs].
 * - [VideoItem.duration] is a **String** ("m:ss" / "h:mm:ss"). It is parsed to
 *   milliseconds and hidden when unparseable.
 * - [PersonalizedFeedItem.durationMs] is already milliseconds.
 * - [PlaybackHistoryEntity.totalDurationMs] / [VideoHistoryEntity.durationMs]
 *   are already milliseconds.
 */
fun HomeVideoItem.toNeonCardData(
    progressFraction: Float? = null,
    contentType: String = "video",
): NeonCardData =
    NeonCardData(
        id = id,
        title = title,
        channelName = channelName,
        thumbnailUrl = thumbnailUrl,
        channelAvatarUrl = channelAvatarUrl,
        viewCount = viewCount.takeIf { it > 0 },
        uploadDate = uploadDate.takeIf { it.isNotBlank() && it != "—" },
        durationMs = duration.takeIf { it > 0L }?.times(1000L),
        isLive = isLive,
        progressFraction = progressFraction,
        cardType = NeonCardType.VIDEO,
        contentType = contentType,
    )

fun HomeMusicItem.toNeonCardData(
    progressFraction: Float? = null,
    contentType: String = "music",
): NeonCardData =
    NeonCardData(
        id = id,
        title = title,
        channelName = artist,
        thumbnailUrl = thumbnailUrl,
        viewCount = playCount.takeIf { it > 0 },
        durationMs = duration.takeIf { it > 0L }?.times(1000L),
        cardType = NeonCardType.MUSIC,
        contentType = contentType,
        progressFraction = progressFraction,
    )

fun PersonalizedFeedItem.toNeonCardData(
    progressFraction: Float? = null,
    contentType: String = "music",
): NeonCardData =
    NeonCardData(
        id = id,
        title = title,
        channelName = artist,
        thumbnailUrl = artworkUrl,
        viewCount = playCount.takeIf { it > 0 },
        durationMs = durationMs.takeIf { it > 0L },
        // Real provenance badge only — never id-hash derived.
        qualityLabel = sourceBadge?.takeIf { it.isNotBlank() },
        cardType = NeonCardType.MUSIC,
        contentType = contentType,
        progressFraction = progressFraction,
    )

fun LibraryItem.toNeonCardData(
    progressFraction: Float? = null,
    contentType: String = "library",
): NeonCardData =
    NeonCardData(
        id = id,
        title = title,
        channelName = artist,
        thumbnailUrl = artworkUrl,
        viewCount = playCount.toLong().takeIf { it > 0 },
        durationMs = null,
        cardType = NeonCardType.COMPACT,
        contentType = contentType,
        progressFraction = progressFraction,
    )

fun VideoItem.toNeonCardData(
    progressFraction: Float? = null,
    contentType: String = "video",
): NeonCardData =
    NeonCardData(
        id = videoId,
        title = title,
        channelName = artist,
        // VideoItem carries no thumbnail URL of its own — derive the YouTube
        // hqdefault URL from the id so the card renders a real thumbnail
        // instead of falling back to a blank/default frame.
        thumbnailUrl = "https://i.ytimg.com/vi/${videoId}/hqdefault.jpg",
        durationMs = parseDurationStringMs(duration),
        cardType = NeonCardType.VIDEO,
        contentType = contentType,
        progressFraction = progressFraction,
    )

fun PlaybackHistoryEntity.toNeonCardData(
    progressFraction: Float? = null,
    contentType: String = "history",
): NeonCardData =
    NeonCardData(
        id = mediaId,
        title = title,
        channelName = artist,
        thumbnailUrl = artworkUri,
        durationMs = totalDurationMs.takeIf { it > 0L },
        progressFraction = progressFraction,
        cardType = NeonCardType.COMPACT,
        contentType = contentType,
    )

fun VideoHistoryEntity.toNeonCardData(
    progressFraction: Float? = null,
    contentType: String = "history",
): NeonCardData =
    NeonCardData(
        id = videoId,
        title = title,
        channelName = null,
        thumbnailUrl = thumbnailUri,
        durationMs = durationMs.takeIf { it > 0L },
        progressFraction = progressFraction,
        cardType = NeonCardType.COMPACT,
        contentType = contentType,
    )

/** Parse a "m:ss" or "h:mm:ss" duration string into milliseconds; null when
 *  the string is blank or unparseable so the badge hides cleanly. */
private fun parseDurationStringMs(raw: String): Long? {
    if (raw.isBlank()) return null
    val parts = raw.trim().split(':')
    return try {
        when (parts.size) {
            2 -> {
                val m = parts[0].toLong()
                val s = parts[1].toLong()
                (m * 60 + s) * 1000L
            }
            3 -> {
                val h = parts[0].toLong()
                val m = parts[1].toLong()
                val s = parts[2].toLong()
                (h * 3600 + m * 60 + s) * 1000L
            }
            else -> null
        }
    } catch (_: NumberFormatException) {
        null
    }
}
