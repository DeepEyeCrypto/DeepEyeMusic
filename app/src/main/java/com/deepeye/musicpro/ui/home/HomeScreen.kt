// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.home

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.deepeye.musicpro.domain.model.MediaItem
import com.deepeye.musicpro.domain.recommendation.RecommendationRow
import com.deepeye.musicpro.domain.recommendation.VideoItem
import com.deepeye.musicpro.ui.components.GlassCard
import com.deepeye.musicpro.ui.components.SectionHeaderSkeleton
import com.deepeye.musicpro.ui.components.VideoCardSkeleton
import com.deepeye.musicpro.ui.motion.premiumScrollHaptics
import com.deepeye.musicpro.ui.theme.*
import com.deepeye.musicpro.ui.util.adaptiveColumnCount
import com.deepeye.musicpro.ui.util.evenCarouselCardWidth
import com.deepeye.musicpro.ui.util.minTouchTarget
import java.util.Calendar

private val MoodFilters = listOf(
    "All",
    "⚡ Energize",
    "🌌 Chill & Relax",
    "🎧 Studio Hi-Fi",
    "🔥 Trending",
    "📻 Focus & Flow",
    "🌙 Night Drive"
)

@Composable
fun HomeTopBar(
    onSearchClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
) {
    val greeting = remember { getDynamicGreeting() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = CardGeometry.ScreenGutter, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "DEEPEYE",
                    // titleMedium, not a hardcoded 24.sp: the wordmark is brand
                    // chrome sitting above a scrolling list, and 24.sp on a
                    // landscape phone consumed vertical space the carousels
                    // needed more.
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(NeonCyan)
                        .shadow(8.dp, CircleShape, spotColor = NeonCyan)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PRO",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    color = GlowOrange,
                    modifier = Modifier
                        .background(GlowOrange.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = greeting,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF131722).copy(alpha = 0.85f))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    .clickable(onClick = onSearchClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF131722).copy(alpha = 0.85f))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    .clickable(onClick = onSettingsClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
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

    var selectedMoodIndex by remember { mutableIntStateOf(0) }

    // Carousel cards now size themselves from the actual viewport instead of a
    // hardcoded 168/220.dp. See evenCarouselCardWidth: a fixed width left a
    // third of the landscape row empty, which is the dead space being removed.
    // maxWidth is read through BoxWithConstraints below, so nothing is guessed.
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val cardWidth = evenCarouselCardWidth(
            availableWidth = maxWidth,
            minItemWidth = CardGeometry.Compact.minWidth,
        )
        val quickColumns = adaptiveColumnCount(
            availableWidth = maxWidth,
            minItemWidth = 200.dp,
        )

        Scaffold(
            topBar = {
                HomeTopBar(
                    onSearchClick = { /* Search Navigation */ },
                    onSettingsClick = { /* Settings Navigation */ }
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding(),
                bottom = 90.dp // mini-player and nav-bar safe inset
            ),
            modifier = Modifier
                .fillMaxSize()
                .premiumScrollHaptics(listState)
        ) {
            // 1. Mood / Vibe Filter Carousel
            item {
                LazyRow(
                    contentPadding = PaddingValues(
                        horizontal = CardGeometry.ScreenGutter,
                        vertical = 4.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(MoodFilters) { index, mood ->
                        val isSelected = selectedMoodIndex == index
                        val bgBrush = if (isSelected) {
                            Brush.horizontalGradient(listOf(NeonCyan.copy(alpha = 0.35f), ElectricViolet.copy(alpha = 0.45f)))
                        } else {
                            Brush.horizontalGradient(listOf(Color(0xFF131722).copy(alpha = 0.85f), Color(0xFF131722).copy(alpha = 0.85f)))
                        }
                        val borderColor = if (isSelected) NeonCyan else Color.White.copy(alpha = 0.18f)
                        val textColor = if (isSelected) Color.White else Color.White.copy(alpha = 0.75f)

                        Box(
                            modifier = Modifier
                                .heightIn(min = 36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(bgBrush)
                                .border(if (isSelected) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(18.dp))
                                .clickable { selectedMoodIndex = index }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mood,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 2. Quick-Play dense grid. The column count follows the viewport so
            // this fills the width on a landscape phone, instead of stretching two
            // comically wide rows across 891dp.
            recs?.perfectForNow?.items?.take(8)?.takeIf { it.isNotEmpty() }?.let { quickItems ->
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = CardGeometry.ScreenGutter)
                    ) {
                        quickItems.chunked(quickColumns).forEach { rowItems ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.spacedBy(CardGeometry.CardSpacing)
                            ) {
                                rowItems.forEach { video ->
                                    QuickGridItem(
                                        video = video,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            playMusic(video, quickItems, playerViewModel, onNavigateToNowPlaying)
                                        }
                                    )
                                }
                                // Pad a short final row so its cards keep the same
                                // width as every other row instead of stretching.
                                repeat(quickColumns - rowItems.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }

            // 3. Featured Cinematic Hero Banner
            item {
                FeaturedHeroBanner(
                    onPlayMix = {
                        recs?.perfectForNow?.items?.firstOrNull()?.let { firstVideo ->
                            playMusic(firstVideo, recs?.perfectForNow?.items ?: emptyList(), playerViewModel, onNavigateToNowPlaying)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 4. Pull-to-refresh / background sync progress line
            if (isRefreshing && recs != null) {
                item {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = NeonCyan,
                        trackColor = Color.White.copy(alpha = 0.08f)
                    )
                }
            }

            // 5. Error & Retry State
            if (recs == null && error != null) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Connection Offline",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error ?: "Check network and tap to reload recommendations.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadRecommendations() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Retry", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 6. Recommendation Carousels
            recs?.let { result ->
                // Because You Listened
                items(result.becauseYouListened, key = { "byl-${it.title}" }) { row ->
                    ModernRecommendationRow(
                        row = row,
                        cardWidth = cardWidth,
                        accentColor = NeonCyan,
                        onVideoClick = { video ->
                            playMusic(video, row.items, playerViewModel, onNavigateToNowPlaying)
                        }
                    )
                }

                // Perfect for right now
                item(key = "perfect-for-now") {
                    ModernRecommendationRow(
                        row = result.perfectForNow,
                        cardWidth = cardWidth,
                        accentColor = GlowOrange,
                        isHighlighted = true,
                        onVideoClick = { video ->
                            playMusic(video, result.perfectForNow.items, playerViewModel, onNavigateToNowPlaying)
                        }
                    )
                }

                // Favorite artists
                items(result.favoriteArtists, key = { "fav-${it.title}" }) { row ->
                    ModernRecommendationRow(
                        row = row,
                        cardWidth = cardWidth,
                        accentColor = ElectricViolet,
                        onVideoClick = { video ->
                            playMusic(video, row.items, playerViewModel, onNavigateToNowPlaying)
                        }
                    )
                }

                // Trending
                item(key = "trending-row") {
                    ModernRecommendationRow(
                        row = result.trending,
                        cardWidth = cardWidth,
                        accentColor = NeonCyan,
                        onVideoClick = { video ->
                            playMusic(video, result.trending.items, playerViewModel, onNavigateToNowPlaying)
                        }
                    )
                }

                // Genre dives
                items(result.genreDive, key = { "genre-${it.title}" }) { row ->
                    ModernRecommendationRow(
                        row = row,
                        cardWidth = cardWidth,
                        accentColor = GlowTeal,
                        onVideoClick = { video ->
                            playMusic(video, row.items, playerViewModel, onNavigateToNowPlaying)
                        }
                    )
                }

                // Hidden gems
                item(key = "hidden-gems") {
                    ModernRecommendationRow(
                        row = result.hiddenGems,
                        cardWidth = cardWidth,
                        accentColor = GlowOrange,
                        isHighlighted = true,
                        onVideoClick = { video ->
                            playMusic(video, result.hiddenGems.items, playerViewModel, onNavigateToNowPlaying)
                        }
                    )
                }
            }

            // Loading Shimmer Skeletons
            if (isRefreshing && recs == null) {
                items(4) {
                    ShimmerRecommendationRow(cardWidth = cardWidth)
                }
            }
        }
        }
    }
}

@Composable
private fun QuickGridItem(
    video: VideoItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF131722).copy(alpha = 0.85f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = "https://i.ytimg.com/vi/${video.videoId}/hqdefault.jpg",
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = video.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            )
        }
    }
}

@Composable
private fun FeaturedHeroBanner(
    onPlayMix: () -> Unit = {}
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = CardGeometry.ScreenGutter),
        tintColor = ElectricViolet.copy(alpha = 0.2f),
        cornerRadius = 12.dp,
        refractionHeight = 0.35f
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        colors = listOf(NeonCyan.copy(alpha = 0.25f), ElectricViolet.copy(alpha = 0.18f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(200f, 100f),
                        radius = 600f
                    )
                )
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SPATIAL STUDIO AUDIO",
                            // Eyebrow label, not body copy — sits below the
                            // UiScale.MinReadableFontSize floor as a decorative
                            // tracking-wide label by design.
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp,
                            color = NeonCyan,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Today's Discovery Mix",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Curated high-bitrate lossless audio for your drive",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(NeonCyan)
                        .shadow(8.dp, CircleShape, spotColor = NeonCyan)
                        .clickable(onClick = onPlayMix),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play Mix",
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp)
                    )
                }
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
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val cardWidth = evenCarouselCardWidth(
            availableWidth = maxWidth,
            minItemWidth = CardGeometry.Compact.minWidth,
        )
        ModernRecommendationRow(
            row = row,
            cardWidth = cardWidth,
            accentColor = if (isHighlighted) GlowOrange else NeonCyan,
            isHighlighted = isHighlighted,
            onVideoClick = onVideoClick
        )
    }
}

