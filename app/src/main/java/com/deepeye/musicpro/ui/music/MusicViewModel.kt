// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.music

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.musicpro.data.source.remote.youtube.YoutubeRemoteDataSource
import com.deepeye.musicpro.domain.model.MediaItem
import com.deepeye.musicpro.domain.model.Song
import com.deepeye.musicpro.domain.model.home.HomeMusicItem
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
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class MusicViewModel @Inject constructor(
    private val youtubeRemoteDataSource: YoutubeRemoteDataSource,
    private val musicRepository: MusicRepository,
    private val playerController: PlayerController
) : ViewModel() {

    private val _uiState = MutableStateFlow(MusicUiState())
    val uiState: StateFlow<MusicUiState> = _uiState.asStateFlow()

    init {
        loadRecommendations()
        observeLocalSongs()
        syncLibrary()
    }

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
                _uiState.value = _uiState.value.copy(error = "Sync failed: ${e.message}")
            }
        }
    }

    fun loadRecommendations() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                // Using search for 'trending music' as a proxy for recommendations
                val music = youtubeRemoteDataSource.searchMusic("trending music")
                _uiState.value = _uiState.value.copy(recommendedMusic = music, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun playMusic(music: HomeMusicItem) {
        val mediaItems = _uiState.value.recommendedMusic.map { item ->
            MediaItem.Remote(
                id = item.id,
                title = item.title,
                artist = item.artist,
                artworkUri = Uri.parse(item.thumbnailUrl),
                duration = item.duration * 1000L,
                isVideo = false
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
