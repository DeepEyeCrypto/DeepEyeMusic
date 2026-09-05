// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.music

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.musicpro.account.AccountSession
import com.deepeye.musicpro.account.AccountSessionManager
import com.deepeye.musicpro.domain.model.MediaItem
import com.deepeye.musicpro.domain.model.Song
import com.deepeye.musicpro.domain.model.personalization.PersonalizedFeedItem
import com.deepeye.musicpro.domain.model.personalization.PersonalizedFeedState
import com.deepeye.musicpro.domain.model.personalization.PersonalizedSectionType
import com.deepeye.musicpro.domain.repository.MusicRepository
import com.deepeye.musicpro.domain.repository.PersonalizationRepository
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
    val personalizedFeed: PersonalizedFeedState = PersonalizedFeedState(),
    val localSongs: List<Song> = emptyList(),
    val hasAuth: Boolean = false,
)

@HiltViewModel
class MusicViewModel
@Inject
constructor(
    private val personalizationRepository: PersonalizationRepository,
    private val accountSessionManager: AccountSessionManager,
    private val musicRepository: MusicRepository,
    private val playerController: PlayerController,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MusicUiState())
    val uiState: StateFlow<MusicUiState> = _uiState.asStateFlow()

    companion object {
        private const val TAG = "MusicViewModel"
    }

    init {
        observePersonalizedFeed()
        observeAuth()
        observeLocalSongs()
        viewModelScope.launch { musicRepository.syncFromMediaStore() }
    }

    private fun observePersonalizedFeed() {
        viewModelScope.launch {
            personalizationRepository.observePersonalizedFeed().collectLatest { feed ->
                _uiState.update { it.copy(personalizedFeed = feed) }
            }
        }
    }

    private fun observeAuth() {
        viewModelScope.launch {
            accountSessionManager.accountSession.collect { session ->
                val auth = session is AccountSession.Connected
                _uiState.update { it.copy(hasAuth = auth) }
            }
        }
    }

    private fun observeLocalSongs() {
        viewModelScope.launch {
            musicRepository.getAllSongs().collectLatest { songs ->
                _uiState.update { it.copy(localSongs = songs) }
            }
        }
    }

    // ── Personalized Feed Actions ──────────────────────────────────────────

    fun playPersonalizedItem(item: PersonalizedFeedItem, itemsInSection: List<PersonalizedFeedItem>) {
        val mediaItems = itemsInSection.mapNotNull {
            it.mediaItem ?: MediaItem.Remote(
                id = it.id,
                title = it.title,
                artist = it.artist,
                artworkUri = it.artworkUrl?.let { url -> Uri.parse(url) },
                duration = it.durationMs,
                isVideo = false,
            )
        }
        val index = itemsInSection.indexOfFirst { it.id == item.id }
        playerController.setQueue(mediaItems, if (index >= 0) index else 0)
    }

    fun playNextPersonalizedItem(item: PersonalizedFeedItem) {
        val mediaItem = resolveMediaItem(item)
        playerController.addToQueue(mediaItem)
        // Move the just-appended item to after the currently playing item
        val q = playerController.playerState.value.queue
        val currentIdx = playerController.playerState.value.currentIndex
        if (q.isNotEmpty() && currentIdx in q.indices) {
            val fromIndex = q.size - 1
            val toIndex = currentIdx + 1
            if (fromIndex > toIndex) {
                playerController.moveQueueItem(fromIndex, toIndex)
            }
        }
        Log.d(TAG, "playNext key=${item.id}")
    }

    fun addPersonalizedItemToQueue(item: PersonalizedFeedItem) {
        playerController.addToQueue(resolveMediaItem(item))
        Log.d(TAG, "addToQueue key=${item.id}")
    }

    fun refreshPersonalizedFeed() {
        viewModelScope.launch {
            personalizationRepository.refreshFeed(forceRefresh = true)
        }
    }

    fun refreshPersonalizedSection(sectionType: PersonalizedSectionType) {
        viewModelScope.launch {
            personalizationRepository.refreshSection(sectionType)
        }
    }

    // ── Phase 5: Personalization quality and user control ───────────────────

    fun hidePersonalizedItem(item: PersonalizedFeedItem, alsoHideArtist: Boolean) {
        viewModelScope.launch {
            personalizationRepository.hideItem(
                itemId = item.id,
                label = item.title,
                alsoHideArtist = if (alsoHideArtist) item.artist else null,
            )
        }
    }

    fun undoHidePersonalizedItem(itemId: String) {
        viewModelScope.launch {
            personalizationRepository.undoHideItem(itemId)
        }
    }

    fun resetAllHiddenContent() {
        viewModelScope.launch {
            personalizationRepository.resetAllHiddenContent()
            personalizationRepository.refreshFeed(forceRefresh = true)
        }
    }

    fun clearPersonalizationHistory() {
        viewModelScope.launch {
            personalizationRepository.clearLocalHistory()
            personalizationRepository.refreshFeed(forceRefresh = true)
        }
    }

    fun clearPersonalizationCache() {
        viewModelScope.launch {
            personalizationRepository.clearPersonalizationCache()
            personalizationRepository.refreshFeed(forceRefresh = true)
        }
    }

    // ── Local Library ──────────────────────────────────────────────────────

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

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun resolveMediaItem(item: PersonalizedFeedItem): MediaItem =
        item.mediaItem ?: MediaItem.Remote(
            id = item.id,
            title = item.title,
            artist = item.artist,
            artworkUri = item.artworkUrl?.let { url -> Uri.parse(url) },
            duration = item.durationMs,
            isVideo = false,
        )
}