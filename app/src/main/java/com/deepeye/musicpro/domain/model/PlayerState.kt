// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * State representing the current playback status.
 */
@Immutable
data class PlayerState(
    val isPlaying: Boolean = false,
    val currentItem: MediaItem? = null,
    val position: Long = 0,
    val duration: Long = 0,
    val queue: ImmutableList<MediaItem> = persistentListOf(),
    val currentIndex: Int = -1,
    val repeatMode: RepeatMode = RepeatMode.NONE,
    val shuffleMode: ShuffleMode = ShuffleMode.OFF,
    val playbackSpeed: Float = 1.0f,
    val isVideo: Boolean = false,
    val isLoading: Boolean = false,
    val isAppInForeground: Boolean = true,
    val sponsorSegments: ImmutableList<SponsorSegment> = persistentListOf(),
    val autoplayEnabled: Boolean = true,
    // Keep legacy field for backward compatibility during migration
    val currentSong: Song? = null,
)

@Immutable
data class SponsorSegment(
    val startMs: Long,
    val endMs: Long,
    val category: String,
)

enum class RepeatMode {
    NONE,
    ONE,
    ALL,
}

enum class ShuffleMode {
    OFF,
    ON,
}
