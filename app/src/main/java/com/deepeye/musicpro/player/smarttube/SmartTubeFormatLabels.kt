// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.player.smarttube

import java.util.Locale

object SmartTubeFormatLabels {

    fun formatBitrate(bitrate: Long?): String {
        if (bitrate == null || bitrate <= 0) return "Adaptive"
        return if (bitrate >= 1_000_000) {
            String.format(Locale.US, "%.1f Mbps", bitrate / 1_000_000.0)
        } else {
            "${bitrate / 1_000} kbps"
        }
    }

    fun formatResolution(height: Int?, frameRate: Float?): String {
        if (height == null || height <= 0) return "Auto"
        val fps = frameRate?.toInt() ?: 0
        return if (fps > 30) "${height}p$fps" else "${height}p"
    }

    fun formatVideoSummary(format: DeepEyePlaybackFormat?): String {
        if (format == null) return "Auto (Recommended)"
        val res = formatResolution(format.height, format.frameRate)
        val codec = format.videoCodec?.name ?: "Video"
        val dr = if (format.dynamicRange != null && format.dynamicRange != DeepEyeDynamicRange.SDR && format.dynamicRange != DeepEyeDynamicRange.UNKNOWN) {
            " • ${format.dynamicRange.displayName}"
        } else ""
        val br = format.bitrate?.takeIf { it > 0 }?.let { " • ${formatBitrate(it)}" } ?: ""
        return "$res • $codec$dr$br"
    }

    fun formatAudioSummary(format: DeepEyePlaybackFormat?): String {
        if (format == null) return "Auto (Best Quality)"
        val lang = format.languageLabel ?: format.languageTag ?: "Default"
        val isOrig = if (format.isOriginalAudio == true) " (Original)" else ""
        val codec = format.audioCodec?.displayName ?: "Audio"
        val channels = when (format.channelCount) {
            6 -> " • 5.1 Surround"
            1 -> " • Mono"
            else -> " • Stereo"
        }
        val br = format.bitrate?.takeIf { it > 0 }?.let { " • ${formatBitrate(it)}" } ?: ""
        return "$lang$isOrig • $codec$channels$br"
    }
}
