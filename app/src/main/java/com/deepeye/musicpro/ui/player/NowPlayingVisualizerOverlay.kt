// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.player

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * 32-Bar Mirror Spectrum Visualizer with spring dynamics.
 *
 * Uses spring physical animation model (stiffness=300, damping=15) for
 * smooth, natural bar movement. Bars mirror from center for symmetry.
 * Includes Bezier waveform underneath for ambient flow effect.
 */
@Composable
fun NowPlayingVisualizerOverlay(
    fftData: FloatArray,
    dominantColor: Color,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 32,
) {
    val alpha by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.15f,
        animationSpec = tween(400),
        label = "visAlpha",
    )

    // Spring-animated bar heights for natural physical feel
    val animatedHeights = remember(barCount) {
        List(barCount) { Animatable(0f) }
    }

    val scope = rememberCoroutineScope()

    LaunchedEffect(fftData, isPlaying) {
        if (!isPlaying || fftData.isEmpty()) return@LaunchedEffect
        val step = (fftData.size.toFloat() / barCount).coerceAtLeast(1f)
        for (i in 0 until barCount) {
            val dataIndex = (i * step).toInt().coerceIn(0, fftData.size - 1)
            // Apply power curve for better low/mid frequency visualization
            val raw = fftData[dataIndex].coerceIn(0f, 1f)
            val targetHeight = Math.pow(raw.toDouble(), 0.85).toFloat().coerceIn(0f, 1f)
            scope.launch {
                animatedHeights[i].animateTo(
                    targetValue = targetHeight,
                    animationSpec = spring(
                        dampingRatio = 0.55f, // ~15 damping equivalent
                        stiffness = 300f,
                    )
                )
            }
        }
    }

    val visualizerGradient = remember(dominantColor) {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.85f),
                dominantColor.copy(alpha = 0.6f),
                Color.Transparent,
            ),
        )
    }

    val waveGlow = remember(dominantColor) {
        Brush.verticalGradient(
            colors = listOf(dominantColor.copy(alpha = 0.25f), Color.Transparent),
        )
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .alpha(alpha),
    ) {
        val barWidth = size.width / (barCount * 2.5f)
        val gap = barWidth * 0.7f
        val centerX = size.width / 2f

        // ── Smooth Bezier Waveform ──
        if (fftData.isNotEmpty()) {
            val path = Path()
            val points = (fftData.size / 4).coerceAtLeast(2)
            val step = size.width / points

            path.moveTo(0f, size.height)

            var prevX = 0f
            var prevY = size.height

            for (i in 0 until points) {
                val mag = fftData[i.coerceIn(0, fftData.size - 1)]
                val y = size.height - (mag * size.height * 0.4f) - 8.dp.toPx()
                val x = i * step

                if (i == 0) {
                    path.lineTo(x, y)
                } else {
                    path.quadraticTo(
                        prevX + (x - prevX) / 2f,
                        prevY,
                        x,
                        y,
                    )
                }
                prevX = x
                prevY = y
            }

            // Fill under the wave
            val waveFill = Path().apply {
                addPath(path)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(path = waveFill, brush = waveGlow)

            // Glowing wave stroke
            drawPath(
                path = path,
                color = dominantColor.copy(alpha = 0.7f),
                style = Stroke(width = 2.dp.toPx()),
            )
        }

        // ── Mirror FFT Bars (Spring-animated) ──
        for (i in 0 until barCount) {
            val magnitude = animatedHeights.getOrNull(i)?.value ?: 0f

            val baseHeight = 3.dp.toPx()
            val barHeight = baseHeight + (magnitude * size.height * 0.75f)

            // Mirror X relative to center
            val xRight = centerX + i * (barWidth + gap)
            val xLeft = centerX - i * (barWidth + gap) - barWidth

            listOf(xRight, xLeft).forEach { x ->
                if (x >= 0 && x + barWidth <= size.width) {
                    drawRoundRect(
                        brush = visualizerGradient,
                        topLeft = Offset(x, size.height - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2f),
                    )
                }
            }
        }
    }
}
