// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.musicpro.data.source.remote.youtube.SmartTubeEngine
import com.deepeye.musicpro.domain.recommendation.ContentFetcher
import com.deepeye.musicpro.domain.recommendation.RecommendationResult
import com.deepeye.musicpro.domain.recommendation.RecommendationRow
import com.deepeye.musicpro.domain.recommendation.VideoItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecommendationViewModel @Inject constructor(
    private val smartTubeEngine: SmartTubeEngine,
    private val contentFetcher: ContentFetcher,
    private val cacheManager: com.deepeye.musicpro.data.cache.CacheManager,
    private val sourceResolverManager: com.deepeye.musicpro.domain.resolver.SourceResolverManager,
) : ViewModel() {
    private val _recommendations = MutableStateFlow<RecommendationResult?>(null)
    val recommendations = _recommendations.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    init {
        loadRecommendations()
    }

    fun loadRecommendations() {
        viewModelScope.launch {
            _error.value = null
            val cached = cacheManager.loadCachedRecommendations()
            if (cached != null) {
                _recommendations.value = cached
                prefetchFirstRecommendedTracks(cached)
            } else {
                _isRefreshing.value = true
            }

            try {
                _isRefreshing.value = true
                val smartHomeSections = smartTubeEngine.getPersonalizedHome()
                val trendingTracks = contentFetcher.getTrendingMusic("IN", 20)

                val trendingRow = RecommendationRow(
                    title = "🔥 Trending Hits",
                    subtitle = "Popular right now",
                    items = trendingTracks
                )

                val dynamicRows = smartHomeSections.map { sec ->
                    RecommendationRow(
                        title = sec.title,
                        subtitle = sec.subtitle ?: "",
                        items = sec.items.map { item ->
                            VideoItem(
                                videoId = item.id,
                                title = item.title,
                                artist = item.artist,
                                channelId = "",
                                duration = (item.durationMs / 1000).toString(),
                                genre = "YouTube-Music"
                            )
                        }
                    )
                }

                val perfectRow = dynamicRows.firstOrNull() ?: trendingRow
                val favoriteRows = if (dynamicRows.size > 1) listOf(dynamicRows[1]) else emptyList()
                val genreRows = if (dynamicRows.size > 2) dynamicRows.drop(2) else emptyList()

                val fresh = RecommendationResult(
                    becauseYouListened = emptyList(),
                    favoriteArtists = favoriteRows,
                    perfectForNow = perfectRow,
                    trending = trendingRow,
                    genreDive = genreRows,
                    hiddenGems = trendingRow,
                )

                cacheManager.saveRecommendations(fresh)
                _recommendations.value = fresh
                _error.value = null
                prefetchFirstRecommendedTracks(fresh)
            } catch (e: Exception) {
                e.printStackTrace()
                if (_recommendations.value == null) {
                    _error.value = "Please check your network connection and try again."
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun prefetchFirstRecommendedTracks(result: RecommendationResult) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val items = result.perfectForNow.items.take(3)
            for (item in items) {
                try {
                    sourceResolverManager.resolve(item.videoId, false)
                } catch (e: Exception) {
                    android.util.Log.w("RecommendationViewModel", "Prefetch failed for ${item.videoId}: ${e.message}")
                }
            }
        }
    }

    fun onSongCompleted(
        videoId: String,
        title: String,
        artist: String,
        channelId: String,
        listenMs: Long,
        totalMs: Long,
        wasSkipped: Boolean = false,
        wasLiked: Boolean = false,
        wasDisliked: Boolean = false,
        wasAddedToPlaylist: Boolean = false,
    ) {
        // Handled natively by YouTube server-side when logged in
    }
}
