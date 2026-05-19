package com.deepeye.musicpro.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.musicpro.domain.model.MediaItem
import com.deepeye.musicpro.domain.model.Song
import com.deepeye.musicpro.domain.model.home.HomeMusicItem
import com.deepeye.musicpro.domain.usecase.search.SearchHybridUseCase
import com.deepeye.musicpro.player.controller.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.net.Uri

data class SearchUiState(
    val query: String = "",
    val localResults: List<Song> = emptyList(),
    val remoteResults: List<HomeMusicItem> = emptyList(),
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchHybridUseCase: SearchHybridUseCase,
    private val playerController: PlayerController
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    private var searchJob: Job? = null

    fun onQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(
                localResults = emptyList(), 
                remoteResults = emptyList(), 
                hasSearched = false
            )
            return
        }
        searchJob = viewModelScope.launch {
            delay(500) // debounce
            _uiState.value = _uiState.value.copy(isSearching = true)
            searchHybridUseCase(query).collect { response ->
                _uiState.value = _uiState.value.copy(
                    localResults = response.localSongs,
                    remoteResults = response.remoteItems,
                    isSearching = false,
                    hasSearched = true
                )
            }
        }
    }

    fun playLocal(song: Song) {
        val mediaItems = _uiState.value.localResults.map { MediaItem.Local(it) }
        val index = _uiState.value.localResults.indexOf(song)
        playerController.setQueue(mediaItems, index)
    }

    fun playRemote(item: HomeMusicItem) {
        val mediaItems = _uiState.value.remoteResults.map { remote ->
            MediaItem.Remote(
                id = remote.id,
                title = remote.title,
                artist = remote.artist,
                artworkUri = Uri.parse(remote.thumbnailUrl),
                duration = 0 // Search results might not have duration
            )
        }
        val index = _uiState.value.remoteResults.indexOf(item)
        playerController.setQueue(mediaItems, index)
    }
}
