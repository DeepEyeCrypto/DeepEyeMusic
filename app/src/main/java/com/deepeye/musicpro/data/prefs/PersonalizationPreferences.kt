// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.prefs

/**
 * User-controllable settings that shape what the personalized Music tab shows.
 *
 * Every toggle is privacy-safe: it only changes on-device selection and ranking
 * behavior, never what local listening data is shared anywhere.
 */
data class PersonalizationPreferences(
    /** Master toggle for personalized music recommendations. */
    val enablePersonalization: Boolean = true,
    /** Show sections that depend on a connected YouTube account (e.g. Liked Music from account). */
    val enableAccountSections: Boolean = true,
    /** Show sections derived from on-device listening history and affinity. */
    val enableLocalListeningSections: Boolean = true,
    /** Let recent searches influence discovery suggestions. */
    val recentSearchInfluence: Boolean = true,
    /** Show Liked Music section from account. */
    val enableLikedMusic: Boolean = true,
    /** Show New From Subscriptions section from account. */
    val enableSubscriptions: Boolean = true,
    /** Show "Based on your listening" / Local Mix section. */
    val enableLocalMix: Boolean = true,
    /** Show Trending Music section. */
    val enableTrending: Boolean = true,
    /** Region code used for the Trending Music section (e.g. "US", "IN", "GB"). Empty = auto. */
    val trendingRegion: String = "US",
    /** Hide video / non-music content (shorts, podcasts without audio overlap). */
    val hideNonMusicContent: Boolean = true,
    /** Blend a controlled number of discovery candidates into familiar sections. */
    val discoveryBlendCount: Int = 2,
    /** Maximum times an artist may appear at the top of a single section. */
    val maxRepeatedArtistPerSection: Int = 2,
    /** Cooldown duration in hours before recently skipped songs can appear high in recommendations. */
    val recentlySkippedCooldownHours: Int = 24,
    /** Timestamp (epoch millis) of the last manual feed refresh. */
    val lastRefreshMillis: Long = 0L,
)
