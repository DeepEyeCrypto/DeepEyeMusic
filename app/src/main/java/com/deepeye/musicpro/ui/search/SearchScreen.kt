package com.deepeye.musicpro.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.deepeye.musicpro.core.utils.TimeFormatter

@Composable
fun SearchScreen(
    onNavigateToNowPlaying: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        Text("Search", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(20.dp))

        OutlinedTextField(
            value = uiState.query,
            onValueChange = { viewModel.onQueryChanged(it) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            placeholder = { Text("Search songs, albums, artists…") },
            leadingIcon = { Icon(Icons.Filled.Search, "Search") },
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        Spacer(Modifier.height(16.dp))

        when {
            uiState.isSearching -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            uiState.hasSearched && uiState.results.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No results found", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            else -> LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                items(uiState.results, key = { it.id }) { song ->
                    Row(Modifier.fillMaxWidth().clickable { onNavigateToNowPlaying() }.padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(model = song.artUri, contentDescription = null, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(song.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${song.artist} · ${TimeFormatter.formatDuration(song.duration)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
