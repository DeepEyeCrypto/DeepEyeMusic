package com.deepeye.musicpro.ui.youtube

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.musicpro.data.source.remote.youtube.YoutubeRemoteDataSource
import com.deepeye.musicpro.domain.model.MediaItem
import com.deepeye.musicpro.domain.model.home.HomeVideoItem
import com.deepeye.musicpro.player.controller.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class YouTubeUiState(
    val trendingVideos: List<HomeVideoItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class YouTubeViewModel @Inject constructor(
    private val youtubeRemoteDataSource: YoutubeRemoteDataSource,
    private val playerController: PlayerController
) : ViewModel() {

    private val _uiState = MutableStateFlow(YouTubeUiState())
    val uiState: StateFlow<YouTubeUiState> = _uiState.asStateFlow()

    init {
        loadTrending()
    }

    fun loadTrending() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val videos = youtubeRemoteDataSource.getTrending()
                _uiState.value = _uiState.value.copy(trendingVideos = videos, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun playVideo(video: HomeVideoItem) {
        val mediaItem = MediaItem.Remote(
            id = video.id,
            title = video.title,
            artist = video.channelName,
            artworkUri = Uri.parse(video.thumbnailUrl),
            duration = video.duration * 1000L,
            isVideo = true
        )
        playerController.playMedia(mediaItem)
    }
}
