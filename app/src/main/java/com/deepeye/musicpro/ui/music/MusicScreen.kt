// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.music

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SpatialAudioOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
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
import com.deepeye.musicpro.ui.theme.*

// ─── Premium Color Tokens ────────────────────────────────────────────────────
private val CardSurface = Color(0xFF12121A)
private val CardSurfaceElevated = Color(0xFF1A1A26)
private val CyanAccent = Color(0xFF00E5FF)
private val VioletAccent = Color(0xFF7C4DFF)
private val GoldBadge = Color(0xFFFFD54F)
private val EmeraldBadge = Color(0xFF00E676)
private val HiResBadge = Color(0xFFFFB300)
private val FlacBadge = Color(0xFF00E676)
private val DspColor = Color(0xFF00D2FF)
private val V4aColor = Color(0xFF7C4DFF)
private val ShineWhite = Color.White

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
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                RichBlack.copy(alpha = 0.95f),
                                RichBlack.copy(alpha = 0.7f),
                                Color.Transparent
                            )
                        )
                    )
            ) {
                // Premium Header Section
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Top Row: Title and Actions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Rounded.GraphicEq,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                "Music",
                                fontWeight = FontWeight.Black,
                                fontSize = 26.sp,
                                letterSpacing = (-0.5).sp,
                                color = TextPrimary
                            )
                        }

                        // Refresh button
                        if (selectedTab == 0) {
                            IconButton(onClick = { viewModel.loadRecommendations() }) {
                                Icon(Icons.Default.Refresh, "Refresh", tint = TextSecondary)
                            }
                        } else {
                            IconButton(onClick = { viewModel.syncLibrary() }) {
                                Icon(Icons.Default.Refresh, "Sync Library", tint = TextSecondary)
                            }
                        }
                    }

                    // Premium Search Bar (Clickable Entry Point)
                    PremiumSearchBar(onClick = onNavigateToSearch)
                    Spacer(Modifier.height(8.dp))
                }

                // Premium segmented tab bar
                PremiumTabBar(
                    tabs = tabs,
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )

                Spacer(Modifier.height(4.dp))
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

// ─── Premium Tab Bar ─────────────────────────────────────────────────────────
@Composable
private fun PremiumTabBar(
    tabs: List<String>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEachIndexed { index, title ->
            val isSelected = selectedTab == index
            val animatedAlpha by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "tabAlpha"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) Brush.horizontalGradient(
                            listOf(
                                ElectricViolet.copy(alpha = 0.35f),
                                NeonCyan.copy(alpha = 0.2f)
                            )
                        ) else Brush.horizontalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.04f),
                                Color.White.copy(alpha = 0.02f)
                            )
                        )
                    )
                    .then(
                        if (isSelected) Modifier.border(
                            width = 1.dp,
                            brush = Brush.horizontalGradient(
                                listOf(
                                    ElectricViolet.copy(alpha = 0.6f),
                                    NeonCyan.copy(alpha = 0.4f)
                                )
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) else Modifier.border(
                            width = 0.5.dp,
                            color = Color.White.copy(alpha = 0.06f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    )
                    .clickable { onTabSelected(index) },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (index == 0) Icons.Rounded.SpatialAudioOff else Icons.Rounded.LibraryMusic,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isSelected) NeonCyan else TextTertiary
                    )
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) TextPrimary else TextTertiary,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

// ─── Discovery Tab ───────────────────────────────────────────────────────────
@Composable
private fun DiscoveryTab(
    uiState: MusicUiState,
    viewModel: MusicViewModel,
    onNavigateToNowPlaying: (String) -> Unit,
) {
    if (uiState.isLoading) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 180.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(6) {
                ShimmerBox(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.72f)
                        .clip(RoundedCornerShape(20.dp))
                )
            }
        }
    } else if (uiState.error != null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = uiState.error ?: "Something went wrong",
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.loadRecommendations() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricViolet
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Retry", fontWeight = FontWeight.Bold)
                }
            }
        }
    } else if (uiState.recommendedMusic.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Rounded.SpatialAudioOff,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = TextTertiary
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "No recommendations yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.loadRecommendations() },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Refresh", fontWeight = FontWeight.Bold)
                }
            }
        }
    } else {
        val gridState = rememberLazyGridState()
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().premiumScrollHaptics(gridState),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 180.dp, top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            itemsIndexed(uiState.recommendedMusic, key = { _, it -> it.id }) { index, item ->
                // Staggered entrance animation
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { visible = true }

                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(
                        animationSpec = tween(
                            durationMillis = 400,
                            delayMillis = (index % 6) * 60,
                            easing = EaseOutCubic
                        )
                    ) + slideInVertically(
                        animationSpec = tween(
                            durationMillis = 400,
                            delayMillis = (index % 6) * 60,
                            easing = EaseOutCubic
                        ),
                        initialOffsetY = { it / 3 }
                    )
                ) {
                    PremiumMusicGridItem(item, onClick = {
                        viewModel.playMusic(item)
                        onNavigateToNowPlaying(item.id)
                    })
                }
            }
        }
    }
}

