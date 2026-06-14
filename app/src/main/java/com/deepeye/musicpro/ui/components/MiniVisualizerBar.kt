// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun MiniVisualizerBar(
    fftData: FloatArray,
    barColor: Color,
    modifier: Modifier = Modifier,
    barCount: Int = 32,
) {
    val animatedBars = remember { Animatable(0f) }

    LaunchedEffect(fftData.toList()) {
        animatedBars.animateTo(
            1f,
            animationSpec = tween(80, easing = LinearEasing),
        )
        animatedBars.snapTo(0f)
    }

    Canvas(modifier = modifier) {
        val barWidth = size.width / (barCount * 1.5f)
        val gap = barWidth * 0.5f

        for (i in 0 until barCount) {
            val rawMagnitude =
                if (fftData.isNotEmpty()) {
                    val fftIndex =
                        (i * (fftData.size / barCount))
                            .coerceIn(0, fftData.size - 1)
                    fftData[fftIndex].coerceIn(0f, 1f)
                } else {
                    0f
                }

            val barHeight = (rawMagnitude * size.height).coerceAtLeast(4.dp.toPx())
            val x = i * (barWidth + gap)
            val y = size.height - barHeight

            drawRoundRect(
                color =
                barColor.copy(
                    alpha = 0.4f + (rawMagnitude * 0.6f),
                ),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(2.dp.toPx()),
            )
        }
    }
}
