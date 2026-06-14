// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.queue

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun SwipeQueueCard(
    content: @Composable () -> Unit,
    onRemove: () -> Unit,
    onPlayNext: () -> Unit,
    onPin: () -> Unit,
) {
    var offsetX by remember { mutableStateOf(0f) }
    var isDismissed by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = !isDismissed,
        exit = fadeOut(tween(200)) + shrinkVertically(tween(200)),
    ) {
        Box(
            modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    when {
                        offsetX < -50f -> MaterialTheme.colorScheme.error.copy(alpha = 0.5f) // Red for remove
                        offsetX > 50f -> com.deepeye.musicpro.ui.theme.TealDim.copy(alpha = 0.5f) // Teal for action
                        else -> Color.Transparent
                    },
                )
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount
                        },
                        onDragEnd = {
                            when {
                                offsetX < -220f -> {
                                    isDismissed = true
                                    onRemove()
                                }
                                offsetX > 220f -> {
                                    onPlayNext()
                                    offsetX = 0f
                                }
                                offsetX > 120f -> {
                                    onPin()
                                    offsetX = 0f
                                }
                                else -> offsetX = 0f
                            }
                        },
                    )
                },
        ) {
            // Background Action Hints
            if (offsetX < 0) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(end = 24.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.White)
                }
            } else if (offsetX > 0) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(start = 24.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Icon(
                        imageVector = if (offsetX > 220f) Icons.Default.PlayArrow else Icons.Default.PushPin,
                        contentDescription = "Action",
                        tint = Color.White,
                    )
                }
            }

            // Foreground Content
            Box(
                modifier =
                Modifier
                    .offset { IntOffset(offsetX.roundToInt(), 0) }
                    .background(MaterialTheme.colorScheme.surface), // Queue background
            ) {
                content()
            }
        }
    }
}
