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
    val error: String? = null,
    // Episode sheet state
    val episodeSheet: EpisodeSheetState? = null,
)

data class EpisodeSheetState(
    val dramaTitle: String,
    val episodes: List<HomeVideoItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
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
                val deferredWebSeries = async { youtubeRemoteDataSource.searchVideosFirstPage("popular hindi web series 2024 2025") }
                val deferredPakistani = async { youtubeRemoteDataSource.searchVideosFirstPage("popular pakistani drama 2024 2025") }
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

    /**
     * Opens the episode picker sheet for a specific drama/series.
     * Searches YouTube specifically for episodes of THAT drama.
     */
    fun openEpisodePicker(drama: HomeVideoItem) {
        // Extract a clean show name from the title (remove episode numbers, dates, etc.)
        val showName = extractShowName(drama.title)
        val channelName = drama.channelName

        _uiState.update {
            it.copy(
                episodeSheet = EpisodeSheetState(
                    dramaTitle = showName,
                    isLoading = true
                )
            )
        }

        viewModelScope.launch {
            try {
                // Search specifically for this drama's episodes using show name + channel
                val query = "\"$showName\" episode full"
                val results = youtubeRemoteDataSource.searchVideos(query)

                // If results are too few, try with channel name
                val episodes = if (results.size >= 3) {
                    results
                } else {
                    youtubeRemoteDataSource.searchVideos("$showName $channelName episodes")
                }

                _uiState.update {
                    it.copy(
                        episodeSheet = it.episodeSheet?.copy(
                            episodes = episodes,
                            isLoading = false
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        episodeSheet = it.episodeSheet?.copy(
                            isLoading = false,
                            error = "Could not load episodes"
                        )
                    )
                }
            }
        }
    }

    fun closeEpisodePicker() {
        _uiState.update { it.copy(episodeSheet = null) }
    }

    /**
     * Extracts a clean drama/show name from a YouTube video title.
     * Removes episode numbers, dates, [Eng Sub], HD, etc.
     */
    private fun extractShowName(title: String): String {
        return title
            .replace(Regex("\\[.*?\\]"), "") // remove [Eng Sub], [HD], etc.
            .replace(Regex("\\(.*?\\)"), "") // remove (2024), (Official), etc.
            .replace(Regex("(?i)episode\\s*\\d+", RegexOption.IGNORE_CASE), "")
            .replace(Regex("(?i)ep\\.?\\s*\\d+", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\d{1,2}\\s*(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec).*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\|.*"), "") // remove everything after |
            .replace(Regex("-\\s*$"), "") // trailing dash
            .replace(Regex("(?i)(full|hd|4k|new|latest|official|video|2024|2025|2026)", RegexOption.IGNORE_CASE), "")
            .trim()
            .split("\\s+".toRegex())
            .take(5)
            .joinToString(" ")
            .trim()
            .ifBlank { title.take(40) }
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