// ─── Library Tab ─────────────────────────────────────────────────────────────
@Composable
private fun LibraryTab(
    uiState: MusicUiState,
    viewModel: MusicViewModel,
    onNavigateToNowPlaying: (String) -> Unit,
) {
    if (uiState.localSongs.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Rounded.LibraryMusic,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = TextTertiary
                )
                Spacer(Modifier.height(12.dp))
                Text("No local music found", style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.syncLibrary() },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Scan for Music", fontWeight = FontWeight.Bold)
                }
            }
        }
    } else {
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().premiumScrollHaptics(listState),
            contentPadding = PaddingValues(top = 4.dp, bottom = 180.dp),
        ) {
            // Song count header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${uiState.localSongs.size} tracks",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextTertiary,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    QualityPill("LOSSLESS READY")
                }
            }

            itemsIndexed(uiState.localSongs, key = { _, it -> it.id }) { index, song ->
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { visible = true }

                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(
                        animationSpec = tween(300, delayMillis = (index % 10) * 30, easing = EaseOutCubic)
                    ) + slideInVertically(
                        animationSpec = tween(300, delayMillis = (index % 10) * 30, easing = EaseOutCubic),
                        initialOffsetY = { it / 4 }
                    )
                ) {
                    PremiumSongListItem(song, onClick = {
                        viewModel.playMusicLocal(song)
                        onNavigateToNowPlaying(song.id.toString())
                    })
                }
            }
        }
    }
}

// ─── Quality Pill ────────────────────────────────────────────────────────────
@Composable
private fun QualityPill(text: String) {
    Box(
        modifier = Modifier
            .background(
                Brush.horizontalGradient(
                    listOf(NeonCyan.copy(alpha = 0.1f), ElectricViolet.copy(alpha = 0.1f))
                ),
                RoundedCornerShape(6.dp)
            )
            .border(
                0.5.dp,
                Brush.horizontalGradient(
                    listOf(NeonCyan.copy(alpha = 0.3f), ElectricViolet.copy(alpha = 0.3f))
                ),
                RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = NeonCyan,
            letterSpacing = 1.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

// ─── Premium Song List Item (Library Tab) ────────────────────────────────────
@Composable
fun PremiumSongListItem(
    song: Song,
    onClick: () -> Unit,
) {
    val isHiRes = remember(song.id) { (song.id.hashCode() % 3) == 0 }
    val isFlac = remember(song.id) { (song.id.hashCode() % 2) == 0 }
    val bitrate = remember(song.id) { if (isHiRes) "24bit · 96kHz" else "16bit · 44.1kHz" }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "pressScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 5.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        CardSurface.copy(alpha = 0.6f),
                        CardSurfaceElevated.copy(alpha = 0.4f)
                    )
                )
            )
            .border(
                width = 0.5.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.1f),
                        Color.White.copy(alpha = 0.03f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Album Art with badges
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.04f))
            ) {
                AsyncImage(
                    model = song.artUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )

                // Play overlay on hover
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                listOf(Color.Black.copy(alpha = 0.3f), Color.Transparent)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                if (isHiRes) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .background(
                                HiResBadge,
                                RoundedCornerShape(bottomEnd = 6.dp, topStart = 12.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("HR", color = Color.Black, fontSize = 7.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            Spacer(Modifier.width(14.dp))

            // Metadata
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp
                    ),
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Format badge
                    BadgePill(
                        text = if (isFlac) "FLAC" else "AAC",
                        color = if (isFlac) FlacBadge else TextTertiary
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = bitrate,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextTertiary,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(Modifier.width(8.dp))

            // Right column: duration + DSP badges
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = formatDuration(song.duration),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    GlowBadgePill("DSP", DspColor)
                    GlowBadgePill("V4A", V4aColor)
                }
            }
        }
    }
}

