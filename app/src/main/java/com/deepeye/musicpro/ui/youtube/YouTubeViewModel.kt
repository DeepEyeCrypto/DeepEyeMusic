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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class YouTubeUiState(
    val videos: List<HomeVideoItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedCategory: String = "Home",
    val searchQuery: String = "",
    val activeSponsorBlockCategories: Set<String> =
        setOf("sponsor", "selfpromo", "interaction", "intro", "outro", "preview"),
    val showStatsForNerds: Boolean = false,
    val searchSuggestions: List<String> = emptyList(),
    val isMoreLoading: Boolean = false,
    val hasMore: Boolean = false,
    val shieldsEnabled: Boolean = true,
    val shieldMode: String = "Standard", // "Standard" or "Aggressive"
    val hideShorts: Boolean = false,
)

@HiltViewModel
class YouTubeViewModel
@Inject
constructor(
    private val youtubeRemoteDataSource: YoutubeRemoteDataSource,
    private val playerController: PlayerController,
    private val homeFeedRepository: com.deepeye.musicpro.data.repository.HomeFeedRepository,
    private val tasteProfileRepository: com.deepeye.musicpro.domain.repository.TasteProfileRepository,
    private val libraryRepository: com.deepeye.musicpro.domain.repository.library.LibraryRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(YouTubeUiState())
    val uiState: StateFlow<YouTubeUiState> = _uiState.asStateFlow()

    private val _homeFeedState = MutableStateFlow(com.deepeye.musicpro.domain.model.home.HomeFeedState())
    val homeFeedState: StateFlow<com.deepeye.musicpro.domain.model.home.HomeFeedState> = _homeFeedState.asStateFlow()

    val player = playerController.player
    val playerState = playerController.playerState

    fun togglePlayPause() {
        playerController.togglePlayPause()
    }

    init {
        loadCategory("Home")
        loadHomeFeed()
    }

    private fun loadHomeFeed() {
        viewModelScope.launch {
            try {
                _homeFeedState.value = homeFeedRepository.getHomeFeed()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    private var nextPageToken: String? = null
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

        suggestionsJob =
            viewModelScope.launch {
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
        viewModelScope.launch {
            var baseQuery =
                when (category) {
                    "Home" -> "trending music"
                    "Music" -> "official music video songs hits"
                    "Movies" -> "full movies action thriller comedy romance"
                    "Gaming" -> "gaming gameplay walkthrough let's play"
                    "News" -> "news highlights live report world news"
                    else -> return@launch // Search / SponsorBlock handle separately
                }
            
            if (category == "Home") {
                val subs = libraryRepository.getAllSubscribedChannels()
                if (subs.isNotEmpty()) {
                    val channels = subs.shuffled().take(3).joinToString(" | ") { it.channelName }
                    baseQuery = "$channels latest videos"
                }
            }
            
            try {
                val prefs = tasteProfileRepository.getTasteProfile().first()
                val langString = prefs.preferredLanguages.joinToString(" ")
                val finalQuery = if (langString.isNotEmpty() && category != "News") "$baseQuery $langString" else baseQuery
                fetchVideos(finalQuery)
            } catch (e: Exception) {
                fetchVideos(baseQuery)
            }
        }
    }

    private fun fetchVideos(query: String) {
        currentActiveQuery = query
        nextPageToken = null
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, hasMore = false) }
            try {
                val result = youtubeRemoteDataSource.searchVideosFirstPage(query)
                nextPageToken = result.nextPageUrl
                _uiState.update {
                    it.copy(
                        videos = result.items,
                        isLoading = false,
                        hasMore = result.nextPageUrl != null,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Unable to search YouTube right now. Please check your connection.") }
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
                nextPageToken = result.nextPageUrl
                _uiState.update { state ->
                    state.copy(
                        videos = state.videos + result.items,
                        isMoreLoading = false,
                        hasMore = result.nextPageUrl != null,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isMoreLoading = false) }
            }
        }
    }

    fun playVideo(video: HomeVideoItem) {
        val mediaItems =
            _uiState.value.videos.map { item ->
                MediaItem.Remote(
                    id = item.id,
                    title = item.title,
                    artist = item.channelName,
                    artworkUri = Uri.parse(item.thumbnailUrl),
                    duration = item.duration * 1000L,
                    isVideo = true,
                )
            }
        val index = _uiState.value.videos.indexOfFirst { it.id == video.id }
        if (index >= 0) {
            playerController.setQueue(mediaItems, index)
        }
    }
}
