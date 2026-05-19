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

    val visualizerGradient = remember(dominantColor) {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.9f),
                dominantColor.copy(alpha = 0.7f),
                Color.Transparent
            )
        )
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .alpha(alpha)
    ) {
        val barCount = 32
        val barWidth = size.width / (barCount * 2.5f)
        val gap = barWidth * 0.8f
        val centerX = size.width / 2f

        // ── Smooth Bezier Waveform ──
        if (fftData.isNotEmpty()) {
            val path = Path()
            val points = fftData.size / 4
            val step = size.width / points

            path.moveTo(0f, size.height)
            
            var prevX = 0f
            var prevY = size.height
            
            for (i in 0 until points) {
                val mag = fftData[i.coerceIn(0, fftData.size - 1)]
                val y = size.height - (mag * size.height * 0.5f) - 10.dp.toPx()
                val x = i * step
                
                if (i == 0) {
                    path.lineTo(x, y)
                } else {
                    // Cubic Bezier curve for smooth flowing wave
                    path.quadraticBezierTo(
                        prevX + (x - prevX) / 2f, prevY, 
                        x, y
                    )
                }
                prevX = x
                prevY = y
            }
            
            // Draw translucent gradient wave under the line
            val waveFill = Path().apply {
                addPath(path)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(
                path = waveFill,
                brush = Brush.verticalGradient(
                    colors = listOf(dominantColor.copy(alpha = 0.3f), Color.Transparent)
                )
            )
            
            // Draw the glowing wave line
            drawPath(
                path = path,
                color = dominantColor.copy(alpha = 0.8f),
                style = Stroke(width = 3.dp.toPx())
            )
        }

        // ── Mirror FFT Bars ──
        for (i in 0 until barCount) {
            val magnitude = if (fftData.isNotEmpty()) {
                val fftIndex = (i * (fftData.size / barCount)).coerceIn(0, fftData.size - 1)
                // Slight curve mapping for better visualization of low/mid frequencies
                Math.pow(fftData[fftIndex].toDouble(), 1.2).toFloat().coerceIn(0f, 1f)
            } else 0f

            val baseHeight = 4.dp.toPx()
            val barHeight = baseHeight + (magnitude * size.height * 0.8f)

            // Mirror X relative to center
            val xRight = centerX + i * (barWidth + gap)
            val xLeft = centerX - i * (barWidth + gap) - barWidth

            listOf(xRight, xLeft).forEach { x ->
                if (x >= 0 && x + barWidth <= size.width) {
                    drawRoundRect(
                        brush = visualizerGradient,
                        topLeft = Offset(x, size.height - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2f)
                    )
                }
            }
        }
    }
}
