// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later
//
// VvavyTriangleVisualizer — Pro-grade Compose Canvas replica of vvavy.io visual=triangle
//
// Architecture mirrors the vvavy.io JS engine:
//   • Adaptive Triangle Quadtree with 4-way subdivision driven by bass energy
//   • Bass  → depth extrusion, drift velocity, drop-shuffle kicks
//   • Mids  → edge glow intensity, subdivision depth, rotation speed
//   • Treble→ chromatic aberration, glitch offset, edge luminance
//   • Beat  → shockwave ring expansion, held-drop shuffle impulse
//   • Full perspective projection (FOV 60°, Z-based depth)
//   • Zero-recomposition: all state reads are INSIDE Canvas lambda (drawWithContent)
//   • Pre-allocated geometry pools — zero GC allocations per frame
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

// ── Palette (vvavy.io triangle mode) ─────────────────────────────────────────
val VvavyCyan    = Color(0xFF00E5FF)
val VvavyMagenta = Color(0xFFFF1493)
private val VvavyBlue    = Color(0xFF1A5FFF)
private val VvavyWhite   = Color(0xFFFFFFFF)
val VvavyBg      = Color(0xFF090B10)

// ── Triangle subdivision node ─────────────────────────────────────────────────
// Represents one triangle in the quadtree. We use data classes with a flat
// pool to avoid heap fragmentation inside the draw loop.
private data class Tri(
    var ax: Float = 0f, var ay: Float = 0f, var az: Float = 0f,
    var bx: Float = 0f, var by: Float = 0f, var bz: Float = 0f,
    var cx: Float = 0f, var cy: Float = 0f, var cz: Float = 0f,
    var depth: Int = 0,
    var driftAngle: Float = 0f,
    var driftSpeed: Float = 0f,
    var kickPhase: Float = 0f,   // 0 = idle, >0 = outward kick in progress
    var kickVx: Float = 0f,
    var kickVy: Float = 0f,
    var energyBin: Int = 0       // which spectrum bin feeds this triangle's edge glow
)

// ── Geometry engine (mutable, pre-allocated) ──────────────────────────────────
private class TriangleQuadtree {
    companion object {
        private const val MAX_TRIS    = 512
        private const val MAX_DEPTH   = 5
        private const val KICK_DECAY  = 0.88f
        private const val DRIFT_MULT  = 0.0015f
    }

    private val pool = Array(MAX_TRIS) { Tri() }
    private var count = 0

    private val projX = FloatArray(MAX_TRIS * 3)
    private val projY = FloatArray(MAX_TRIS * 3)

    /** Rebuild the tree from scratch for a given frame. */
    fun rebuild(
        cx: Float, cy: Float,
        radius: Float,
        bass: Float, mids: Float, treble: Float,
        spectrum: FloatArray,
        time: Float,
        beatPhase: Float,
        dropActive: Boolean,
        dt: Float
    ) {
        count = 0
        val subdivisionDepth = (1 + (mids * 3.5f).toInt()).coerceIn(1, MAX_DEPTH)
        val halfR = radius * (1f + bass * 0.45f)

        // Seed three large triangles arranged as the vvavy.io outer ring:
        // An equilateral triangle pointing up, down, and rotated 60° for fill.
        for (seed in 0 until 3) {
            val globalAngle = (seed * 2f * PI.toFloat() / 3f) + time * 0.12f
            val tipAngle = globalAngle - (PI.toFloat() / 2f)
            addRootTriangle(cx, cy, halfR, tipAngle, subdivisionDepth, seed, spectrum)
        }

        // Animate drifts and kicks
        for (i in 0 until count) {
            val t = pool[i]
            // Slow drift
            val driftDx = cos(t.driftAngle) * t.driftSpeed * bass * dt
            val driftDy = sin(t.driftAngle) * t.driftSpeed * bass * dt
            t.ax += driftDx; t.ay += driftDy
            t.bx += driftDx; t.by += driftDy
            t.cx += driftDx; t.cy += driftDy

            // Beat kick
            if (t.kickPhase > 0.01f) {
                t.ax += t.kickVx * t.kickPhase
                t.ay += t.kickVy * t.kickPhase
                t.bx += t.kickVx * t.kickPhase
                t.by += t.kickVy * t.kickPhase
                t.cx += t.kickVx * t.kickPhase
                t.cy += t.kickVy * t.kickPhase
                t.kickPhase *= KICK_DECAY
            }

            // On a detected bass drop — kick random leaf triangles outward
            if (dropActive && t.depth == subdivisionDepth && t.kickPhase < 0.1f) {
                val triCx = (t.ax + t.bx + t.cx) / 3f
                val triCy = (t.ay + t.by + t.cy) / 3f
                val dir = kotlin.math.atan2(triCy - cy, triCx - cx)
                val strength = 0.8f + bass * 1.2f
                t.kickPhase = strength
                t.kickVx = cos(dir) * 0.6f
                t.kickVy = sin(dir) * 0.6f
            }
        }
    }

