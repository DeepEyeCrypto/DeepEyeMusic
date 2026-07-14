// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.screens

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.deepeye.musicpro.domain.model.home.HomeVideoItem
import com.deepeye.musicpro.ui.components.GlassContainer
import com.deepeye.musicpro.ui.LocalHazeState
import kotlinx.coroutines.launch

/** Categories that show an episode-picker bottom sheet before playing */
private val PLAYLIST_CATEGORIES = setOf("Pakistani Dramas", "WEB Series")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetMirrorScreen(
    modifier: Modifier = Modifier,
    viewModel: NetMirrorViewModel = hiltViewModel(),
    onExpandPlayer: () -> Unit = {}
) {
    val hazeState = LocalHazeState.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val episodeSheet = uiState.episodeSheet
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    if (uiState.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF00E5C3))
        }
        return
    }

    // Episode Picker Bottom Sheet — driven by ViewModel state
    if (episodeSheet != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.closeEpisodePicker() },
            sheetState = sheetState,
            containerColor = Color(0xFF0D0D12),
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        ) {
            EpisodePickerSheet(
                title = episodeSheet.dramaTitle,
                episodes = episodeSheet.episodes,
                isLoading = episodeSheet.isLoading,
                error = episodeSheet.error,
                onEpisodeClick = { index ->
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        viewModel.closeEpisodePicker()
                    }
                    viewModel.playVideoFromCategory(episodeSheet.episodes, index)
                    onExpandPlayer()
                }
            )
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Hero Banner
        uiState.heroMovie?.let { hero ->
            item {
                HeroBanner(
                    movie = hero,
                    onPlay = {
                        viewModel.playVideo(hero)
                        onExpandPlayer()
                    }
                )
            }
        }

        // Bollywood — direct play
        if (uiState.bollywoodMovies.isNotEmpty()) {
            item {
                CategoryRow(
                    title = "Bollywood Movies",
                    items = uiState.bollywoodMovies,
                    isPlaylist = false,
                    onPlayFromIndex = { index ->
                        viewModel.playVideoFromCategory(uiState.bollywoodMovies, index)
                        onExpandPlayer()
                    },
                    onOpenPlaylist = {}
                )
            }
        }

        // Hollywood — direct play
        if (uiState.hollywoodMovies.isNotEmpty()) {
            item {
                CategoryRow(
                    title = "Hollywood Movies",
                    items = uiState.hollywoodMovies,
                    isPlaylist = false,
                    onPlayFromIndex = { index ->
                        viewModel.playVideoFromCategory(uiState.hollywoodMovies, index)
                        onExpandPlayer()
                    },
                    onOpenPlaylist = {}
                )
            }
        }

        // South — direct play
        if (uiState.southDubbedMovies.isNotEmpty()) {
            item {
                CategoryRow(
                    title = "South (Hindi Dubbed)",
                    items = uiState.southDubbedMovies,
                    isPlaylist = false,
                    onPlayFromIndex = { index ->
                        viewModel.playVideoFromCategory(uiState.southDubbedMovies, index)
                        onExpandPlayer()
                    },
                    onOpenPlaylist = {}
                )
            }
        }

        // WEB Series — tap drama card → fetch that show's episodes
        if (uiState.webSeries.isNotEmpty()) {
            item {
                CategoryRow(
                    title = "WEB Series",
                    items = uiState.webSeries,
                    isPlaylist = true,
                    onPlayFromIndex = {},
                    onCardClick = { drama -> viewModel.openEpisodePicker(drama) }
                )
            }
        }

        // Pakistani Dramas — tap drama card → fetch that show's episodes
        if (uiState.pakistaniDramas.isNotEmpty()) {
            item {
                CategoryRow(
                    title = "Pakistani Dramas",
                    items = uiState.pakistaniDramas,
                    isPlaylist = true,
                    onPlayFromIndex = {},
                    onCardClick = { drama -> viewModel.openEpisodePicker(drama) }
                )
            }
        }

        // Kids — direct play
        if (uiState.kids.isNotEmpty()) {
            item {
                CategoryRow(
                    title = "Kids & Cartoons",
                    items = uiState.kids,
                    isPlaylist = false,
                    onPlayFromIndex = { index ->
                        viewModel.playVideoFromCategory(uiState.kids, index)
                        onExpandPlayer()
                    },
                    onOpenPlaylist = {}
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Episode Picker Bottom Sheet Content
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun EpisodePickerSheet(
    title: String,
    episodes: List<HomeVideoItem>,
    isLoading: Boolean = false,
    error: String? = null,
    onEpisodeClick: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Sheet handle + title
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                modifier = Modifier.align(Alignment.CenterStart)
            )
            // Episode count / loading badge
            Surface(
                color = Color(0xFF00E5C3).copy(alpha = 0.15f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                if (isLoading) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            color = Color(0xFF00E5C3),
                            strokeWidth = 1.5.dp
                        )
                        Text("Loading...", color = Color(0xFF00E5C3), fontSize = 12.sp)
                    }
                } else {
                    Text(
                        text = "${episodes.size} Episodes",
                        color = Color(0xFF00E5C3),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(8.dp))

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF00E5C3))
                }
            }
            error != null -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(error, color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)
                ) {
                    itemsIndexed(episodes) { index, episode ->
                        EpisodeListItem(
                            episodeNumber = index + 1,
                            episode = episode,
                            onClick = { onEpisodeClick(index) }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun EpisodeListItem(
    episodeNumber: Int,
    episode: HomeVideoItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Episode number badge
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF00E5C3).copy(alpha = 0.12f))
        ) {
            Text(
                text = "$episodeNumber",
                color = Color(0xFF00E5C3),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        // Thumbnail
        Box(
            modifier = Modifier
                .width(90.dp)
                .height(52.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            AsyncImage(
                model = episode.thumbnailUrl,
                contentDescription = episode.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Play overlay icon
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Title + channel
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = episode.title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = episode.channelName,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Category Row
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CategoryRow(
    title: String,
    items: List<HomeVideoItem>,
    isPlaylist: Boolean,
    onPlayFromIndex: (Int) -> Unit,
    onCardClick: ((HomeVideoItem) -> Unit)? = null,
    onOpenPlaylist: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
    ) {
        // Title row — for playlist categories, entire title row is clickable
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                .then(if (isPlaylist) Modifier.clickable { onOpenPlaylist() } else Modifier),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            if (isPlaylist) {
                Text(
                    text = "All Episodes →",
                    color = Color(0xFF00E5C3),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items.size) { index ->
                VideoCard(
                    movie = items[index],
                    onClick = {
                        when {
                            isPlaylist && onCardClick != null -> onCardClick(items[index])
                            isPlaylist -> onOpenPlaylist()
                            else -> onPlayFromIndex(index)
                        }
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Hero Banner
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun HeroBanner(
    movie: HomeVideoItem,
    onPlay: () -> Unit
) {
    val hazeState = LocalHazeState.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(500.dp)
            .background(Color.Transparent)
    ) {
        AsyncImage(
            model = movie.thumbnailUrl.replace("hqdefault", "maxresdefault"),
            contentDescription = movie.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.8f)
                        ),
                        startY = 300f
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = movie.title,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp
                ),
                maxLines = 2,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = movie.channelName, color = Color.LightGray, fontSize = 14.sp)
                Text(text = "•", color = Color.LightGray, fontSize = 14.sp)
                Text(text = "4K HDR", color = Color(0xFF00E5C3), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = onPlay,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = "Play", tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Play", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                GlassContainer(
                    tintColor = Color.White.copy(alpha = 0.2f),
                    hazeState = hazeState,
                    cornerRadius = 8.dp,
                    modifier = Modifier
                        .height(48.dp)
                        .clickable { /* TODO */ }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(imageVector = Icons.Filled.Info, contentDescription = "More Info", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("More Info", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Video Thumbnail Card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun VideoCard(
    movie: HomeVideoItem,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(160.dp)
            .height(90.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = movie.thumbnailUrl,
            contentDescription = movie.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                    )
                )
        )

        Text(
            text = movie.title,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
