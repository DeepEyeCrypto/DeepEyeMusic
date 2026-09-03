// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.player.format

import androidx.compose.runtime.Immutable

/**
 * Type of media stream format.
 */
enum class FormatType {
    VIDEO,
    AUDIO
}

/**
 * Predefined quality presets for adaptive stream selection.
 */
enum class QualityPreset(val displayName: String, val description: String) {
    AUTO("Auto (Adaptive)", "Dynamically adapts to device & network speed"),
    DATA_SAVER("Data Saver (480p / Low Bitrate)", "Limits video to 480p and reduces audio bandwidth"),
    BALANCED("Balanced (1080p / Standard)", "Full HD 1080p streaming with balanced Opus/AAC audio"),
    HIGH_QUALITY("High Quality (1440p / 60fps HQ)", "2K Quad-HD & 60fps streaming with high-bitrate audio"),
    ULTRA_HD("Ultra HD (4K / 8K Audiophile)", "Maximum available 4K/8K HDR video and audiophile audio"),
    CUSTOM("Custom (Manual Selection)", "User-defined specific stream format override")
}

/**
 * Playback buffer profiles for network resilience and latency tuning.
 */
enum class BufferProfile(
    val displayName: String,
    val description: String,
    val minBufferMs: Int,
    val maxBufferMs: Int,
    val bufferForPlaybackMs: Int,
    val bufferForPlaybackAfterRebufferMs: Int,
    val backBufferDurationMs: Int
) {
    ULTRA_LOW_LATENCY(
        displayName = "Ultra-Low Latency",
        description = "Instant start (200ms) & snappy seeking; best on fast Wi-Fi",
        minBufferMs = 5000,
        maxBufferMs = 20000,
        bufferForPlaybackMs = 200,
        bufferForPlaybackAfterRebufferMs = 500,
        backBufferDurationMs = 5000
    ),
    BALANCED(
        displayName = "Balanced (Recommended)",
        description = "Optimal balance of start speed (500ms) and buffer safety (50s)",
        minBufferMs = 15000,
        maxBufferMs = 50000,
        bufferForPlaybackMs = 500,
        bufferForPlaybackAfterRebufferMs = 1000,
        backBufferDurationMs = 10000
    ),
    AGGRESSIVE_PRELOAD(
        displayName = "Aggressive Preload",
        description = "Deep preload (up to 3 min) for flaky mobile data and travel",
        minBufferMs = 60000,
        maxBufferMs = 180000,
        bufferForPlaybackMs = 500,
        bufferForPlaybackAfterRebufferMs = 2000,
        backBufferDurationMs = 30000
    ),
    DATA_SAVER(
        displayName = "Data Saver Buffer",
        description = "Limits prefetching to save bandwidth when browsing queues",
        minBufferMs = 8000,
        maxBufferMs = 25000,
        bufferForPlaybackMs = 250,
        bufferForPlaybackAfterRebufferMs = 1000,
        backBufferDurationMs = 5000
    )
}

/**
 * Normalized media stream format model representing a selectable video or audio track.
 */
@Immutable
data class DeepEyeFormat(
    val id: String,
    val groupIndex: Int,
    val trackIndex: Int,
    val type: FormatType,
    val mimeType: String,
    val codecName: String,
    val rawCodecs: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val frameRate: Float = 0.0f,
    val bitrate: Int = 0,
    val audioChannels: Int = 0,
    val audioSampleRate: Int = 0,
    val container: String = "",
    val qualityLabel: String = "",
    val isHdr: Boolean = false,
    val isHardwareAccelerated: Boolean = true,
    val isSupported: Boolean = true,
    val isSelected: Boolean = false,
    val isAuto: Boolean = false
) {
    /**
     * Formatted human-readable label for video tracks (e.g., "1080p60 HDR", "4K (2160p)", "720p").
     */
    val formattedVideoResolution: String
        get() {
            if (isAuto) return "Auto"
            if (height <= 0) return qualityLabel.ifEmpty { "Unknown" }
            val fpsSuffix = if (frameRate >= 50f) "${frameRate.toInt()}" else ""
            val hdrSuffix = if (isHdr) " HDR" else ""
            return when {
                height >= 4320 -> "8K (4320p$fpsSuffix)$hdrSuffix"
                height >= 2160 -> "4K (2160p$fpsSuffix)$hdrSuffix"
                height >= 1440 -> "1440p$fpsSuffix QHD$hdrSuffix"
                height >= 1080 -> "1080p$fpsSuffix FHD$hdrSuffix"
                height >= 720 -> "720p$fpsSuffix HD$hdrSuffix"
                height >= 480 -> "480p SD"
                height >= 360 -> "360p"
                else -> "${height}p$fpsSuffix"
            }
        }

    /**
     * Formatted human-readable label for audio tracks (e.g., "Opus 160 kbps • 48 kHz", "AAC 256 kbps").
     */
    val formattedAudioSpec: String
        get() {
            if (isAuto) return "Auto (Highest Quality)"
            val bitrateStr = if (bitrate > 0) "${bitrate / 1000} kbps" else ""
            val sampleRateStr = if (audioSampleRate > 0) "${audioSampleRate / 1000}.${(audioSampleRate % 1000) / 100} kHz" else ""
            val channelStr = when (audioChannels) {
                1 -> "Mono"
                2 -> "Stereo"
                6 -> "5.1 Surround"
                else -> if (audioChannels > 0) "${audioChannels}ch" else ""
            }
            return listOf(codecName, bitrateStr, sampleRateStr, channelStr)
                .filter { it.isNotEmpty() }
                .joinToString(" • ")
        }

    /**
     * Bitrate formatted nicely (e.g., "4.2 Mbps", "160 kbps").
     */
    val formattedBitrate: String
        get() {
            if (bitrate <= 0) return ""
            return if (bitrate >= 1_000_000) {
                String.format(java.util.Locale.US, "%.1f Mbps", bitrate / 1_000_000.0)
            } else {
                "${bitrate / 1000} kbps"
            }
        }
}
