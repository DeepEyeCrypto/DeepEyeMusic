// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class YouTubeUiState(
    val videos: List<HomeVideoItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedCategory: String = "Home",
    val searchQuery: String = "",
    val activeSponsorBlockCategories: Set<String> = setOf("sponsor", "selfpromo", "interaction", "intro", "outro", "preview"),
    val showStatsForNerds: Boolean = false,
    val searchSuggestions: List<String> = emptyList(),
    val isMoreLoading: Boolean = false,
    val hasMore: Boolean = false,
    val shieldsEnabled: Boolean = true,
    val shieldMode: String = "Standard", // "Standard" or "Aggressive"
    val hideShorts: Boolean = false
)

@HiltViewModel
class YouTubeViewModel @Inject constructor(
    private val youtubeRemoteDataSource: YoutubeRemoteDataSource,
    private val playerController: PlayerController
) : ViewModel() {

    private val _uiState = MutableStateFlow(YouTubeUiState())
    val uiState: StateFlow<YouTubeUiState> = _uiState.asStateFlow()

    val player = playerController.player
    val playerState = playerController.playerState

    init {
        loadCategory("Home")
    }

    private var nextPageToken: com.yushosei.newpipe.extractor.Page? = null
    private var currentActiveQuery: String = ""
    private var suggestionsJob: kotlinx.coroutines.Job? = null

    fun selectCategory(category: String) {
        if (_uiState.value.selectedCategory == category && category != "Search") return
        _uiState.update { it.copy(selectedCategory = category) }
        loadCategory(category)
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        
        suggestionsJob?.cancel()
        if (query.trim().isEmpty()) {
            _uiState.update { it.copy(searchSuggestions = emptyList()) }
            return
        }
        
        suggestionsJob = viewModelScope.launch {
            kotlinx.coroutines.delay(300)
            val suggestions = youtubeRemoteDataSource.getSearchSuggestions(query)
            _uiState.update { it.copy(searchSuggestions = suggestions) }
        }
    }

    fun performSearch() {
        val query = _uiState.value.searchQuery.trim()
        if (query.isEmpty()) return
        suggestionsJob?.cancel()
        _uiState.update { it.copy(searchSuggestions = emptyList()) }
        fetchVideos(query)
    }

    fun toggleSponsorBlockCategory(category: String) {
        _uiState.update { state ->
            val updated = state.activeSponsorBlockCategories.toMutableSet()
            if (updated.contains(category)) {
                updated.remove(category)
            } else {
                updated.add(category)
            }
            state.copy(activeSponsorBlockCategories = updated)
        }
    }

    fun toggleStatsForNerds() {
        _uiState.update { it.copy(showStatsForNerds = !it.showStatsForNerds) }
    }

    fun toggleShieldsEnabled() {
        _uiState.update { it.copy(shieldsEnabled = !it.shieldsEnabled) }
    }

    fun setShieldMode(mode: String) {
        _uiState.update { it.copy(shieldMode = mode) }
    }

    fun toggleHideShorts() {
        _uiState.update { it.copy(hideShorts = !it.hideShorts) }
    }

    private fun loadCategory(category: String) {
        val query = when (category) {
            "Home" -> "trending music"
            "Music" -> "official music video songs hits"
            "Gaming" -> "gaming gameplay walkthrough let's play"
            "News" -> "news highlights live report world news"
            else -> return // Search / SponsorBlock handle separately
        }
        fetchVideos(query)
    }

    private fun fetchVideos(query: String) {
        currentActiveQuery = query
        nextPageToken = null
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, hasMore = false) }
            try {
                val result = youtubeRemoteDataSource.searchVideosFirstPage(query)
                nextPageToken = result.nextPage
                _uiState.update { 
                    it.copy(
                        videos = result.items, 
                        isLoading = false,
                        hasMore = result.nextPage != null
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun loadMoreVideos() {
        val token = nextPageToken ?: return
        if (_uiState.value.isMoreLoading) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isMoreLoading = true) }
            try {
                val result = youtubeRemoteDataSource.searchVideosNextPage(currentActiveQuery, token)
                nextPageToken = result.nextPage
                _uiState.update { state ->
                    state.copy(
                        videos = state.videos + result.items,
                        isMoreLoading = false,
                        hasMore = result.nextPage != null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isMoreLoading = false) }
            }
        }
    }

    fun playVideo(video: HomeVideoItem) {
        val mediaItems = _uiState.value.videos.map { item ->
            MediaItem.Remote(
                id = item.id,
                title = item.title,
                artist = item.channelName,
                artworkUri = Uri.parse(item.thumbnailUrl),
                duration = item.duration * 1000L,
                isVideo = true
            )
        }
        val index = _uiState.value.videos.indexOf(video)
        if (index >= 0) {
            playerController.setQueue(mediaItems, index)
        }
    }
}
