// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.deepeye.musicpro.ui.components.GlassCard
import com.deepeye.musicpro.ui.components.GlassSurface

@Composable
fun MiniPlayerContent(
    sheetState: MiniPlayerSheetState,
    currentAnchor: MiniSheetAnchor,
    progress: Float,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
    onHalfExpand: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onPlayPause: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
    expandedContent: @Composable () -> Unit,
) {
    val playerState = viewModel.playerState.collectAsStateWithLifecycle().value
    val dominantColor by viewModel.dominantColor.collectAsStateWithLifecycle()
    val fftData by viewModel.fftData.collectAsStateWithLifecycle()
    val currentItem = playerState.currentItem
    val playbackFraction = if (playerState.duration > 0) {
        (playerState.position.toFloat() / playerState.duration.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val horizontalPadding = (12 * (1f - progress)).dp
    val bottomPadding = (12 * (1f - progress)).dp
    val animatedCornerRadius = (24 * (1f - progress)).dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = horizontalPadding)
            .padding(bottom = bottomPadding)
    ) {
        // When expanded or nearly expanded, render content directly without GlassSurface shader.
        // The liquidGlassEffect shader makes full-screen content invisible on many devices.
        // Render expanded content when sheet is past halfway OR when anchor state
        // says EXPANDED (covers race condition where progress hasn't caught up yet)
        val shouldShowExpanded = progress > 0.5f || currentAnchor == MiniSheetAnchor.EXPANDED

        // ALWAYS keep expanded content in the tree (even when collapsed) so the WebView
        // stays attached and audio continues playing. Hide it visually when collapsed.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (shouldShowExpanded) 1f else 0f)
        ) {
            expandedContent()
        }

        if (shouldShowExpanded) {
            // Show collapsed header fading out during transition
            if (progress < 0.99f) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(1f - progress),
                ) {
                    DragHandleBar()
                    if (currentItem != null) {
                        PlayerHeaderRow(
                            title = currentItem.title,
                            artist = currentItem.artist,
                            artworkUri = currentItem.artworkUri.toString(),
                            isPlaying = playerState.isPlaying,
                            dominantColor = dominantColor,
                            fftData = fftData,
                            onArtworkTap = onExpand,
                            onPlayPause = onPlayPause,
                            onNext = onNext,
                            onPrev = onPrev,
                        )
                    }
                    MiniPlayerProgressLine(
                        progress = playbackFraction,
                        accentColor = dominantColor,
                    )
                }
            }
        } else {
            // Collapsed: show mini-player with glass surface
            val miniPlayerInteractionSource = remember { MutableInteractionSource() }
            val isMiniPlayerPressed by miniPlayerInteractionSource.collectIsPressedAsState()
            val miniPlayerScale by animateFloatAsState(
                targetValue = if (isMiniPlayerPressed) 0.97f else 1f,
                animationSpec = spring(stiffness = 500f, dampingRatio = 0.8f),
                label = "miniPlayerScale"
            )
            GlassSurface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 8.dp)
                    .graphicsLayer(scaleX = miniPlayerScale, scaleY = miniPlayerScale)
                    .clickable(
                        interactionSource = miniPlayerInteractionSource,
                        indication = null,
                        onClick = onExpand
                    ),
                shape = RoundedCornerShape(24.dp), // Premium floating pill
                tintColor = dominantColor.copy(alpha = 0.12f),
                borderColor = Color.White.copy(alpha = 0.15f),
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // The collapsed/half-expanded headers
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(1f - progress),
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (currentItem != null) {
                            PlayerHeaderRow(
                                title = currentItem.title,
                                artist = currentItem.artist,
                                artworkUri = currentItem.artworkUri.toString(),
                                isPlaying = playerState.isPlaying,
                                dominantColor = dominantColor,
                                fftData = fftData,
                                onArtworkTap = onExpand,
                                onPlayPause = onPlayPause,
                                onNext = onNext,
                                onPrev = onPrev,
                            )
                        }

                        AnimatedVisibility(visible = currentAnchor == MiniSheetAnchor.HALF_EXPANDED) {
                            QueuePeekBar(onExpand = onExpand)
                        }
                    }
                    
                    Box(modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 24.dp).padding(bottom = 6.dp).alpha(1f - progress)) {
                        MiniPlayerProgressLine(
                            progress = playbackFraction,
                            accentColor = dominantColor,
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun DragHandleBar() {
    Box(
        modifier =
        Modifier
            .fillMaxWidth()
            .height(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
            Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.3f)),
        )
    }
}

