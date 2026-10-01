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
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.*
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
import com.deepeye.musicpro.ui.components.bouncyClickable
import com.deepeye.musicpro.ui.theme.CardGeometry
import kotlinx.coroutines.launch

private val neonCyan = Color(0xFF00E5FF)
private val darkSurface = Color(0xFF131722).copy(alpha = 0.85f)
private val glassBorder = Color(0x22FFFFFF)

/**
 * Width of one Video Hub card.
 *
 * Sized from the viewport (see evenCarouselCardWidth at the call site) so the
 * rail fills its row; this is only the fallback for previews and narrow windows.
 * It is intentionally derived from the SSOT floor rather than re-declared, so a
 * change to CardGeometry.Video.minWidth cannot leave this card below the width
 * the adaptive layout was built for.
 */
private val VideoHubMovieCardWidth = CardGeometry.Video.minWidth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetMirrorScreen(
    modifier: Modifier = Modifier,
    viewModel: NetMirrorViewModel = hiltViewModel(),
    onExpandPlayer: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val episodeSheet = uiState.episodeSheet
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    if (uiState.isLoading) {
        Box(
            modifier = modifier.fillMaxSize().background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = neonCyan, strokeWidth = 3.dp)
        }
        return
    }

    // Episode Picker Bottom Sheet
    if (episodeSheet != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.closeEpisodePicker() },
            sheetState = sheetState,
            containerColor = Color(0xFF090B10),
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 8.dp)
                        .width(48.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.3f))
                )
            }
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
        modifier = modifier.fillMaxSize().background(Color.Transparent),
        contentPadding = PaddingValues(bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        // Futuristic Hero Movie Banner
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

        // Bollywood Movies
        if (uiState.bollywoodMovies.isNotEmpty()) {
            item {
                CategoryRow(
                    title = "Bollywood Blockbusters",
                    badge = "4K HDR",
                    items = uiState.bollywoodMovies,
                    isPlaylist = false,
                    onPlayFromIndex = { index ->
                        viewModel.playVideoFromCategory(uiState.bollywoodMovies, index)
                        onExpandPlayer()
                    }
                )
            }
        }

        // Hollywood Movies
        if (uiState.hollywoodMovies.isNotEmpty()) {
            item {
                CategoryRow(
                    title = "Hollywood Cinema",
                    badge = "DUBBED",
                    items = uiState.hollywoodMovies,
                    isPlaylist = false,
                    onPlayFromIndex = { index ->
                        viewModel.playVideoFromCategory(uiState.hollywoodMovies, index)
                        onExpandPlayer()
                    }
                )
            }
        }

        // South Indian Movies
        if (uiState.southDubbedMovies.isNotEmpty()) {
            item {
                CategoryRow(
                    title = "South Blockbusters",
                    badge = "HINDI",
                    items = uiState.southDubbedMovies,
                    isPlaylist = false,
                    onPlayFromIndex = { index ->
                        viewModel.playVideoFromCategory(uiState.southDubbedMovies, index)
                        onExpandPlayer()
                    }
                )
            }
        }

        // WEB Series
        if (uiState.webSeries.isNotEmpty()) {
            item {
                CategoryRow(
                    title = "Web Series & Originals",
                    badge = "SERIES",
                    items = uiState.webSeries,
                    isPlaylist = true,
                    onPlayFromIndex = {},
                    onCardClick = { drama -> viewModel.openEpisodePicker(drama) }
                )
            }
        }

        // Pakistani Dramas
        if (uiState.pakistaniDramas.isNotEmpty()) {
            item {
                CategoryRow(
                    title = "Popular Dramas",
                    badge = "EPISODES",
                    items = uiState.pakistaniDramas,
                    isPlaylist = true,
                    onPlayFromIndex = {},
                    onCardClick = { drama -> viewModel.openEpisodePicker(drama) }
                )
            }
        }

        // Kids & Animation
        if (uiState.kids.isNotEmpty()) {
            item {
                CategoryRow(
                    title = "Kids & Animation",
                    badge = "FAMILY",
                    items = uiState.kids,
                    isPlaylist = false,
                    onPlayFromIndex = { index ->
                        viewModel.playVideoFromCategory(uiState.kids, index)
                        onExpandPlayer()
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Futuristic Hero Movie Banner
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun HeroBanner(
    movie: HomeVideoItem,
    onPlay: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(26.dp))
            .border(1.dp, glassBorder, RoundedCornerShape(26.dp))
    ) {
        AsyncImage(
            model = movie.thumbnailUrl.replace("hqdefault", "maxresdefault"),
            contentDescription = movie.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.5f),
                            Color(0xFF090B10).copy(alpha = 0.95f)
                        ),
                        startY = 80f
                    )
                )
        )

        // Banner Content
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFFF0033), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("FEATURED CINEMA", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .background(Color(0x3300E5FF), RoundedCornerShape(8.dp))
                        .border(1.dp, neonCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("4K ULTRA HD", color = neonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Text(
                text = movie.title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                lineHeight = 28.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = movie.channelName,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                // High-End Play Button (64dp touch target)
                Surface(
                    onClick = onPlay,
                    shape = RoundedCornerShape(18.dp),
                    color = neonCyan,
                    modifier = Modifier.heightIn(min = 52.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Watch Now",
                            color = Color.Black,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Category Carousel Row
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CategoryRow(
    title: String,
    badge: String,
    items: List<HomeVideoItem>,
    isPlaylist: Boolean,
    onPlayFromIndex: (Int) -> Unit,
    onCardClick: ((HomeVideoItem) -> Unit)? = null,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Category Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(neonCyan)
                )
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Box(
                    modifier = Modifier
                        .background(Color(0x1AFFFFFF), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badge,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (isPlaylist) {
                Text(
                    text = "Browse Episodes →",
                    color = neonCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Horizontal Movies Row (260dp width cards)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(items.size) { index ->
                VideoHubMovieCard(
                    movie = items[index],
                    isPlaylist = isPlaylist,
                    onClick = {
                        if (isPlaylist && onCardClick != null) {
                            onCardClick(items[index])
                        } else {
                            onPlayFromIndex(index)
                        }
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// VideoHub Movie Card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun VideoHubMovieCard(
    movie: HomeVideoItem,
    isPlaylist: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(VideoHubMovieCardWidth)
            .clip(CardGeometry.Video.shape)
            .bouncyClickable(downScale = 0.95f, onClick = onClick),
        shape = CardGeometry.Video.shape,
        colors = CardDefaults.cardColors(containerColor = darkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, glassBorder)
    ) {
        Column {
            // 16:9 Thumbnail Poster — ratio and radius from CardGeometry (SSOT),
            // so VideoCardSkeleton reserves exactly this box.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(CardGeometry.Video.aspectRatio)
                    .clip(
                        RoundedCornerShape(
                            topStart = CardGeometry.Video.cornerRadius,
                            topEnd = CardGeometry.Video.cornerRadius,
                        )
                    )
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
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.8f)
                                )
                            )
                        )
                )

                // Play icon pill overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .border(1.dp, neonCyan.copy(alpha = 0.6f), RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaylist) Icons.Filled.Movie else Icons.Filled.PlayArrow,
                        contentDescription = "Play",
                        tint = neonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Title & Channel
            Column(
                modifier = Modifier.padding(CardGeometry.Video.contentPadding),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = movie.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = CardGeometry.Video.titleMaxLines,
                    lineHeight = 18.sp,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = movie.channelName,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = "${episodes.size} Episodes Available",
                    fontSize = 13.sp,
                    color = neonCyan,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = glassBorder)
        Spacer(Modifier.height(16.dp))

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = neonCyan)
                }
            }
            error != null -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(error, color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
                }
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)
                ) {
                    itemsIndexed(episodes) { index, episode ->
                        Surface(
                            onClick = { onEpisodeClick(index) },
                            shape = RoundedCornerShape(16.dp),
                            color = darkSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, glassBorder),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0x3300E5FF))
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        color = neonCyan,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = episode.title,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = episode.channelName,
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = "Play",
                                    tint = neonCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
