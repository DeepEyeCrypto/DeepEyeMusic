// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later
//
// VvavyTriangleVisualizer — Pro-grade Compose Canvas replica of vvavy.io visual=triangle
// Generative Multi-Topology Geometry Engine with Dynamic Audio-Reactive Pattern Morphing.
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.deepeye.musicpro.ui.player.visualizer.vvavy.VvavyGeometryEngine
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// ── Palette (vvavy.io triangle mode) ─────────────────────────────────────────
val VvavyCyan    = Color(0xFF00E5FF)
val VvavyMagenta = Color(0xFFFF1493)
private val VvavyWhite = Color(0xFFFFFFFF)
val VvavyBg      = Color(0xFF090B10)

// ── Particle Spark Pool for Beat Burst ────────────────────────────────────────
private data class SparkParticle(
    var x: Float = 0f,
    var y: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var life: Float = 0f,
    var maxLife: Float = 1f,
    var size: Float = 2f,
    var color: Color = VvavyCyan
)

private class SparkParticlePool(val capacity: Int = 64) {
    val particles = Array(capacity) { SparkParticle() }
    private var head = 0

    fun emit(x: Float, y: Float, speed: Float, angle: Float, color: Color, size: Float = 3f) {
        val p = particles[head % capacity]
        head++
        p.x = x
        p.y = y
        p.vx = cos(angle) * speed
        p.vy = sin(angle) * speed
        p.life = 1f
        p.maxLife = 1f
        p.size = size
        p.color = color
    }

    fun updateAndDraw(drawScope: DrawScope, dt: Float) {
        for (p in particles) {
            if (p.life > 0f) {
                p.x += p.vx * dt
                p.y += p.vy * dt
                p.vx *= 0.94f
                p.vy *= 0.94f
                p.life -= dt * 2.2f

                val alpha = p.life.coerceIn(0f, 1f)
                drawScope.drawCircle(
                    color = p.color.copy(alpha = alpha),
                    radius = p.size * alpha,
                    center = Offset(p.x, p.y)
                )
            }
        }
    }
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
    val geometryEngine = remember { VvavyGeometryEngine(512) }
    val sparkPool = remember { SparkParticlePool(64) }
    val triPath = remember { Path() }
    val prevBass = remember { FloatArray(1) { 0f } }
    val lastMutationTime = remember { FloatArray(1) { 0f } }

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
        animationSpec = infiniteRepeatable(tween((34000 * speedScale).toInt(), easing = LinearEasing)),
        label = "TumbleX"
    )

    val frameTick = remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameNanos { frameNanos ->
                frameTick.longValue = frameNanos
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val nowNanos = frameTick.longValue
        interpolator.update(
            rawBands = frequencyBands.value,
            rawSpectrum = fftSpectrum.value,
            intensity = intensity,
            reducedMotion = reducedMotion,
            nowNanos = if (nowNanos > 0L) nowNanos else System.nanoTime()
        )

        val bands = interpolator.bands
        val spectrum = interpolator.spectrum

        val bass = if (bands.isNotEmpty()) bands[0].coerceIn(0f, 2f) else 0f
        val mids = if (bands.size > 2) bands[2].coerceIn(0f, 1.5f) else 0f
        val highMid = if (bands.size > 3) bands[3].coerceIn(0f, 1.5f) else 0f
        val treble = if (bands.size > 4) bands[4].coerceIn(0f, 1.5f) else 0f

        val cx = size.width / 2f
        val cy = size.height / 2f

        // Beat/Drop detection
        val bassDelta = bass - prevBass[0]
        val dropActive = bassDelta > 0.38f
        prevBass[0] = bass * 0.9f + prevBass[0] * 0.1f

        // Auto-morph topology on massive beat drops or every 8 seconds
        if ((dropActive && (timeSec - lastMutationTime[0]) > 2.5f) || (timeSec - lastMutationTime[0]) > 8f) {
            geometryEngine.mutateTopology()
            lastMutationTime[0] = timeSec
        }

        val baseRadius = min(size.width, size.height) * 0.36f
        val focalLength = size.width * 0.9f
        val bassZ = bass * 0.4f

        // ── 1. Background Ambient Atmosphere ────────────────────────────────
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color.Black.copy(alpha = 0.35f), Color.Black.copy(alpha = 0.55f))
            )
        )
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

        // ── 2. Build Generative Geometry & 3D Tumble ────────────────────────
        geometryEngine.build(
            cx = 0f, cy = 0f,
            radius = baseRadius,
            bass = bass, mids = mids, treble = treble,
            spectrum = spectrum,
            time = timeSec,
            dropActive = dropActive,
            dt = 0.016f
        )

        rotateGeometryInPlace(geometryEngine, tumbleY, tumbleX, timeSec * 0.3f, bass)
        geometryEngine.project(cx, cy, focalLength, bassZ)

        // ── 3. Draw Triangles with Chromatic Aberration ─────────────────────
        val totalTris = geometryEngine.count
        val chromaShift = treble * 14f + bassDelta * 20f

        for (i in 0 until totalTris) {
            val tri = geometryEngine.pool[i]
            val leafDepth = tri.depth

            val binEnergy = if (spectrum.isNotEmpty()) spectrum[tri.energyBin % spectrum.size] else 0f
            val edgeGlow = (binEnergy * 1.5f + mids * 0.5f).coerceIn(0f, 1f)
            val strokeWidth = (1.2f + edgeGlow * 3.5f + bass * 1.5f)

            // Spawn sparks on bass drop from leaf vertices
            if (dropActive && i % 4 == 0) {
                val px = geometryEngine.projX[i * 3 + 0]
                val py = geometryEngine.projY[i * 3 + 0]
                sparkPool.emit(
                    x = px, y = py,
                    speed = 250f + bass * 300f,
                    angle = (i * 1.618f) % (2f * PI.toFloat()),
                    color = if (i % 2 == 0) VvavyCyan else VvavyMagenta,
                    size = 3.5f
                )
            }

            // Chromatic Aberration (Cyan / Magenta split)
            if (chromaShift > 3f) {
                drawProjectedTriPath(
                    geometryEngine, i, triPath,
                    shiftX = -chromaShift * 0.5f, shiftY = 0f,
                    color = VvavyCyan.copy(alpha = 0.35f + edgeGlow * 0.3f),
                    strokeWidth = strokeWidth * 0.7f
                )
                drawProjectedTriPath(
                    geometryEngine, i, triPath,
                    shiftX = chromaShift * 0.5f, shiftY = 0f,
                    color = VvavyMagenta.copy(alpha = 0.35f + edgeGlow * 0.3f),
                    strokeWidth = strokeWidth * 0.7f
                )
            }

            // Core Triangle (Interpolated Color by Depth)
            val depthT = leafDepth.toFloat() / 5f
            val coreColor = lerp3(accentColor, VvavyCyan, VvavyWhite, depthT).copy(alpha = 0.55f + edgeGlow * 0.45f)

            drawProjectedTriPath(
                geometryEngine, i, triPath,
                shiftX = 0f, shiftY = 0f,
                color = coreColor,
                strokeWidth = strokeWidth
            )
        }

        // ── 4. Render Spark Particle Bursts ─────────────────────────────────
        sparkPool.updateAndDraw(this, 0.016f)

        // ── 5. Shockwave Ring & Bass Bloom Glow ─────────────────────────────
        if (dropActive || bass > 0.7f) {
            val shockR = baseRadius * (1.3f + bassDelta * 1.8f)
            drawCircle(
                color = accentColor.copy(alpha = (bassDelta * 0.8f).coerceIn(0f, 0.4f)),
                radius = shockR,
                center = Offset(cx, cy),
                style = Stroke(width = 2f + bassDelta * 4f)
            )
        }

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
private fun rotateGeometryInPlace(
    engine: VvavyGeometryEngine,
    yaw: Float, pitch: Float, roll: Float,
    bassBoost: Float
) {
    val cosYaw = cos(yaw); val sinYaw = sin(yaw)
    val cosPitch = cos(pitch); val sinPitch = sin(pitch)
    val cosRoll = cos(roll * (1f + bassBoost * 0.4f))
    val sinRoll = sin(roll * (1f + bassBoost * 0.4f))

    val n = engine.count
    for (i in 0 until n) {
        val t = engine.pool[i]
        rotateVertex(t.a, cosYaw, sinYaw, cosPitch, sinPitch, cosRoll, sinRoll)
        rotateVertex(t.b, cosYaw, sinYaw, cosPitch, sinPitch, cosRoll, sinRoll)
        rotateVertex(t.c, cosYaw, sinYaw, cosPitch, sinPitch, cosRoll, sinRoll)
    }
}