@Composable
fun PlayerHeaderRow(
    title: String,
    artist: String,
    artworkUri: String,
    isPlaying: Boolean,
    dominantColor: Color = Color.White,
    fftData: FloatArray = FloatArray(0),
    onArtworkTap: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
) {
    // Extract bass amplitude for mini ring pulse
    val bassAmplitude = if (fftData.size >= 4) {
        ((fftData[0] + fftData[1] + fftData[2] + fftData[3]) / 4f).coerceIn(0f, 1f)
    } else {
        0f
    }

    val miniRingAlpha by animateFloatAsState(
        targetValue = if (isPlaying) 0.3f + bassAmplitude * 0.5f else 0f,
        animationSpec = spring(stiffness = 400f),
        label = "miniRingAlpha",
    )

    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Album art with bass ring
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(52.dp),
        ) {
            // Mini bass pulse ring
            Canvas(
                modifier = Modifier.size(52.dp)
            ) {
                drawCircle(
                    color = dominantColor.copy(alpha = miniRingAlpha * 0.6f),
                    radius = size.minDimension / 2f + 2.dp.toPx(),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx()),
                )
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF2A2A35), Color(0xFF1E1E28))))
                    .clickable { onArtworkTap() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = dominantColor.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
                AsyncImage(
                    model = artworkUri.takeIf { it.isNotBlank() },
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                modifier = Modifier.basicMarquee(),
            )
            Text(
                text = artist.takeIf { it != "<unknown>" && it.isNotBlank() } ?: "Unknown Artist",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 1,
                modifier = Modifier.basicMarquee(),
            )
        }

        TactileIconButton(onClick = onPrev, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color.White)
        }

        Spacer(Modifier.width(4.dp))

        TactileIconButton(onClick = onPlayPause, modifier = Modifier.size(44.dp)) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayCircle,
                contentDescription = "Play/Pause",
                tint = Color.White,
                modifier = Modifier.size(32.dp),
            )
        }

        Spacer(Modifier.width(4.dp))

        TactileIconButton(onClick = onNext, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White)
        }
    }
}

@Composable
private fun TactileIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(stiffness = 500f, dampingRatio = 0.85f),
        label = "tactileScale"
    )
    Box(
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Thin accent-glowing progress line at the bottom of the mini player.
 */
@Composable
fun MiniPlayerProgressLine(
    progress: Float,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp)
    ) {
        // Background track
        drawRoundRect(
            color = Color.White.copy(alpha = 0.08f),
            topLeft = Offset.Zero,
            size = Size(size.width, size.height),
            cornerRadius = CornerRadius(size.height / 2f),
        )
        // Active track with accent glow
        val activeWidth = size.width * progress.coerceIn(0f, 1f)
        if (activeWidth > 0f) {
            // Glow
            drawRoundRect(
                color = accentColor.copy(alpha = 0.3f),
                topLeft = Offset(0f, -1.dp.toPx()),
                size = Size(activeWidth, size.height + 2.dp.toPx()),
                cornerRadius = CornerRadius(size.height),
            )
            // Solid line
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.5f),
                        accentColor,
                    )
                ),
                topLeft = Offset.Zero,
                size = Size(activeWidth, size.height),
                cornerRadius = CornerRadius(size.height / 2f),
            )
        }
    }
}

@Composable
fun QueuePeekBar(onExpand: () -> Unit) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable { onExpand() },
        tintColor = Color.White.copy(alpha = 0.05f),
        cornerRadius = 12.dp,
        refractionHeight = 0.1f
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Swipe up for full queue",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.6f),
            )
        }
    }
}
