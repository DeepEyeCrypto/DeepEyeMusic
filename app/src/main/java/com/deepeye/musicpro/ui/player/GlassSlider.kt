// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.deepeye.musicpro.ui.motion.rememberPremiumHaptics
import kotlin.math.abs

/**
 * Premium glass progress slider with neon accent glow and glass track border.
 *
 * Features:
 * - Glass-transparent track with subtle white border
 * - Active portion uses horizontal gradient with vibrant dominant color
 * - Neon glow behind the thumb for a premium "breathing" look
 * - Smooth spring animation on thumb position for responsive feel
 */
@Composable
fun GlassSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    accentColor: Color = Color.White,
    testTag: String = "glass_seek_slider",
) {
    val normalizedValue = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start))
        .coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = normalizedValue,
        animationSpec = spring(stiffness = 600f, dampingRatio = 0.85f),
        label = "sliderProgress",
    )

    var isDragging by remember { mutableStateOf(false) }
    var accumulatedDrag by remember { mutableStateOf(0f) }
    val haptics = rememberPremiumHaptics()

    val trackHeightDpAnimated by animateFloatAsState(
        targetValue = if (isDragging) 16f else 6f,
        animationSpec = spring(stiffness = 500f, dampingRatio = 0.7f),
        label = "trackHeightAnimated"
    )

    val thumbRadiusDpAnimated by animateFloatAsState(
        targetValue = if (isDragging) 12f else 0f,
        animationSpec = spring(stiffness = 500f, dampingRatio = 0.7f),
        label = "thumbRadiusAnimated"
    )

    val glowAlphaAnimated by animateFloatAsState(
        targetValue = if (isDragging) 0.35f else 0.05f,
        animationSpec = spring(stiffness = 400f),
        label = "glowAlphaAnimated"
    )

    val trackGradient = remember(accentColor) {
        Brush.horizontalGradient(
            colors = listOf(
                accentColor.copy(alpha = 0.6f),
                accentColor,
                Color.White.copy(alpha = 0.95f),
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .testTag(testTag)
            .pointerInput(valueRange) {
                detectTapGestures { offset ->
                    haptics.heavyClick()
                    val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                    val newValue = valueRange.start + fraction * (valueRange.endInclusive - valueRange.start)
                    onValueChange(newValue)
                }
            }
            .pointerInput(valueRange) {
                detectHorizontalDragGestures(
                    onDragStart = { 
                        isDragging = true
                        accumulatedDrag = 0f
                        haptics.click()
                    },
                    onDragEnd = { 
                        isDragging = false
                        haptics.heavyClick()
                    },
                    onDragCancel = { isDragging = false },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        accumulatedDrag += abs(dragAmount)
                        if (accumulatedDrag > 12f) { // Optimized: Tick every ~12px of movement
                            haptics.slideTick()
                            accumulatedDrag = 0f
                        }
                        val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        val newValue = valueRange.start + fraction * (valueRange.endInclusive - valueRange.start)
                        onValueChange(newValue)
                    },
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val trackHeight = trackHeightDpAnimated.dp.toPx()
            val trackY = (size.height - trackHeight) / 2f
            val trackRadius = trackHeight / 2f
            val thumbRadius = thumbRadiusDpAnimated.dp.toPx()
            val thumbX = animatedProgress * size.width

            // Glass track background
            drawRoundRect(
                color = Color.White.copy(alpha = 0.10f),
                topLeft = Offset(0f, trackY),
                size = Size(size.width, trackHeight),
                cornerRadius = CornerRadius(trackRadius),
            )

            // Glass track border
            drawRoundRect(
                color = Color.White.copy(alpha = 0.15f),
                topLeft = Offset(0f, trackY),
                size = Size(size.width, trackHeight),
                cornerRadius = CornerRadius(trackRadius),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 0.5.dp.toPx()),
            )

            // Active track gradient
            if (animatedProgress > 0f) {
                drawRoundRect(
                    brush = trackGradient,
                    topLeft = Offset(0f, trackY),
                    size = Size(thumbX, trackHeight),
                    cornerRadius = CornerRadius(trackRadius),
                )
            }

            // Thumb glow (neon effect)
            if (thumbRadius > 0f) {
                drawCircle(
                    color = accentColor.copy(alpha = glowAlphaAnimated),
                    radius = thumbRadius * 2.2f,
                    center = Offset(thumbX, size.height / 2f),
                )

                // Thumb outer
                drawCircle(
                    color = Color.White,
                    radius = thumbRadius,
                    center = Offset(thumbX, size.height / 2f),
                )

                // Inner thumb accent dot
                drawCircle(
                    color = accentColor.copy(alpha = 0.8f),
                    radius = thumbRadius * 0.4f,
                    center = Offset(thumbX, size.height / 2f),
                )
            }
        }
    }
}
