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
    val currentLyrics: com.deepeye.musicpro.domain.model.Lyrics? = null,
    // Quality, Codec & Buffer Settings
    val availableVideoFormats: ImmutableList<com.deepeye.musicpro.player.format.DeepEyeFormat> = persistentListOf(),
    val availableAudioFormats: ImmutableList<com.deepeye.musicpro.player.format.DeepEyeFormat> = persistentListOf(),
    val selectedVideoFormat: com.deepeye.musicpro.player.format.DeepEyeFormat? = null,
    val selectedAudioFormat: com.deepeye.musicpro.player.format.DeepEyeFormat? = null,
    val qualityPreset: com.deepeye.musicpro.player.format.QualityPreset = com.deepeye.musicpro.player.format.QualityPreset.AUTO,
    val bufferProfile: com.deepeye.musicpro.player.format.BufferProfile = com.deepeye.musicpro.player.format.BufferProfile.BALANCED,
    val bufferedPercentage: Int = 0,
    val bufferedDurationMs: Long = 0L,
    val droppedFrames: Long = 0L,
    val estimatedBandwidthBps: Long = 0L,
    val activeVideoDecoderName: String = "",
    val activeAudioDecoderName: String = "",
    val isRecovering: Boolean = false,
    val recoveryMessage: String? = null,
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
