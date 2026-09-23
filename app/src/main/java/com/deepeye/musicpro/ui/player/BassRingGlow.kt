// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.player

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.*

private val NeonCyan = Color(0xFF00E5FF)
private val DeepElectricBlue = Color(0xFF0051FF)
private val CyberPurple = Color(0xFF9D00FF)
private val SpecularWhite = Color(0xFFFFFFFF)

/**
 * 3D Reactive Triangle Audio Visualizer (vvavy.io geometric style)
 * pulsing with live FFT data around the album art.
 */
@Composable
fun BassRingGlow(
    fftData: FloatArray,
    dominantColor: Color,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    val rawBass = remember(fftData) {
        if (fftData.size >= 4) {
            ((fftData[0] + fftData[1] + fftData[2] + fftData[3]) / 4f).coerceIn(0f, 1.5f)
        } else if (fftData.isNotEmpty()) {
            fftData[0].coerceIn(0f, 1.5f)
        } else {
            0f
        }
    }

    val rawMid = remember(fftData) {
        if (fftData.size >= 12) {
            var sum = 0f
            for (i in 4..11) sum += fftData[i]
            (sum / 8f).coerceIn(0f, 1.5f)
        } else 0f
    }

    val rawTreble = remember(fftData) {
        if (fftData.size >= 24) {
            var sum = 0f
            for (i in 12..23) sum += fftData[i]
            (sum / 12f).coerceIn(0f, 1.5f)
        } else 0f
    }

    val infiniteTransition = rememberInfiniteTransition(label = "GeometricRotation")
    val rotationRad by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "BaseRotation"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val baseRadius = (min(size.width, size.height) * 0.48f) * (1f + if (isPlaying) rawBass * 0.18f else 0f)

        // 1. Ambient Bass Glow Aura
        val auraRadius = baseRadius * (1.25f + if (isPlaying) rawBass * 0.35f else 0f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    dominantColor.copy(alpha = if (isPlaying) (0.35f + rawBass * 0.45f).coerceIn(0f, 0.85f) else 0.12f),
                    DeepElectricBlue.copy(alpha = if (isPlaying) (0.20f + rawMid * 0.30f).coerceIn(0f, 0.5f) else 0.05f),
                    Color.Transparent
                ),
                center = Offset(centerX, centerY),
                radius = auraRadius
            )
        )

        // 2. 3D Nested Reactive Wireframe Triangles (vvavy.io aesthetic)
        val layers = 3
        for (layer in 0 until layers) {
            val layerScale = 1f - (layer * 0.20f)
            val layerRadius = baseRadius * layerScale
            val layerRotation = rotationRad * (if (layer % 2 == 0) 1f else -0.8f) + (layer * 0.5f)
            val layerAlpha = (1f - (layer * 0.22f)).coerceIn(0.2f, 1f)

            draw3DTriangleMesh(
                centerX = centerX,
                centerY = centerY,
                radius = layerRadius,
                rotation = layerRotation,
                bass = if (isPlaying) rawBass else 0f,
                mid = if (isPlaying) rawMid else 0f,
                treble = if (isPlaying) rawTreble else 0f,
                fftData = fftData,
                layerIndex = layer,
                accentColor = if (layer == 0) dominantColor else if (layer == 1) NeonCyan else CyberPurple,
                alpha = if (isPlaying) layerAlpha else layerAlpha * 0.4f
            )
        }
    }
}

private fun DrawScope.draw3DTriangleMesh(
    centerX: Float,
    centerY: Float,
    radius: Float,
    rotation: Float,
    bass: Float,
    mid: Float,
    treble: Float,
    fftData: FloatArray,
    layerIndex: Int,
    accentColor: Color,
    alpha: Float
) {
    val vertices = 3
    val points = mutableListOf<Offset>()

    for (i in 0 until vertices) {
        val angle = rotation + (i * 2 * PI / vertices).toFloat()
        val fftIdx = (i * 4 + layerIndex * 2) % (if (fftData.isNotEmpty()) fftData.size else 1)
        val harmonicShift = if (fftData.isNotEmpty()) fftData[fftIdx] * 25f else 0f

        val r = radius + harmonicShift + (if (i == 0) bass * 22f else if (i == 1) mid * 18f else treble * 15f)
        val x = centerX + r * cos(angle)
        val y = centerY + r * sin(angle)
        points.add(Offset(x, y))
    }

    val path = Path()
    val subdivisions = 6

    for (i in 0 until vertices) {
        val p1 = points[i]
        val p2 = points[(i + 1) % vertices]

        if (i == 0) path.moveTo(p1.x, p1.y)

        for (s in 1..subdivisions) {
            val t = s.toFloat() / subdivisions
            val midX = p1.x + (p2.x - p1.x) * t
            val midY = p1.y + (p2.y - p1.y) * t

            val edgeDx = p2.x - p1.x
            val edgeDy = p2.y - p1.y
            val len = hypot(edgeDx, edgeDy).coerceAtLeast(1f)
            val normX = -edgeDy / len
            val normY = edgeDx / len

            val specOffset = (i * subdivisions + s) % (if (fftData.isNotEmpty()) fftData.size else 1)
            val ripple = if (fftData.isNotEmpty()) (fftData[specOffset] - 0.5f) * 14f else 0f

            val subPoint = Offset(midX + normX * ripple, midY + normY * ripple)
            path.lineTo(subPoint.x, subPoint.y)
        }
    }
    path.close()

    // 1. Facet Translucent Sheen
    if (layerIndex == 0) {
        drawPath(
            path = path,
            brush = Brush.radialGradient(
                colors = listOf(
                    accentColor.copy(alpha = (0.18f + bass * 0.18f).coerceIn(0f, 0.5f) * alpha),
                    DeepElectricBlue.copy(alpha = 0.06f * alpha),
                    Color.Transparent
                ),
                center = Offset(centerX, centerY),
                radius = radius * 1.05f
            )
        )
    }

    // 2. High-Precision Neon Wireframe Edge
    val strokeWidth = (3.2f - layerIndex * 0.6f).coerceAtLeast(1.4f)
    drawPath(
        path = path,
        brush = Brush.linearGradient(
            colors = listOf(
                SpecularWhite.copy(alpha = alpha),
                accentColor.copy(alpha = alpha),
                DeepElectricBlue.copy(alpha = alpha * 0.85f),
                CyberPurple.copy(alpha = alpha * 0.70f)
            ),
            start = points.firstOrNull() ?: Offset.Zero,
            end = points.lastOrNull() ?: Offset.Zero
        ),
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )

    // 3. Glowing Corner Vertex Nodes
    for (pt in points) {
        drawCircle(
            color = SpecularWhite.copy(alpha = alpha),
            radius = (4.0f - layerIndex * 0.8f).coerceAtLeast(2f),
            center = pt
        )
        drawCircle(
            color = accentColor.copy(alpha = (alpha * 0.65f)),
            radius = (8.5f - layerIndex * 1.2f).coerceAtLeast(4f),
            center = pt
        )
    }
}
