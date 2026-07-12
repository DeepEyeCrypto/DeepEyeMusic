// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.screens

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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import javax.inject.Inject

data class NetMirrorUiState(
    val heroMovie: HomeVideoItem? = null,
    val bollywoodMovies: List<HomeVideoItem> = emptyList(),
    val hollywoodMovies: List<HomeVideoItem> = emptyList(),
    val southDubbedMovies: List<HomeVideoItem> = emptyList(),
    val webSeries: List<HomeVideoItem> = emptyList(),
    val pakistaniDramas: List<HomeVideoItem> = emptyList(),
    val kids: List<HomeVideoItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class NetMirrorViewModel @Inject constructor(
    private val youtubeRemoteDataSource: YoutubeRemoteDataSource,
    private val playerController: PlayerController
) : ViewModel() {

    private val _uiState = MutableStateFlow(NetMirrorUiState())
    val uiState: StateFlow<NetMirrorUiState> = _uiState.asStateFlow()

    init {
        loadContent()
    }

    private fun loadContent() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val deferredBollywood = async { youtubeRemoteDataSource.searchVideosFirstPage("latest official bollywood full movies 2024 -south -dubbed -hollywood -bhojpuri") }
                val deferredHollywood = async { youtubeRemoteDataSource.searchVideosFirstPage("latest hollywood full movies action english -hindi -dubbed") }
                val deferredSouth = async { youtubeRemoteDataSource.searchVideosFirstPage("latest south indian movies dubbed in hindi full -bollywood") }
                val deferredWebSeries = async { youtubeRemoteDataSource.searchVideosFirstPage("latest hindi web series full episodes") }
                val deferredPakistani = async { youtubeRemoteDataSource.searchVideosFirstPage("latest pakistani drama episodes") }
                val deferredKids = async { youtubeRemoteDataSource.searchVideosFirstPage("latest kids cartoons in hindi full -horror") }

                val bollywood = deferredBollywood.await().items
                val hollywood = deferredHollywood.await().items
                val south = deferredSouth.await().items
                val webSeries = deferredWebSeries.await().items
                val pakistani = deferredPakistani.await().items
                val kidsContent = deferredKids.await().items

                _uiState.update {
                    it.copy(
                        heroMovie = if (bollywood.isNotEmpty()) bollywood.first() else null,
                        bollywoodMovies = if (bollywood.size > 1) bollywood.drop(1) else bollywood,
                        hollywoodMovies = hollywood,
                        southDubbedMovies = south,
                        webSeries = webSeries,
                        pakistaniDramas = pakistani,
                        kids = kidsContent,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Failed to load movies") }
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
        playerController.setQueue(listOf(mediaItem), 0)
    }

    /**
     * Play a video from a category row as a playlist.
     * Sets the entire category list as queue and starts from the clicked episode index.
     * This enables episode-by-episode auto-play (e.g., drama/web series episodes).
     */
    fun playVideoFromCategory(categoryItems: List<HomeVideoItem>, clickedIndex: Int) {
        val queue = categoryItems.map { video ->
            MediaItem.Remote(
                id = video.id,
                title = video.title,
                artist = video.channelName,
                artworkUri = Uri.parse(video.thumbnailUrl),
                duration = video.duration * 1000L,
                isVideo = true
            )
        }
        playerController.setQueue(queue, clickedIndex.coerceIn(0, queue.lastIndex))
    }
}
