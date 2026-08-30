package com.deepeye.musicpro.ui.search

import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.musicpro.data.repository.YouTubeRepository
import com.deepeye.musicpro.domain.model.MediaItem
import com.deepeye.musicpro.domain.model.search.SearchFilter
import com.deepeye.musicpro.domain.model.search.SearchResultItem
import com.deepeye.musicpro.domain.model.search.SearchSort
import com.deepeye.musicpro.domain.repository.TasteProfileRepository
import com.deepeye.musicpro.domain.repository.search.SearchRepository
import com.deepeye.musicpro.player.controller.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel
@Inject
constructor(
    private val youtubeRepository: YouTubeRepository,
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

    // Mobile optimization: manual result accumulation (infinite scroll) instead of flatMapLatest.
    private val _results = MutableStateFlow<List<SearchResultItem>>(emptyList())
    val results: StateFlow<List<SearchResultItem>> = _results.asStateFlow()

    private val _page = MutableStateFlow(1)
    val page = _page.asStateFlow()

    private val _hasMoreResults = MutableStateFlow(true)
    val hasMoreResults = _hasMoreResults.asStateFlow()

    // Pull-to-refresh trigger: bumping this key re-runs the search collector.
    private val _refreshTick = MutableStateFlow(0)

    private var refreshRequested = false
    private var loadedPages = 0

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions = _suggestions.asStateFlow()

    val recentSearches: StateFlow<List<String>> = historyRepository.getRecentSearches()
        .map { list -> list.map { it.query } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    companion object {
        private const val TAG = "SearchViewModel"
        private const val DEBOUNCE_MS = 500L
        private const val MAX_RESULTS_PER_PAGE = 30 // backend cap in SearchRepository
        private const val MAX_PAGES = 4
    }

    init {
        loadSuggestions()
        viewModelScope.launch {
            combine(query, selectedFilter, sort, _refreshTick) { q, f, s, _ ->
                Triple(q, f, s)
            }.collectLatest { (q, filter, s) ->
                if (q.isBlank()) {
                    _isLoading.value = false
                    _results.value = emptyList()
                    _page.value = 1
                    loadedPages = 0
                    _hasMoreResults.value = true
                    return@collectLatest
                }

                _isLoading.value = true
                delay(DEBOUNCE_MS)
                try {
                    historyRepository.saveSearch(q, "local", filter.name)
                    val forceRefresh = refreshRequested
                    refreshRequested = false
                    val firstPage = youtubeRepository.search(q, filter, s, forceRefresh = forceRefresh)
                    _page.value = 1
                    loadedPages = 1
                    _results.value = firstPage
                    _hasMoreResults.value = firstPage.size >= MAX_RESULTS_PER_PAGE
                } catch (e: Exception) {
                    Log.e(TAG, "search event=failed query='$q' reason=\"${e.message}\"", e)
                    _results.value = emptyList()
                    _hasMoreResults.value = false
                } finally {
                    _isLoading.value = false
                }
            }
        }
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

    /**
     * Infinite scroll: re-fetch with cache bypass and merge new unique items.
     * Stops after [MAX_PAGES] pages or when no new items arrive.
     */
    fun loadMore() {
        if (_isLoading.value || !_hasMoreResults.value) return
        val q = query.value
        if (q.isBlank()) return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val current = results.value
                val nextPage = youtubeRepository.search(q, selectedFilter.value, sort.value, forceRefresh = true)
                val merged = (current + nextPage).distinctBy { it.id }
                _page.value++
                loadedPages++
                _results.value = merged
                val addedNewItems = merged.size > current.size
                if (!addedNewItems || loadedPages >= MAX_PAGES) {
                    _hasMoreResults.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadMore event=failed query='$q' reason=\"${e.message}\"", e)
                _hasMoreResults.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Pull-to-refresh: clear cache, mark forced refresh, and re-run the collector. */
    fun refresh() {
        viewModelScope.launch {
            youtubeRepository.clearCache()
            refreshRequested = true
            _page.value = 1
            loadedPages = 0
            _hasMoreResults.value = true
            _refreshTick.value++
        }
    }

/** Add a result to the end of the current playback queue (mobile "Add to queue"). */
    fun addToQueue(item: SearchResultItem) {
        val remote = MediaItem.Remote(
            id = item.id,
            title = item.title,
            artist = item.artist ?: item.subtitle,
            artworkUri = item.thumbnailUrl?.let { Uri.parse(it) } ?: Uri.EMPTY,
            duration = 0,
            isVideo = item.type == SearchFilter.VIDEOS,
        )
        playerController.addToQueue(remote)
        Log.d(TAG, "addToQueue event=added id=" + item.id)
    }

    fun loadSuggestions() {
        viewModelScope.launch {
            val prefs = tasteProfileRepository.getTasteProfile().first()
            _suggestions.value = youtubeRepository.buildSuggestions(prefs)
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