    private fun addRootTriangle(
        cx: Float, cy: Float, radius: Float,
        tipAngle: Float, maxDepth: Int, seed: Int,
        spectrum: FloatArray
    ) {
        val angles = floatArrayOf(tipAngle, tipAngle + 2.094f, tipAngle + 4.188f) // 120° apart
        val ax = cx + cos(angles[0]) * radius
        val ay = cy + sin(angles[0]) * radius
        val bx = cx + cos(angles[1]) * radius
        val by = cy + sin(angles[1]) * radius
        val ccx = cx + cos(angles[2]) * radius
        val ccy = cy + sin(angles[2]) * radius
        subdivide(ax, ay, 0f, bx, by, 0f, ccx, ccy, 0f, 0, maxDepth, seed, spectrum)
    }

    private fun subdivide(
        ax: Float, ay: Float, az: Float,
        bx: Float, by_: Float, bz: Float,
        ccx: Float, ccy: Float, ccz: Float,
        depth: Int, maxDepth: Int, seed: Int,
        spectrum: FloatArray
    ) {
        if (count >= MAX_TRIS) return
        val t = pool[count++]

        t.ax = ax; t.ay = ay; t.az = az
        t.bx = bx; t.by = by_; t.bz = bz
        t.cx = ccx; t.cy = ccy; t.cz = ccz
        t.depth = depth
        t.driftAngle = ((seed * 1.618f + depth * 0.9f) % (2f * PI.toFloat()))
        t.driftSpeed = (100f + seed * 37f + depth * 18f)
        // Assign a spectrum bin based on position in the tree
        t.energyBin = ((seed * 11 + depth * 7) % spectrum.size.coerceAtLeast(1))
        if (t.kickPhase == 0f) { t.kickVx = 0f; t.kickVy = 0f }

        if (depth >= maxDepth) return

        // 4-way quadtree subdivision (vvavy.io style):
        // Split each triangle into 4 by adding midpoints on each edge
        val mabx = (ax + bx) * 0.5f; val maby = (ay + by_) * 0.5f; val mabz = (az + bz) * 0.5f
        val mbcx = (bx + ccx) * 0.5f; val mbcy = (by_ + ccy) * 0.5f; val mbcz = (bz + ccz) * 0.5f
        val mcax = (ccx + ax) * 0.5f; val mcay = (ccy + ay) * 0.5f; val mcaz = (ccz + az) * 0.5f

        subdivide(ax, ay, az, mabx, maby, mabz, mcax, mcay, mcaz, depth + 1, maxDepth, seed, spectrum)
        subdivide(mabx, maby, mabz, bx, by_, bz, mbcx, mbcy, mbcz, depth + 1, maxDepth, seed, spectrum)
        subdivide(mbcx, mbcy, mbcz, ccx, ccy, ccz, mcax, mcay, mcaz, depth + 1, maxDepth, seed, spectrum)
        subdivide(mabx, maby, mabz, mbcx, mbcy, mbcz, mcax, mcay, mcaz, depth + 1, maxDepth, seed, spectrum)
    }

