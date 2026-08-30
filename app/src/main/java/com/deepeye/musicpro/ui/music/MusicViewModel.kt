// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.music

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.musicpro.data.prefs.SettingsDataStore
import com.deepeye.musicpro.data.source.remote.youtube.AuthenticatedYouTubeClient
import com.deepeye.musicpro.data.source.remote.youtube.MusicFilter
import com.deepeye.musicpro.data.source.remote.youtube.YoutubeRemoteDataSource
import com.deepeye.musicpro.domain.model.MediaItem
import com.deepeye.musicpro.domain.model.Song
import com.deepeye.musicpro.domain.model.home.HomeMusicItem
import com.deepeye.musicpro.domain.model.home.HomeVideoItem
import com.deepeye.musicpro.domain.repository.MusicRepository
import com.deepeye.musicpro.player.controller.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MusicUiState(
    val recommendedMusic: List<HomeMusicItem> = emptyList(),
    val localSongs: List<Song> = emptyList(),
    val selectedCategory: String = "For You",
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasAuth: Boolean = false,
)

@HiltViewModel
class MusicViewModel
@Inject
constructor(
    private val youtubeRemoteDataSource: YoutubeRemoteDataSource,
    private val musicRepository: MusicRepository,
    private val playerController: PlayerController,
    private val authClient: AuthenticatedYouTubeClient,
    private val settingsDataStore: SettingsDataStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MusicUiState())
    val uiState: StateFlow<MusicUiState> = _uiState.asStateFlow()

    companion object {
        private const val TAG = "MusicViewModel"
    }

    init {
        observeAuth()
        observeLocalSongs()
        syncLibrary()
    }

    private var isFirstAuthEmission = true

    private fun observeAuth() {
        viewModelScope.launch {
            settingsDataStore.settings.collect { settings ->
                val auth = settings.youtubeAccessToken != null
                val changed = auth != _uiState.value.hasAuth
                _uiState.value = _uiState.value.copy(hasAuth = auth)
                // Load on the first emission (covers the initial screen) and whenever
                // account state changes, so the feed is fetched from the connected
                // YouTube account (by id).
                if (changed || isFirstAuthEmission) {
                    isFirstAuthEmission = false
                    loadRecommendations()
                }
            }
        }
    }

    private fun HomeVideoItem.toHomeMusic(): HomeMusicItem =
        HomeMusicItem(
            id = id,
            title = title,
            artist = channelName,
            thumbnailUrl = thumbnailUrl,
            duration = duration,
        )

    private fun observeLocalSongs() {
        viewModelScope.launch {
            musicRepository.getAllSongs().collectLatest { songs ->
                _uiState.value = _uiState.value.copy(localSongs = songs)
            }
        }
    }

    fun syncLibrary() {
        viewModelScope.launch {
            try {
                musicRepository.syncFromMediaStore()
            } catch (e: Exception) {
                Log.e(TAG, "syncLibrary failed", e)
                _uiState.value = _uiState.value.copy(error = "Unable to sync local library right now.")
            }
        }
    }

    fun selectCategory(category: String) {
        if (_uiState.value.selectedCategory == category) return
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        loadRecommendations(category)
    }

    fun loadRecommendations(category: String = _uiState.value.selectedCategory) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val auth = _uiState.value.hasAuth
                Log.d(TAG, "loadRecommendations (hasAuth=$auth, category=$category)")
                val rawList: List<HomeMusicItem> = when (category) {
                    "For You" -> {
                        val trending = youtubeRemoteDataSource.searchMusic("trending songs official audio video")
                        if (auth) {
                            val accountMusic = authClient.getMusicFeed()
                            val personal = accountMusic
                                .map { it.toHomeMusic() }
                                .filter { MusicFilter.isMusicTrack(it.title, it.artist, it.duration) }
                            (personal + trending).distinctBy { it.id }
                        } else {
                            trending
                        }
                    }
                    "Liked", "Liked Music" -> {
                        if (auth) {
                            val liked = authClient.getLikedMusic()
                            val filtered = liked
                                .map { it.toHomeMusic() }
                                .filter { MusicFilter.isMusicTrack(it.title, it.artist, it.duration) }
                            filtered.ifEmpty { youtubeRemoteDataSource.searchMusic("top hit songs official audio") }
                        } else {
                            youtubeRemoteDataSource.searchMusic("top hit songs official audio")
                        }
                    }
                    "History" -> {
                        if (auth) {
                            val history = authClient.getMusicHistory()
                            val filtered = history
                                .map { it.toHomeMusic() }
                                .filter { MusicFilter.isMusicTrack(it.title, it.artist, it.duration) }
                            filtered.ifEmpty { youtubeRemoteDataSource.searchMusic("latest hindi songs audio") }
                        } else {
                            youtubeRemoteDataSource.searchMusic("latest hindi songs audio")
                        }
                    }
                    "Trending" -> youtubeRemoteDataSource.searchMusic("trending songs official audio video")
                    "Bollywood" -> youtubeRemoteDataSource.searchMusic("latest bollywood songs hindi hits official audio")
                    "Punjabi" -> youtubeRemoteDataSource.searchMusic("trending punjabi songs latest hits")
                    "Romantic" -> youtubeRemoteDataSource.searchMusic("romantic hindi love songs audio")
                    "Lo-Fi" -> youtubeRemoteDataSource.searchMusic("hindi lofi chill songs audio")
                    "Pop" -> youtubeRemoteDataSource.searchMusic("top pop songs global hits")
                    "Hip-Hop" -> youtubeRemoteDataSource.searchMusic("trending hip hop rap songs")
                    "Devotional" -> youtubeRemoteDataSource.searchMusic("hindi bhakti bhajan devotional songs")
                    else -> youtubeRemoteDataSource.searchMusic("$category songs official audio")
                }
                val music = rawList.filter { MusicFilter.isMusicTrack(it.title, it.artist, it.duration) }
                _uiState.value = _uiState.value.copy(
                    recommendedMusic = if (music.isNotEmpty()) music else rawList,
                    isLoading = false
                )
            } catch (e: Exception) {
                Log.e(TAG, "loadRecommendations failed", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Couldn't load songs. Check your connection and retry."
                )
            }
        }
    }

    fun playMusic(music: HomeMusicItem) {
        val mediaItems =
            _uiState.value.recommendedMusic.map { item ->
                MediaItem.Remote(
                    id = item.id,
                    title = item.title,
                    artist = item.artist,
                    artworkUri = Uri.parse(item.thumbnailUrl),
                    duration = item.duration * 1000L,
                    isVideo = false,
                )
            }
        // Use indexOfFirst by ID to avoid object-equality issues with data classes
        val index = _uiState.value.recommendedMusic.indexOfFirst { it.id == music.id }
        playerController.setQueue(mediaItems, if (index >= 0) index else 0)
    }

    fun playMusicLocal(song: Song) {
        val mediaItems = _uiState.value.localSongs.map { MediaItem.Local(it) }
        // Use indexOfFirst by ID to avoid object-equality issues with data classes
        val index = _uiState.value.localSongs.indexOfFirst { it.id == song.id }
        playerController.setQueue(mediaItems, if (index >= 0) index else 0)
    }
}
