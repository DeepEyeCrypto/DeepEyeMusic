package com.deepeye.musicpro.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.musicpro.domain.model.Album
import com.deepeye.musicpro.domain.model.Song
import com.deepeye.musicpro.domain.usecase.GetAllAlbumsUseCase
import com.deepeye.musicpro.domain.usecase.GetRecentlyAddedUseCase
import com.deepeye.musicpro.domain.usecase.SyncLibraryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val recentlyAdded: List<Song> = emptyList(),
    val featuredAlbums: List<Album> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getRecentlyAddedUseCase: GetRecentlyAddedUseCase,
    private val getAllAlbumsUseCase: GetAllAlbumsUseCase,
    private val syncLibraryUseCase: SyncLibraryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                syncLibraryUseCase()
            } catch (e: Exception) {
                // Non-fatal — library may already be synced
            }
        }

        viewModelScope.launch {
            getRecentlyAddedUseCase(20).collect { songs ->
                _uiState.value = _uiState.value.copy(
                    recentlyAdded = songs,
                    isLoading = false
                )
            }
        }

        viewModelScope.launch {
            getAllAlbumsUseCase().collect { albums ->
                _uiState.value = _uiState.value.copy(
                    featuredAlbums = albums.take(10)
                )
            }
        }
    }

    fun refresh() = loadData()
}
