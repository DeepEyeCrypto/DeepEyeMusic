package com.deepeye.musicpro.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.deepeye.musicpro.ui.theme.DeepEyePrimary
import com.deepeye.musicpro.ui.theme.DeepEyeSecondary

/**
 * Canvas-based audio visualizer with bar animation.
 *
 * Renders FFT magnitude data as vertical bars with gradient coloring
 * and smooth height transitions.
 */
@Composable
fun AudioVisualizer(
    fftData: FloatArray,
    modifier: Modifier = Modifier,
    barCount: Int = 32,
    barWidth: Dp = 4.dp,
    barSpacing: Dp = 2.dp,
    cornerRadius: Dp = 2.dp,
    primaryColor: Color = DeepEyePrimary,
    secondaryColor: Color = DeepEyeSecondary,
    minBarHeight: Float = 0.05f
) {
    // Animate each bar's height for smooth transitions
    val animatedHeights = remember(barCount) {
        List(barCount) { Animatable(minBarHeight) }
    }

    LaunchedEffect(fftData) {
        val step = fftData.size / barCount
        for (i in 0 until barCount) {
            val dataIndex = (i * step).coerceIn(0, fftData.size - 1)
            val targetHeight = fftData[dataIndex].coerceIn(minBarHeight, 1f)
            animatedHeights[i].animateTo(
                targetValue = targetHeight,
                animationSpec = tween(100, easing = LinearEasing)
            )
        }
    }

    Canvas(modifier = modifier) {
        val totalBarWidth = barWidth.toPx()
        val totalSpacing = barSpacing.toPx()
        val availableWidth = size.width
        val barsToRender = ((availableWidth + totalSpacing) / (totalBarWidth + totalSpacing)).toInt()
            .coerceAtMost(barCount)

        val totalBarsWidth = barsToRender * totalBarWidth + (barsToRender - 1) * totalSpacing
        val startX = (availableWidth - totalBarsWidth) / 2

        for (i in 0 until barsToRender) {
            val height = animatedHeights.getOrNull(i)?.value ?: minBarHeight
            val barHeight = size.height * height
            val x = startX + i * (totalBarWidth + totalSpacing)
            val y = size.height - barHeight

            // Gradient from primary to secondary based on height
            val barBrush = Brush.verticalGradient(
                colors = listOf(secondaryColor, primaryColor),
                startY = y,
                endY = size.height
            )

            drawRoundRect(
                brush = barBrush,
                topLeft = Offset(x, y),
                size = Size(totalBarWidth, barHeight),
                cornerRadius = CornerRadius(cornerRadius.toPx())
            )
        }
    }
}

@Preview
@Composable
private fun AudioVisualizerPreview() {
    val mockData = FloatArray(64) { (it % 8).toFloat() / 8f }
    AudioVisualizer(
        fftData = mockData,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
    )
}
