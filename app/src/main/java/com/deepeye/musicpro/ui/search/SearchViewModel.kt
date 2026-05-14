package com.deepeye.musicpro.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.musicpro.domain.model.Song
import com.deepeye.musicpro.domain.usecase.SearchSongsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<Song> = emptyList(),
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchSongsUseCase: SearchSongsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    private var searchJob: Job? = null

    fun onQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(results = emptyList(), hasSearched = false)
            return
        }
        searchJob = viewModelScope.launch {
            delay(300) // debounce
            _uiState.value = _uiState.value.copy(isSearching = true)
            searchSongsUseCase(query).collect { results ->
                _uiState.value = _uiState.value.copy(results = results, isSearching = false, hasSearched = true)
            }
        }
    }
}
