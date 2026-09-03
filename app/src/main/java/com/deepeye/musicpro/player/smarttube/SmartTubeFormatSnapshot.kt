// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.player.smarttube

import androidx.compose.runtime.Immutable

/**
 * Selection strategy active for the current playback session.
 */
enum class SelectionMode {
    AUTOMATIC,
    SMARTTUBE_PRESET,
    MANUAL
}

/**
 * Single observable snapshot of all available and selected SmartTube formats for the current media.
 */
@Immutable
data class SmartTubeFormatSnapshot(
    val mediaKey: String = "",
    val title: String? = null,
    val videoFormats: List<DeepEyePlaybackFormat> = emptyList(),
    val audioFormats: List<DeepEyePlaybackFormat> = emptyList(),
    val currentVideoFormatId: String? = null,
    val currentAudioFormatId: String? = null,
    val selectionMode: SelectionMode = SelectionMode.AUTOMATIC,
    val videoQualityPreset: VideoQualityPreset = VideoQualityPreset.AUTO,
    val videoCodecPreference: VideoCodecPreference = VideoCodecPreference.AUTO,
    val audioLanguagePreference: AudioLanguagePreference = AudioLanguagePreference.ORIGINAL,
    val audioQualityPreference: AudioQualityPreference = AudioQualityPreference.AUTO,
    val audioCodecPreference: AudioCodecPreference = AudioCodecPreference.AUTO,
    val channelPreference: ChannelPreference = ChannelPreference.AUTO,
    val isLoading: Boolean = false,
    val lastError: String? = null
) {
    val activeVideoFormat: DeepEyePlaybackFormat?
        get() = videoFormats.firstOrNull { it.isSelected }
            ?: videoFormats.firstOrNull { it.stableId == currentVideoFormatId }
            ?: videoFormats.firstOrNull { it.isDefault }

    val activeAudioFormat: DeepEyePlaybackFormat?
        get() = audioFormats.firstOrNull { it.isSelected }
            ?: audioFormats.firstOrNull { it.stableId == currentAudioFormatId }
            ?: audioFormats.firstOrNull { it.isDefault }
}