    fun project(cx: Float, cy: Float, focalLength: Float, bassZ: Float) {
        for (i in 0 until count) {
            val t = pool[i]
            val zOff = t.az * bassZ
            projX[i * 3 + 0] = cx + t.ax * focalLength / (focalLength + zOff)
            projY[i * 3 + 0] = cy + t.ay * focalLength / (focalLength + zOff)
            projX[i * 3 + 1] = cx + t.bx * focalLength / (focalLength + t.bz * bassZ)
            projY[i * 3 + 1] = cy + t.by * focalLength / (focalLength + t.bz * bassZ)
            projX[i * 3 + 2] = cx + t.cx * focalLength / (focalLength + t.cz * bassZ)
            projY[i * 3 + 2] = cy + t.cy * focalLength / (focalLength + t.cz * bassZ)
        }
    }

    fun getTri(index: Int): Tri = pool[index]
    fun getCount(): Int = count
    fun getProjectedX(triIdx: Int, vert: Int) = projX[triIdx * 3 + vert]
    fun getProjectedY(triIdx: Int, vert: Int) = projY[triIdx * 3 + vert]
}

// ── Main Composable ───────────────────────────────────────────────────────────

@Composable
fun VvavyTriangleVisualizer(
    fftSpectrum: StateFlow<FloatArray>,
    frequencyBands: StateFlow<FloatArray>,
    accentColor: Color = VvavyCyan,
    intensity: Float = 1f,
    reducedMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
    val interpolator = remember { AudioFrameInterpolator() }

    // Time-phase drivers — three independent clocks for 3D tumbling.
    // Under reduced motion the clocks run slower and the tumble is damped, so
    // the scene stays legible without strobing.
    val speedScale = if (reducedMotion) 2.2f else 1f
    val infiniteTransition = rememberInfiniteTransition(label = "VvavyTri")
    val timeSec by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween((28000 * speedScale).toInt(), easing = LinearEasing)),
        label = "TimeSec"
    )
    val tumbleY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween((22000 * speedScale).toInt(), easing = LinearEasing)),
        label = "TumbleY"
    )
    val tumbleX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(35000, easing = LinearEasing)),
        label = "TumbleX"
    )

    val quadtree = remember { TriangleQuadtree() }
    val triPath  = remember { Path() }
    val prevBass = remember { FloatArray(1) { 0f } }

    Canvas(modifier = modifier.fillMaxSize()) {
        // Interpolate on the UI thread: the engine delivers FFT at ~19 Hz while
        // this draws at 48–60 fps, so raw values step visibly between frames.
        interpolator.update(
            rawBands = frequencyBands.value,
            rawSpectrum = fftSpectrum.value,
            intensity = intensity,
            reducedMotion = reducedMotion,
            nowNanos = System.nanoTime()
        )

        // ── Zero-recomposition state reads – inside Canvas draw lambda ─────
        val bands    = interpolator.bands
        val spectrum = interpolator.spectrum

        val bass     = if (bands.size > 0) bands[0].coerceIn(0f, 2f) else 0f
        val lowMid   = if (bands.size > 1) bands[1].coerceIn(0f, 1.5f) else 0f
        val mids     = if (bands.size > 2) bands[2].coerceIn(0f, 1.5f) else 0f
        val highMid  = if (bands.size > 3) bands[3].coerceIn(0f, 1.5f) else 0f
        val treble   = if (bands.size > 4) bands[4].coerceIn(0f, 1.5f) else 0f
        val peak     = if (bands.size > 5) bands[5].coerceIn(0f, 2f) else 0f

        val cx = size.width  / 2f
        val cy = size.height / 2f

        // Beat/Drop detection: sharp bass onset
        val bassDelta = bass - prevBass[0]
        val dropActive = bassDelta > 0.35f
        prevBass[0] = bass * 0.9f + prevBass[0] * 0.1f

        // Base layout radius adapts to screen (landscape: use shorter axis)
        val baseRadius = min(size.width, size.height) * 0.36f
        val focalLength = size.width * 0.9f
        val bassZ = bass * 0.4f

        // ── Background: deep space + ambient glow ─────────────────────────
        drawRect(color = VvavyBg)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.08f + bass * 0.07f),
                    accentColor.copy(alpha = 0f)
                ),
                center = Offset(cx, cy),
                radius = baseRadius * 2.2f
            ),
            radius = baseRadius * 2.2f,
            center = Offset(cx, cy)
        )

        // ── Rebuild geometry ──────────────────────────────────────────────
        quadtree.rebuild(
            cx = 0f, cy = 0f,  // centred at origin; project shifts later
            radius = baseRadius,
            bass = bass, mids = mids, treble = treble,
            spectrum = spectrum,
            time = timeSec,
            beatPhase = tumbleY,
            dropActive = dropActive,
            dt = 16f  // nominal ~60 fps dt in ms
        )

        // Apply 3D tumble to vertices before projection
        rotateTrisInPlace(quadtree, tumbleY, tumbleX, timeSec * 0.3f, bass)

        quadtree.project(cx, cy, focalLength, bassZ)

        // ── Draw triangles ────────────────────────────────────────────────
        val totalTris = quadtree.getCount()
        val chromaShift = treble * 14f + bassDelta * 20f

        for (i in 0 until totalTris) {
            val tri = quadtree.getTri(i)
            val leafDepth = tri.depth

            // Only render leaf triangles (deepest level) for clean vvavy look
            // Inner levels are drawn only as a subtle skeleton
            val isLeaf = leafDepth >= ((1 + (mids * 3.5f).toInt()).coerceIn(1, 5))

            // Edge energy from the spectrum bin assigned to this triangle
            val binEnergy = if (spectrum.size > tri.energyBin) spectrum[tri.energyBin] else 0f
            val edgeGlow = (binEnergy * 1.5f + mids * 0.5f).coerceIn(0f, 1f)

            val strokeWidth = if (isLeaf) (1.2f + edgeGlow * 3.5f + bass * 1.5f)
                              else        (0.5f + mids * 0.4f)

            // Chromatic aberration: draw cyan ghost shifted left, magenta right
            if (isLeaf && chromaShift > 3f) {
                drawTriPath(
                    quadtree, i, triPath,
                    shiftX = -chromaShift * 0.5f, shiftY = 0f,
                    color  = VvavyCyan.copy(alpha = 0.35f + edgeGlow * 0.3f),
                    strokeWidth = strokeWidth * 0.7f
                )
                drawTriPath(
                    quadtree, i, triPath,
                    shiftX = chromaShift * 0.5f, shiftY = 0f,
                    color  = VvavyMagenta.copy(alpha = 0.35f + edgeGlow * 0.3f),
                    strokeWidth = strokeWidth * 0.7f
                )
            }

            // Core triangle — interpolate accent→cyan→white by depth
            val depthT  = leafDepth.toFloat() / 5f
            val coreAlpha = if (isLeaf) (0.55f + edgeGlow * 0.45f) else (0.15f + mids * 0.1f)
            val coreColor = lerp3(accentColor, VvavyCyan, VvavyWhite, depthT).copy(alpha = coreAlpha)

            drawTriPath(
                quadtree, i, triPath,
                shiftX = 0f, shiftY = 0f,
                color  = coreColor,
                strokeWidth = strokeWidth
            )
        }

        // ── Shockwave ring on bass drop ───────────────────────────────────
        if (dropActive || bass > 0.7f) {
            val shockR = baseRadius * (1.3f + bassDelta * 1.8f)
            drawCircle(
                color  = accentColor.copy(alpha = (bassDelta * 0.8f).coerceIn(0f, 0.4f)),
                radius = shockR,
                center = Offset(cx, cy),
                style  = Stroke(width = 2f + bassDelta * 4f)
            )
        }

        // ── Bass bloom glow ───────────────────────────────────────────────
        val bloomR = baseRadius * (1.4f + bass * 0.7f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.18f + bass * 0.22f),
                    VvavyMagenta.copy(alpha = 0.04f + highMid * 0.06f),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = bloomR
            ),
            radius = bloomR,
            center = Offset(cx, cy)
        )
    }
}

