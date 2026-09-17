// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.musicpro.domain.model.Lyrics
import com.deepeye.musicpro.ui.components.GlassBottomSheet
import com.deepeye.musicpro.ui.motion.premiumScrollHaptics
import com.deepeye.musicpro.ui.theme.NeonCyan

/**
 * Production-grade Karaoke-style Synchronized Lyrics Sheet.
 *
 * Features:
 * - Real-time active lyric line tracking with progressive glow
 * - Tap any line to seek ExoPlayer immediately
 * - Fluid auto-scroll with viewport centering
 * - Dynamic font sizing and glassmorphism styling
 */
@Composable
fun LyricsBottomSheet(
    lyrics: Lyrics?,
    playbackPositionMs: Long,
    dominantColor: Color,
    onSeekTo: (Long) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var fontSizeMultiplier by remember { mutableFloatStateOf(1.0f) }
    var showFontControls by remember { mutableStateOf(false) }

    GlassBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier.fillMaxHeight(0.88f),
        tintColor = dominantColor.copy(alpha = 0.28f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = NeonCyan.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f)),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (lyrics?.isSynced == true) Icons.Default.Sync else Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Live Lyrics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (lyrics?.isSynced == true) "Karaoke Synchronized" else "Plain Text",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (lyrics?.isSynced == true) NeonCyan else Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showFontControls = !showFontControls },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatSize,
                            contentDescription = "Font Size",
                            tint = if (showFontControls) NeonCyan else Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Optional Font Size Adjuster
            AnimatedVisibility(
                visible = showFontControls,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Size: ${(fontSizeMultiplier * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Slider(
                        value = fontSizeMultiplier,
                        onValueChange = { fontSizeMultiplier = it },
                        valueRange = 0.8f..1.5f,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (lyrics == null || lyrics.lines.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No lyrics found for this track",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                val listState = rememberLazyListState()

                val activeIndex = remember(lyrics, playbackPositionMs) {
                    if (!lyrics.isSynced) {
                        -1
                    } else {
                        val index = lyrics.lines.indexOfLast { it.timestampMs <= playbackPositionMs }
                        if (index == -1) 0 else index
                    }
                }

                // Smoothly auto-scroll to center active line
                LaunchedEffect(activeIndex) {
                    if (lyrics.isSynced && activeIndex >= 0) {
                        val layoutInfo = listState.layoutInfo
                        val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
                        val offset = if (viewportHeight > 0) -viewportHeight / 3 else -250
                        listState.animateScrollToItem(index = activeIndex, scrollOffset = offset)
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .premiumScrollHaptics(listState),
                    verticalArrangement = Arrangement.spacedBy(22.dp),
                    contentPadding = PaddingValues(vertical = 40.dp)
                ) {
                    itemsIndexed(lyrics.lines) { index, line ->
                        val isActive = index == activeIndex
                        val isPast = lyrics.isSynced && index < activeIndex
                        val isFuture = lyrics.isSynced && index > activeIndex

                        val targetAlpha = when {
                            isActive -> 1.0f
                            isPast -> 0.45f
                            isFuture -> 0.25f
                            else -> 0.85f
                        }

                        val targetScale = if (isActive) 1.06f else 0.98f

                        val animatedAlpha by animateFloatAsState(
                            targetValue = targetAlpha,
                            animationSpec = tween(300),
                            label = "lyricAlpha"
                        )

                        val animatedScale by animateFloatAsState(
                            targetValue = targetScale,
                            animationSpec = spring(stiffness = 300f),
                            label = "lyricScale"
                        )

                        val animatedColor by animateColorAsState(
                            targetValue = if (isActive) NeonCyan else Color.White,
                            animationSpec = tween(300),
                            label = "lyricColor"
                        )

                        val baseFontSize = (if (isActive) 22.sp else 18.sp) * fontSizeMultiplier

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    scaleX = animatedScale
                                    scaleY = animatedScale
                                }
                                .alpha(animatedAlpha)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isActive) dominantColor.copy(alpha = 0.15f) else Color.Transparent
                                )
                                .clickable(enabled = lyrics.isSynced) {
                                    onSeekTo(line.timestampMs)
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = line.text,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = baseFontSize,
                                    fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                                    lineHeight = (baseFontSize.value * 1.35f).sp
                                ),
                                color = animatedColor,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
