package com.deepeye.musicpro.ui.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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

@Composable
fun NowPlayingVisualizerOverlay(
    fftData: FloatArray,
    dominantColor: Color,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val alpha by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.2f,
        animationSpec = tween(500),
        label = "visAlpha"
    )

    // Pre-calculate gradient to avoid per-frame allocations
    val visualizerGradient = remember(dominantColor) {
        Brush.verticalGradient(
            colors = listOf(
                dominantColor.copy(alpha = 0.8f),
                dominantColor.copy(alpha = 0.0f)
            )
        )
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .alpha(alpha)
    ) {
        val barCount = 48
        val barWidth = size.width / (barCount * 2f)
        val gap = barWidth * 0.6f
        val centerX = size.width / 2f

        // ── Draw Mirror FFT Bars ──
        for (i in 0 until barCount) {
            val magnitude = if (fftData.isNotEmpty()) {
                val fftIndex = (i * (fftData.size / barCount)).coerceIn(0, fftData.size - 1)
                fftData[fftIndex].coerceIn(0f, 1f)
            } else 0f

            val barHeight = magnitude * size.height * 0.7f + 2.dp.toPx()

            // Calculate X positions for mirror effect
            val xRight = centerX + i * (barWidth + gap)
            val xLeft = centerX - i * (barWidth + gap) - barWidth

            listOf(xRight, xLeft).forEach { x ->
                if (x >= 0 && x + barWidth <= size.width) {
                    drawRoundRect(
                        brush = visualizerGradient,
                        topLeft = Offset(x, size.height - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                }
            }
        }

        // ── Draw Soft Waveform Overlay ──
        if (fftData.isNotEmpty()) {
            val path = Path()
            val points = fftData.size / 4
            val step = size.width / points

            path.moveTo(0f, size.height * 0.8f)
            for (i in 0 until points) {
                val mag = fftData[i.coerceIn(0, fftData.size - 1)]
                val y = size.height * 0.8f - (mag * size.height * 0.2f)
                path.lineTo(i * step, y)
            }

            drawPath(
                path = path,
                color = dominantColor.copy(alpha = 0.4f),
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}
