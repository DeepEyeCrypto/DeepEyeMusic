// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun SwipeSkipSurface(
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var dragX by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    Box(
        modifier =
        modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, amount ->
                        dragX += amount
                        isDragging = true
                    },
                    onDragEnd = {
                        isDragging = false
                        when {
                            dragX < -180f -> onSwipeLeft() // Swiped left -> Next
                            dragX > 180f -> onSwipeRight() // Swiped right -> Previous
                        }
                        dragX = 0f
                    },
                    onDragCancel = {
                        isDragging = false
                        dragX = 0f
                    },
                )
            }
            .clickable { onTap() },
    ) {
        content()

        // Visual Feedback Indicators
        AnimatedVisibility(
            visible = isDragging && dragX < -100f,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier =
            Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 24.dp),
        ) {
            Box(
                modifier =
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.FastForward, contentDescription = "Next", tint = Color.White)
            }
        }

        AnimatedVisibility(
            visible = isDragging && dragX > 100f,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier =
            Modifier
                .align(Alignment.CenterStart)
                .padding(start = 24.dp),
        ) {
            Box(
                modifier =
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.FastRewind, contentDescription = "Previous", tint = Color.White)
            }
        }
    }
}
