// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.domain.resolver

/**
 * Strategy interface for resolving streaming URLs from third-party media sources (e.g. YouTube).
 * Follows the "SourceResolver" abstraction requested in Stage 4 of AEOS GOD PROMPT.
 */
interface SourceResolver {
    /**
     * Unique identifier for this resolver (e.g. "NewPipe", "Innertube", "WebViewBridge").
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
    suspend fun resolveStreamUrl(videoId: String, preferVideo: Boolean): String?
}
