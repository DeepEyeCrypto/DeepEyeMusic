package com.deepeye.musicpro.ui.artist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.musicpro.domain.model.search.SearchFilter
import com.deepeye.musicpro.domain.model.search.SearchResultItem
import com.deepeye.musicpro.domain.recommendation.ContentFetcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Artist page.
 *
 * State persistence strategy:
 * - `artistName` is read from SavedStateHandle (navigation argument) — survives rotation & process death.
 * - `_uiState` is held in MutableStateFlow.
 * - Note: ViewModel instances survive rotation. On process death recreation, the artist data
 *   is re-fetched using the restored `artistName`.
 */
@HiltViewModel
class ArtistViewModel
@Inject
constructor(
    private val contentFetcher: ContentFetcher,
    private val playerController: com.deepeye.musicpro.player.controller.PlayerController,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val artistName: String = savedStateHandle.get<String>("artistName") ?: ""

    private val _uiState = MutableStateFlow(
        savedStateHandle.get<ArtistPageState>("artist_state") ?: ArtistPageState(artistName = artistName, isLoading = true)
    )
    val uiState = _uiState.asStateFlow()

    init {
        loadArtistData()
    }

    private fun loadArtistData() {
        if (artistName.isBlank()) {
            _uiState.value = _uiState.value.copy(isLoading = false)
            return
        }

        // Return immediately if state was already restored from SavedStateHandle
        if (savedStateHandle.contains("artist_state")) {
            return
        }

        viewModelScope.launch {
            try {
                // Fetch songs for the artist
                val songs =
                    contentFetcher.searchByArtist(artistName, 10).map { video ->
                        SearchResultItem(
                            id = video.videoId,
                            title = video.title,
                            subtitle = video.artist,
                            type = SearchFilter.SONGS,
                            thumbnailUrl = "https://img.youtube.com/vi/${video.videoId}/hqdefault.jpg",
                            artist = video.artist,
                            videoId = video.videoId,
                            channelId = video.channelId,
                        )
                    }

                val newState = _uiState.value.copy(
                    isLoading = false,
                    topSongs = songs,
                    subscribersText = "2M+ listeners",
                    heroImageUrl = songs.firstOrNull()?.thumbnailUrl,
                )
                _uiState.value = newState
                savedStateHandle["artist_state"] = newState
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                )
            }
        }
    }

    private fun getMediaItems() =
        _uiState.value.topSongs.map { remote ->
            com.deepeye.musicpro.domain.model.MediaItem.Remote(
                id = remote.id,
                title = remote.title,
                artist = remote.artist ?: "",
                artworkUri = remote.thumbnailUrl?.let { android.net.Uri.parse(it) } ?: android.net.Uri.EMPTY,
                duration = 0,
            )
        }

    fun playAll() {
        val items = getMediaItems()
        if (items.isNotEmpty()) {
            playerController.setQueue(items, 0)
        }
    }

    fun shuffleAll() {
        val items = getMediaItems()
        if (items.isNotEmpty()) {
            playerController.setQueue(items.shuffled(), 0)
        }
    }

    fun playSong(song: SearchResultItem) {
        val items = getMediaItems()
        val index = items.indexOfFirst { it.id == song.id }
        if (items.isNotEmpty()) {
            playerController.setQueue(items, if (index >= 0) index else 0)
        }
    }
}
