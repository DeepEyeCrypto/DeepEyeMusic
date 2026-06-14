// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.player

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

/**
 * Animated bass ring glow that pulses around album art.
 *
 * Extracts low-frequency amplitude (first ~4 FFT bins representing 20-100Hz)
 * and scales the ring + opacity accordingly. Uses spring animation for smooth
 * transitions between beat pulses.
 *
 * Also includes a subtle rotation animation for the gradient sweep.
 */
@Composable
fun BassRingGlow(
    fftData: FloatArray,
    dominantColor: Color,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    // Extract bass amplitude from low-frequency bins
    val rawBassAmplitude = remember(fftData) {
        if (fftData.size >= 4) {
            // Average first 4 bins (sub-bass to low-mid)
            ((fftData[0] + fftData[1] + fftData[2] + fftData[3]) / 4f).coerceIn(0f, 1f)
        } else if (fftData.isNotEmpty()) {
            fftData[0].coerceIn(0f, 1f)
        } else {
            0f
        }
    }

    // Spring-animated scale for natural beat pulse (1.0 -> 1.05 max)
    val pulseScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f + rawBassAmplitude * 0.05f else 1f,
        animationSpec = spring(
            dampingRatio = 0.4f,
            stiffness = 400f,
        ),
        label = "bassScale",
    )

    // Spring-animated alpha for ring glow
    val ringAlpha by animateFloatAsState(
        targetValue = if (isPlaying) (0.15f + rawBassAmplitude * 0.6f) else 0f,
        animationSpec = spring(
            dampingRatio = 0.5f,
            stiffness = 350f,
        ),
        label = "ringAlpha",
    )

    // Outer expanding ring alpha (fades out as it expands)
    val outerRingAlpha by animateFloatAsState(
        targetValue = if (isPlaying && rawBassAmplitude > 0.3f) rawBassAmplitude * 0.35f else 0f,
        animationSpec = tween(200),
        label = "outerAlpha",
    )

    // Slow rotation for gradient sweep visual interest
    val infiniteTransition = rememberInfiniteTransition(label = "ringRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rotAngle",
    )

    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val baseRadius = (size.minDimension / 2f) * pulseScale

        // Inner soft radial aura instead of a hard stroke ring
        if (ringAlpha > 0.01f) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        dominantColor.copy(alpha = ringAlpha * 0.5f),
                        dominantColor.copy(alpha = ringAlpha * 0.2f),
                        dominantColor.copy(alpha = 0f)
                    ),
                    center = androidx.compose.ui.geometry.Offset(cx, cy),
                    radius = baseRadius + 16.dp.toPx()
                ),
                radius = baseRadius + 16.dp.toPx()
            )
        }

        // Outer expanding soft aura
        if (outerRingAlpha > 0.01f) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        dominantColor.copy(alpha = outerRingAlpha * 0.4f),
                        dominantColor.copy(alpha = 0f)
                    ),
                    center = androidx.compose.ui.geometry.Offset(cx, cy),
                    radius = baseRadius + 32.dp.toPx()
                ),
                radius = baseRadius + 32.dp.toPx()
            )
        }
    }
}
