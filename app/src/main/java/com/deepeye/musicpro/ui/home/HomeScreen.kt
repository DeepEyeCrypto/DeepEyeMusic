// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.home

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.deepeye.musicpro.domain.model.MediaItem
import com.deepeye.musicpro.domain.recommendation.RecommendationRow
import com.deepeye.musicpro.domain.recommendation.VideoItem
import com.deepeye.musicpro.ui.components.ShimmerBox

val TealGlow = Color(0xFF00FFCC)
val White = Color.White

@Composable
fun HomeScreen(
    onNavigateToNowPlaying: () -> Unit,
    onNavigateToAlbum: (Long) -> Unit,
    viewModel: RecommendationViewModel = hiltViewModel(),
    playerViewModel: com.deepeye.musicpro.ui.player.PlayerViewModel = hiltViewModel(),
    windowSizeClass: WindowSizeClass
) {
    val recs by viewModel.recommendations.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LazyColumn(Modifier.fillMaxSize()) {
        // ── Hero section ──────────────────────────
        item {
            HeroSection()
        }

        // ── Recommendation rows ───────────────────
        recs?.let { result ->

            // Because you listened
            items(result.becauseYouListened) { row ->
                RecommendationRowUI(row, windowSizeClass) { video ->
                    playVideo(video, row.items, playerViewModel, onNavigateToNowPlaying)
                }
            }

            // Perfect for right now
            item {
                RecommendationRowUI(result.perfectForNow, windowSizeClass, isHighlighted = true) { video ->
                    playVideo(video, result.perfectForNow.items, playerViewModel, onNavigateToNowPlaying)
                }
            }

            // Favorite artists
            items(result.favoriteArtists) { row ->
                RecommendationRowUI(row, windowSizeClass) { video ->
                    playVideo(video, row.items, playerViewModel, onNavigateToNowPlaying)
                }
            }

            // Trending
            item {
                RecommendationRowUI(result.trending, windowSizeClass) { video ->
                    playVideo(video, result.trending.items, playerViewModel, onNavigateToNowPlaying)
                }
            }

            // Genre dives
            items(result.genreDive) { row ->
                RecommendationRowUI(row, windowSizeClass) { video ->
                    playVideo(video, row.items, playerViewModel, onNavigateToNowPlaying)
                }
            }

            // Hidden gems
            item {
                RecommendationRowUI(result.hiddenGems, windowSizeClass, isHighlighted = true) { video ->
                    playVideo(video, result.hiddenGems.items, playerViewModel, onNavigateToNowPlaying)
                }
            }
        }

        // Loading shimmer
        if (isLoading) {
            items(3) { ShimmerRecommendationRow() }
        }
        
        item { Spacer(Modifier.height(80.dp)) } // padding for bottom nav
    }
}

private fun playVideo(
    video: VideoItem, 
    contextList: List<VideoItem>, 
    playerViewModel: com.deepeye.musicpro.ui.player.PlayerViewModel,
    onNavigateToNowPlaying: () -> Unit
) {
    val mediaItems = contextList.map { 
        MediaItem.Remote(
            id = it.videoId,
            title = it.title,
            artist = it.artist,
            artworkUri = Uri.parse("https://i.ytimg.com/vi/${it.videoId}/mqdefault.jpg"),
            duration = 180000L, // Mock duration
            isVideo = true
        )
    }
    val index = contextList.indexOfFirst { it.videoId == video.videoId }
    playerViewModel.setQueue(mediaItems, if (index >= 0) index else 0)
    onNavigateToNowPlaying()
}

@Composable
fun HeroSection() {
    Column(modifier = Modifier.padding(20.dp)) {
        Text(
            text = "Good Evening",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Here's what we curated for you",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun RecommendationRowUI(
    row: RecommendationRow,
    windowSizeClass: WindowSizeClass,
    isHighlighted: Boolean = false,
    onVideoClick: (VideoItem) -> Unit
) {
    if (row.items.isEmpty()) return

    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        // Row header
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    row.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isHighlighted) TealGlow else MaterialTheme.colorScheme.onBackground
                )
                if (row.subtitle.isNotBlank()) {
                    Text(
                        row.subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                }
            }
            TextButton(onClick = { /* show all */ }) {
                Text("See all", color = TealGlow,
                    style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(Modifier.height(10.dp))

        // Horizontal scrollable cards
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(row.items, key = { it.videoId }) { video ->
                VideoRecommendationCard(
                    video = video,
                    cardWidth = when (windowSizeClass.widthSizeClass) {
                        WindowWidthSizeClass.Compact  -> 140.dp
                        WindowWidthSizeClass.Medium   -> 170.dp
                        else                          -> 200.dp
                    },
                    onClick = { onVideoClick(video) }
                )
            }
        }
    }
}

@Composable
fun VideoRecommendationCard(
    video: VideoItem,
    cardWidth: Dp,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .width(cardWidth)
            .clickable(onClick = onClick)
    ) {
        // Thumbnail
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(0.05f))
        ) {
            AsyncImage(
                model = "https://i.ytimg.com/vi/${video.videoId}/mqdefault.jpg",
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Duration badge
            Box(
                Modifier.align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .background(Color.Black.copy(0.75f),
                        RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(video.duration, color = White,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp)
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            video.title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            video.artist,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ShimmerRecommendationRow() {
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                ShimmerBox(Modifier.width(150.dp).height(20.dp).clip(RoundedCornerShape(4.dp)))
                Spacer(Modifier.height(4.dp))
                ShimmerBox(Modifier.width(100.dp).height(12.dp).clip(RoundedCornerShape(4.dp)))
            }
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(4) {
                Column(Modifier.width(140.dp)) {
                    ShimmerBox(Modifier.fillMaxWidth().aspectRatio(16f/9f).clip(RoundedCornerShape(10.dp)))
                    Spacer(Modifier.height(6.dp))
                    ShimmerBox(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(4.dp)))
                    Spacer(Modifier.height(4.dp))
                    ShimmerBox(Modifier.width(80.dp).height(12.dp).clip(RoundedCornerShape(4.dp)))
                }
            }
        }
    }
}
