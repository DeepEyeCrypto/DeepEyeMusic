package com.deepeye.musicpro.ui.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.deepeye.musicpro.core.utils.TimeFormatter
import com.deepeye.musicpro.domain.model.Artist
import com.deepeye.musicpro.domain.model.Song
import com.deepeye.musicpro.domain.usecase.GetArtistByIdUseCase
import com.deepeye.musicpro.domain.usecase.GetSongsByArtistUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArtistDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getArtistByIdUseCase: GetArtistByIdUseCase,
    getSongsByArtistUseCase: GetSongsByArtistUseCase
) : ViewModel() {
    private val artistId: Long = savedStateHandle.get<Long>("artistId") ?: 0L
    private val _artist = MutableStateFlow<Artist?>(null)
    val artist: StateFlow<Artist?> = _artist.asStateFlow()
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    init {
        viewModelScope.launch { getArtistByIdUseCase(artistId).collect { _artist.value = it } }
        viewModelScope.launch { getSongsByArtistUseCase(artistId).collect { _songs.value = it } }
    }
}

@Composable
fun ArtistDetailScreen(
    artistId: Long,
    onNavigateBack: () -> Unit,
    viewModel: ArtistDetailViewModel = hiltViewModel()
) {
    val artist by viewModel.artist.collectAsStateWithLifecycle()
    val songs by viewModel.songs.collectAsStateWithLifecycle()

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                Text(artist?.name ?: "", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(start = 8.dp))
            }
            artist?.let {
                Text("${it.albumCount} albums · ${it.songCount} songs", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 20.dp))
            }
            Spacer(Modifier.height(16.dp))
        }
        items(songs, key = { it.id }) { song ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(song.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(song.album, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(TimeFormatter.formatDuration(song.duration), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
