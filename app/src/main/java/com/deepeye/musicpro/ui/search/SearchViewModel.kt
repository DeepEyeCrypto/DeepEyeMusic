package com.deepeye.musicpro.ui.search

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.musicpro.domain.model.MediaItem
import com.deepeye.musicpro.domain.model.search.SearchFilter
import com.deepeye.musicpro.domain.model.search.SearchResultItem
import com.deepeye.musicpro.domain.model.search.SearchSort
import com.deepeye.musicpro.domain.repository.TasteProfileRepository
import com.deepeye.musicpro.domain.repository.search.SearchRepository
import com.deepeye.musicpro.player.controller.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel
@Inject
constructor(
    private val repository: SearchRepository,
    private val tasteProfileRepository: TasteProfileRepository,
    private val historyRepository: com.deepeye.musicpro.domain.repository.HistoryRepository,
    private val playerController: PlayerController,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val query = savedStateHandle.getStateFlow("query", "")

    val selectedFilter = savedStateHandle.getStateFlow("selectedFilter", SearchFilter.ALL)

    val sort = savedStateHandle.getStateFlow("sort", SearchSort.RELEVANCE)

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val results: StateFlow<List<SearchResultItem>> = combine(query, selectedFilter, sort) { q, filter, s ->
        Triple(q, filter, s)
    }
        .flatMapLatest { (q, filter, s) ->
            if (q.isBlank()) {
                _isLoading.value = false
                flowOf(emptyList<SearchResultItem>())
            } else {
                flow<List<SearchResultItem>> {
                    _isLoading.value = true
                    delay(500) // Debounce
                    try {
                        historyRepository.saveSearch(q, "local", filter.name)
                        emit(repository.search(q, filter, s))
                    } catch (e: Exception) {
                        emit(emptyList<SearchResultItem>())
                    } finally {
                        _isLoading.value = false
                    }
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions = _suggestions.asStateFlow()

    val recentSearches: StateFlow<List<String>> = historyRepository.getRecentSearches()
        .map { list -> list.map { it.query } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadSuggestions()
    }

    fun onQueryChange(value: String) {
        savedStateHandle["query"] = value
    }

    fun onFilterChange(filter: SearchFilter) {
        savedStateHandle["selectedFilter"] = filter
    }

    fun onSortChange(newSort: SearchSort) {
        savedStateHandle["sort"] = newSort
    }

    fun loadSuggestions() {
        viewModelScope.launch {
            val prefs = tasteProfileRepository.getTasteProfile().first()
            _suggestions.value = repository.buildSuggestions(prefs)
        }
    }

    fun playResult(item: SearchResultItem) {
        val mediaItems =
            results.value.filter { it.videoId != null }.map { remote ->
                MediaItem.Remote(
                    id = remote.id,
                    title = remote.title,
                    artist = remote.artist ?: "",
                    artworkUri = remote.thumbnailUrl?.let { Uri.parse(it) } ?: Uri.EMPTY,
                    duration = 0,
                    isVideo = remote.type == SearchFilter.VIDEOS,
                )
            }
        val index = mediaItems.indexOfFirst { it.id == item.id }
        playerController.setQueue(mediaItems, if (index >= 0) index else 0)
    }
}