@Composable
fun ModernRecommendationRow(
    row: RecommendationRow,
    cardWidth: Dp,
    accentColor: Color = NeonCyan,
    isHighlighted: Boolean = false,
    onVideoClick: (VideoItem) -> Unit,
) {
    if (row.items.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Row Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CardGeometry.ScreenGutter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = row.title,
                        // titleSmall, not a 20.sp headline: a row header in a
                        // stacked carousel list is chrome, not a headline, and 20.sp
                        // for eight consecutive rows is what made the screen read
                        // as oversized.
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (row.subtitle.isNotBlank()) {
                        Text(
                            text = row.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .minTouchTarget()
                    .clip(CircleShape)
                    .background(Color(0xFF131722).copy(alpha = 0.85f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                    .clickable { /* View all */ },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "See all",
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Horizontal Carousel
        val rowState = rememberLazyListState()
        LazyRow(
            state = rowState,
            modifier = Modifier.premiumScrollHaptics(rowState),
            contentPadding = PaddingValues(horizontal = CardGeometry.ScreenGutter),
            horizontalArrangement = Arrangement.spacedBy(CardGeometry.CardSpacing),
        ) {
            itemsIndexed(row.items, key = { index, video -> "$index-${video.videoId}" }) { _, video ->
                ModernVideoCard(
                    video = video,
                    cardWidth = cardWidth,
                    onClick = { onVideoClick(video) },
                )
            }
        }
    }
}

@Composable
fun ModernVideoCard(
    video: VideoItem,
    cardWidth: Dp,
    onClick: () -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "cardPressScale"
    )

    Column(
        modifier = Modifier
            .width(cardWidth)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        // Thumbnail Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                // Geometry from CardGeometry (SSOT). 16:9 rather than 1:1: these
                // are YouTube video frames, and a square crop both crops the
                // subject and costs ~44% more vertical space per card.
                .aspectRatio(CardGeometry.Video.aspectRatio)
                .clip(CardGeometry.Video.shape)
                .background(Color(0xFF131722).copy(alpha = 0.85f))
                .border(1.dp, Color.White.copy(alpha = 0.15f), CardGeometry.Video.shape)
        ) {
            AsyncImage(
                model = "https://i.ytimg.com/vi/${video.videoId}/hqdefault.jpg",
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            // Bottom Gradient Overlay for readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)),
                            startY = 100f
                        )
                    )
            )

            // Duration Pill Badge
            if (video.duration.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(5.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = video.duration,
                        // Thumbnail metadata overlay: 10-11sp by the documented
                        // UiScale.MinReadableFontSize carve-out for scrim-backed
                        // labels, which must not blow out the pill at this width.
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Title (2 lines max)
        Text(
            text = video.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        // Artist (1 line)
        if (video.artist.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = video.artist,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.65f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ShimmerRecommendationRow(cardWidth: Dp = CardGeometry.Video.minWidth) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CardGeometry.ScreenGutter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionHeaderSkeleton()
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = CardGeometry.ScreenGutter),
            horizontalArrangement = Arrangement.spacedBy(CardGeometry.CardSpacing),
        ) {
            items(4) {
                // VideoCardSkeleton reads CardGeometry.Video, which is the same
                // source ModernVideoCard uses — identical box by construction,
                // and the cardWidth passed in is the one the loaded rail computes.
                VideoCardSkeleton(width = cardWidth)
            }
        }
    }
}

private fun playMusic(
    video: VideoItem,
    contextList: List<VideoItem>,
    playerViewModel: com.deepeye.musicpro.ui.player.PlayerViewModel,
    onNavigateToNowPlaying: () -> Unit,
) {
    val mediaItems = contextList.map {
        MediaItem.Remote(
            id = it.videoId,
            title = it.title,
            artist = it.artist,
            artworkUri = Uri.parse("https://i.ytimg.com/vi/${it.videoId}/hqdefault.jpg"),
            duration = 180000L,
            isVideo = true,
        )
    }
    val index = contextList.indexOfFirst { it.videoId == video.videoId }
    playerViewModel.setQueue(mediaItems, if (index >= 0) index else 0)
    onNavigateToNowPlaying()
}

private fun getDynamicGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good Morning ☀️"
        in 12..16 -> "Good Afternoon 🌤️"
        in 17..21 -> "Good Evening 🌆"
        else -> "Night Owl Session 🌙"
    }
}