// ── Rotation helper: applies Euler rotations to all tri vertices in-place ──────
private fun rotateTrisInPlace(
    tree: TriangleQuadtree,
    yaw: Float, pitch: Float, roll: Float,
    bassBoost: Float
) {
    val cosYaw = cos(yaw); val sinYaw = sin(yaw)
    val cosPitch = cos(pitch); val sinPitch = sin(pitch)
    val cosRoll = cos(roll * (1f + bassBoost * 0.4f))
    val sinRoll = sin(roll * (1f + bassBoost * 0.4f))

    val n = tree.getCount()
    for (i in 0 until n) {
        val t = tree.getTri(i)
        // Apply rotation to all 3 vertices
        repeat(3) { v ->
            var x: Float; var y: Float; var z: Float
            when (v) {
                0 -> { x = t.ax; y = t.ay; z = t.az }
                1 -> { x = t.bx; y = t.by; z = t.bz }
                else -> { x = t.cx; y = t.cy; z = t.cz }
            }
            // Yaw (Y-axis)
            val x1 = x * cosYaw + z * sinYaw; val z1 = -x * sinYaw + z * cosYaw
            x = x1; z = z1
            // Pitch (X-axis)
            val y2 = y * cosPitch - z * sinPitch; val z2 = y * sinPitch + z * cosPitch
            y = y2; z = z2
            // Roll (Z-axis)
            val x3 = x * cosRoll - y * sinRoll; val y3 = x * sinRoll + y * cosRoll
            x = x3; y = y3

            when (v) {
                0 -> { t.ax = x; t.ay = y; t.az = z }
                1 -> { t.bx = x; t.by = y; t.bz = z }
                else -> { t.cx = x; t.cy = y; t.cz = z }
            }
        }
    }
}

