// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later
//
// WaveformVisualizer — scrolling oscilloscope trace built from the live spectrum.
//
// The engine exposes a log-spectrum rather than raw time-domain samples, so this
// reconstructs a scrolling ribbon by walking the spectrum left-to-right and
// mirroring it vertically. Pre-allocated ring buffer, zero per-frame allocation.

package com.deepeye.musicpro.ui.player.visualizer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.sin

private const val TRACE_POINTS = 96
private const val TRACE_HISTORY = 64

@Composable
fun WaveformVisualizer(
    fftSpectrum: StateFlow<FloatArray>,
    frequencyBands: StateFlow<FloatArray>,
    accentColor: Color = VvavyCyan,
    primaryColor: Color = accentColor,
    secondaryColor: Color = Color(0xFFFF007F),
    intensity: Float = 1f,
    reducedMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
    val interpolator = remember { AudioFrameInterpolator() }
    val history = remember { Array(TRACE_HISTORY) { FloatArray(TRACE_POINTS) } }
    val head = remember { intArrayOf(0) }
    val path = remember { Path() }
    val frameClock = remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        var startNanos = 0L
        while (isActive) {
            withFrameNanos { frameNanos ->
                if (startNanos == 0L) startNanos = frameNanos
                frameClock.floatValue = (frameNanos - startNanos) / 1_000_000_000f
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val time = frameClock.floatValue
        interpolator.update(
            rawBands = frequencyBands.value,
            rawSpectrum = fftSpectrum.value,
            intensity = intensity,
            reducedMotion = reducedMotion,
            nowNanos = System.nanoTime()
        )

        val spectrum = interpolator.spectrum
        val bass = interpolator.bands.getOrElse(0) { 0f }
        val mid = interpolator.bands.getOrElse(2) { 0f }
        val treble = interpolator.bands.getOrElse(4) { 0f }
        val peak = interpolator.bands.getOrElse(5) { 0f }

        val w = size.width
        val h = size.height
        val centerY = h / 2f
        val amp = h * 0.34f

        // Soft translucent scrim to allow ambient artwork to glow through
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color.Black.copy(alpha = 0.35f), Color.Black.copy(alpha = 0.55f))
            )
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primaryColor.copy(alpha = 0.08f + bass * 0.10f), Color.Transparent),
                center = Offset(w / 2f, centerY),
                radius = maxOf(w, h) * 0.55f
            ),
            radius = maxOf(w, h) * 0.55f,
            center = Offset(w / 2f, centerY)
        )

        // Capture one new column of samples per frame into the ring buffer.
        val column = history[head[0]]
        for (i in 0 until TRACE_POINTS) {
            val t = i.toFloat() / (TRACE_POINTS - 1)
            val src = t * (spectrum.size - 1)
            val lo = src.toInt()
            val hi = if (lo + 1 < spectrum.size) lo + 1 else spectrum.size - 1
            val frac = src - lo
            val mag = (spectrum[lo] * (1f - frac) + spectrum[hi] * frac).coerceIn(0f, 1f)
            // Phase-shifted sine keeps the ribbon organic rather than a flat comb.
            val phase = t * 6f * PI.toFloat()
            column[i] = (mag * sin(phase) * 0.75f + mag * 0.25f) * amp
        }
        head[0] = (head[0] + 1) % TRACE_HISTORY

        val stepX = w / (TRACE_POINTS - 1)

        // Oldest-to-newest, so the trace scrolls right as new audio arrives.
        for (age in TRACE_HISTORY - 1 downTo 0) {
            val idx = (head[0] - 1 - age + TRACE_HISTORY * 2) % TRACE_HISTORY
            val col = history[idx]
            val ageT = 1f - age.toFloat() / (TRACE_HISTORY - 1) // 0 = oldest

            path.reset()
            for (i in 0 until TRACE_POINTS) {
                val x = i * stepX
                val y = centerY - col[i] * (0.35f + ageT * 0.65f)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            drawPath(
                path = path,
                color = lerpAccentToWhite(if (age % 2 == 0) primaryColor else secondaryColor, ageT)
                    .copy(alpha = 0.06f + ageT * (0.35f + mid * 0.35f + peak * 0.2f)),
                style = Stroke(width = 1f + ageT * (1.2f + treble * 1.6f))
            )
        }

        // Bright leading edge.
        val newest = history[(head[0] - 1 + TRACE_HISTORY) % TRACE_HISTORY]
        path.reset()
        for (i in 0 until TRACE_POINTS) {
            val x = i * stepX
            val y = centerY - newest[i]
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            brush = Brush.horizontalGradient(
                colors = listOf(primaryColor.copy(alpha = 0.35f), Color.White.copy(alpha = 0.95f))
            ),
            style = Stroke(width = 2f + peak * 2.5f)
        )

        // Centre reference line.
        drawLine(
            color = Color.White.copy(alpha = 0.10f),
            start = Offset(0f, centerY),
            end = Offset(w, centerY),
            strokeWidth = 1f
        )
    }
}

private fun lerpAccentToWhite(accent: Color, t: Float): Color {
    val k = t.coerceIn(0f, 1f)
    return Color(
        red = accent.red + (1f - accent.red) * k * 0.7f,
        green = accent.green + (1f - accent.green) * k * 0.7f,
        blue = accent.blue + (1f - accent.blue) * k * 0.7f,
        alpha = 1f
    )
}