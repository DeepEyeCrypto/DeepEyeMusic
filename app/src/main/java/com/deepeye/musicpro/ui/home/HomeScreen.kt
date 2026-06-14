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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.deepeye.musicpro.ui.components.DynamicLabel
import com.deepeye.musicpro.ui.components.SecondaryLabel
import com.deepeye.musicpro.ui.components.TertiaryLabel
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Brush
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.deepeye.musicpro.domain.model.MediaItem
import com.deepeye.musicpro.domain.recommendation.RecommendationRow
import com.deepeye.musicpro.domain.recommendation.VideoItem
import com.deepeye.musicpro.ui.components.ShimmerBox

import com.deepeye.musicpro.ui.theme.GlowTeal
import com.deepeye.musicpro.ui.theme.ElectricViolet
import com.deepeye.musicpro.ui.theme.RichBlack
import com.deepeye.musicpro.ui.theme.TextPrimary
import com.deepeye.musicpro.ui.theme.TextSecondary
import com.deepeye.musicpro.ui.theme.GlowOrange
import com.deepeye.musicpro.ui.theme.NeonCyan
import com.deepeye.musicpro.ui.theme.GlassBorderLight
import com.deepeye.musicpro.ui.motion.premiumScrollHaptics

val White = Color.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar() {
    com.deepeye.musicpro.ui.components.GlassTopAppBar(
        title = {
            Text(
                text = "DeepEye Music",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        navigationIcon = {
            Box(
                modifier = Modifier
                    .padding(start = 16.dp, end = 12.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(GlowTeal.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = GlowTeal,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        actions = {
            IconButton(onClick = { /* Search */ }) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            IconButton(onClick = { /* Settings */ }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    )
}

@Composable
fun HomeScreen(
    onNavigateToNowPlaying: () -> Unit,
    onNavigateToAlbum: (Long) -> Unit,
    viewModel: RecommendationViewModel = hiltViewModel(),
    playerViewModel: com.deepeye.musicpro.ui.player.PlayerViewModel = hiltViewModel(),
    windowSizeClass: WindowSizeClass,
) {
    val recs by viewModel.recommendations.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()

    Scaffold(
        topBar = { HomeTopBar() },
        containerColor = Color.Transparent
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding(),
                bottom = 160.dp // High bottom padding for mini-player overlap!
            ),
            modifier = Modifier.fillMaxSize().premiumScrollHaptics(listState)
        ) {
            // ── Hero section ──────────────────────────
        item {
            HeroSection()
        }

        // ── Refresh Indicator (Network reload in progress) ──
        if (isRefreshing && recs != null) {
            item {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    color = GlowTeal,
                    trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
                )
            }
        }

        // ── Error/Empty State with Retry ───────────────────
        if (recs == null && error != null) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Unable to load recommendations",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error ?: "An unknown error occurred",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.loadRecommendations() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GlowTeal,
                            contentColor = Color.Black
                        )
                    ) {
                        Text("Retry")
                    }
                }
            }
        }

        // ── Recommendation rows ───────────────────
        recs?.let { result ->

            // Because you listened
            items(result.becauseYouListened) { row ->
                RecommendationRowUI(row, windowSizeClass) { video ->
                    playMusic(video, row.items, playerViewModel, onNavigateToNowPlaying)
                }
            }

            // Perfect for right now
            item {
                RecommendationRowUI(result.perfectForNow, windowSizeClass, isHighlighted = true) { video ->
                    playMusic(video, result.perfectForNow.items, playerViewModel, onNavigateToNowPlaying)
                }
            }

            // Favorite artists
            items(result.favoriteArtists) { row ->
                RecommendationRowUI(row, windowSizeClass) { video ->
                    playMusic(video, row.items, playerViewModel, onNavigateToNowPlaying)
                }
            }

            // Trending
            item {
                RecommendationRowUI(result.trending, windowSizeClass) { video ->
                    playMusic(video, result.trending.items, playerViewModel, onNavigateToNowPlaying)
                }
            }

            // Genre dives
            items(result.genreDive) { row ->
                RecommendationRowUI(row, windowSizeClass) { video ->
                    playMusic(video, row.items, playerViewModel, onNavigateToNowPlaying)
                }
            }

            // Hidden gems
            item {
                RecommendationRowUI(result.hiddenGems, windowSizeClass, isHighlighted = true) { video ->
                    playMusic(video, result.hiddenGems.items, playerViewModel, onNavigateToNowPlaying)
                }
            }
        }
        
        // Loading shimmer
        if (isRefreshing && recs == null) {
            items(3) { ShimmerRecommendationRow() }
        }
        // Removed Spacer since contentPadding now handles bottom insets
    }
    }
}

private fun playMusic(
    video: VideoItem,
    contextList: List<VideoItem>,
    playerViewModel: com.deepeye.musicpro.ui.player.PlayerViewModel,
    onNavigateToNowPlaying: () -> Unit,
) {
    val mediaItems =
        contextList.map {
            MediaItem.Remote(
                id = it.videoId,
                title = it.title,
                artist = it.artist,
                artworkUri = Uri.parse("https://i.ytimg.com/vi/${it.videoId}/mqdefault.jpg"),
                duration = 180000L, // Mock duration
                isVideo = false,
            )
        }
    val index = contextList.indexOfFirst { it.videoId == video.videoId }
    playerViewModel.setQueue(mediaItems, if (index >= 0) index else 0)
    onNavigateToNowPlaying()
}

