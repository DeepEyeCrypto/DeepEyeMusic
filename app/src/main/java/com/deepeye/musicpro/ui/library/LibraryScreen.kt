// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.deepeye.musicpro.core.utils.TimeFormatter
import com.deepeye.musicpro.domain.model.Album
import com.deepeye.musicpro.domain.model.Artist
import com.deepeye.musicpro.domain.model.Song
import com.deepeye.musicpro.domain.model.library.LibraryItem
import com.deepeye.musicpro.ui.components.bouncyClickable
import com.deepeye.musicpro.ui.motion.premiumScrollHaptics
import com.deepeye.musicpro.ui.theme.CardGeometry
import com.deepeye.musicpro.ui.theme.sdp

private val neonCyan = Color(0xFF00E5FF)
private val darkSurface = Color(0xFF131722).copy(alpha = 0.85f)
private val glassBorder = Color(0x22FFFFFF)

@Composable
fun LibraryScreen(
    windowSizeClass: androidx.compose.material3.windowsizeclass.WindowSizeClass,
    onNavigateToAlbum: (Long) -> Unit,
    onNavigateToArtist: (Long) -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToLikedSongs: () -> Unit,
    onNavigateToSavedItems: () -> Unit,
    onNavigateToPlaylists: () -> Unit,
    onNavigateToNowPlaying: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
    playerViewModel: com.deepeye.musicpro.ui.player.PlayerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tabs = listOf("Songs", "Albums", "Artists", "Genres")
    val libraryHome = uiState.libraryHome

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(Color.Transparent)
    ) {
        // ── Cyberpunk Header ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.sdp, vertical = 12.sdp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF7B1FA2), neonCyan))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LibraryMusic,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "DEEPEYE LIBRARY",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Local & Cloud High-Fidelity Audio Vault",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            // Offline Mode Toggle Pill & Downloads Action
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.sdp)
            ) {
                Surface(
                    onClick = { viewModel.toggleOfflineMode() },
                    shape = RoundedCornerShape(16.dp),
                    color = if (uiState.offlineMode) Color(0x3300E5FF) else darkSurface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (uiState.offlineMode) neonCyan else glassBorder
                    ),
                    modifier = Modifier.heightIn(min = 44.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.sdp, vertical = 8.sdp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.sdp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OfflinePin,
                            contentDescription = null,
                            tint = if (uiState.offlineMode) neonCyan else Color.White.copy(0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (uiState.offlineMode) "OFFLINE" else "ONLINE",
                            color = if (uiState.offlineMode) neonCyan else Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(
                    onClick = onNavigateToDownloads,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(darkSurface)
                        .border(1.dp, glassBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Downloads",
                        tint = neonCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // ── Quick Access Frosted Tiles ──
        val quickCardsState = rememberLazyListState()
        LazyRow(
            state = quickCardsState,
            contentPadding = PaddingValues(horizontal = 20.sdp, vertical = 6.sdp),
            horizontalArrangement = Arrangement.spacedBy(14.sdp),
            modifier = Modifier.fillMaxWidth().premiumScrollHaptics(quickCardsState),
        ) {
            item {
                LibraryQuickCard(
                    icon = Icons.Default.Favorite,
                    label = "Liked",
                    count = libraryHome.likedCount,
                    accentColor = Color(0xFFE91E63),
                    onClick = onNavigateToLikedSongs,
                )
            }
            item {
                LibraryQuickCard(
                    icon = Icons.Default.Bookmark,
                    label = "Saved",
                    count = 0,
                    accentColor = Color(0xFF9C27B0),
                    onClick = onNavigateToSavedItems,
                )
            }
            item {
                LibraryQuickCard(
                    icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                    label = "Playlists",
                    count = libraryHome.playlistCount,
                    accentColor = Color(0xFF2196F3),
                    onClick = onNavigateToPlaylists,
                )
            }
            item {
                LibraryQuickCard(
                    icon = Icons.Default.Download,
                    label = "Downloads",
                    count = libraryHome.downloadCount,
                    accentColor = Color(0xFF00E676),
                    onClick = onNavigateToDownloads,
                )
            }
            item {
                LibraryQuickCard(
                    icon = Icons.Default.History,
                    label = "Recent",
                    count = libraryHome.recentCount,
                    accentColor = Color(0xFFFF9100),
                    onClick = onNavigateToHistory,
                )
            }
            item {
                LibraryQuickCard(
                    icon = Icons.Default.OfflinePin,
                    label = "Offline",
                    count = libraryHome.downloadCount,
                    accentColor = neonCyan,
                    onClick = onNavigateToDownloads,
                )
            }
        }

        Spacer(Modifier.height(12.sdp))

        // ── Horizontal Media Tabs Ribbon ──
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.sdp, vertical = 6.sdp),
            horizontalArrangement = Arrangement.spacedBy(10.sdp)
        ) {
            itemsIndexed(tabs) { index, title ->
                val isSelected = uiState.selectedTab == index
                Surface(
                    onClick = { viewModel.selectTab(index) },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) Color(0x3300E5FF) else darkSurface,
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) neonCyan else glassBorder
                    ),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 20.sdp, vertical = 10.sdp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            color = if (isSelected) neonCyan else Color.White,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.sdp))

        // ── Content Area ──
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (uiState.selectedTab) {
                0 -> SongsTab(uiState.songs) { song ->
                    val mediaItems = uiState.songs.map { com.deepeye.musicpro.domain.model.MediaItem.Local(it) }
                    val index = uiState.songs.indexOfFirst { it.id == song.id }
                    playerViewModel.setQueue(mediaItems, if (index >= 0) index else 0)
                    onNavigateToNowPlaying()
                }
                1 -> AlbumsTab(uiState.albums, onNavigateToAlbum)
                2 -> ArtistsTab(uiState.artists, onNavigateToArtist)
                3 -> GenresTab()
            }
        }
    }
}

