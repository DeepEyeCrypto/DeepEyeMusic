// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.source.remote.youtube.resolver

import com.deepeye.musicpro.data.source.remote.youtube.YoutubeRemoteDataSource
import com.deepeye.musicpro.domain.resolver.ResolvedSource
import com.deepeye.musicpro.domain.resolver.SourceResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Stage 4 Resolver: Fallback mechanism using the internal WebView bridge.
 */
class WebViewBridgeResolver @Inject constructor(
    private val youtubeRemoteDataSource: YoutubeRemoteDataSource
) : SourceResolver {
    
    override val name: String = "WebViewBridge"
    
    override val priority: Int = 100 // Lowest priority fallback

    override suspend fun resolveSource(videoId: String, preferVideo: Boolean): ResolvedSource? {
        return withContext(Dispatchers.IO) {
            val result = youtubeRemoteDataSource.getStreamUrl(
                videoId = videoId, 
                preferVideo = preferVideo, 
                onLayerFallback = {}
            ) ?: return@withContext null

            android.util.Log.d("DeepEyeHQ", "event=bridge_result videoCount=${result.videoFormats.size} audioCount=${result.audioFormats.size}")

            ResolvedSource(
                url = result.url,
                isDirectStream = !result.isAdaptive,
                headers = result.headers,
                isVideo = result.isVideo,
                expiresAt = result.expiresAt,
                videoFormats = result.videoFormats,
                audioFormats = result.audioFormats,
                dashManifestUrl = result.dashManifestUrl,
                hlsManifestUrl = result.hlsManifestUrl
            )
        }
    }

    override suspend fun resolveStreamUrl(videoId: String, preferVideo: Boolean): String? {
        return resolveSource(videoId, preferVideo)?.url
    }
}
