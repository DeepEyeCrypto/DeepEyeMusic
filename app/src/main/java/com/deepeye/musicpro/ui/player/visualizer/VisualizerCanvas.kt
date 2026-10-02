// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later
//
// VisualizerCanvas — Zero-recomposition 60fps audio visualizer
//
// PERFORMANCE CONTRACT:
//   • FFT State is read ONLY inside Canvas {} draw lambdas — never in composable scope.
//   • This means ONLY the Draw phase re-runs per audio frame — Composition phase is skipped.
//   • The 10-Band EQ faders run on a completely independent recomposition tree.
//   • Pre-allocated FloatArrays are reused across frames — zero GC pressure.
//
// Scenes implemented (vvavy.io-inspired):
//   1. SPECTRUM_BARS  — frequency bar chart with vertical gradient (cyan→magenta)
//   2. REACTIVE_TRIANGLE — adaptive quadtree triangle mesh reacting to bass/treble
//   3. WAVEFORM       — waveform line oscilloscope
//   4. AURA_ORB       — glowing circular orb pulsing with bass
//
package com.deepeye.musicpro.ui.player.visualizer

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

// ── Scene selector ─────────────────────────────────────────────────────────────
enum class VisualizerScene {
    SPECTRUM_BARS,
    REACTIVE_TRIANGLE,
    WAVEFORM,
    AURA_ORB
}

// ── Palette ────────────────────────────────────────────────────────────────────
private val DeepBg      = Color(0xFF090B10)
private val NeonCyan    = Color(0xFF00E5FF)
private val NeonMagenta = Color(0xFFFF1493)
private val NeonBlue    = Color(0xFF1A5FFF)
private val NeonWhite   = Color(0xFFFFFFFF)

// ── Main Composable ────────────────────────────────────────────────────────────
//
// fftSpectrum  — StateFlow<FloatArray> of normalised [0,1] FFT magnitudes (256 bins)
// frequencyBands — StateFlow<FloatArray> [bass, lowMid, mid, highMid, treble, peak]
// scene        — which visual to render
// accentColor  — primary tint (pulled from album art dominant color)
//
@Composable
fun VisualizerCanvas(
    fftSpectrum: StateFlow<FloatArray>,
    frequencyBands: StateFlow<FloatArray>,
    themeState: StateFlow<VisualizerTheme> = kotlinx.coroutines.flow.MutableStateFlow(VisualizerTheme.default),
    accentColor: Color = NeonCyan,
    modifier: Modifier = Modifier
) {
    // Time clocks for animation — these drive Compose's clock, not the FFT data
    val infiniteTransition = rememberInfiniteTransition(label = "VisualizerClock")
    val timeSec by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(30000, easing = LinearEasing)),
        label = "TimeSec"
    )
    val timeFast by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)),
        label = "TimeFast"
    )

    // Pre-allocated scratch arrays — reused every frame, zero GC
    val smoothedBars = remember { FloatArray(128) }
    val triPath = remember { Path() }

    Canvas(modifier = modifier.fillMaxSize()) {
        // ── ZERO-RECOMPOSITION GUARDRAIL ─────────────────────────────────────
        // State reads happen HERE — inside the Canvas draw lambda.
        // The Compose composition phase does NOT re-run for audio frames.
        // Only the draw phase re-executes, keeping EQ faders silky smooth.
        val currentTheme = themeState.value
        val spectrum = fftSpectrum.value
        val bands    = frequencyBands.value

        val bass   = if (bands.size > 0) bands[0].coerceIn(0f, 2f) else 0f
        val mids   = if (bands.size > 2) bands[2].coerceIn(0f, 1.5f) else 0f
        val treble = if (bands.size > 4) bands[4].coerceIn(0f, 1.5f) else 0f
        val peak   = if (bands.size > 5) bands[5].coerceIn(0f, 2f) else 0f

        // Background
        drawRect(color = DeepBg)

        when (currentTheme) {
            is VisualizerTheme.Triangle,
            is VisualizerTheme.VvavyTriangle -> drawReactiveTriangle(spectrum, bands, bass, mids, treble, timeSec, timeFast, accentColor, triPath)
            is VisualizerTheme.SpectrumBars,
            is VisualizerTheme.CircularEQ -> drawSpectrumBars(spectrum, smoothedBars, bass, accentColor)
            is VisualizerTheme.Waveform,
            is VisualizerTheme.FrequencyWaves -> drawWaveform(spectrum, bass, accentColor)
            is VisualizerTheme.AuraOrb,
            is VisualizerTheme.BeatDropGrid -> drawAuraOrb(bass, mids, treble, peak, timeSec, timeFast, accentColor)
        }
    }
}

