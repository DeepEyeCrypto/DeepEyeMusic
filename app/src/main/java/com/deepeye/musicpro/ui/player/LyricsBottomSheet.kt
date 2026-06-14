// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.musicpro.domain.model.Lyrics
import com.deepeye.musicpro.ui.components.GlassBottomSheet
import com.deepeye.musicpro.ui.motion.premiumScrollHaptics

@Composable
fun LyricsBottomSheet(
    lyrics: Lyrics?,
    playbackPositionMs: Long,
    dominantColor: Color,
    onSeekTo: (Long) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier.fillMaxHeight(0.85f),
        tintColor = dominantColor.copy(alpha = 0.22f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            // Header
            Text(
                text = "Lyrics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                textAlign = TextAlign.Center
            )

            if (lyrics == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No lyrics found for this track",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.5f)
                    )
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

                // Auto-scroll to center active line
                LaunchedEffect(activeIndex) {
                    if (lyrics.isSynced && activeIndex >= 0) {
                        val layoutInfo = listState.layoutInfo
                        val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
                        val offset = if (viewportHeight > 0) {
                            -viewportHeight / 3
                        } else {
                            -200
                        }
                        listState.animateScrollToItem(index = activeIndex, scrollOffset = offset)
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .premiumScrollHaptics(listState),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    contentPadding = PaddingValues(vertical = 40.dp)
                ) {
                    itemsIndexed(lyrics.lines) { index, line ->
                        val isActive = index == activeIndex
                        val isPast = lyrics.isSynced && index < activeIndex
                        val isFuture = lyrics.isSynced && index > activeIndex

                        val targetAlpha = when {
                            isActive -> 1.0f
                            isPast -> 0.4f
                            isFuture -> 0.2f
                            else -> 0.7f // Non-synced lyrics
                        }

                        val targetColor = if (isActive) {
                            dominantColor
                        } else {
                            Color.White
                        }

                        val animatedAlpha by animateFloatAsState(
                            targetValue = targetAlpha,
                            animationSpec = tween(400),
                            label = "lyricAlpha"
                        )

                        val animatedColor by animateColorAsState(
                            targetValue = targetColor,
                            animationSpec = tween(400),
                            label = "lyricColor"
                        )

                        val fontSize = if (isActive) 22.sp else 18.sp
                        val fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal

                        Text(
                            text = line.text,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = fontSize,
                                fontWeight = fontWeight,
                                lineHeight = 28.sp
                            ),
                            color = animatedColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(animatedAlpha)
                                .clickable(enabled = lyrics.isSynced) {
                                    onSeekTo(line.timestampMs)
                                },
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
