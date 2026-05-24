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
class RecommendationViewModel @Inject constructor(
    private val engine: RecommendationEngine
) : ViewModel() {

    private val _recommendations = MutableStateFlow<RecommendationResult?>(null)
    val recommendations = _recommendations.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    init {
        loadRecommendations()
    }

    fun loadRecommendations() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = engine.buildRecommendations()
                _recommendations.value = result
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
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
        wasAddedToPlaylist: Boolean = false
    ) {
        viewModelScope.launch {
            engine.trackListenEvent(
                videoId, title, artist, channelId,
                listenMs, totalMs, wasSkipped, wasLiked,
                wasDisliked, wasAddedToPlaylist, false
            )
            // Optionally, we could refresh recommendations here in background
            // loadRecommendations()
        }
    }
}