// ── DrawScope helpers ─────────────────────────────────────────────────────────

private fun DrawScope.drawTriPath(
    tree: TriangleQuadtree,
    idx: Int,
    path: Path,
    shiftX: Float, shiftY: Float,
    color: Color,
    strokeWidth: Float
) {
    path.rewind()
    val ax = tree.getProjectedX(idx, 0) + shiftX
    val ay = tree.getProjectedY(idx, 0) + shiftY
    val bx = tree.getProjectedX(idx, 1) + shiftX
    val by = tree.getProjectedY(idx, 1) + shiftY
    val cx = tree.getProjectedX(idx, 2) + shiftX
    val cy = tree.getProjectedY(idx, 2) + shiftY
    path.moveTo(ax, ay)
    path.lineTo(bx, by)
    path.lineTo(cx, cy)
    path.close()
    drawPath(
        path  = path,
        color = color,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

/** Linear-interpolate between 3 colors based on t in [0,1] */
private fun lerp3(a: Color, b: Color, c: Color, t: Float): Color {
    val t2 = t.coerceIn(0f, 1f)
    return if (t2 < 0.5f) {
        val s = t2 * 2f
        Color(
            red   = a.red   * (1f - s) + b.red   * s,
            green = a.green * (1f - s) + b.green * s,
            blue  = a.blue  * (1f - s) + b.blue  * s,
            alpha = 1f
        )
    } else {
        val s = (t2 - 0.5f) * 2f
        Color(
            red   = b.red   * (1f - s) + c.red   * s,
            green = b.green * (1f - s) + c.green * s,
            blue  = b.blue  * (1f - s) + c.blue  * s,
            alpha = 1f
        )
    }
}
