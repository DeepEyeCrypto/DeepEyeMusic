// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.dsp.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A studio-grade real-time audio visualizer that draws FFT data as glowing bars.
 */
@Composable
fun StudioVisualizer(
    fftData: FloatArray,
    modifier: Modifier = Modifier,
    barColor: Color = Color(0xFF00E5FF)
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
    ) {
        if (fftData.isEmpty()) return@Canvas

        val canvasWidth = size.width
        val canvasHeight = size.height
        
        // We might get up to 512 bins, but drawing all of them is too dense.
        // Let's sample and draw a fixed number of bars (e.g., 32 or 64).
        val numBars = 48
        val spacing = 4.dp.toPx()
        val barWidth = (canvasWidth - (spacing * (numBars - 1))) / numBars
        
        // Bin size
        val binsPerBar = (fftData.size / 2) / numBars // Use first half of FFT (lower frequencies are more active)
        
        for (i in 0 until numBars) {
            // Calculate average magnitude for this bar's frequency range
            var sum = 0f
            val startIndex = i * binsPerBar
            for (j in 0 until binsPerBar) {
                if (startIndex + j < fftData.size) {
                    sum += fftData[startIndex + j]
                }
            }
            val averageMagnitude = if (binsPerBar > 0) sum / binsPerBar else 0f
            
            // Apply a slight exponential curve to make quiet sounds visible but loud sounds punchy
            val visualHeight = (Math.pow(averageMagnitude.toDouble(), 0.7).toFloat() * canvasHeight).coerceIn(4.dp.toPx(), canvasHeight)
            
            val x = i * (barWidth + spacing)
            val y = canvasHeight - visualHeight

            // Draw glowing bar
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(barColor, barColor.copy(alpha = 0.2f)),
                    startY = y,
                    endY = canvasHeight
                ),
                topLeft = Offset(x, y),
                size = Size(barWidth, visualHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