/**
 * Formats a collection size for the quick-access tiles: "0 items", "1 item", "12 items".
 * Internal rather than private so the pluralisation is unit-testable.
 */
internal fun itemCountLabel(count: Int): String =
    if (count == 1) "1 item" else "$count items"

// ── Quick Access Tile ──

/**
 * One quick-access tile. Internal rather than private so its font-scale
 * behaviour is testable — see LibraryQuickCardFontScaleTest (androidTest).
 *
 * Must stay `internal`: a Robolectric (src/test) version of that test was tried
 * first and proved worthless — Robolectric measures "Downloads" as a constant
 * 10px that ignores fontScale entirely, so no card width ever wraps and the
 * assertion passes even against the original buggy `.width(160.dp)`. Only the
 * instrumented runtime measures text for real.
 */
@Composable
internal fun LibraryQuickCard(
    icon: ImageVector,
    label: String,
    count: Int,
    accentColor: Color,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            // Adaptive floor from the SSOT. Was a bare `160.dp` literal, which
            // meant the floor here could not be retuned alongside the grids in
            // this same file without editing both.
            .widthIn(min = CardGeometry.Music.minWidth, max = 260.dp)
            .height(84.dp)
            .clip(CardGeometry.Music.shape)
            .bouncyClickable(downScale = 0.95f, onClick = onClick),
        shape = CardGeometry.Music.shape,
        colors = CardDefaults.cardColors(containerColor = darkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, glassBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 16.sdp, vertical = 14.sdp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.sdp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentColor.copy(alpha = 0.18f))
                    .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accentColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = label,
                    fontSize = 15.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = itemCountLabel(count),
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.LibraryMusic,
            contentDescription = null,
            tint = neonCyan.copy(alpha = 0.4f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.sdp))
        Text(
            text = message,
            fontSize = 17.sp,
            color = Color.White.copy(alpha = 0.7f),
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ── Songs Tab ──

@Composable
private fun SongsTab(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
) {
    if (songs.isEmpty()) {
        EmptyLibraryState("No local songs found in storage.")
        return
    }
    val songsState = rememberLazyListState()
    LazyColumn(
        state = songsState,
        modifier = Modifier.premiumScrollHaptics(songsState),
        contentPadding = PaddingValues(horizontal = 20.sdp, vertical = 12.sdp),
        verticalArrangement = Arrangement.spacedBy(10.sdp)
    ) {
        items(songs, key = { it.id }) { song ->
            Surface(
                onClick = { onSongClick(song) },
                shape = RoundedCornerShape(18.dp),
                color = darkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, glassBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 68.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        model = song.artUri,
                        contentDescription = null,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentScale = ContentScale.Crop,
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            fontSize = 16.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = "${song.artist} · ${TimeFormatter.formatDuration(song.duration)}",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.65f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { /* More Options */ }, modifier = Modifier.size(44.dp)) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── Albums Tab ──

@Composable
private fun AlbumsTab(
    albums: List<Album>,
    onAlbumClick: (Long) -> Unit,
) {
    if (albums.isEmpty()) {
        EmptyLibraryState("No local albums found.")
        return
    }
    val gridState = rememberLazyGridState()
    LazyVerticalGrid(
        state = gridState,
        modifier = Modifier.premiumScrollHaptics(gridState),
        // Adaptive, not Fixed(columns). The old `GridCells.Fixed(columns)` derived
        // 2/3/4 from WindowWidthSizeClass, which left a partly-filled final row
        // and a fixed column count that could not exploit extra landscape width.
        // Adaptive fits as many 1:1 album tiles as naturally fit.
        columns = GridCells.Adaptive(CardGeometry.Music.minWidth),
        contentPadding = PaddingValues(
            horizontal = CardGeometry.ScreenGutter,
            vertical = 12.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(CardGeometry.CardSpacing),
        verticalArrangement = Arrangement.spacedBy(CardGeometry.CardSpacing),
    ) {
        items(albums, key = { it.id }) { album ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CardGeometry.Music.shape)
                    .bouncyClickable(downScale = 0.95f, onClick = { onAlbumClick(album.id) }),
                shape = CardGeometry.Music.shape,
                colors = CardDefaults.cardColors(containerColor = darkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, glassBorder)
            ) {
                Column(modifier = Modifier.padding(CardGeometry.Music.contentPadding)) {
                    AsyncImage(
                        model = album.artUri,
                        contentDescription = album.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(CardGeometry.Music.aspectRatio)
                            .clip(RoundedCornerShape(CardGeometry.Music.cornerRadius / 1.5f)),
                        contentScale = ContentScale.Crop,
                    )
                    Spacer(Modifier.height(8.sdp))
                    Text(
                        text = album.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = CardGeometry.Music.titleMaxLines,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = album.artist,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ── Artists Tab ──

@Composable
private fun ArtistsTab(
    artists: List<Artist>,
    onArtistClick: (Long) -> Unit,
) {
    if (artists.isEmpty()) {
        EmptyLibraryState("No local artists found.")
        return
    }
    val gridState = rememberLazyGridState()
    LazyVerticalGrid(
        state = gridState,
        modifier = Modifier.premiumScrollHaptics(gridState),
        columns = GridCells.Adaptive(CardGeometry.Music.minWidth),
        contentPadding = PaddingValues(
            horizontal = CardGeometry.ScreenGutter,
            vertical = 12.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(CardGeometry.CardSpacing),
        verticalArrangement = Arrangement.spacedBy(CardGeometry.CardSpacing),
    ) {
        items(artists, key = { it.id }) { artist ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CardGeometry.Music.shape)
                    .bouncyClickable(downScale = 0.95f, onClick = { onArtistClick(artist.id) }),
                shape = CardGeometry.Music.shape,
                colors = CardDefaults.cardColors(containerColor = darkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, glassBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(Color(0x3300E5FF))
                            .border(1.5.dp, neonCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = artist.name.take(1).uppercase(),
                            color = neonCyan,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(Modifier.height(12.sdp))
                    Text(
                        text = artist.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${artist.songCount} songs",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

// ── Genres Tab ──

@Composable
private fun GenresTab() {
    EmptyLibraryState("Smart Genre Classification active.")
}
