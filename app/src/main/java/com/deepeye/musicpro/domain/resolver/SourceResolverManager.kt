// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.domain.resolver

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates multiple SourceResolvers to guarantee playback resilience.
 * Iterates through resolvers based on priority until one succeeds.
 */
@Singleton
class SourceResolverManager @Inject constructor(
    private val resolvers: Set<@JvmSuppressWildcards SourceResolver>
) {
    private val sortedResolvers = resolvers.sortedBy { it.priority }

    private data class CachedSource(
        val source: ResolvedSource,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() - timestamp > 4 * 60 * 60 * 1000L // 4 hours TTL
    }

    private val cache = java.util.concurrent.ConcurrentHashMap<String, CachedSource>()

    private fun getCacheKey(videoId: String, preferVideo: Boolean): String = "${videoId}_${preferVideo}"

    suspend fun resolveSource(videoId: String, preferVideo: Boolean, forceRefresh: Boolean = false): ResolvedSource? {
        val key = getCacheKey(videoId, preferVideo)
        if (forceRefresh) {
            cache.remove(key)
        }
        val cached = cache[key]
        if (cached != null && !cached.isExpired()) {
            Log.i("SourceResolverManager", "🚀 Cache HIT for video $videoId (preferVideo=$preferVideo)")
            return cached.source
        }

        for (resolver in sortedResolvers) {
            try {
                Log.d("SourceResolverManager", "Trying resolver: ${resolver.name} for video $videoId")
                val source = resolver.resolveSource(videoId, preferVideo)
                if (source != null && source.url.isNotEmpty()) {
                    Log.i("SourceResolverManager", "Successfully resolved via ${resolver.name}")
                    cache[key] = CachedSource(source)
                    return source
                }
            } catch (e: Exception) {
                Log.w("SourceResolverManager", "Resolver ${resolver.name} failed: ${e.message}")
            }
        }
        Log.e("SourceResolverManager", "All resolvers failed for video $videoId")
        return null
    }

    suspend fun resolve(videoId: String, preferVideo: Boolean, forceRefresh: Boolean = false): String? {
        return resolveSource(videoId, preferVideo, forceRefresh)?.url
    }
}
