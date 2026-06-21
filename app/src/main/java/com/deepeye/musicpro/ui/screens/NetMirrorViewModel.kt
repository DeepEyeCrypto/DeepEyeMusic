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
    val bollywoodMovies: List<HomeVideoItem> = emptyList(),
    val hollywoodMovies: List<HomeVideoItem> = emptyList(),
    val southDubbedMovies: List<HomeVideoItem> = emptyList(),
    val webSeries: List<HomeVideoItem> = emptyList(),
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
                // Fetch Bollywood
                val bollywoodResult = youtubeRemoteDataSource.searchVideosFirstPage("latest blockbuster bollywood full movies")
                // Fetch Hollywood
                val hollywoodResult = youtubeRemoteDataSource.searchVideosFirstPage("latest hollywood full movies action")
                // Fetch South (Hindi Dubbed)
                val southResult = youtubeRemoteDataSource.searchVideosFirstPage("new released south indian movies dubbed in hindi full")
                // Fetch Web Series
                val webSeriesResult = youtubeRemoteDataSource.searchVideosFirstPage("latest hindi web series full episodes")

                val bollywood = bollywoodResult.items
                val hollywood = hollywoodResult.items
                val south = southResult.items
                val webSeries = webSeriesResult.items

                _uiState.update {
                    it.copy(
                        heroMovie = if (bollywood.isNotEmpty()) bollywood.first() else null,
                        bollywoodMovies = if (bollywood.size > 1) bollywood.drop(1) else bollywood,
                        hollywoodMovies = hollywood,
                        southDubbedMovies = south,
                        webSeries = webSeries,
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