private fun rotateVertex(
    v: com.deepeye.musicpro.ui.player.visualizer.vvavy.VvavyVertex,
    cosYaw: Float, sinYaw: Float,
    cosPitch: Float, sinPitch: Float,
    cosRoll: Float, sinRoll: Float
) {
    var x = v.x; var y = v.y; var z = v.z

    // Yaw (Y-axis)
    val x1 = x * cosYaw + z * sinYaw
    val z1 = -x * sinYaw + z * cosYaw
    x = x1; z = z1

    // Pitch (X-axis)
    val y2 = y * cosPitch - z * sinPitch
    val z2 = y * sinPitch + z * cosPitch
    y = y2; z = z2

    // Roll (Z-axis)
    val x3 = x * cosRoll - y * sinRoll
    val y3 = x * sinRoll + y * cosRoll
    x = x3; y = y3

    v.x = x
    v.y = y
    v.z = z
}

private fun DrawScope.drawProjectedTriPath(
    engine: VvavyGeometryEngine,
    idx: Int,
    path: Path,
    shiftX: Float, shiftY: Float,
    color: Color,
    strokeWidth: Float
) {
    path.rewind()
    val ax = engine.projX[idx * 3 + 0] + shiftX
    val ay = engine.projY[idx * 3 + 0] + shiftY
    val bx = engine.projX[idx * 3 + 1] + shiftX
    val by = engine.projY[idx * 3 + 1] + shiftY
    val cx = engine.projX[idx * 3 + 2] + shiftX
    val cy = engine.projY[idx * 3 + 2] + shiftY

    path.moveTo(ax, ay)
    path.lineTo(bx, by)
    path.lineTo(cx, cy)
    path.close()
    drawPath(
        path = path,
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
