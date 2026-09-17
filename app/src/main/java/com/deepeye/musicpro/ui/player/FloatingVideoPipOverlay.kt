// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Offset
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
 * - 2-Finger Pinch-to-Resize: Dynamically resize the entire PiP card window from small to large
 * - Double-Tap to cycle PiP card sizes (Small / Medium / Large)
 * - Draggable anywhere on the canvas with fluid boundary clamping
 * - Quick touch controls (Play/Pause, Expand Fullscreen, Dismiss)
 * - Glassmorphism cyan neon aesthetic styling
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
    var pipWidthDp by remember { mutableStateOf(300.dp) }
    var showControls by remember { mutableStateOf(true) }

    // Auto-hide floating controls after 3 seconds
    LaunchedEffect(showControls, playerState.isPlaying) {
        if (showControls && playerState.isPlaying) {
            delay(3000)
            showControls = false
        }
    }

    val title = playerState.currentSong?.title ?: playerState.currentItem?.title ?: "Playing Video"
    val isPlaying = playerState.isPlaying

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val density = LocalDensity.current
        val parentWidthPx = constraints.maxWidth.toFloat()
        val parentHeightPx = constraints.maxHeight.toFloat()

        val minWidthPx = with(density) { 180.dp.toPx() }
        val maxWidthPx = (parentWidthPx - with(density) { 16.dp.toPx() }).coerceAtLeast(minWidthPx)

        val currentPipWidthPx = with(density) { pipWidthDp.toPx() }.coerceIn(minWidthPx, maxWidthPx)
        val currentPipHeightPx = currentPipWidthPx * (9f / 16f)

        // Draggable boundary clamping
        val minX = 0f
        val maxX = (parentWidthPx - currentPipWidthPx).coerceAtLeast(0f)
        val minY = 0f
        val maxY = (parentHeightPx - currentPipHeightPx).coerceAtLeast(0f)

        // Keep inside bounds if screen size or PiP size changes
        LaunchedEffect(currentPipWidthPx, currentPipHeightPx, parentWidthPx, parentHeightPx) {
            if (offsetX == 0f && offsetY == 0f && maxX > 0 && maxY > 0) {
                // Initialize to bottom-right corner
                offsetX = (maxX - with(density) { 16.dp.toPx() }).coerceIn(minX, maxX)
                offsetY = (maxY - with(density) { 16.dp.toPx() }).coerceIn(minY, maxY)
            } else {
                offsetX = offsetX.coerceIn(minX, maxX)
                offsetY = offsetY.coerceIn(minY, maxY)
            }
        }

        var isPinching by remember { mutableStateOf(false) }
        var lastTapTime by remember { mutableStateOf(0L) }
        var lastTapPos by remember { mutableStateOf(Offset(0f, 0f)) }
        val slopThreshold = with(density) { 12f }

        // Double-tap cycle or single-tap controls toggle
        fun handleTap(pos: Offset) {
            val now = System.currentTimeMillis()
            val sinceLastTap = now - lastTapTime
            val movedSinceLastTap = kotlin.math.hypot(pos.x - lastTapPos.x, pos.y - lastTapPos.y) > slopThreshold

            if (!movedSinceLastTap && sinceLastTap < 350L) {
                // Double tap: cycle card size (Small -> Medium -> Large -> Medium)
                val smallDp = 200.dp
                val medDp = 300.dp
                val maxDp = with(density) { maxWidthPx.toDp() }

                pipWidthDp = when {
                    pipWidthDp < 250.dp -> medDp
                    pipWidthDp < 350.dp -> maxDp
                    else -> smallDp
                }
                lastTapTime = 0L
                return
            }

            lastTapTime = now
            lastTapPos = pos
            showControls = !showControls
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .size(
                    width = with(density) { currentPipWidthPx.toDp() },
                    height = with(density) { currentPipHeightPx.toDp() }
                )
                .pointerInput(parentWidthPx, parentHeightPx) {
                    awaitPointerEventScope {
                        var initialDistance = 0f
                        var initialWidth = currentPipWidthPx
                        var initialOffsetX = offsetX
                        var initialOffsetY = offsetY
                        var initialCentroid = Offset.Zero
                        isPinching = false

                        while (true) {
                            val event = awaitPointerEvent()
                            val changes = event.changes
                            if (changes.isEmpty()) continue

                            val downCount = changes.count { it.pressed }

                            if (downCount == 1) {
                                val change = changes.first()
                                if (change.pressed) {
                                    if (!isPinching) {
                                        // 1-Finger Drag PiP Card
                                        val dragX = change.position.x - change.previousPosition.x
                                        val dragY = change.position.y - change.previousPosition.y
                                        offsetX = (offsetX + dragX).coerceIn(minX, maxX)
                                        offsetY = (offsetY + dragY).coerceIn(minY, maxY)
                                        showControls = true
                                    }
                                } else {
                                    val upChange = changes.firstOrNull { !it.pressed && !it.isConsumed }
                                    if (upChange != null && !isPinching) {
                                        handleTap(upChange.position)
                                    }
                                    isPinching = false
                                }
                                change.consume()
                            } else if (downCount >= 2) {
                                // 2-Finger Pinch: Resize Card directly
                                isPinching = true
                                showControls = false

                                val p1 = changes[0].position
                                val p2 = changes[1].position
                                val currentDistance = kotlin.math.hypot(p1.x - p2.x, p1.y - p2.y)
                                val currentCentroid = Offset((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)

                                if (initialDistance <= 0f) {
                                    initialDistance = currentDistance
                                    initialWidth = currentPipWidthPx
                                    initialOffsetX = offsetX
                                    initialOffsetY = offsetY
                                    initialCentroid = currentCentroid
                                } else if (initialDistance > 10f) {
                                    val scaleRatio = currentDistance / initialDistance
                                    val targetWidthPx = (initialWidth * scaleRatio).coerceIn(minWidthPx, maxWidthPx)
                                    val targetHeightPx = targetWidthPx * (9f / 16f)

                                    // Expand/shrink smoothly around pinch centroid
                                    val widthDiff = targetWidthPx - initialWidth
                                    val heightDiff = targetHeightPx - (initialWidth * (9f / 16f))

                                    val targetMaxX = (parentWidthPx - targetWidthPx).coerceAtLeast(0f)
                                    val targetMaxY = (parentHeightPx - targetHeightPx).coerceAtLeast(0f)

                                    offsetX = (initialOffsetX - (widthDiff * 0.5f)).coerceIn(0f, targetMaxX)
                                    offsetY = (initialOffsetY - (heightDiff * 0.5f)).coerceIn(0f, targetMaxY)

                                    pipWidthDp = with(density) { targetWidthPx.toDp() }
                                }
                                changes.forEach { it.consume() }
                            } else {
                                initialDistance = 0f
                                isPinching = false
                            }
                        }
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
        ) {
            // 1. Live Video Surface View (Fills the Card Box directly)
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
                                .size(34.dp)
                                .clickable { onExpandFullscreen() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Fullscreen,
                                    contentDescription = "Expand Fullscreen",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .size(34.dp)
                                .clickable { onClose() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
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
