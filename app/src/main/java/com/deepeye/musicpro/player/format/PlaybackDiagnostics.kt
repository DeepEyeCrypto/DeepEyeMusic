// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.player.format

import androidx.compose.runtime.Immutable

/**
 * Real-time playback and engine telemetry snapshot for the SmartTube Stats for Nerds HUD.
 */
@Immutable
data class PlaybackDiagnostics(
    val videoId: String = "",
    val mediaTitle: String = "",
    val activeVideoFormat: DeepEyeFormat? = null,
    val activeAudioFormat: DeepEyeFormat? = null,
    val activeVideoDecoder: String = "",
    val activeAudioDecoder: String = "",
    val currentPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val bufferedDurationMs: Long = 0L,
    val bufferedPercentage: Int = 0,
    val playbackSpeed: Float = 1.0f,
    val estimatedBandwidthBps: Long = 0L,
    val droppedFrames: Long = 0L,
    val qualityPreset: QualityPreset = QualityPreset.AUTO,
    val bufferProfile: BufferProfile = BufferProfile.BALANCED,
    val isHardwareAccelerated: Boolean = true,
    val audioSinkSpec: String = "48000Hz • 2ch Float32 • Low-latency DSP",
    val playerStateName: String = "READY"
) {
    val formattedBandwidth: String
        get() {
            if (estimatedBandwidthBps <= 0L) return "Measuring..."
            val mbps = estimatedBandwidthBps / 1_000_000.0
            return String.format(java.util.Locale.US, "%.2f Mbps", mbps)
        }

    val formattedBufferHealth: String
        get() {
            val secs = bufferedDurationMs / 1000.0
            return String.format(java.util.Locale.US, "%.1fs (%d%%)", secs, bufferedPercentage)
        }
}
