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
import javax.inject.Inject

data class NetMirrorUiState(
    val heroMovie: HomeVideoItem? = null,
    val trendingMovies: List<HomeVideoItem> = emptyList(),
    val sciFiMovies: List<HomeVideoItem> = emptyList(),
    val newReleases: List<HomeVideoItem> = emptyList(),
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
                // Fetch Trending
                val trendingResult = youtubeRemoteDataSource.searchVideosFirstPage("trending full movies 2024")
                // Fetch Sci-Fi
                val sciFiResult = youtubeRemoteDataSource.searchVideosFirstPage("sci-fi full movies")
                // Fetch New Releases
                val newReleasesResult = youtubeRemoteDataSource.searchVideosFirstPage("new release movies full")

                val trending = trendingResult.items
                val sciFi = sciFiResult.items
                val newReleases = newReleasesResult.items

                _uiState.update {
                    it.copy(
                        heroMovie = if (trending.isNotEmpty()) trending.first() else null,
                        trendingMovies = if (trending.size > 1) trending.drop(1) else trending,
                        sciFiMovies = sciFi,
                        newReleases = newReleases,
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
}
