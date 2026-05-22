// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.domain.model

/**
 * State representing the current playback status.
 */
data class PlayerState(
    val isPlaying: Boolean = false,
    val currentItem: MediaItem? = null,
    val position: Long = 0,
    val duration: Long = 0,
    val queue: List<MediaItem> = emptyList(),
    val currentIndex: Int = -1,
    val repeatMode: RepeatMode = RepeatMode.NONE,
    val shuffleMode: ShuffleMode = ShuffleMode.OFF,
    val playbackSpeed: Float = 1.0f,
    val isVideo: Boolean = false,
    val isLoading: Boolean = false,
    val isAppInForeground: Boolean = true,
    val sponsorSegments: List<SponsorSegment> = emptyList(),
    val autoplayEnabled: Boolean = true,
    // Keep legacy field for backward compatibility during migration
    val currentSong: Song? = null 
)

data class SponsorSegment(
    val startMs: Long,
    val endMs: Long,
    val category: String
)

enum class RepeatMode {
    NONE, ONE, ALL
}

enum class ShuffleMode {
    OFF, ON
}
