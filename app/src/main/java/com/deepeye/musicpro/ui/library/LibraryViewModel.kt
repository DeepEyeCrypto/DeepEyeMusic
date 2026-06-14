// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.musicpro.domain.model.Album
import com.deepeye.musicpro.domain.model.Artist
import com.deepeye.musicpro.domain.model.Song
import com.deepeye.musicpro.domain.repository.MusicRepository
import com.deepeye.musicpro.domain.repository.library.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val selectedTab: Int = 0,
    val libraryHome: LibraryHomeState = LibraryHomeState(),
    val offlineMode: Boolean = false,
)

@HiltViewModel
class LibraryViewModel
@Inject
constructor(
    private val musicRepository: MusicRepository,
    private val libraryRepository: LibraryRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val selectedTab = savedStateHandle.getStateFlow("selected_tab", 0)
    val offlineMode = savedStateHandle.getStateFlow("offline_mode", false)

    val uiState: StateFlow<LibraryUiState> =
        combine(
            combine(
                musicRepository.getAllSongs(),
                musicRepository.getAllAlbums(),
                musicRepository.getAllArtists(),
                ::Triple
            ),
            selectedTab,
            libraryRepository.observeLibraryHome(),
            offlineMode,
        ) { (songs, albums, artists), tab, home, isOffline ->
            val processedHome = if (isOffline) {
                home.copy(
                    likedTracks = home.likedTracks.filter { it.isOfflineAvailable },
                    playlists = home.playlists, // assume playlists are metadata, can be viewed offline
                    downloads = home.downloads,
                    recentPlays = home.recentPlays.filter { it.isOfflineAvailable }
                )
            } else {
                home
            }
            LibraryUiState(songs, albums, artists, tab, processedHome, isOffline)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LibraryUiState(),
        )

    fun selectTab(index: Int) {
        savedStateHandle["selected_tab"] = index
    }

    fun toggleOfflineMode() {
        savedStateHandle["offline_mode"] = !offlineMode.value
    }

    fun likeTrack(
        videoId: String,
        title: String,
        artist: String,
        channelId: String,
        artworkUrl: String? = null,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                libraryRepository.likeTrack(videoId, title, artist, channelId, artworkUrl)
            } catch (e: Exception) {
                // Log or handle offline sync error silently
            }
        }
    }

    fun saveTrack(
        videoId: String,
        title: String,
        artist: String,
        channelId: String,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                libraryRepository.saveTrack(videoId, title, artist, channelId)
            } catch (e: Exception) {
            }
        }
    }

    fun createPlaylist(
        name: String,
        description: String = "",
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                libraryRepository.createPlaylist(name, description)
            } catch (e: Exception) {
            }
        }
    }

    fun recordRecentPlay(
        videoId: String,
        title: String,
        artist: String,
        artworkUrl: String? = null,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                libraryRepository.recordRecentPlay(videoId, title, artist, artworkUrl)
            } catch (e: Exception) {
            }
        }
    }

    // ── Subscribed Channels ──
    fun toggleSubscription(channelId: String, channelName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val isSubscribed = libraryRepository.isChannelSubscribed(channelId).first()
                if (isSubscribed) {
                    libraryRepository.unsubscribeChannel(channelId)
                } else {
                    libraryRepository.subscribeChannel(channelId, channelName)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun isChannelSubscribed(channelId: String): Flow<Boolean> = libraryRepository.isChannelSubscribed(channelId)
}