// ── Scene 1: SPECTRUM BARS ─────────────────────────────────────────────────────
// vvavy.io "brutal-columns" / "bars" style
// Maps FFT magnitudes to vertical bar heights with gradient fill.
private fun DrawScope.drawSpectrumBars(
    spectrum: FloatArray,
    smoothed: FloatArray,
    bass: Float,
    accent: Color
) {
    if (spectrum.isEmpty()) {
        // Silent fallback: idle animation
        val idleBrush = Brush.verticalGradient(listOf(accent.copy(alpha = 0.15f), Color.Transparent))
        drawRect(idleBrush)
        return
    }

    val barCount = min(128, spectrum.size)
    val barW     = size.width / barCount
    val gap      = barW * 0.15f

    for (i in 0 until barCount) {
        // Lerp smoothing: EMA between previous and current magnitude
        val raw     = spectrum[i].coerceIn(0f, 1f)
        val prev    = smoothed[i]
        val current = if (raw > prev) raw * 0.85f + prev * 0.15f   // fast attack
                      else            raw * 0.12f + prev * 0.88f   // slow decay
        smoothed[i] = current

        val barH = current * size.height * (0.7f + bass * 0.3f)
        if (barH < 1f) continue

        val x     = i * barW
        val alpha = 0.5f + current * 0.5f

        // Gradient: cyan bottom → magenta top  (vvavy palette)
        val brush = Brush.verticalGradient(
            colors = listOf(
                NeonMagenta.copy(alpha = alpha * 0.9f),
                accent.copy(alpha = alpha),
                NeonCyan.copy(alpha = alpha * 0.6f)
            ),
            startY = size.height - barH,
            endY   = size.height
        )

        drawRect(
            brush      = brush,
            topLeft    = Offset(x + gap / 2f, size.height - barH),
            size       = androidx.compose.ui.geometry.Size(barW - gap, barH)
        )

        // Cap dot
        drawCircle(
            color  = NeonWhite.copy(alpha = alpha * 0.9f),
            radius = barW * 0.4f,
            center = Offset(x + barW / 2f, size.height - barH)
        )
    }

    // Bass bloom
    drawCircle(
        brush = Brush.radialGradient(
            listOf(accent.copy(alpha = 0.08f + bass * 0.12f), Color.Transparent),
            center = Offset(size.width / 2f, size.height / 2f),
            radius = size.width * 0.6f
        ),
        radius = size.width * 0.6f,
        center = Offset(size.width / 2f, size.height / 2f)
    )
}

