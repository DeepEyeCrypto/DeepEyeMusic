package com.deepeye.musicpro.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.musicpro.domain.recommendation.RecommendationEngine
import com.deepeye.musicpro.domain.recommendation.RecommendationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecommendationViewModel
@Inject
constructor(
    private val engine: RecommendationEngine,
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
            // 1. Load from cache INSTANTLY
            val cached = cacheManager.loadCachedRecommendations()
            if (cached != null) {
                _recommendations.value = cached
                prefetchFirstRecommendedTracks(cached)
            } else {
                _isRefreshing.value = true
            }

            // 2. Refresh from network silently
            try {
                _isRefreshing.value = true
                val fresh = engine.buildRecommendations()
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
                    android.util.Log.d("RecommendationViewModel", "Prefetching stream URL for recommended track: ${item.title} (${item.videoId})")
                    sourceResolverManager.resolve(item.videoId, false)
                } catch (e: Exception) {
                    android.util.Log.w("RecommendationViewModel", "Prefetch failed for ${item.videoId}: ${e.message}")
                }
            }
        }
    }

    // Called from PlayerController after each song
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
        viewModelScope.launch {
            engine.trackListenEvent(
                videoId, title, artist, channelId,
                listenMs, totalMs, wasSkipped, wasLiked,
                wasDisliked, wasAddedToPlaylist, false,
            )
            // Optionally, we could refresh recommendations here in background
            // loadRecommendations()
        }
    }
}
