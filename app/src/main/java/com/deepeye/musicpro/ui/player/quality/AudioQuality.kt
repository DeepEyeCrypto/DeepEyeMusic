// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.player.quality

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Format
import androidx.media3.exoplayer.ExoPlayer

// ═══════════════════════════════════════════════════
// Data Model
// ═══════════════════════════════════════════════════

/**
 * Represents the audio quality metadata of the currently playing track.
 */
data class AudioQualityInfo(
    val sampleRate: Int = 0,        // e.g., 44100, 48000, 96000
    val bitDepth: Int = 0,          // e.g., 16, 24, 32
    val codec: String = "",         // e.g., "FLAC", "AAC", "OPUS", "MP3"
    val isHiRes: Boolean = false,   // sampleRate > 44100 || bitDepth > 16
    val isDvcActive: Boolean = false,
    val bitrate: Int = 0,           // kbps
) {
    companion object {
        val EMPTY = AudioQualityInfo()

        /**
         * Extract audio quality info from ExoPlayer's current audio format.
         */
        fun fromPlayer(player: ExoPlayer): AudioQualityInfo {
            val format = player.audioFormat ?: return EMPTY

            val sampleRate = format.sampleRate.coerceAtLeast(0)
            val bitDepth = extractBitDepth(format)
            val codec = extractCodecName(format)
            val bitrate = (format.bitrate / 1000).coerceAtLeast(0)

            return AudioQualityInfo(
                sampleRate = sampleRate,
                bitDepth = bitDepth,
                codec = codec,
                isHiRes = sampleRate > 44100 || bitDepth > 16,
                isDvcActive = false, // Updated by DSP engine state
                bitrate = bitrate,
            )
        }

        private fun extractBitDepth(format: Format): Int {
            // pcmEncoding gives bit depth for PCM formats
            return when (format.pcmEncoding) {
                android.media.AudioFormat.ENCODING_PCM_8BIT -> 8
                android.media.AudioFormat.ENCODING_PCM_16BIT -> 16
                android.media.AudioFormat.ENCODING_PCM_24BIT_PACKED -> 24
                android.media.AudioFormat.ENCODING_PCM_32BIT -> 32
                android.media.AudioFormat.ENCODING_PCM_FLOAT -> 32
                else -> {
                    // For compressed formats, infer from codec
                    when {
                        format.sampleMimeType?.contains("flac") == true -> 16 // Default, could be 24
                        format.sampleMimeType?.contains("alac") == true -> 16
                        else -> 16 // Default assumption for compressed
                    }
                }
            }
        }

        private fun extractCodecName(format: Format): String {
            val mime = format.sampleMimeType ?: return "Unknown"
            return when {
                mime.contains("flac") -> "FLAC"
                mime.contains("alac") -> "ALAC"
                mime.contains("opus") -> "OPUS"
                mime.contains("vorbis") -> "Vorbis"
                mime.contains("mp4a") || mime.contains("aac") -> "AAC"
                mime.contains("mpeg") && mime.contains("audio") -> "MP3"
                mime.contains("mp3") -> "MP3"
                mime.contains("wav") || mime.contains("x-wav") -> "WAV"
                mime.contains("ac3") -> "AC3"
                mime.contains("eac3") -> "EAC3"
                mime.contains("dts") -> "DTS"
                mime.contains("pcm") || mime.contains("raw") -> "PCM"
                else -> mime.substringAfterLast("/").uppercase().take(6)
            }
        }
    }
}

// ═══════════════════════════════════════════════════
// Composable Badge
// ═══════════════════════════════════════════════════

/**
 * Premium audio quality badge displaying codec, sample rate, bit depth,
 * and Hi-Res/DVC indicators.
 */
@Composable
fun AudioQualityBadge(
    info: AudioQualityInfo,
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF00D2FF),
) {
    if (info == AudioQualityInfo.EMPTY) return

    val shape = RoundedCornerShape(8.dp)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Codec badge
        if (info.codec.isNotEmpty()) {
            QualityChip(
                text = info.codec,
                color = Color.White.copy(alpha = 0.7f),
            )
        }

        // Sample rate
        if (info.sampleRate > 0) {
            QualityChip(
                text = "${info.sampleRate / 1000}kHz",
                color = Color.White.copy(alpha = 0.6f),
            )
        }

        // Bit depth
        if (info.bitDepth > 0) {
            QualityChip(
                text = "${info.bitDepth}bit",
                color = Color.White.copy(alpha = 0.6f),
            )
        }

        // Hi-Res badge (highlighted)
        if (info.isHiRes) {
            QualityChip(
                text = "Hi-Res",
                color = accentColor,
                highlighted = true,
            )
        }

        // DVC indicator
        if (info.isDvcActive) {
            QualityChip(
                text = "DVC",
                color = Color(0xFF4CAF50),
                highlighted = true,
            )
        }
    }
}

@Composable
private fun QualityChip(
    text: String,
    color: Color,
    highlighted: Boolean = false,
) {
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .then(
                if (highlighted) {
                    Modifier
                        .background(color.copy(alpha = 0.15f), shape)
                        .border(0.5.dp, color.copy(alpha = 0.4f), shape)
                } else {
                    Modifier.background(Color.White.copy(alpha = 0.06f), shape)
                }
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 10.sp,
            fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Medium,
            letterSpacing = 0.5.sp,
        )
    }
}
