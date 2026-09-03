// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.player.format

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Intelligent track selection and quality control engine.
 */
@Singleton
class QualitySelectionEngine @Inject constructor(
    private val deviceCodecCapabilities: DeviceCodecCapabilities
) {

    fun extractVideoFormats(tracks: Tracks): List<DeepEyeFormat> {
        val result = mutableListOf<DeepEyeFormat>()
        var groupIndex = 0

        for (group in tracks.groups) {
            if (group.type == C.TRACK_TYPE_VIDEO) {
                for (trackIndex in 0 until group.length) {
                    val format = group.getTrackFormat(trackIndex)
                    val isSelected = group.isTrackSelected(trackIndex)
                    val isSupported = group.isTrackSupported(trackIndex)

                    val mimeType = format.sampleMimeType ?: format.containerMimeType ?: MimeTypes.VIDEO_UNKNOWN
                    val isHw = deviceCodecCapabilities.isHardwareAccelerated(mimeType)
                    val codecName = deviceCodecCapabilities.getCodecDisplayName(mimeType, format.codecs ?: "")

                    val h = if (format.height != Format.NO_VALUE) format.height else 0
                    val qLabel = format.label?.takeIf { it.isNotBlank() } ?: (if (h > 0) "${h}p" else "")

                    val deepEyeFormat = DeepEyeFormat(
                        id = format.id ?: "v_${groupIndex}_$trackIndex",
                        groupIndex = groupIndex,
                        trackIndex = trackIndex,
                        type = FormatType.VIDEO,
                        mimeType = mimeType,
                        codecName = codecName,
                        rawCodecs = format.codecs ?: "",
                        width = if (format.width != Format.NO_VALUE) format.width else 0,
                        height = h,
                        frameRate = if (format.frameRate != Format.NO_VALUE.toFloat()) format.frameRate else 0f,
                        bitrate = if (format.bitrate != Format.NO_VALUE) format.bitrate else 0,
                        container = format.containerMimeType?.substringAfter("/") ?: "mp4",
                        qualityLabel = qLabel,
                        isHdr = isFormatHdr(format),
                        isHardwareAccelerated = isHw,
                        isSupported = isSupported,
                        isSelected = isSelected,
                        isAuto = false
                    )
                    result.add(deepEyeFormat)
                }
            }
            groupIndex++
        }

        val sorted = result.sortedWith(
            compareByDescending<DeepEyeFormat> { it.height }
                .thenByDescending { it.frameRate }
                .thenByDescending { it.bitrate }
        )

        val isAnySelected = sorted.any { it.isSelected }
        val autoFormat = DeepEyeFormat(
            id = "video_auto",
            groupIndex = -1,
            trackIndex = -1,
            type = FormatType.VIDEO,
            mimeType = "video/adaptive",
            codecName = "Adaptive Auto",
            qualityLabel = "Auto",
            isAuto = true,
            isSelected = !isAnySelected || sorted.all { !it.isSelected }
        )

        return listOf(autoFormat) + sorted
    }

    fun extractAudioFormats(tracks: Tracks): List<DeepEyeFormat> {
        val result = mutableListOf<DeepEyeFormat>()
        var groupIndex = 0

        for (group in tracks.groups) {
            if (group.type == C.TRACK_TYPE_AUDIO) {
                for (trackIndex in 0 until group.length) {
                    val format = group.getTrackFormat(trackIndex)
                    val isSelected = group.isTrackSelected(trackIndex)
                    val isSupported = group.isTrackSupported(trackIndex)

                    val mimeType = format.sampleMimeType ?: format.containerMimeType ?: MimeTypes.AUDIO_UNKNOWN
                    val isHw = deviceCodecCapabilities.isHardwareAccelerated(mimeType)
                    val codecName = deviceCodecCapabilities.getCodecDisplayName(mimeType, format.codecs ?: "")
                    val br = if (format.bitrate != Format.NO_VALUE) format.bitrate else 0
                    val qLabel = format.label?.takeIf { it.isNotBlank() } ?: (if (br > 0) "${br / 1000} kbps" else "")

                    val deepEyeFormat = DeepEyeFormat(
                        id = format.id ?: "a_${groupIndex}_$trackIndex",
                        groupIndex = groupIndex,
                        trackIndex = trackIndex,
                        type = FormatType.AUDIO,
                        mimeType = mimeType,
                        codecName = codecName,
                        rawCodecs = format.codecs ?: "",
                        bitrate = br,
                        audioChannels = if (format.channelCount != Format.NO_VALUE) format.channelCount else 2,
                        audioSampleRate = if (format.sampleRate != Format.NO_VALUE) format.sampleRate else 48000,
                        container = format.containerMimeType?.substringAfter("/") ?: "m4a",
                        qualityLabel = qLabel,
                        isHardwareAccelerated = isHw,
                        isSupported = isSupported,
                        isSelected = isSelected,
                        isAuto = false
                    )
                    result.add(deepEyeFormat)
                }
            }
            groupIndex++
        }

        val sorted = result.sortedWith(
            compareByDescending<DeepEyeFormat> { it.bitrate }
                .thenByDescending { it.audioSampleRate }
        )

        val isAnySelected = sorted.any { it.isSelected }
        val autoFormat = DeepEyeFormat(
            id = "audio_auto",
            groupIndex = -1,
            trackIndex = -1,
            type = FormatType.AUDIO,
            mimeType = "audio/adaptive",
            codecName = "Auto (Highest Quality)",
            qualityLabel = "Auto",
            isAuto = true,
            isSelected = !isAnySelected || sorted.all { !it.isSelected }
        )

        return listOf(autoFormat) + sorted
    }

    fun applyPreset(player: ExoPlayer, preset: QualityPreset) {
        val currentParams = player.trackSelectionParameters
        val builder = currentParams.buildUpon()
        builder.clearOverridesOfType(C.TRACK_TYPE_VIDEO)

        when (preset) {
            QualityPreset.AUTO -> {
                builder.clearVideoSizeConstraints()
                builder.setMaxVideoBitrate(Int.MAX_VALUE)
                builder.setMaxVideoFrameRate(Int.MAX_VALUE)
            }
            QualityPreset.DATA_SAVER -> {
                builder.setMaxVideoSize(854, 480)
                builder.setMaxVideoBitrate(1_200_000)
                builder.setMaxVideoFrameRate(30)
            }
            QualityPreset.BALANCED -> {
                builder.setMaxVideoSize(1920, 1080)
                builder.setMaxVideoBitrate(6_000_000)
                builder.setMaxVideoFrameRate(60)
            }
            QualityPreset.HIGH_QUALITY -> {
                builder.setMaxVideoSize(2560, 1440)
                builder.setMaxVideoBitrate(15_000_000)
                builder.setMaxVideoFrameRate(60)
            }
            QualityPreset.ULTRA_HD -> {
                builder.clearVideoSizeConstraints()
                builder.setMaxVideoBitrate(Int.MAX_VALUE)
                builder.setMaxVideoFrameRate(Int.MAX_VALUE)
            }
            QualityPreset.CUSTOM -> { }
        }
        player.trackSelectionParameters = builder.build()
    }

    fun selectVideoFormat(player: ExoPlayer, format: DeepEyeFormat) {
        val builder = player.trackSelectionParameters.buildUpon()
        if (format.isAuto || format.groupIndex < 0) {
            builder.clearOverridesOfType(C.TRACK_TYPE_VIDEO)
            builder.clearVideoSizeConstraints()
            builder.setMaxVideoBitrate(Int.MAX_VALUE)
        } else {
            val tracks = player.currentTracks
            var realGroupIndex = 0
            var matched = false
            for (group in tracks.groups) {
                if (group.type == C.TRACK_TYPE_VIDEO) {
                    if (realGroupIndex == format.groupIndex && format.trackIndex < group.length) {
                        val override = TrackSelectionOverride(group.mediaTrackGroup, format.trackIndex)
                        builder.clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                        builder.addOverride(override)
                        matched = true
                        break
                    }
                    if (!matched) {
                        for (tIdx in 0 until group.length) {
                            val tf = group.getTrackFormat(tIdx)
                            if (tf.height == format.height && (format.bitrate == 0 || Math.abs(tf.bitrate - format.bitrate) < 500000)) {
                                val override = TrackSelectionOverride(group.mediaTrackGroup, tIdx)
                                builder.clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                                builder.addOverride(override)
                                matched = true
                                break
                            }
                        }
                    }
                }
                realGroupIndex++
            }
        }
        player.trackSelectionParameters = builder.build()
    }

    fun selectAudioFormat(player: ExoPlayer, format: DeepEyeFormat) {
        val builder = player.trackSelectionParameters.buildUpon()
        if (format.isAuto || format.groupIndex < 0) {
            builder.clearOverridesOfType(C.TRACK_TYPE_AUDIO)
        } else {
            val tracks = player.currentTracks
            var realGroupIndex = 0
            var matched = false
            for (group in tracks.groups) {
                if (group.type == C.TRACK_TYPE_AUDIO) {
                    if (realGroupIndex == format.groupIndex && format.trackIndex < group.length) {
                        val override = TrackSelectionOverride(group.mediaTrackGroup, format.trackIndex)
                        builder.clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                        builder.addOverride(override)
                        matched = true
                        break
                    }
                    if (!matched) {
                        for (tIdx in 0 until group.length) {
                            val tf = group.getTrackFormat(tIdx)
                            if (tf.bitrate == format.bitrate || (format.bitrate > 0 && Math.abs(tf.bitrate - format.bitrate) < 50000)) {
                                val override = TrackSelectionOverride(group.mediaTrackGroup, tIdx)
                                builder.clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                                builder.addOverride(override)
                                matched = true
                                break
                            }
                        }
                    }
                }
                realGroupIndex++
            }
        }
        player.trackSelectionParameters = builder.build()
    }

    private fun isFormatHdr(format: Format): Boolean {
        val colorInfo = format.colorInfo ?: return false
        return colorInfo.colorTransfer == C.COLOR_TRANSFER_ST2084 ||
            colorInfo.colorTransfer == C.COLOR_TRANSFER_HLG
    }

    companion object {
        fun selectVideoFormat(
            available: List<VideoTrackFormat>,
            preset: QualityPreset,
            capabilities: DeviceCodecCapabilities
        ): VideoTrackFormat? {
            val supported = filterSupportedVideoFormats(available, capabilities)
            if (supported.isEmpty()) return available.firstOrNull()

            return when (preset) {
                QualityPreset.AUTO, QualityPreset.ULTRA_HD, QualityPreset.HIGH_QUALITY -> {
                    supported.maxByOrNull { estimateQualityScore(it, capabilities.supportsAv1, capabilities.supportsVp9) }
                }
                QualityPreset.DATA_SAVER -> {
                    supported.minByOrNull { it.bitrate.takeIf { b -> b > 0 } ?: (it.height * 1000) }
                }
                QualityPreset.BALANCED -> {
                    val filteredByHeight = supported.filter { it.height <= 1080 }
                    (if (filteredByHeight.isNotEmpty()) filteredByHeight else supported)
                        .maxByOrNull { estimateQualityScore(it, capabilities.supportsAv1, capabilities.supportsVp9) }
                }
                QualityPreset.CUSTOM -> supported.firstOrNull()
            }
        }

        fun selectAudioFormat(
            available: List<AudioTrackFormat>,
            preset: QualityPreset
        ): AudioTrackFormat? {
            if (available.isEmpty()) return null
            return when (preset) {
                QualityPreset.DATA_SAVER -> available.minByOrNull { it.bitrate }
                else -> available.maxByOrNull { it.bitrate }
            }
        }

        fun filterSupportedVideoFormats(
            formats: List<VideoTrackFormat>,
            capabilities: DeviceCodecCapabilities
        ): List<VideoTrackFormat> {
            return formats.filter { format ->
                val widthOk = capabilities.maxVideoWidth <= 0 || format.width <= capabilities.maxVideoWidth
                val heightOk = capabilities.maxVideoHeight <= 0 || format.height <= capabilities.maxVideoHeight
                val codecOk = when {
                    format.sampleMimeType.contains("av01", ignoreCase = true) || format.codecs.contains("av01", ignoreCase = true) -> capabilities.supportsAv1
                    format.sampleMimeType.contains("vp9", ignoreCase = true) || format.codecs.contains("vp09", ignoreCase = true) -> capabilities.supportsVp9
                    format.sampleMimeType.contains("hevc", ignoreCase = true) || format.codecs.contains("hvc1", ignoreCase = true) || format.codecs.contains("hev1", ignoreCase = true) -> capabilities.supportsHevc
                    else -> true
                }
                val hdrOk = !format.isHdr || capabilities.supportsHdr
                widthOk && heightOk && codecOk && hdrOk
            }
        }

        fun estimateQualityScore(
            format: VideoTrackFormat,
            isAv1Supported: Boolean,
            isVp9Supported: Boolean
        ): Long {
            val codecMultiplier = when {
                (format.sampleMimeType.contains("av01", ignoreCase = true) || format.codecs.contains("av01", ignoreCase = true)) && isAv1Supported -> 1.4
                (format.sampleMimeType.contains("vp9", ignoreCase = true) || format.codecs.contains("vp09", ignoreCase = true)) && isVp9Supported -> 1.2
                else -> 1.0
            }
            val resScore = format.width.toLong() * format.height.toLong()
            val fpsScore = if (format.fps > 30f) 1.2 else 1.0
            return (resScore * fpsScore * codecMultiplier + (format.bitrate / 1000)).toLong()
        }
    }
}

data class VideoTrackFormat(
    val id: String,
    val label: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val fps: Float = 30.0f,
    val bitrate: Int = 0,
    val sampleMimeType: String = "",
    val codecs: String = "",
    val isHdr: Boolean = false
)

data class AudioTrackFormat(
    val id: String,
    val label: String = "",
    val language: String? = null,
    val bitrate: Int = 0,
    val sampleRate: Int = 48000,
    val channelCount: Int = 2,
    val sampleMimeType: String = "",
    val codecs: String = "",
    val isDefault: Boolean = false
)
