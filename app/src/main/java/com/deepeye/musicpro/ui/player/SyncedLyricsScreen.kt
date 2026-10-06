// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncDisabled
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.musicpro.domain.model.Lyrics
import com.deepeye.musicpro.domain.model.LyricsLine
import com.deepeye.musicpro.ui.theme.NeonCyan

/**
 * SyncedLyricsScreen: Flagship Kinetic Typography Synchronized Lyrics View.
 *
 * Implements real-time viewport auto-centering, dynamic luminance scaling,
 * interactive line seeking, and transparent ambient blur layering.
 */
@Composable
fun SyncedLyricsScreen(
    lyrics: Lyrics?,
    activeLineIndex: Int,
    dominantColor: Color = NeonCyan,
    onSeekTo: (Long) -> Unit,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val centerOffset = -(screenHeightPx / 3f).toInt()

    // Smooth Kinetic Auto-Centering Loop
    LaunchedEffect(activeLineIndex) {
        if (lyrics != null && lyrics.isSynced && activeLineIndex in lyrics.lines.indices) {
            try {
                listState.animateScrollToItem(
                    index = activeLineIndex,
                    scrollOffset = centerOffset
                )
            } catch (_: Exception) {
                // Ignore rapid animation cancellation during scrubbing
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        if (lyrics == null || lyrics.lines.isEmpty()) {
            // Empty / Loading State
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.SyncDisabled,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No synchronized lyrics available",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    top = configuration.screenHeightDp.dp / 3,
                    bottom = configuration.screenHeightDp.dp / 2,
                    start = 24.dp,
                    end = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(
                    items = lyrics.lines,
                    key = { idx, line -> "${line.timestampMs}_$idx" }
                ) { index, line ->
                    val isActive = lyrics.isSynced && index == activeLineIndex
                    val isPast = lyrics.isSynced && index < activeLineIndex

                    val targetScale = if (isActive) 1.12f else if (isPast) 0.94f else 0.90f
                    val targetAlpha = if (isActive) 1.0f else if (isPast) 0.55f else 0.35f

                    val scale by animateFloatAsState(
                        targetValue = targetScale,
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
                        label = "kineticScale"
                    )

                    val alpha by animateFloatAsState(
                        targetValue = targetAlpha,
                        animationSpec = tween(durationMillis = 200),
                        label = "kineticAlpha"
                    )

                    val textColor by animateColorAsState(
                        targetValue = if (isActive) dominantColor else Color.White,
                        animationSpec = tween(durationMillis = 200),
                        label = "kineticColor"
                    )

                    KineticLyricLineRow(
                        text = line.text,
                        isActive = isActive,
                        scale = scale,
                        alpha = alpha,
                        textColor = textColor,
                        onClick = {
                            if (lyrics.isSynced) {
                                onSeekTo(line.timestampMs)
                            }
                        }
                    )
                }
            }
        }

        // Top Dismiss Icon (if presented in modal or overlay mode)
        if (onDismiss != null) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(16.dp)
                    .align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Lyrics",
                    tint = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun KineticLyricLineRow(
    text: String,
    isActive: Boolean,
    scale: Float,
    alpha: Float,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 6.dp, horizontal = 8.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = if (isActive) 26.sp else 22.sp,
                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                lineHeight = if (isActive) 34.sp else 30.sp,
                letterSpacing = (-0.3).sp
            ),
            color = textColor,
            textAlign = TextAlign.Start
        )
    }
}
