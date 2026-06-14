// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.deepeye.musicpro.core.utils.TimeFormatter
import com.deepeye.musicpro.domain.model.Album
import com.deepeye.musicpro.domain.model.Artist
import com.deepeye.musicpro.domain.model.Song
import com.deepeye.musicpro.domain.model.library.LibraryItem
import com.deepeye.musicpro.ui.motion.premiumScrollHaptics

@Composable
fun LibraryScreen(
    windowSizeClass: androidx.compose.material3.windowsizeclass.WindowSizeClass,
    onNavigateToAlbum: (Long) -> Unit,
    onNavigateToArtist: (Long) -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToNowPlaying: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
    playerViewModel: com.deepeye.musicpro.ui.player.PlayerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tabs = listOf("Songs", "Albums", "Artists", "Genres")
    val libraryHome = uiState.libraryHome
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        // ── Header ──
        com.deepeye.musicpro.ui.components.GlassTopAppBar(
            title = {
                Text(
                    text = "Library",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )
            },
            actions = {
                FilterChip(
                    selected = uiState.offlineMode,
                    onClick = { viewModel.toggleOfflineMode() },
                    label = { Text("Offline Mode") },
                    leadingIcon = if (uiState.offlineMode) {
                        {
                            Icon(
                                Icons.Default.OfflinePin,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    } else {
                        null
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onNavigateToDownloads) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Downloads",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        )

        // ── Quick Access Cards ──
        val quickCardsState = rememberLazyListState()
        LazyRow(
            state = quickCardsState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().premiumScrollHaptics(quickCardsState),
        ) {
            item {
                LibraryQuickCard(
                    icon = Icons.Default.Favorite,
                    label = "Liked",
                    count = libraryHome.likedCount,
                    gradientColors = listOf(Color(0xFFE91E63), Color(0xFFC2185B)),
                    onClick = { android.widget.Toast.makeText(context, "Liked songs coming soon", android.widget.Toast.LENGTH_SHORT).show() },
                )
            }
            item {
                LibraryQuickCard(
                    icon = Icons.Default.Bookmark,
                    label = "Saved",
                    count = 0,
                    gradientColors = listOf(Color(0xFF7C4DFF), Color(0xFF651FFF)),
                    onClick = { android.widget.Toast.makeText(context, "Saved items coming soon", android.widget.Toast.LENGTH_SHORT).show() },
                )
            }
            item {
                LibraryQuickCard(
                    icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                    label = "Playlists",
                    count = libraryHome.playlistCount,
                    gradientColors = listOf(Color(0xFF00BCD4), Color(0xFF0097A7)),
                    onClick = { android.widget.Toast.makeText(context, "Playlists coming soon", android.widget.Toast.LENGTH_SHORT).show() },
                )
            }
            item {
                LibraryQuickCard(
                    icon = Icons.Default.Download,
                    label = "Downloads",
                    count = libraryHome.downloadCount,
                    gradientColors = listOf(Color(0xFF4CAF50), Color(0xFF388E3C)),
                    onClick = onNavigateToDownloads,
                )
            }
            item {
                LibraryQuickCard(
                    icon = Icons.Default.History,
                    label = "Recent",
                    count = libraryHome.recentCount,
                    gradientColors = listOf(Color(0xFFFF9800), Color(0xFFF57C00)),
                    onClick = { android.widget.Toast.makeText(context, "Recent plays coming soon", android.widget.Toast.LENGTH_SHORT).show() },
                )
            }
            item {
                LibraryQuickCard(
                    icon = Icons.Default.OfflinePin,
                    label = "Offline",
                    count = libraryHome.downloadCount,
                    gradientColors = listOf(Color(0xFF607D8B), Color(0xFF455A64)),
                    onClick = { android.widget.Toast.makeText(context, "Offline downloads coming soon", android.widget.Toast.LENGTH_SHORT).show() },
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Liked Tracks Preview ──
        LibrarySectionHeader("Liked songs", libraryHome.likedTracks.size)
        if (libraryHome.likedTracks.isNotEmpty()) {
            val likedState = rememberLazyListState()
            LazyRow(
                state = likedState,
                modifier = Modifier.premiumScrollHaptics(likedState),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(libraryHome.likedTracks.take(10), key = { it.id }) { item ->
                    LibraryItemCard(item)
                }
            }
        } else {
            EmptyLibraryState("No liked songs yet.")
        }
        Spacer(Modifier.height(12.dp))

        // ── Recently Played Preview ──
        LibrarySectionHeader("Recently played", libraryHome.recentPlays.size)
        if (libraryHome.recentPlays.isNotEmpty()) {
            val recentState = rememberLazyListState()
            LazyRow(
                state = recentState,
                modifier = Modifier.premiumScrollHaptics(recentState),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(libraryHome.recentPlays.take(10), key = { it.id }) { item ->
                    LibraryItemCard(item)
                }
            }
        } else {
            EmptyLibraryState("No recent plays.")
        }
        Spacer(Modifier.height(12.dp))

        // ── Local Media Tabs ──
        TabRow(selectedTabIndex = uiState.selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = uiState.selectedTab == index,
                    onClick = { viewModel.selectTab(index) },
                    text = { Text(title) },
                )
            }
        }

        when (uiState.selectedTab) {
            0 ->
                SongsTab(uiState.songs) { song ->
                    val mediaItems = uiState.songs.map { com.deepeye.musicpro.domain.model.MediaItem.Local(it) }
                    val index = uiState.songs.indexOfFirst { it.id == song.id }
                    playerViewModel.setQueue(mediaItems, if (index >= 0) index else 0)
                    onNavigateToNowPlaying()
                }
            1 -> AlbumsTab(
                uiState.albums,
                onNavigateToAlbum,
                windowSizeClass
            )
            2 -> ArtistsTab(uiState.artists, onNavigateToArtist)
            3 -> GenresTab()
        }
    }
}

// ── Quick Access Card ──

@Composable
private fun LibraryQuickCard(
    icon: ImageVector,
    label: String,
    count: Int,
    gradientColors: List<Color>,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(110.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Box(
            modifier =
            Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(gradientColors), RoundedCornerShape(20.dp))
                .padding(vertical = 16.dp, horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(28.dp))
                Spacer(Modifier.height(6.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                if (count > 0) {
                    Text("$count", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                }
            }
        }
    }
}

// ── Empty State ──

@Composable
private fun EmptyLibraryState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.LibraryMusic,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}

// ── Section Header ──

@Composable
private fun LibrarySectionHeader(
    title: String,
    count: Int,
) {
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── Library Item Card ──

@Composable
private fun LibraryItemCard(item: LibraryItem) {
    Column(
        modifier = Modifier.width(120.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AsyncImage(
            model = item.artworkUrl,
            contentDescription = item.title,
            modifier =
            Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = item.subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ── Existing Tabs ──

@Composable
private fun SongsTab(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
) {
    if (songs.isEmpty()) {
        EmptyLibraryState("No local songs found.")
        return
    }
    val songsState = rememberLazyListState()
    LazyColumn(
        state = songsState,
        modifier = Modifier.premiumScrollHaptics(songsState),
        contentPadding = PaddingValues(top = 8.dp, bottom = 180.dp)
    ) {
        items(songs, key = { it.id }) { song ->
            Row(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { onSongClick(song) }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AsyncImage(
                    model = song.artUri,
                    contentDescription = null,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop,
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        song.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${song.artist} · ${TimeFormatter.formatDuration(song.duration)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { /* TODO: More options */ }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumsTab(
    albums: List<Album>,
    onAlbumClick: (Long) -> Unit,
    windowSizeClass: androidx.compose.material3.windowsizeclass.WindowSizeClass,
) {
    if (albums.isEmpty()) {
        EmptyLibraryState("No local albums found.")
        return
    }
    val columns =
        when (windowSizeClass.widthSizeClass) {
            androidx.compose.material3.windowsizeclass.WindowWidthSizeClass.Compact -> 2
            androidx.compose.material3.windowsizeclass.WindowWidthSizeClass.Medium -> 3
            else -> 4
        }
    val gridState = rememberLazyGridState()
    LazyVerticalGrid(
        state = gridState,
        modifier = Modifier.premiumScrollHaptics(gridState),
        columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 180.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(albums, key = { it.id }) { album ->
            Column(Modifier.clickable { onAlbumClick(album.id) }) {
                AsyncImage(
                    model = album.artUri,
                    contentDescription = album.title,
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    album.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    album.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ArtistsTab(
    artists: List<Artist>,
    onArtistClick: (Long) -> Unit,
) {
    if (artists.isEmpty()) {
        EmptyLibraryState("No local artists found.")
        return
    }
    val artistsState = rememberLazyListState()
    LazyColumn(
        state = artistsState,
        modifier = Modifier.premiumScrollHaptics(artistsState),
        contentPadding = PaddingValues(top = 8.dp, bottom = 180.dp)
    ) {
        items(artists, key = { it.id }) { artist ->
            Row(
                Modifier.fillMaxWidth().clickable {
                    onArtistClick(artist.id)
                }.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(artist.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${artist.albumCount} albums · ${artist.songCount} songs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun GenresTab() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            "Genres coming soon",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
