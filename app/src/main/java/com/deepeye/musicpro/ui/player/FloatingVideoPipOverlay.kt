// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.musicpro.domain.model.PlayerState
import com.deepeye.musicpro.ui.components.StablePlayerHolder
import com.deepeye.musicpro.ui.components.VideoPlayerView
import com.deepeye.musicpro.ui.theme.NeonCyan
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Pro Floating In-App PiP Mini Video Player.
 *
 * Smoothly floats over all browsable screens (Home Hub, YouTube, Library) when exiting fullscreen.
 * Features:
 * - Continuous video playback (0 buffer interruptions)
 * - Draggable anywhere on the landscape canvas with boundary clamping
 * - Quick touch controls (Play/Pause, Expand Fullscreen, Dismiss)
 * - Smooth spring animations and glassmorphism styling
 */
@Composable
fun FloatingVideoPipOverlay(
    playerHolder: StablePlayerHolder,
    playerState: PlayerState,
    onExpandFullscreen: () -> Unit,
    onPlayPause: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var showControls by remember { mutableStateOf(true) }

    // Auto-hide floating controls after 3 seconds
    LaunchedEffect(showControls, playerState.isPlaying) {
        if (showControls && playerState.isPlaying) {
            delay(3000)
            showControls = false
        }
    }

    val title = playerState.currentSong?.title ?: playerState.currentItem?.title ?: "Playing Video"
    val artist = playerState.currentSong?.artist ?: playerState.currentItem?.artist ?: ""
    val isPlaying = playerState.isPlaying

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val density = LocalDensity.current
        val parentWidthPx = constraints.maxWidth.toFloat()
        val parentHeightPx = constraints.maxHeight.toFloat()

        val pipWidthPx = with(density) { 240.dp.toPx() }
        val pipHeightPx = with(density) { 135.dp.toPx() }

        // Draggable boundary clamping
        val minX = 0f
        val maxX = (parentWidthPx - pipWidthPx).coerceAtLeast(0f)
        val minY = 0f
        val maxY = (parentHeightPx - pipHeightPx).coerceAtLeast(0f)

        // Initialize to bottom-right corner on first composition
        LaunchedEffect(parentWidthPx, parentHeightPx) {
            if (offsetX == 0f && offsetY == 0f && maxX > 0 && maxY > 0) {
                offsetX = maxX - with(density) { 20.dp.toPx() }
                offsetY = maxY - with(density) { 20.dp.toPx() }
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .size(width = 240.dp, height = 135.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX = (offsetX + dragAmount.x).coerceIn(minX, maxX)
                        offsetY = (offsetY + dragAmount.y).coerceIn(minY, maxY)
                        showControls = true
                    }
                }
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(16.dp),
                    spotColor = NeonCyan.copy(alpha = 0.5f),
                    ambientColor = Color.Black
                )
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black)
                .border(
                    BorderStroke(
                        1.5.dp,
                        Brush.linearGradient(
                            colors = listOf(NeonCyan, Color.White.copy(alpha = 0.4f), NeonCyan)
                        )
                    ),
                    RoundedCornerShape(16.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    showControls = !showControls
                }
        ) {
            // 1. Live Video Surface View
            VideoPlayerView(
                playerHolder = playerHolder,
                modifier = Modifier.fillMaxSize()
            )

            // 2. Floating Quick Controls Overlay
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) + scaleIn(initialScale = 0.95f),
                exit = fadeOut(spring(stiffness = Spring.StiffnessMediumLow)) + scaleOut(targetScale = 0.95f),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(6.dp)
                ) {
                    // Top Actions (Expand to Fullscreen & Close)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.8f)),
                            modifier = Modifier
                                .size(28.dp)
                                .clickable { onExpandFullscreen() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Fullscreen,
                                    contentDescription = "Expand Fullscreen",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .size(28.dp)
                                .clickable { onClose() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Center Play / Pause Button
                    Surface(
                        shape = CircleShape,
                        color = NeonCyan.copy(alpha = 0.25f),
                        border = BorderStroke(1.2.dp, NeonCyan),
                        modifier = Modifier
                            .size(36.dp)
                            .align(Alignment.Center)
                            .clickable { onPlayPause() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Bottom Title Badge
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .clickable { onExpandFullscreen() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
