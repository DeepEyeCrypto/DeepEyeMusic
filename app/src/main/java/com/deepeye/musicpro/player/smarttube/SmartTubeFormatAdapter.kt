// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.player.smarttube

import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartTubeFormatAdapter @Inject constructor(
    private val capabilityChecker: SmartTubeCodecCapabilityChecker
) {

    fun parseVideoCodec(codecs: String?, mimeType: String?): DeepEyeVideoCodec {
        val str = (codecs ?: mimeType ?: "").lowercase()
        return when {
            str.contains("av01") || str.contains("av1") -> DeepEyeVideoCodec.AV1
            str.contains("vp9") || str.contains("vp09") -> DeepEyeVideoCodec.VP9
            str.contains("avc") || str.contains("h264") || str.contains("mp4v") -> DeepEyeVideoCodec.AVC
            str.contains("hevc") || str.contains("hvc1") || str.contains("hev1") || str.contains("h265") -> DeepEyeVideoCodec.HEVC
            else -> DeepEyeVideoCodec.UNKNOWN
        }
    }

    fun parseAudioCodec(codecs: String?, mimeType: String?): DeepEyeAudioCodec {
        val str = (codecs ?: mimeType ?: "").lowercase()
        return when {
            str.contains("opus") -> DeepEyeAudioCodec.OPUS
            str.contains("mp4a") || str.contains("aac") -> DeepEyeAudioCodec.AAC
            str.contains("ec-3") || str.contains("eac3") -> DeepEyeAudioCodec.EAC3
            str.contains("ac-3") || str.contains("ac3") -> DeepEyeAudioCodec.AC3
            else -> DeepEyeAudioCodec.UNKNOWN
        }
    }

    fun parseDynamicRange(colorInfoObj: JSONObject?, qualityLabel: String?): DeepEyeDynamicRange {
        if (qualityLabel?.contains("HDR", ignoreCase = true) == true) return DeepEyeDynamicRange.HDR10
        if (colorInfoObj == null) return DeepEyeDynamicRange.SDR
        val transfer = colorInfoObj.optString("transferCharacteristics", "")
        return when {
            transfer.contains("2084", ignoreCase = true) || transfer.contains("PQ", ignoreCase = true) -> DeepEyeDynamicRange.HDR10
            transfer.contains("HLG", ignoreCase = true) || transfer.contains("ARIB", ignoreCase = true) -> DeepEyeDynamicRange.HLG
            else -> DeepEyeDynamicRange.SDR
        }
    }

    fun adaptRawFormat(
        f: JSONObject,
        url: String?,
        isAdaptive: Boolean,
        currentSelectedTag: Int?
    ): DeepEyePlaybackFormat? {
        val itag = f.optInt("itag", -1)
        if (itag <= 0) return null
        val mime = f.optString("mimeType", "")
        val isVideo = mime.contains("video", ignoreCase = true)
        val isAudio = mime.contains("audio", ignoreCase = true)
        if (!isVideo && !isAudio) return null

        val codecs = if (mime.contains("codecs=\"")) {
            mime.substringAfter("codecs=\"").substringBefore("\"")
        } else ""

        val vCodec = if (isVideo) parseVideoCodec(codecs, mime) else null
        val aCodec = if (isAudio) parseAudioCodec(codecs, mime) else null

        val width = f.optInt("width", 0).takeIf { it > 0 }
        val height = f.optInt("height", 0).takeIf { it > 0 }
        val fps = f.optDouble("fps", 0.0).toFloat().takeIf { it > 0 }
        val bitrate = f.optLong("bitrate", 0L).takeIf { it > 0 } ?: (f.optLong("averageBitrate", 0L).takeIf { it > 0 })
        val qualityLabel = f.optString("qualityLabel").ifEmpty { height?.let { "${it}p" } ?: "" }
        val dynamicRange = if (isVideo) parseDynamicRange(f.optJSONObject("colorInfo"), qualityLabel) else null

        val sampleRate = f.optString("audioSampleRate").toIntOrNull()
        val channels = f.optInt("audioChannels", if (isAudio) 2 else 0).takeIf { it > 0 }

        val audioTrackObj = f.optJSONObject("audioTrack")
        val langTag = audioTrackObj?.optString("id")?.substringBefore(".") ?: audioTrackObj?.optString("languageCode")
        val langLabel = audioTrackObj?.optString("displayName")

        val streamType = when {
            isVideo && isAudio -> DeepEyeStreamType.VIDEO_PROGRESSIVE
            isVideo -> DeepEyeStreamType.VIDEO_ONLY
            isAudio -> DeepEyeStreamType.AUDIO_ONLY
            else -> DeepEyeStreamType.UNKNOWN
        }

        val (compat, reason) = capabilityChecker.evaluateCompatibility(
            mimeType = mime,
            videoCodec = vCodec,
            audioCodec = aCodec,
            width = width,
            height = height,
            frameRate = fps,
            dynamicRange = dynamicRange
        )

        val stableId = "st_${if (isVideo) "v" else "a"}_${itag}_${height ?: bitrate ?: 0}"

        return DeepEyePlaybackFormat(
            stableId = stableId,
            smartTubeFormatId = itag.toString(),
            streamType = streamType,
            container = if (mime.contains("webm")) "webm" else "mp4",
            mimeType = mime,
            videoCodec = vCodec,
            audioCodec = aCodec,
            width = width,
            height = height,
            frameRate = fps,
            bitrate = bitrate,
            dynamicRange = dynamicRange,
            sampleRateHz = sampleRate,
            channelCount = channels,
            languageTag = langTag,
            languageLabel = langLabel,
            isOriginalAudio = audioTrackObj?.optBoolean("audioIsDefault", false),
            isDefault = f.optBoolean("isDefault", false),
            isSelected = (currentSelectedTag == itag),
            isDeviceCompatible = compat,
            incompatibilityReason = reason,
            isVideoOnly = isVideo && !isAudio,
            isAudioOnly = isAudio && !isVideo,
            isProgressive = isVideo && isAudio,
            streamUrl = url
        )
    }

    fun parseStreamingData(
        streamingData: JSONObject,
        urlResolver: (JSONObject) -> String?,
        selectedItag: Int? = null
    ): Pair<List<DeepEyePlaybackFormat>, List<DeepEyePlaybackFormat>> {
        val progressive = streamingData.optJSONArray("formats")
        val adaptive = streamingData.optJSONArray("adaptiveFormats")

        val videoList = mutableListOf<DeepEyePlaybackFormat>()
        val audioList = mutableListOf<DeepEyePlaybackFormat>()

        if (progressive != null) {
            for (i in 0 until progressive.length()) {
                val f = progressive.optJSONObject(i) ?: continue
                val url = urlResolver(f)
                val adapted = adaptRawFormat(f, url, isAdaptive = false, currentSelectedTag = selectedItag) ?: continue
                if (adapted.isVideo) videoList.add(adapted)
                if (adapted.isAudio) audioList.add(adapted)
            }
        }

        if (adaptive != null) {
            for (i in 0 until adaptive.length()) {
                val f = adaptive.optJSONObject(i) ?: continue
                val url = urlResolver(f)
                val adapted = adaptRawFormat(f, url, isAdaptive = true, currentSelectedTag = selectedItag) ?: continue
                if (adapted.isVideo) videoList.add(adapted)
                if (adapted.isAudio) audioList.add(adapted)
            }
        }

        val sortedVideos = videoList.distinctBy { it.stableId }.sortedWith(
            compareByDescending<DeepEyePlaybackFormat> { it.height ?: 0 }
                .thenByDescending { it.frameRate ?: 0f }
                .thenByDescending { it.bitrate ?: 0L }
        )

        val sortedAudio = audioList.distinctBy { it.stableId }.sortedWith(
            compareByDescending<DeepEyePlaybackFormat> { it.bitrate ?: 0L }
                .thenByDescending { it.sampleRateHz ?: 0 }
        )

        return Pair(sortedVideos, sortedAudio)
    }
}
