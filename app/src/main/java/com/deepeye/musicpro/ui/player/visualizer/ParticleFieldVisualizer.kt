// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later
//
// ParticleFieldVisualizer — orbiting particle swarm reacting to spectral flux.
//
// Fixed-size particle pool with SoA layout (separate x/y/vx/vy/energy arrays) so
// the update loop stays cache-friendly and allocation-free. Particles that drift
// out of bounds are recycled toward the centre rather than respawned, which
// avoids both allocation and the visible "pop" of a respawn.

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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private const val PARTICLE_COUNT = 140
private const val PARTICLE_TRAIL = 3
private const val MAX_RADIUS_SQ = 1.35f

private class ParticlePool {
    val px = FloatArray(PARTICLE_COUNT)
    val py = FloatArray(PARTICLE_COUNT)
    val vx = FloatArray(PARTICLE_COUNT)
    val vy = FloatArray(PARTICLE_COUNT)
    val energy = FloatArray(PARTICLE_COUNT)
    val bin = IntArray(PARTICLE_COUNT)
    val trailX = Array(PARTICLE_COUNT) { FloatArray(PARTICLE_TRAIL) }
    val trailY = Array(PARTICLE_COUNT) { FloatArray(PARTICLE_TRAIL) }

    /** Seeds positions on a ring so the first frame already reads as a swarm. */
    fun reset() {
        val rnd = Random(0x5EED)
        for (i in 0 until PARTICLE_COUNT) {
            val ang = rnd.nextFloat() * 2f * PI.toFloat()
            val rad = 0.12f + rnd.nextFloat() * 0.80f
            px[i] = cos(ang) * rad
            py[i] = sin(ang) * rad
            vx[i] = 0f
            vy[i] = 0f
            energy[i] = 0f
            bin[i] = rnd.nextInt(0, 32)
            for (t in 0 until PARTICLE_TRAIL) {
                trailX[i][t] = px[i]
                trailY[i][t] = py[i]
            }
        }
    }
}

@Composable
fun ParticleFieldVisualizer(
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
    val pool = remember { ParticlePool().apply { reset() } }
    val spin = remember { floatArrayOf(0f) }
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
            nowNanos = (time * 1_000_000_000f).toLong()
        )

        val spectrum = interpolator.spectrum
        val bass = interpolator.bands.getOrElse(0) { 0f }.coerceIn(0f, 2f)
        val mid = interpolator.bands.getOrElse(2) { 0f }.coerceIn(0f, 1.5f)
        val treble = interpolator.bands.getOrElse(4) { 0f }.coerceIn(0f, 1.5f)
        val peak = interpolator.bands.getOrElse(5) { 0f }.coerceIn(0f, 2f)

        val cx = size.width / 2f
        val cy = size.height / 2f
        val unit = minOf(size.width, size.height) * 0.5f

        // Reduced motion freezes orbital drift but keeps the swarm energy-reactive,
        // so the scene stays informative without sweeping the whole field.
        if (!reducedMotion) spin[0] += 0.0022f + mid * 0.002f
        val cosSpin = cos(spin[0])
        val sinSpin = sin(spin[0])

        drawRect(color = VvavyBg)

        // Bass-driven central bloom.
        val bloomR = unit * (0.7f + bass * 0.35f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.14f + bass * 0.16f),
                    secondaryColor.copy(alpha = 0.06f),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = bloomR
            ),
            radius = bloomR,
            center = Offset(cx, cy)
        )

        val push = 0.0016f + bass * 0.0022f + peak * 0.0012f
        val damping = if (reducedMotion) 0.86f else 0.93f

        for (i in 0 until PARTICLE_COUNT) {
            val b = pool.bin[i]
            val e = (if (b < spectrum.size) spectrum[b] else 0f).coerceIn(0f, 1f)
            pool.energy[i] += (e - pool.energy[i]) * 0.25f

            // Rotate into a spinning frame, then push outward along the radius.
            val rx = pool.px[i] * cosSpin - pool.py[i] * sinSpin
            val ry = pool.px[i] * sinSpin + pool.py[i] * cosSpin
            val mag = sqrt(rx * rx + ry * ry)
            val dirX = if (mag > 0.0001f) rx / mag else 0f
            val dirY = if (mag > 0.0001f) ry / mag else 1f

            pool.vx[i] = pool.vx[i] * damping + dirX * push * (0.35f + pool.energy[i])
            pool.vy[i] = pool.vy[i] * damping + dirY * push * (0.35f + pool.energy[i])

            var nx = pool.px[i] + pool.vx[i]
            var ny = pool.py[i] + pool.vy[i]

            // Recycle at the boundary instead of respawning: no allocation, no pop.
            if (nx * nx + ny * ny > MAX_RADIUS_SQ) {
                nx = dirX * 0.06f
                ny = dirY * 0.06f
                pool.vx[i] = 0f
                pool.vy[i] = 0f
            }
            pool.px[i] = nx
            pool.py[i] = ny

            // Shift trail history (oldest -> newest within each slot).
            val tx = pool.trailX[i]
            val ty = pool.trailY[i]
            var prevX = nx
            var prevY = ny
            for (t in 0 until PARTICLE_TRAIL - 1) {
                val oldX = tx[t]
                val oldY = ty[t]
                tx[t] = prevX
                ty[t] = prevY
                prevX = oldX
                prevY = oldY
            }
            tx[PARTICLE_TRAIL - 1] = prevX
            ty[PARTICLE_TRAIL - 1] = prevY

            val alpha = (0.20f + pool.energy[i] * 0.75f).coerceIn(0f, 1f)
            val col = if (pool.energy[i] > 0.55f) Color.White else accentColor

            for (t in 1 until PARTICLE_TRAIL) {
                drawLine(
                    color = col.copy(alpha = alpha * 0.18f * t),
                    start = Offset(cx + tx[t - 1] * unit, cy + ty[t - 1] * unit),
                    end = Offset(cx + tx[t] * unit, cy + ty[t] * unit),
                    strokeWidth = 1f
                )
            }

            drawCircle(
                color = col.copy(alpha = alpha),
                radius = 1.1f + pool.energy[i] * 3.4f + treble * 0.8f,
                center = Offset(cx + nx * unit, cy + ny * unit)
            )
        }
    }
}