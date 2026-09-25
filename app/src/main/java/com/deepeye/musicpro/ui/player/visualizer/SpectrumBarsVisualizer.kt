// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later
//
// SpectrumBarsVisualizer — classic mirrored FFT bar analyzer.
//
// Zero-recomposition: all audio state is read and interpolated INSIDE the Canvas
// draw lambda. Buffers are pre-allocated; the draw loop performs no allocation.

package com.deepeye.musicpro.ui.player.visualizer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.max
import kotlin.math.min

private const val BAR_COUNT = 48
private const val BAR_GAP_RATIO = 0.28f

@Composable
fun SpectrumBarsVisualizer(
    fftSpectrum: StateFlow<FloatArray>,
    frequencyBands: StateFlow<FloatArray>,
    accentColor: Color = VvavyCyan,
    intensity: Float = 1f,
    reducedMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
    val interpolator = remember { AudioFrameInterpolator() }

    Canvas(modifier = modifier.fillMaxSize()) {
        interpolator.update(
            rawBands = frequencyBands.value,
            rawSpectrum = fftSpectrum.value,
            intensity = intensity,
            reducedMotion = reducedMotion,
            nowNanos = System.nanoTime()
        )

        val spectrum = interpolator.spectrum
        val bass = interpolator.bands.getOrElse(0) { 0f }
        val treble = interpolator.bands.getOrElse(4) { 0f }
        val peak = interpolator.bands.getOrElse(5) { 0f }

        val w = size.width
        val h = size.height
        val centerY = h / 2f

        drawRect(color = VvavyBg)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accentColor.copy(alpha = 0.10f + bass * 0.08f), Color.Transparent),
                center = Offset(w / 2f, centerY),
                radius = max(w, h) * 0.6f
            ),
            radius = max(w, h) * 0.6f,
            center = Offset(w / 2f, centerY)
        )

        val slot = w / BAR_COUNT
        val barWidth = slot * (1f - BAR_GAP_RATIO)
        val maxBar = h * 0.46f
        val radius = CornerRadius(barWidth / 2f, barWidth / 2f)

        for (i in 0 until BAR_COUNT) {
            // Log-ish bin mapping so low frequencies are not all crammed left.
            val bin = (i.toFloat() / (BAR_COUNT - 1))
            val src = bin * (spectrum.size - 1)
            val lo = src.toInt()
            val hi = min(lo + 1, spectrum.size - 1)
            val frac = src - lo
            val v = (spectrum[lo] * (1f - frac) + spectrum[hi] * frac).coerceIn(0f, 1f)

            val barH = max(barWidth, v * maxBar * (0.85f + peak * 0.3f))
            val x = i * slot + (slot - barWidth) / 2f

            // Mirror above and below the centre line.
            val topY = centerY - barH
            val botY = centerY

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.25f + treble * 0.5f),
                        accentColor.copy(alpha = 0.95f)
                    ),
                    startY = topY,
                    endY = botY
                ),
                topLeft = Offset(x, topY),
                size = Size(barWidth, barH),
                cornerRadius = radius
            )
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.95f),
                        accentColor.copy(alpha = 0.25f + treble * 0.5f)
                    ),
                    startY = botY,
                    endY = botY + barH
                ),
                topLeft = Offset(x, botY),
                size = Size(barWidth, barH),
                cornerRadius = radius
            )
        }
    }
}