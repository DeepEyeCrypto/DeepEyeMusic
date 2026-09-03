// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.player.smarttube

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.Build
import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartTubeCodecCapabilityChecker @Inject constructor() {

    data class DecoderInfo(
        val mimeType: String,
        val decoderName: String,
        val isHardware: Boolean,
        val maxWidth: Int,
        val maxHeight: Int,
        val maxFps: Double,
        val supportsHdr: Boolean
    )

    private val cache = ConcurrentHashMap<String, List<DecoderInfo>>()

    fun getDecodersForMime(mimeType: String): List<DecoderInfo> {
        val clean = mimeType.substringBefore(";").trim().lowercase()
        return cache.getOrPut(clean) { queryDecoders(clean) }
    }

    fun evaluateCompatibility(
        mimeType: String?,
        videoCodec: DeepEyeVideoCodec?,
        audioCodec: DeepEyeAudioCodec?,
        width: Int?,
        height: Int?,
        frameRate: Float?,
        dynamicRange: DeepEyeDynamicRange?
    ): Pair<Boolean, String?> {
        val mime = mimeType?.substringBefore(";")?.trim()?.lowercase() ?: return true to null
        if (mime.startsWith("video/")) {
            val target = when (videoCodec) {
                DeepEyeVideoCodec.AV1 -> "video/av01"
                DeepEyeVideoCodec.VP9 -> "video/x-vnd.on2.vp9"
                DeepEyeVideoCodec.AVC -> "video/avc"
                DeepEyeVideoCodec.HEVC -> "video/hevc"
                else -> mime
            }
            val decs = getDecodersForMime(target)
            if (decs.isEmpty() && videoCodec == DeepEyeVideoCodec.AV1) return false to "No AV1 decoder found"
            if (decs.isNotEmpty()) {
                val w = width ?: 0
                val h = height ?: 0
                val fps = frameRate?.toDouble() ?: 0.0
                if (w > 0 && h > 0) {
                    val matching = decs.filter { it.maxWidth >= w && it.maxHeight >= h }
                    if (matching.isEmpty()) {
                        val max = decs.maxByOrNull { it.maxWidth * it.maxHeight }
                        return false to "Exceeds decoder limit (${max?.maxWidth}x${max?.maxHeight})"
                    }
                    if (fps > 30.0 && matching.none { it.maxFps >= fps }) {
                        return false to "FPS exceeds maximum decoder limit"
                    }
                }
            }
        }
        return true to null
    }

    private fun queryDecoders(mime: String): List<DecoderInfo> {
        val result = mutableListOf<DecoderInfo>()
        try {
            val list = MediaCodecList(MediaCodecList.ALL_CODECS)
            for (info in list.codecInfos) {
                if (info.isEncoder) continue
                val match = info.supportedTypes?.firstOrNull { it.equals(mime, ignoreCase = true) } ?: continue
                val caps = try { info.getCapabilitiesForType(match) } catch (_: Throwable) { null }
                val isHw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) info.isHardwareAccelerated else !info.name.lowercase().contains("sw")
                var w = 3840; var h = 2160; var fps = 60.0; var hdr = false
                if (caps != null) {
                    caps.videoCapabilities?.let {
                        w = try { it.supportedWidths?.upper ?: 3840 } catch (_: Throwable) { 3840 }
                        h = try { it.supportedHeights?.upper ?: 2160 } catch (_: Throwable) { 2160 }
                        fps = try { (it.supportedFrameRates?.upper as? Number)?.toDouble() ?: 60.0 } catch (_: Throwable) { 60.0 }
                    }
                }
                result.add(DecoderInfo(match, info.name, isHw, w, h, fps, hdr))
            }
        } catch (e: Throwable) {
            Log.w("SmartTubeCaps", "Query failed: ${e.message}")
        }
        return result
    }
}
