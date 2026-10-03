// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.music

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.musicpro.domain.model.MediaItem
import com.deepeye.musicpro.domain.model.Song
import com.deepeye.musicpro.domain.repository.MusicRepository
import com.deepeye.musicpro.player.controller.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MusicUiState(
    val localSongs: List<Song> = emptyList()
)

@HiltViewModel
class MusicViewModel
@Inject
constructor(
    private val musicRepository: MusicRepository,
    private val playerController: PlayerController,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MusicUiState())
    val uiState: StateFlow<MusicUiState> = _uiState.asStateFlow()

    companion object {
        private const val TAG = "MusicViewModel"
    }

    init {
        observeLocalSongs()
        viewModelScope.launch { musicRepository.syncFromMediaStore() }
    }

    private fun observeLocalSongs() {
        viewModelScope.launch {
            musicRepository.getAllSongs().collectLatest { songs ->
                _uiState.update { it.copy(localSongs = songs) }
            }
        }
    }

    fun syncLibrary() {
        viewModelScope.launch {
            try { musicRepository.syncFromMediaStore() }
            catch (e: Exception) { Log.e(TAG, "syncLibrary failed", e) }
        }
    }

    fun playMusicLocal(song: Song) {
        val mediaItems = _uiState.value.localSongs.map { MediaItem.Local(it) }
        val index = _uiState.value.localSongs.indexOfFirst { it.id == song.id }
        playerController.setQueue(mediaItems, if (index >= 0) index else 0)
    }

    fun playAllLocalSongs(shuffle: Boolean = false) {
        val songs = _uiState.value.localSongs
        if (songs.isEmpty()) return
        val mediaItems = if (shuffle) {
            songs.shuffled().map { MediaItem.Local(it) }
        } else {
            songs.map { MediaItem.Local(it) }
        }
        playerController.setQueue(mediaItems, 0)
    }
}
