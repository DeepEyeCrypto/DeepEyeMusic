// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.player.format

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.Build
import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Diagnostics and capability inspection for hardware and software decoders on the device.
 */
@Singleton
class DeviceCodecCapabilities(
    val supportsAv1: Boolean = true,
    val supportsVp9: Boolean = true,
    val supportsHevc: Boolean = true,
    val maxVideoWidth: Int = 3840,
    val maxVideoHeight: Int = 2160,
    val supportsHdr: Boolean = true
) {

    @Inject
    constructor() : this(
        supportsAv1 = true,
        supportsVp9 = true,
        supportsHevc = true,
        maxVideoWidth = 3840,
        maxVideoHeight = 2160,
        supportsHdr = true
    )

    companion object {
        private const val TAG = "DeviceCodecCaps"
        const val MIME_VIDEO_AV1 = "video/av01"
        const val MIME_VIDEO_VP9 = "video/x-vnd.on2.vp9"
        const val MIME_VIDEO_AVC = "video/avc"
        const val MIME_VIDEO_HEVC = "video/hevc"
        const val MIME_AUDIO_OPUS = "audio/opus"
        const val MIME_AUDIO_AAC = "audio/mp4a-latm"
        const val MIME_AUDIO_FLAC = "audio/flac"
    }

    data class CodecDecoderInfo(
        val mimeType: String,
        val decoderName: String,
        val isHardwareAccelerated: Boolean,
        val isSoftwareOnly: Boolean,
        val maxWidth: Int,
        val maxHeight: Int,
        val maxFrameRate: Double,
        val supportsHdr: Boolean
    )

    data class DeviceCapabilitySummary(
        val hasHwAv1: Boolean,
        val hasHwVp9: Boolean,
        val hasHwAvc: Boolean,
        val hasHwHevc: Boolean,
        val maxVideoWidth: Int,
        val maxVideoHeight: Int,
        val supports4K60: Boolean,
        val supports8K: Boolean,
        val supportsHdr: Boolean,
        val decoders: List<CodecDecoderInfo>
    )

    private var cachedSummary: DeviceCapabilitySummary? = null
    private val decoderCache = ConcurrentHashMap<String, List<CodecDecoderInfo>>()

    fun getDecodersForMime(mimeType: String): List<CodecDecoderInfo> {
        return decoderCache.getOrPut(mimeType) {
            queryDecodersForMime(mimeType)
        }
    }

    fun isHardwareAccelerated(mimeType: String): Boolean {
        val decoders = getDecodersForMime(mimeType)
        if (decoders.isEmpty()) return true
        return decoders.any { it.isHardwareAccelerated }
    }

    fun isFormatSupported(format: DeepEyeFormat): Boolean {
        if (format.isAuto) return true
        val decoders = getDecodersForMime(format.mimeType)
        if (decoders.isEmpty()) return true

        if (format.type == FormatType.VIDEO && format.width > 0 && format.height > 0) {
            return decoders.any { dec ->
                dec.maxWidth >= format.width && dec.maxHeight >= format.height &&
                    (format.frameRate <= 0f || dec.maxFrameRate >= format.frameRate.toDouble())
            }
        }
        return true
    }

    fun getCodecDisplayName(mimeType: String, rawCodecs: String = ""): String {
        val baseName = when {
            mimeType.contains("av01") || rawCodecs.contains("av01") -> "AV1"
            mimeType.contains("vp9") || rawCodecs.contains("vp09") -> "VP9"
            mimeType.contains("avc") || rawCodecs.contains("avc1") -> "AVC"
            mimeType.contains("hevc") || rawCodecs.contains("hvc1") || rawCodecs.contains("hev1") -> "HEVC"
            mimeType.contains("opus") -> "Opus"
            mimeType.contains("mp4a") || mimeType.contains("aac") -> "AAC"
            mimeType.contains("flac") -> "FLAC"
            else -> mimeType.substringAfter("/")
        }
        val isHw = isHardwareAccelerated(mimeType)
        return if (isHw) "$baseName [HW]" else "$baseName [SW]"
    }

    fun getSummary(): DeviceCapabilitySummary {
        cachedSummary?.let { return it }
        val allDecoders = mutableListOf<CodecDecoderInfo>()
        listOf(
            MIME_VIDEO_AV1, MIME_VIDEO_VP9, MIME_VIDEO_AVC, MIME_VIDEO_HEVC,
            MIME_AUDIO_OPUS, MIME_AUDIO_AAC, MIME_AUDIO_FLAC
        ).forEach { mime ->
            allDecoders.addAll(getDecodersForMime(mime))
        }

        val hwAv1 = if (allDecoders.isEmpty()) supportsAv1 else allDecoders.any { it.mimeType == MIME_VIDEO_AV1 && it.isHardwareAccelerated }
        val hwVp9 = if (allDecoders.isEmpty()) supportsVp9 else allDecoders.any { it.mimeType == MIME_VIDEO_VP9 && it.isHardwareAccelerated }
        val hwAvc = if (allDecoders.isEmpty()) true else allDecoders.any { it.mimeType == MIME_VIDEO_AVC && it.isHardwareAccelerated }
        val hwHevc = if (allDecoders.isEmpty()) supportsHevc else allDecoders.any { it.mimeType == MIME_VIDEO_HEVC && it.isHardwareAccelerated }

        val maxWidth = (allDecoders.maxOfOrNull { it.maxWidth }?.takeIf { it > 0 }) ?: maxVideoWidth
        val maxHeight = (allDecoders.maxOfOrNull { it.maxHeight }?.takeIf { it > 0 }) ?: maxVideoHeight
        val supports4K60 = allDecoders.any { it.maxWidth >= 3840 && it.maxHeight >= 2160 && it.maxFrameRate >= 59.0 } || (maxWidth >= 3840)
        val supports8K = allDecoders.any { it.maxWidth >= 7680 && it.maxHeight >= 4320 } || (maxWidth >= 7680)
        val hasHdr = if (allDecoders.isEmpty()) supportsHdr else allDecoders.any { it.supportsHdr }

        val summary = DeviceCapabilitySummary(
            hasHwAv1 = hwAv1,
            hasHwVp9 = hwVp9,
            hasHwAvc = hwAvc,
            hasHwHevc = hwHevc,
            maxVideoWidth = maxWidth,
            maxVideoHeight = maxHeight,
            supports4K60 = supports4K60,
            supports8K = supports8K,
            supportsHdr = hasHdr,
            decoders = allDecoders
        )
        cachedSummary = summary
        return summary
    }
    private fun queryDecodersForMime(mimeType: String): List<CodecDecoderInfo> {
        val result = mutableListOf<CodecDecoderInfo>()
        try {
            val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
            for (info in codecList.codecInfos) {
                if (info.isEncoder) continue
                val types = try { info.supportedTypes } catch (_: Throwable) { continue }
                val matchingType = types.firstOrNull { it.equals(mimeType, ignoreCase = true) } ?: continue

                val caps = try { info.getCapabilitiesForType(matchingType) } catch (_: Throwable) { null }
                val isHw = try { isHwCodec(info) } catch (_: Throwable) { false }
                val isSw = try { isSwOnlyCodec(info) } catch (_: Throwable) { true }

                var maxWidth = 0
                var maxHeight = 0
                var maxFps = 30.0
                var supportsHdr = false

                if (caps != null) {
                    val videoCaps = try { caps.videoCapabilities } catch (_: Throwable) { null }
                    if (videoCaps != null) {
                        maxWidth = try { videoCaps.supportedWidths?.upper ?: 0 } catch (_: Throwable) { 0 }
                        maxHeight = try { videoCaps.supportedHeights?.upper ?: 0 } catch (_: Throwable) { 0 }
                        maxFps = try { (videoCaps.supportedFrameRates?.upper as? Number)?.toDouble() ?: 30.0 } catch (_: Throwable) { 30.0 }
                    }
                    try {
                        caps.profileLevels?.forEach { pl ->
                            if (pl.profile in listOf(
                                    MediaCodecInfo.CodecProfileLevel.VP9Profile2,
                                    MediaCodecInfo.CodecProfileLevel.VP9Profile3,
                                    MediaCodecInfo.CodecProfileLevel.VP9Profile2HDR,
                                    MediaCodecInfo.CodecProfileLevel.VP9Profile3HDR,
                                    MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10,
                                    MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10,
                                    MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10Plus,
                                    MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10,
                                    MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10HDR10,
                                    MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10HDR10Plus
                                )
                            ) {
                                supportsHdr = true
                            }
                        }
                    } catch (_: Throwable) {}
                }

                result.add(
                    CodecDecoderInfo(
                        mimeType = matchingType,
                        decoderName = info.name,
                        isHardwareAccelerated = isHw,
                        isSoftwareOnly = isSw,
                        maxWidth = maxWidth,
                        maxHeight = maxHeight,
                        maxFrameRate = maxFps,
                        supportsHdr = supportsHdr
                    )
                )
            }
        } catch (_: Throwable) {
            // JVM / non-Android environment
        }
        return result
    }

    private fun isHwCodec(info: MediaCodecInfo): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            info.isHardwareAccelerated
        } else {
            val name = info.name.lowercase()
            !name.startsWith("omx.google.") && !name.startsWith("c2.android.") && !name.contains("sw")
        }
    }

    private fun isSwOnlyCodec(info: MediaCodecInfo): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            info.isSoftwareOnly
        } else {
            !isHwCodec(info)
        }
    }


}