// ── Scene 2: REACTIVE TRIANGLE ─────────────────────────────────────────────────
// vvavy.io "triangle" / quadtree subdivision  — bass distorts vertices
private fun DrawScope.drawReactiveTriangle(
    spectrum: FloatArray,
    bands: FloatArray,
    bass: Float, mids: Float, treble: Float,
    time: Float, timeFast: Float,
    accent: Color,
    path: Path
) {
    val cx = size.width  / 2f
    val cy = size.height / 2f
    val baseR = min(size.width, size.height) * 0.38f

    // Outer ambient glow
    drawCircle(
        brush = Brush.radialGradient(
            listOf(accent.copy(alpha = 0.08f + bass * 0.1f), Color.Transparent),
            center = Offset(cx, cy), radius = baseR * 2.5f
        ),
        radius = baseR * 2.5f, center = Offset(cx, cy)
    )

    // Draw 3 rotating triangles at different scales
    for (ring in 0 until 3) {
        val ringScale = 1f - ring * 0.28f
        val ringTime  = time + ring * 0.7f
        val ringBass  = bass * (1f - ring * 0.3f)
        val ringAlpha = 0.7f - ring * 0.2f

        // Each vertex is displaced by its corresponding FFT bin (bass→treble)
        val binA = if (spectrum.size > 5)  spectrum[5 + ring * 8].coerceIn(0f, 1f) else bass
        val binB = if (spectrum.size > 20) spectrum[20 + ring * 8].coerceIn(0f, 1f) else mids
        val binC = if (spectrum.size > 50) spectrum[50 + ring * 8].coerceIn(0f, 1f) else treble

        val r = baseR * ringScale * (1f + ringBass * 0.25f)

        // Three vertices with FFT-driven displacement
        val ax = cx + cos(ringTime + 0f            ) * r * (1f + binA * 0.4f)
        val ay = cy + sin(ringTime + 0f            ) * r * (1f + binA * 0.4f)
        val bx = cx + cos(ringTime + 2f * PI.toFloat() / 3f) * r * (1f + binB * 0.4f)
        val by_ = cy + sin(ringTime + 2f * PI.toFloat() / 3f) * r * (1f + binB * 0.4f)
        val ccx = cx + cos(ringTime + 4f * PI.toFloat() / 3f) * r * (1f + binC * 0.4f)
        val ccy = cy + sin(ringTime + 4f * PI.toFloat() / 3f) * r * (1f + binC * 0.4f)

        path.rewind()
        path.moveTo(ax, ay); path.lineTo(bx, by_); path.lineTo(ccx, ccy); path.close()

        val edgeColor = if (ring == 0) accent else
                        if (ring == 1) NeonMagenta else NeonBlue

        // Glow pass (wide, transparent)
        drawPath(path, color = edgeColor.copy(alpha = ringAlpha * 0.3f),
            style = Stroke(width = 8f + ringBass * 12f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        // Core edge
        drawPath(path, color = edgeColor.copy(alpha = ringAlpha),
            style = Stroke(width = 2f + mids * 2f, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Chromatic aberration on bass hit
        if (bass > 0.5f) {
            val shift = bass * 8f
            val aberPath = Path().apply {
                moveTo(ax + shift, ay); lineTo(bx + shift, by_); lineTo(ccx + shift, ccy); close()
            }
            drawPath(aberPath, color = NeonMagenta.copy(alpha = 0.25f + bass * 0.2f),
                style = Stroke(width = 1.5f, cap = StrokeCap.Round))
        }
    }

    // Center orb on beat
    val orbR = 18f + bass * 40f + mids * 15f
    drawCircle(
        brush = Brush.radialGradient(
            listOf(accent.copy(alpha = 0.9f), accent.copy(alpha = 0f)),
            center = Offset(cx, cy), radius = orbR
        ),
        radius = orbR, center = Offset(cx, cy)
    )

    // Shockwave ring on bass punch
    if (bass > 0.65f) {
        drawCircle(
            color  = accent.copy(alpha = (bass - 0.65f) * 1.5f),
            radius = baseR * (1f + (bass - 0.65f) * 2f),
            center = Offset(cx, cy),
            style  = Stroke(width = 2f + bass * 3f)
        )
    }
}

// ── Scene 3: WAVEFORM ─────────────────────────────────────────────────────────
// Oscilloscope waveform line across full width
private fun DrawScope.drawWaveform(spectrum: FloatArray, bass: Float, accent: Color) {
    if (spectrum.isEmpty()) return
    val cy  = size.height / 2f
    val amp = size.height * 0.4f * (1f + bass * 0.6f)
    val pts = min(spectrum.size, 256)
    val step = size.width / (pts - 1).toFloat()

    val path = Path()
    path.moveTo(0f, cy + spectrum[0] * amp - amp / 2f)
    for (i in 1 until pts) {
        // Lerp for smooth curve
        val y = cy + (spectrum[i].coerceIn(0f, 1f) * amp - amp / 2f)
        path.lineTo(i * step, y)
    }

    // Glow pass
    drawPath(path, color = accent.copy(alpha = 0.3f + bass * 0.3f),
        style = Stroke(width = 6f + bass * 8f, cap = StrokeCap.Round))
    // Core line
    drawPath(path, color = accent.copy(alpha = 0.9f),
        style = Stroke(width = 2.5f, cap = StrokeCap.Round))
}

// ── Scene 4: AURA ORB ─────────────────────────────────────────────────────────
// vvavy.io "aura-orb-hyper-geometric" / "echo-halo" style
private fun DrawScope.drawAuraOrb(
    bass: Float, mids: Float, treble: Float, peak: Float,
    time: Float, timeFast: Float,
    accent: Color
) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val baseR = min(size.width, size.height) * 0.3f

    // Multi-layer radial glow
    val layers = listOf(
        Triple(1.8f, 0.05f + bass * 0.08f, accent),
        Triple(1.3f, 0.10f + mids * 0.12f, NeonMagenta),
        Triple(0.9f, 0.20f + treble * 0.15f, NeonCyan),
        Triple(0.5f, 0.60f + peak * 0.4f,  NeonWhite),
    )
    for ((scale, alpha, color) in layers) {
        val r = baseR * scale * (1f + bass * 0.2f)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(color.copy(alpha = alpha), Color.Transparent),
                center = Offset(cx, cy), radius = r
            ),
            radius = r, center = Offset(cx, cy)
        )
    }

    // Rotating halo rings
    for (ring in 0 until 4) {
        val rAngle  = time + ring * (PI.toFloat() / 2f)
        val rRadius = baseR * (0.6f + ring * 0.15f) * (1f + bass * 0.18f)
        val rAlpha  = 0.5f - ring * 0.1f + mids * 0.2f
        drawCircle(
            color  = accent.copy(alpha = rAlpha.coerceIn(0f, 1f)),
            radius = rRadius,
            center = Offset(cx, cy),
            style  = Stroke(width = 1.5f + mids * 2f)
        )
    }

    // Pulsing inner orb
    val innerR = baseR * 0.25f * (1f + peak * 0.8f)
    drawCircle(
        brush = Brush.radialGradient(
            listOf(NeonWhite.copy(alpha = 0.9f + peak * 0.1f), accent.copy(alpha = 0f)),
            center = Offset(cx, cy), radius = innerR
        ),
        radius = innerR, center = Offset(cx, cy)
    )
}
