package com.deepeye.musicpro.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.deepeye.musicpro.domain.model.Album
import com.deepeye.musicpro.domain.model.Song
import com.deepeye.musicpro.ui.components.GlowCard
import com.deepeye.musicpro.ui.components.ShimmerBox

@Composable
fun HomeScreen(
    onNavigateToNowPlaying: () -> Unit,
    onNavigateToAlbum: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    playerViewModel: com.deepeye.musicpro.ui.player.PlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // ── Header ──
        item {
            Text(
                text = "Good Evening",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }

        // ── Featured Albums ──
        item {
            SectionHeader(title = "Featured Albums")
            if (uiState.isLoading) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(5) {
                        ShimmerBox(
                            modifier = Modifier
                                .size(160.dp)
                                .clip(RoundedCornerShape(16.dp))
                        )
                    }
                }
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.featuredAlbums, key = { it.id }) { album ->
                        FeaturedAlbumCard(
                            album = album,
                            onClick = { onNavigateToAlbum(album.id) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // ── Recently Added ──
        item {
            SectionHeader(title = "Recently Added")
        }

        if (uiState.isLoading) {
            items(5) {
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }
        } else if (uiState.recentlyAdded.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No music found on this device",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(uiState.recentlyAdded, key = { it.id }) { song ->
                SongListItem(
                    song = song,
                    onClick = { 
                        val mediaItems = uiState.recentlyAdded.map { com.deepeye.musicpro.domain.model.MediaItem.Local(it) }
                        val index = uiState.recentlyAdded.indexOf(song)
                        playerViewModel.setQueue(mediaItems, index)
                        onNavigateToNowPlaying() 
                    }
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
    )
}

@Composable
private fun FeaturedAlbumCard(
    album: Album,
    onClick: () -> Unit
) {
    GlowCard(
        modifier = Modifier
            .width(160.dp)
            .clickable { onClick() }
    ) {
        Column {
            AsyncImage(
                model = album.artUri,
                contentDescription = album.title,
                modifier = Modifier
                    .size(128.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = album.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = album.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SongListItem(
    song: Song,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = song.artUri,
            contentDescription = "Album art for ${song.title}",
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${song.artist} · ${song.album}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
