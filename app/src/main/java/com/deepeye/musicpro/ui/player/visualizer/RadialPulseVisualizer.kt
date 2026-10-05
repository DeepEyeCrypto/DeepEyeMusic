// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later
//
// RadialPulseVisualizer — concentric reactive rings driven by spectral flux.
//
// Rings are pre-allocated; the expanding shockwave reuses a small fixed pool of
// phases rather than spawning objects per beat.

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
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private const val RING_COUNT = 7
private const val SHOCK_SLOTS = 4

@Composable
fun RadialPulseVisualizer(
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
    val ringPhase = remember { FloatArray(RING_COUNT) }
    val shockPhase = remember { FloatArray(SHOCK_SLOTS) { -1f } }
    val shockNext = remember { intArrayOf(0) }
    val prevBass = remember { floatArrayOf(0f) }
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

        val bass = interpolator.bands.getOrElse(0) { 0f }.coerceIn(0f, 2f)
        val mid = interpolator.bands.getOrElse(2) { 0f }.coerceIn(0f, 1.5f)
        val treble = interpolator.bands.getOrElse(4) { 0f }.coerceIn(0f, 1.5f)
        val peak = interpolator.bands.getOrElse(5) { 0f }.coerceIn(0f, 2f)

        val cx = size.width / 2f
        val cy = size.height / 2f
        val baseR = minOf(size.width, size.height) * 0.42f

        // Soft translucent scrim to allow ambient artwork to glow through
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color.Black.copy(alpha = 0.35f), Color.Black.copy(alpha = 0.55f))
            )
        )

        // Beat gate: a sharp bass rise spawns a shockwave. Disabled under
        // reduced motion so the screen does not strobe.
        val bassRise = bass - prevBass[0]
        prevBass[0] = bass
        if (!reducedMotion && bassRise > 0.30f) {
            shockPhase[shockNext[0]] = 0f
            shockNext[0] = (shockNext[0] + 1) % SHOCK_SLOTS
        }

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.18f + bass * 0.16f),
                    secondaryColor.copy(alpha = 0.08f + mid * 0.08f),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = baseR * 1.6f
            ),
            radius = baseR * 1.6f,
            center = Offset(cx, cy)
        )

        // Core disc scaled by treble for a high-frequency shimmer.
        val coreR = baseR * (0.16f + treble * 0.10f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.85f), primaryColor.copy(alpha = 0.25f)),
                center = Offset(cx, cy),
                radius = coreR
            ),
            radius = coreR,
            center = Offset(cx, cy)
        )

        // Concentric rings. Each ring carries a spectrum sample as displacement.
        for (i in 0 until RING_COUNT) {
            ringPhase[i] += 0.004f + mid * 0.004f
            if (ringPhase[i] > 2f * PI.toFloat()) ringPhase[i] -= 2f * PI.toFloat()

            val ringT = i.toFloat() / (RING_COUNT - 1)
            val wave = 1f + sin(ringPhase[i] + ringT * 2.4f) * (0.04f + mid * 0.05f)
            val r = baseR * (0.22f + ringT * 0.78f) * wave

            drawCircle(
                color = (if (i % 2 == 0) primaryColor else secondaryColor).copy(alpha = 0.18f + (1f - ringT) * 0.35f + peak * 0.15f),
                radius = r,
                center = Offset(cx, cy),
                style = Stroke(width = 1.2f + (1f - ringT) * 2.4f + bass)
            )

            // Orbiting node on every other ring, positioned by ring phase.
            if (i % 2 == 0) {
                val ang = ringPhase[i] * 1.6f
                drawCircle(
                    color = secondaryColor.copy(alpha = 0.55f + treble * 0.35f),
                    radius = 2.2f + treble * 2.6f,
                    center = Offset(cx + cos(ang) * r, cy + sin(ang) * r)
                )
            }
        }

        // Expanding shockwaves from bass onsets.
        for (s in 0 until SHOCK_SLOTS) {
            val p = shockPhase[s]
            if (p < 0f) continue
            val next = p + 0.022f
            if (next > 1f) {
                shockPhase[s] = -1f
                continue
            }
            shockPhase[s] = next
            val r = baseR * (0.2f + next * 1.5f)
            val alpha = (1f - next) * 0.45f
            drawCircle(
                color = accentColor.copy(alpha = alpha.coerceIn(0f, 1f)),
                radius = r,
                center = Offset(cx, cy),
                style = Stroke(width = (1f - next) * 4f + 0.5f)
            )
        }

        // Rotating tick marks for a little mechanical detail.
        val tickR = baseR * 1.05f
        for (t in 0 until 12) {
            val ang = t * (2f * PI.toFloat() / 12f) + ringPhase[0] * 0.5f
            val inner = tickR * (0.94f + abs(sin(ang.toDouble()).toFloat()) * 0.03f)
            drawLine(
                color = Color.White.copy(alpha = 0.20f + treble * 0.25f),
                start = Offset(cx + cos(ang) * inner, cy + sin(ang) * inner),
                end = Offset(cx + cos(ang) * tickR, cy + sin(ang) * tickR),
                strokeWidth = 1.5f
            )
        }
    }
}