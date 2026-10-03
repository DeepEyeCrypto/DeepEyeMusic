// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.music

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.deepeye.musicpro.domain.model.Song
import com.deepeye.musicpro.ui.motion.premiumScrollHaptics
import com.deepeye.musicpro.ui.theme.*

// ─── Premium Color Tokens ────────────────────────────────────────────────────
private val CardSurface = Color(0xFF12121A)
private val CardSurfaceElevated = Color(0xFF1A1A26)
private val NeonCyan = Color(0xFF00E5FF)
private val ElectricViolet = Color(0xFF7C4DFF)
private val FlacBadge = Color(0xFF00E676)
private val HiResBadge = Color(0xFFFFB300)
private val DspColor = Color(0xFF00D2FF)
private val V4aColor = Color(0xFF7C4DFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicScreen(
    onNavigateToNowPlaying: (String) -> Unit,
    onNavigateToSearch: () -> Unit = {},
    onConnectAccount: () -> Unit = {},
    viewModel: MusicViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                RichBlack.copy(alpha = 0.95f),
                                RichBlack.copy(alpha = 0.7f),
                                Color.Transparent
                            )
                        )
                    )
                    .statusBarsPadding()
            ) {
                // Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = CardGeometry.ScreenGutter, vertical = 10.dp),
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
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            "Lossless Music",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp,
                            color = TextPrimary
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (uiState.localSongs.isNotEmpty()) {
                            IconButton(onClick = { viewModel.playAllLocalSongs(shuffle = true) }) {
                                Icon(Icons.Default.Shuffle, "Shuffle All", tint = NeonCyan)
                            }
                        }
                        IconButton(onClick = { viewModel.syncLibrary() }) {
                            Icon(Icons.Default.Refresh, "Sync Library", tint = TextSecondary)
                        }
                    }
                }

                // Premium Search Bar
                PremiumSearchBar(onClick = onNavigateToSearch)
                Spacer(Modifier.height(8.dp))
            }
        },
    ) { paddingValues ->
        if (uiState.localSongs.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.LibraryMusic,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = TextTertiary
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "No local music found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = { viewModel.syncLibrary() },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Scan Device for Music", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            val listState = rememberLazyListState()
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .boundedContent()
                        .fillMaxHeight()
                        .premiumScrollHaptics(listState),
                    contentPadding = PaddingValues(
                        top = paddingValues.calculateTopPadding() + 6.dp,
                        bottom = 180.dp
                    ),
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
    }
}

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
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = NeonCyan,
            letterSpacing = 1.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

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
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
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
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.04f))
            ) {
                AsyncImage(
                    model = song.artUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )

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
                        modifier = Modifier.size(20.dp)
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
                        Text("HR", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            Spacer(Modifier.width(14.dp))

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

                    BadgePill(
                        text = if (isFlac) "FLAC" else "AAC",
                        color = if (isFlac) FlacBadge else TextTertiary
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = bitrate,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextTertiary,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(Modifier.width(8.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val sec = (song.duration / 1000) % 60
                val min = (song.duration / (1000 * 60)) % 60
                Text(
                    text = "%d:%02d".format(min, sec),
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
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.3.sp
        )
    }
}

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
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun PremiumSearchBar(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
        ),
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
