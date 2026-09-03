// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.domain.resolver

import com.deepeye.musicpro.player.smarttube.DeepEyePlaybackFormat

/**
 * Rich resolved playback source containing the direct or manifest URL,
 * active format inventories, and adaptive streaming manifests.
 */
data class ResolvedSource(
    val url: String,
    val isDirectStream: Boolean = true,
    val headers: Map<String, String> = emptyMap(),
    val isVideo: Boolean = false,
    val expiresAt: Long = 0L,
    val videoFormats: List<DeepEyePlaybackFormat> = emptyList(),
    val audioFormats: List<DeepEyePlaybackFormat> = emptyList(),
    val dashManifestUrl: String? = null,
    val hlsManifestUrl: String? = null
)

/**
 * Strategy interface for resolving streaming URLs from third-party media sources (e.g. YouTube).
 * Follows the "SourceResolver" abstraction requested in Stage 4 of AEOS GOD PROMPT.
 */
interface SourceResolver {
    /**
     * Unique identifier for this resolver (e.g. "SmartTubeInnertube", "WebViewBridge").
     */
    val name: String
    
    /**
     * The priority of this resolver. Lower number = higher priority.
     */
    val priority: Int

    /**
     * Attempt to resolve a video ID into a playable stream URL.
     * @param videoId The ID of the media.
     * @param preferVideo If true, requests DASH/HLS manifests. If false, requests M4A/WebM audio-only.
     * @return The direct stream URL, or null if resolution fails.
     */
    suspend fun resolveStreamUrl(videoId: String, preferVideo: Boolean): String? {
        return resolveSource(videoId, preferVideo)?.url
    }

    /**
     * Attempt to resolve a video ID into a rich ResolvedSource with format inventories.
     */
    suspend fun resolveSource(videoId: String, preferVideo: Boolean): ResolvedSource? {
        val url = resolveStreamUrl(videoId, preferVideo) ?: return null
        return ResolvedSource(
            url = url,
            isVideo = preferVideo
        )
    }
}