@Composable
fun HeroSection() {
    var btcPrice by remember { mutableStateOf("Fetching BTC...") }
    val timeFormatter = remember { java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()) }
    var timeString by remember { mutableStateOf(timeFormatter.format(java.util.Date())) }

    LaunchedEffect(Unit) {
        while (true) {
            try {
                btcPrice = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val url = java.net.URL("https://api.binance.com/api/v3/ticker/price?symbol=BTCUSDT")
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.connectTimeout = 3000
                    connection.readTimeout = 3000
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val priceStr = response.substringAfter("\"price\":\"").substringBefore("\"")
                    val formattedPrice = String.format("%,.2f", priceStr.toDoubleOrNull() ?: 0.0)
                    "₿ $$formattedPrice"
                }
            } catch (e: Exception) {
                if (btcPrice == "Fetching BTC...") {
                    btcPrice = "₿ ---"
                }
            }
            timeString = timeFormatter.format(java.util.Date())
            kotlinx.coroutines.delay(5000) // Update every 5 seconds
        }
    }

    val gradientBrush = remember {
        androidx.compose.ui.graphics.Brush.linearGradient(
            colors = listOf(
                GlowOrange,
                GlowTeal,
                ElectricViolet
            )
        )
    }

    com.deepeye.musicpro.ui.components.GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        tintColor = ElectricViolet.copy(alpha = 0.1f),
        cornerRadius = 32.dp,
        refractionHeight = 0.4f
    ) {
        Box(
            modifier = Modifier.padding(32.dp)
        ) {
        Column {
            Text(
                text = "$btcPrice • $timeString",
                style = MaterialTheme.typography.headlineMedium.copy(
                    brush = gradientBrush
                ),
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Here's what we curated for you today",
                style = MaterialTheme.typography.titleMedium,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        }
    }
}

@Composable
fun RecommendationRowUI(
    row: RecommendationRow,
    windowSizeClass: WindowSizeClass,
    isHighlighted: Boolean = false,
    onVideoClick: (VideoItem) -> Unit,
) {
    if (row.items.isEmpty()) return

    Column(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        // Row header
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Colored dot indicator
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isHighlighted) GlowTeal else ElectricViolet)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    DynamicLabel(
                        text = row.title,
                        backgroundColor = Color.Black,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        
                    )
                    if (row.subtitle.isNotBlank()) {
                        SecondaryLabel(
                            text = row.subtitle,
                            backgroundColor = Color.Black,
                            style = MaterialTheme.typography.labelMedium,
                            
                        )
                    }
                }
            }
            IconButton(
                onClick = { /* show all */ },
                modifier = Modifier
                    .size(32.dp)
                    .background(Color.White.copy(alpha = 0.05f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "See all",
                    tint = GlowTeal,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Horizontal scrollable cards
        val rowState = rememberLazyListState()
        LazyRow(
            state = rowState,
            modifier = Modifier.premiumScrollHaptics(rowState),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Composite key (index + videoId): recommendation rows can legitimately
            // contain the same videoId more than once, and a plain videoId key crashes
            // Compose with "Key was already used". Index guarantees uniqueness.
            itemsIndexed(row.items, key = { index, video -> "$index-${video.videoId}" }) { _, video ->
                VideoRecommendationCard(
                    video = video,
                    cardWidth =
                    when (windowSizeClass.widthSizeClass) {
                        WindowWidthSizeClass.Compact -> 140.dp
                        WindowWidthSizeClass.Medium -> 170.dp
                        else -> 200.dp
                    },
                    onClick = { onVideoClick(video) },
                )
            }
        }
    }
}

@Composable
fun VideoRecommendationCard(
    video: VideoItem,
    cardWidth: Dp,
    onClick: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .width(cardWidth)
            .clickable(onClick = onClick)
    ) {
        // Thumbnail with 24dp radius and glass border
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f) // Cinematic square for albums/mixes
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(0.05f))
                .border(
                    width = 1.dp,
                    color = GlassBorderLight,
                    shape = RoundedCornerShape(24.dp)
                ),
        ) {
            AsyncImage(
                model = "https://i.ytimg.com/vi/${video.videoId}/hqdefault.jpg",
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            
            // Play overlay centered
            Box(
                Modifier.fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                        .border(1.dp, Color.White.copy(0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Duration badge
            Box(
                Modifier.align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .background(
                        RichBlack.copy(0.85f),
                        RoundedCornerShape(8.dp),
                    )
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            ) {
                TertiaryLabel(
                    text = video.duration,
                    backgroundColor = RichBlack,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        DynamicLabel(
            text = video.title,
            backgroundColor = Color.Black,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            
        )
        Spacer(Modifier.height(4.dp))
        SecondaryLabel(
            text = video.artist,
            backgroundColor = Color.Black,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            
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
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                ShimmerBox(Modifier.width(150.dp).height(20.dp).clip(RoundedCornerShape(8.dp)))
                Spacer(Modifier.height(4.dp))
                ShimmerBox(Modifier.width(100.dp).height(12.dp).clip(RoundedCornerShape(8.dp)))
            }
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(4) {
                Column(Modifier.width(170.dp)) { // Match the new card width roughly
                    // Match the 24dp rounded corner and square ratio of new cards
                    ShimmerBox(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(24.dp)))
                    Spacer(Modifier.height(12.dp))
                    ShimmerBox(Modifier.fillMaxWidth(0.8f).height(16.dp).clip(RoundedCornerShape(4.dp)))
                    Spacer(Modifier.height(6.dp))
                    ShimmerBox(Modifier.width(100.dp).height(12.dp).clip(RoundedCornerShape(4.dp)))
                }
            }
        }
    }
}
