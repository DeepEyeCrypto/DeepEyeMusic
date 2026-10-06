// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.domain.recommendation

import com.deepeye.musicpro.data.source.remote.youtube.SmartTubeEngine
import com.deepeye.musicpro.data.source.remote.youtube.YoutubeRemoteDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Pure InnerTube & SmartTube TVHTML5 Content Fetcher.
 * Zero dependency on deprecated YouTube Data API v3 endpoints.
 */
@Singleton
class ContentFetcher
@Inject
constructor(
    private val smartTubeEngine: SmartTubeEngine,
    private val youtubeDs: Provider<YoutubeRemoteDataSource>,
) {
    suspend fun getRelatedVideos(
        videoId: String,
        maxResults: Int = 20,
        isVideo: Boolean = false
    ): List<VideoItem> = withContext(Dispatchers.IO) {
        try {
            val auto = smartTubeEngine.getAlgorithmicNext(videoId)
            if (auto != null && auto.videoId.isNotBlank()) {
                val primaryItem = VideoItem(
                    videoId = auto.videoId,
                    title = auto.title,
                    artist = auto.artist,
                    channelId = "",
                    duration = auto.durationSeconds.toString(),
                    genre = "SmartTube-Algorithmic"
                )
                val fallbackList = getRelatedVideosFallback(videoId, maxResults - 1)
                return@withContext (listOf(primaryItem) + fallbackList).distinctBy { it.videoId }
            }
        } catch (e: Exception) {
            android.util.Log.w("ContentFetcher", "SmartTube related fetch fallback: ${e.message}")
        }
        getRelatedVideosFallback(videoId, maxResults)
    }

    private suspend fun getRelatedVideosFallback(videoId: String, maxResults: Int): List<VideoItem> {
        return try {
            val ds = youtubeDs.get()
            ds.getRelatedVideos(videoId).take(maxResults).map { item ->
                VideoItem(
                    videoId = item.id,
                    title = item.title,
                    artist = item.channelName,
                    channelId = "",
                    duration = "${item.duration / 60}:${item.duration % 60}",
                    genre = ""
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("ContentFetcher", "getRelatedVideosFallback failed for $videoId: ${e.message}")
            emptyList()
        }
    }

    // ── Search by artist name + genre via SmartTube TVHTML5 ──
    suspend fun searchByArtist(
        artistName: String,
        maxResults: Int = 15,
    ): List<VideoItem> = withContext(Dispatchers.IO) {
        val query = "$artistName new songs official"
        searchByQuery(query, maxResults)
    }

    suspend fun searchByQuery(
        query: String,
        maxResults: Int = 15,
    ): List<VideoItem> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val smartTubeResult = smartTubeEngine.searchTracks(query)
            if (smartTubeResult.isSuccess) {
                val items = smartTubeResult.getOrNull().orEmpty()
                if (items.isNotEmpty()) {
                    return@withContext items.take(maxResults).map { item ->
                        VideoItem(
                            videoId = item.videoId ?: item.id,
                            title = item.title,
                            artist = item.artist ?: item.subtitle,
                            channelId = item.channelId ?: "",
                            duration = "3:30",
                            genre = ""
                        )
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("ContentFetcher", "SmartTube search fallback: ${e.message}")
        }
        searchByQueryFallback(query, maxResults)
    }

    private suspend fun searchByQueryFallback(query: String, maxResults: Int): List<VideoItem> {
        return try {
            val ds = youtubeDs.get()
            ds.searchVideos(query).take(maxResults).map { item ->
                VideoItem(
                    videoId = item.id,
                    title = item.title,
                    artist = item.channelName,
                    channelId = item.channelId,
                    duration = "${item.duration / 60}:${item.duration % 60}",
                    genre = ""
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("ContentFetcher", "searchByQueryFallback failed for $query: ${e.message}")
            emptyList()
        }
    }

    // ── Trending music via TVHTML5 InnerTube ──
    suspend fun getTrendingMusic(
        regionCode: String = "IN",
        maxResults: Int = 20,
    ): List<VideoItem> = withContext(Dispatchers.IO) {
        getTrendingMusicFallback(maxResults)
    }

    private suspend fun getTrendingMusicFallback(maxResults: Int): List<VideoItem> {
        return try {
            val ds = youtubeDs.get()
            ds.getTrending().take(maxResults).map { item ->
                VideoItem(
                    videoId = item.id,
                    title = item.title,
                    artist = item.channelName,
                    channelId = item.channelId,
                    duration = "${item.duration / 60}:${item.duration % 60}",
                    genre = ""
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("ContentFetcher", "getTrendingMusicFallback failed: ${e.message}")
            emptyList()
        }
    }
}
