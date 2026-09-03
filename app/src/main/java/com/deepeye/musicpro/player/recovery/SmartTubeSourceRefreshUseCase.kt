// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.player.recovery

import com.deepeye.musicpro.domain.resolver.SourceResolverManager
import com.deepeye.musicpro.player.smarttube.DeepEyePlaybackFormat
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Refreshed source descriptor containing non-sensitive format catalogs and safe fallback metadata.
 */
data class RefreshedPlaybackSource(
    val directUrl: String,
    val isVideo: Boolean,
    val isAdaptive: Boolean,
    val videoFormats: List<DeepEyePlaybackFormat>,
    val audioFormats: List<DeepEyePlaybackFormat>,
    val selectedVideoFormatId: String?,
    val selectedAudioFormatId: String?,
    val fallbackReason: String? = null,
    val safeReason: String = "SmartTube source refreshed successfully"
)

/**
 * Executes SmartTube stream extraction with forceRefresh=true and applies format retention rules.
 */
@Singleton
class SmartTubeSourceRefreshUseCase @Inject constructor(
    private val sourceResolverManager: SourceResolverManager
) {
    suspend fun refreshCurrentSource(
        mediaId: String,
        isVideo: Boolean,
        selectedVideoFormatId: String?,
        selectedAudioFormatId: String?,
        qualityMode: String = "AUTO",
        selectedAudioLanguage: String? = null
    ): Result<RefreshedPlaybackSource> {
        return try {
            val resolved = sourceResolverManager.resolveSource(
                videoId = mediaId,
                preferVideo = isVideo,
                forceRefresh = true
            ) ?: return Result.failure(IllegalStateException("Resolver returned null source"))

            if (resolved.url.isEmpty()) {
                return Result.failure(IllegalStateException("Resolver returned empty stream URL"))
            }

            // Retain video format ID if still present, or pick closest compatible
            var chosenVideoFormatId: String? = null
            var fallbackReason: String? = null

            if (selectedVideoFormatId != null) {
                val match = resolved.videoFormats.firstOrNull { it.stableId == selectedVideoFormatId }
                if (match != null) {
                    chosenVideoFormatId = match.stableId
                } else {
                    val fallback = resolved.videoFormats.firstOrNull { it.isDeviceCompatible }
                    chosenVideoFormatId = fallback?.stableId
                    val label = fallback?.height?.let { "${it}p" } ?: "Auto"
                    fallbackReason = "Selected video format unavailable; fell back to $label"
                }
            }

            // Retain audio format ID if still present, or pick closest compatible
            var chosenAudioFormatId: String? = null
            if (selectedAudioFormatId != null) {
                val match = resolved.audioFormats.firstOrNull { it.stableId == selectedAudioFormatId }
                if (match != null) {
                    chosenAudioFormatId = match.stableId
                } else {
                    val fallback = resolved.audioFormats.firstOrNull { it.isDeviceCompatible }
                    chosenAudioFormatId = fallback?.stableId
                    val label = fallback?.bitrate?.let { "${it / 1000} kbps" } ?: "Auto"
                    val audioFb = "Selected audio format unavailable; fell back to $label"
                    fallbackReason = if (fallbackReason != null) "$fallbackReason, $audioFb" else audioFb
                }
            }

            // Determine effective playback URL (direct stream if manual format selected with streamUrl)
            var effectiveUrl = resolved.url
            if (isVideo && chosenVideoFormatId != null) {
                val targetVFormat = resolved.videoFormats.firstOrNull { it.stableId == chosenVideoFormatId }
                if (!targetVFormat?.streamUrl.isNullOrEmpty()) {
                    effectiveUrl = targetVFormat!!.streamUrl
                }
            } else if (!isVideo && chosenAudioFormatId != null) {
                val targetAFormat = resolved.audioFormats.firstOrNull { it.stableId == chosenAudioFormatId }
                if (!targetAFormat?.streamUrl.isNullOrEmpty()) {
                    effectiveUrl = targetAFormat!!.streamUrl
                }
            }

            Result.success(
                RefreshedPlaybackSource(
                    directUrl = effectiveUrl,
                    isVideo = resolved.isVideo,
                    isAdaptive = !resolved.isDirectStream,
                    videoFormats = resolved.videoFormats,
                    audioFormats = resolved.audioFormats,
                    selectedVideoFormatId = chosenVideoFormatId,
                    selectedAudioFormatId = chosenAudioFormatId,
                    fallbackReason = fallbackReason
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
