// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.musicpro.data.cache.HiddenContentManager
import com.deepeye.musicpro.data.cache.HiddenItemEntry
import com.deepeye.musicpro.domain.repository.PersonalizationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class HiddenContentTab {
    SONGS, ARTISTS
}

data class HiddenContentUiState(
    val hiddenSongs: List<HiddenItemEntry> = emptyList(),
    val hiddenArtists: List<String> = emptyList(),
    val searchQuery: String = "",
    val selectedTab: HiddenContentTab = HiddenContentTab.SONGS,
    val activeConfirmation: ConfirmationDialogState? = null,
    val message: String? = null,
)

@HiltViewModel
class HiddenContentViewModel @Inject constructor(
    private val hiddenContentManager: HiddenContentManager,
    private val personalizationRepository: PersonalizationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HiddenContentUiState())
    val uiState: StateFlow<HiddenContentUiState> = _uiState.asStateFlow()

    init {
        observeHiddenSongs()
        observeHiddenArtists()
    }

    private fun observeHiddenSongs() {
        viewModelScope.launch {
            hiddenContentManager.hiddenItems.collectLatest { map ->
                _uiState.update { it.copy(hiddenSongs = map.values.toList()) }
            }
        }
    }

    private fun observeHiddenArtists() {
        viewModelScope.launch {
            hiddenContentManager.hiddenArtistNames.collectLatest { set ->
                _uiState.update { it.copy(hiddenArtists = set.toList()) }
            }
        }
    }

    fun selectTab(tab: HiddenContentTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun restoreItem(itemId: String) {
        viewModelScope.launch {
            personalizationRepository.unhideItem(itemId)
            personalizationRepository.refreshFeed(forceRefresh = true)
            _uiState.update { it.copy(message = "Song restored to recommendations") }
        }
    }

    fun restoreArtist(artistName: String) {
        viewModelScope.launch {
            personalizationRepository.unhideArtist(artistName)
            personalizationRepository.refreshFeed(forceRefresh = true)
            _uiState.update { it.copy(message = "Artist restored to recommendations") }
        }
    }

    fun requestRestoreAll() {
        val totalCount = _uiState.value.hiddenSongs.size + _uiState.value.hiddenArtists.size
        if (totalCount == 0) return

        _uiState.update {
            it.copy(
                activeConfirmation = ConfirmationDialogState(
                    title = "Restore All Hidden Content",
                    message = "This will restore $totalCount hidden item(s) and artist(s) back to your personalized recommendations. No changes will be made to your remote account.",
                    confirmLabel = "Restore All",
                    isDestructive = false,
                    onConfirm = { confirmRestoreAll() },
                )
            )
        }
    }

    fun confirmRestoreAll() {
        _uiState.update { it.copy(activeConfirmation = null) }
        viewModelScope.launch {
            personalizationRepository.resetAllHiddenContent()
            personalizationRepository.refreshFeed(forceRefresh = true)
            _uiState.update { it.copy(message = "All hidden items restored") }
        }
    }

    fun dismissConfirmation() {
        _uiState.update { it.copy(activeConfirmation = null) }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
