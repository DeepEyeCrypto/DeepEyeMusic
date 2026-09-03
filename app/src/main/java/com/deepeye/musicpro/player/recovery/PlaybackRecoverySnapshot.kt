// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.player.recovery

/**
 * In-memory non-sensitive snapshot of player state captured before source replacement/refresh.
 * Contains no stream URLs, cookies, tokens, or raw request data.
 */
data class PlaybackRecoverySnapshot(
    val mediaId: String,
    val title: String,
    val isVideo: Boolean,
    val queueIndex: Int,
    val queueSize: Int,
    val positionMs: Long,
    val durationMs: Long,
    val wasPlaying: Boolean,
    val selectedVideoFormatId: String?,
    val selectedAudioFormatId: String?,
    val qualityMode: String = "AUTO",
    val playbackSpeed: Float = 1.0f,
    val repeatMode: Int = 0,
    val shuffleEnabled: Boolean = false,
    val dspEnabled: Boolean = true,
    val dspPresetId: String? = null,
    val captionsEnabled: Boolean = false,
    val selectedAudioLanguage: String? = null
)
