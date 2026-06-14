// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.music

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.deepeye.musicpro.domain.model.Song
import com.deepeye.musicpro.domain.model.home.HomeMusicItem
import com.deepeye.musicpro.ui.components.ShimmerBox
import com.deepeye.musicpro.ui.motion.premiumScrollHaptics
import androidx.compose.animation.core.*
import androidx.compose.ui.geometry.Offset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicScreen(
    onNavigateToNowPlaying: (String) -> Unit,
    onNavigateToSearch: () -> Unit = {},
    viewModel: MusicViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Discovery", "Library")

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = { Text("Music", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
                    actions = {
                        IconButton(onClick = onNavigateToSearch) {
                            Icon(Icons.Default.Search, "Search Music")
                        }
                        if (selectedTab == 0) {
                            IconButton(onClick = { viewModel.loadRecommendations() }) {
                                Icon(Icons.Default.Refresh, "Refresh Recommendations")
                            }
                        } else if (selectedTab == 1) {
                            IconButton(onClick = { viewModel.syncLibrary() }) {
                                Icon(Icons.Default.Refresh, "Sync Library")
                            }
                        }
                    },
                )
                SecondaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    divider = {},
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) },
                        )
                    }
                }
            }
        },
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (selectedTab) {
                0 -> DiscoveryTab(uiState, viewModel, onNavigateToNowPlaying)
                1 -> LibraryTab(uiState, viewModel, onNavigateToNowPlaying)
            }
        }
    }
}

@Composable
private fun DiscoveryTab(
    uiState: MusicUiState,
    viewModel: MusicViewModel,
    onNavigateToNowPlaying: (String) -> Unit,
) {
    if (uiState.isLoading) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 180.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(6) {
                ShimmerBox(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp)))
            }
        }
    } else if (uiState.error != null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Error: ${uiState.error}",
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = { viewModel.loadRecommendations() }) {
                    Text("Retry")
                }
            }
        }
    } else if (uiState.recommendedMusic.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "No recommendations found",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = { viewModel.loadRecommendations() }) {
                    Text("Refresh")
                }
            }
        }
    } else {
        val gridState = rememberLazyGridState()
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().premiumScrollHaptics(gridState),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 180.dp, top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            items(uiState.recommendedMusic, key = { it.id }) { item ->
                MusicGridItem(item, onClick = {
                    viewModel.playMusic(item)
                    onNavigateToNowPlaying(item.id)
                })
            }
        }
    }
}

@Composable
private fun LibraryTab(
    uiState: MusicUiState,
    viewModel: MusicViewModel,
    onNavigateToNowPlaying: (String) -> Unit,
) {
    if (uiState.localSongs.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No local music found", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { viewModel.syncLibrary() }) {
                    Text("Scan for Music")
                }
            }
        }
    } else {
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().premiumScrollHaptics(listState),
            contentPadding = PaddingValues(bottom = 180.dp),
        ) {
            items(uiState.localSongs, key = { it.id }) { song ->
                SongListItem(song, onClick = {
                    viewModel.playMusicLocal(song)
                    onNavigateToNowPlaying(song.id.toString())
                })
            }
        }
    }
}

@Composable
fun SongListItem(
    song: Song,
    onClick: () -> Unit,
) {
    val isHiRes = remember(song.id) { (song.id.hashCode() % 3) == 0 }
    val isFlac = remember(song.id) { (song.id.hashCode() % 2) == 0 }
    val bitrate = remember(song.id) { if (isHiRes) "24bit • 96kHz" else "16bit • 44.1kHz" }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E24).copy(alpha = 0.35f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 0.5.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.08f),
                    Color.White.copy(alpha = 0.02f)
                )
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.05f))
            ) {
                AsyncImage(
                    model = song.artUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )

                if (isHiRes) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .background(Color(0xFFFFB300), RoundedCornerShape(bottomEnd = 4.dp))
                            .padding(horizontal = 3.dp, vertical = 1.dp)
                    ) {
                        Text("HR", color = Color.Black, fontSize = 6.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(3.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = if (isFlac) "FLAC" else "AAC",
                            color = Color.LightGray,
                            fontSize = 6.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = bitrate,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontSize = 8.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = formatDuration(song.duration),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF00D2FF).copy(alpha = 0.12f), RoundedCornerShape(2.dp))
                            .padding(horizontal = 3.dp, vertical = 1.dp)
                    ) {
                        Text("DSP", color = Color(0xFF00D2FF), fontSize = 6.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF7C4DFF).copy(alpha = 0.12f), RoundedCornerShape(2.dp))
                            .padding(horizontal = 3.dp, vertical = 1.dp)
                    ) {
                        Text("V4A", color = Color(0xFF7C4DFF), fontSize = 6.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun MusicGridItem(
    item: HomeMusicItem,
    onClick: () -> Unit,
) {
    val isHiRes = remember(item.id) { (item.id.hashCode() % 3) == 0 }
    val isFlac = remember(item.id) { (item.id.hashCode() % 2) == 0 }
    val bitrate = remember(item.id) { if (isHiRes) "24bit • 48kHz" else "16bit • 44.1kHz" }

    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "shine")
    val shineOffset by infiniteTransition.animateFloat(
        initialValue = -200f,
        targetValue = 400f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(durationMillis = 3000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "shineOffset"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E24).copy(alpha = 0.45f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF00D2FF).copy(alpha = 0.25f),
                    Color(0xFF7C4DFF).copy(alpha = 0.25f),
                )
            )
        )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.05f))
            ) {
                AsyncImage(
                    model = item.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )

                // Animated shine overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithCache {
                            val shineBrush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.0f),
                                    Color.White.copy(alpha = 0.15f),
                                    Color.White.copy(alpha = 0.0f)
                                ),
                                start = androidx.compose.ui.geometry.Offset(shineOffset, 0f),
                                end = androidx.compose.ui.geometry.Offset(shineOffset + 100f, size.height)
                            )
                            onDrawWithContent {
                                drawContent()
                                drawRect(brush = shineBrush)
                            }
                        }
                )

                // Hi-Res Badges
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isHiRes) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFFB300).copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("HI-RES", color = Color.Black, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (isFlac) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF00E676).copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("FLAC", color = Color.Black, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Bitrate Badge at bottom
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(bitrate, color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = item.artist,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(8.dp))

            // DSP/V4A ready badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFF00D2FF).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .border(0.5.dp, Color(0xFF00D2FF).copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text("DSP READY", color = Color(0xFF00D2FF), fontSize = 7.sp, fontWeight = FontWeight.Bold)
                }

                Box(
                    modifier = Modifier
                        .background(Color(0xFF7C4DFF).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .border(0.5.dp, Color(0xFF7C4DFF).copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text("V4A READY", color = Color(0xFF7C4DFF), fontSize = 7.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val sec = (ms / 1000) % 60
    val min = (ms / (1000 * 60)) % 60
    return "%d:%02d".format(min, sec)
}