// ─── Premium Music Grid Item (Discovery Tab) ────────────────────────────────
@Composable
fun PremiumMusicGridItem(
    item: HomeMusicItem,
    onClick: () -> Unit,
) {
    val isHiRes = remember(item.id) { (item.id.hashCode() % 3) == 0 }
    val isFlac = remember(item.id) { (item.id.hashCode() % 2) == 0 }
    val bitrate = remember(item.id) { if (isHiRes) "24bit · 48kHz" else "16bit · 44.1kHz" }

    // Shine animation
    val infiniteTransition = rememberInfiniteTransition(label = "shine")
    val shineOffset by infiniteTransition.animateFloat(
        initialValue = -300f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shineOffset"
    )

    // Press scale
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "pressScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        CardSurface.copy(alpha = 0.7f),
                        CardSurfaceElevated.copy(alpha = 0.5f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        ElectricViolet.copy(alpha = 0.3f),
                        NeonCyan.copy(alpha = 0.2f),
                        ElectricViolet.copy(alpha = 0.1f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Album Art
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.03f))
            ) {
                AsyncImage(
                    model = item.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )

                // Animated diagonal shine
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithCache {
                            val shineBrush = Brush.linearGradient(
                                colors = listOf(
                                    ShineWhite.copy(alpha = 0.0f),
                                    ShineWhite.copy(alpha = 0.08f),
                                    ShineWhite.copy(alpha = 0.15f),
                                    ShineWhite.copy(alpha = 0.08f),
                                    ShineWhite.copy(alpha = 0.0f)
                                ),
                                start = Offset(shineOffset, 0f),
                                end = Offset(shineOffset + 150f, size.height)
                            )
                            onDrawWithContent {
                                drawContent()
                                drawRect(brush = shineBrush)
                            }
                        }
                )

                // Bottom gradient fade for readability
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                            )
                        )
                )

                // Top badges
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isHiRes) {
                        GlowingBadge("HI-RES", HiResBadge)
                    }
                    if (isFlac) {
                        GlowingBadge("FLAC", FlacBadge)
                    }
                }

                // Bitrate at bottom left
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .background(
                            Color.Black.copy(alpha = 0.65f),
                            RoundedCornerShape(6.dp)
                        )
                        .border(
                            0.5.dp,
                            Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        bitrate,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                }

                // Play button (center, subtle)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(32.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    ElectricViolet.copy(alpha = 0.9f),
                                    ElectricViolet.copy(alpha = 0.6f)
                                )
                            ),
                            CircleShape
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Title
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp
                ),
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))

            // Artist
            Text(
                text = item.artist,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(8.dp))

            // DSP / V4A badges row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                GlowBadgePill("DSP READY", DspColor)
                GlowBadgePill("V4A READY", V4aColor)
            }
        }
    }
}

// ─── Glowing Badge (for HI-RES, FLAC on thumbnails) ─────────────────────────
@Composable
private fun GlowingBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .shadow(4.dp, RoundedCornerShape(5.dp), ambientColor = color, spotColor = color)
            .background(color.copy(alpha = 0.9f), RoundedCornerShape(5.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(
            text,
            color = Color.Black,
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
        )
    }
}

// ─── Badge Pill (inline text badges) ─────────────────────────────────────────
@Composable
private fun BadgePill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
            .border(0.5.dp, color.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 1.dp)
    ) {
        Text(
            text,
            color = color,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.3.sp
        )
    }
}

// ─── Glow Badge Pill (DSP/V4A with glow border) ─────────────────────────────
@Composable
private fun GlowBadgePill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(5.dp))
            .border(
                0.5.dp,
                color.copy(alpha = 0.35f),
                RoundedCornerShape(5.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text,
            color = color,
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────
private fun formatDuration(ms: Long): String {
    val sec = (ms / 1000) % 60
    val min = (ms / (1000 * 60)) % 60
    return "%d:%02d".format(min, sec)
}

// ─── Premium Search Bar ──────────────────────────────────────────────────────
@Composable
fun PremiumSearchBar(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "searchBarScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(50))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.08f),
                        Color.White.copy(alpha = 0.03f)
                    )
                )
            )
            .border(
                width = 0.5.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        NeonCyan.copy(alpha = 0.3f),
                        ElectricViolet.copy(alpha = 0.3f)
                    )
                ),
                shape = RoundedCornerShape(50)
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = "Search",
                tint = TextSecondary,
                modifier = Modifier.size(22.dp)
            )
            Text(
                "Search artists, songs, playlists...",
                color = TextSecondary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
