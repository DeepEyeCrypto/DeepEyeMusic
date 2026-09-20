// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.dsp.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

val EQ_FREQUENCIES = listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")

data class EqPreset(val name: String, val gains: FloatArray)

val STUDIO_EQ_PRESETS = listOf(
    EqPreset("Flat", floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)),
    EqPreset("Bass Boost", floatArrayOf(7f, 6f, 4f, 2f, 0f, 0f, 1f, 2f, 3f, 4f)),
    EqPreset("Vocal Clarity", floatArrayOf(-2f, -1f, 0f, 2f, 5f, 6f, 4f, 2f, 0f, -1f)),
    EqPreset("Club / EDM", floatArrayOf(8f, 7f, 3f, 0f, -2f, 2f, 4f, 6f, 7f, 8f)),
    EqPreset("Rock & Metal", floatArrayOf(6f, 4f, 2f, 0f, -1f, 1f, 4f, 5f, 6f, 6f)),
    EqPreset("Acoustic", floatArrayOf(3f, 3f, 2f, 1f, 2f, 3f, 4f, 3f, 2f, 1f)),
    EqPreset("Treble Sparkle", floatArrayOf(-3f, -2f, -1f, 0f, 1f, 3f, 6f, 8f, 9f, 9f)),
)

private val neonCyan = Color(0xFF00E5FF)
private val neonPurple = Color(0xFF7B1FA2)
private val darkSurface = Color(0xFF131722).copy(alpha = 0.85f)
private val glassBorder = Color(0x22FFFFFF)

/**
 * Pro Hardware Studio 10-Band Equalizer with Bezier Spline Curve, Quick Studio Presets, and Tactile High-Fidelity Faders.
 */
@Composable
fun EqualizerCurveVisualizer(
    eqBands: FloatArray,
    isEnabled: Boolean,
    onBandGainChanged: (Int, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(darkSurface)
            .border(1.dp, glassBorder, RoundedCornerShape(24.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Curve Header & Quick Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x3300E5FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = if (isEnabled) neonCyan else Color.White.copy(0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "10-Band Studio EQ Console",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isEnabled) Color.White else Color.White.copy(0.5f)
                    )
                    Text(
                        text = "Parametric Log-Spline • 31Hz - 16kHz",
                        fontSize = 12.sp,
                        color = Color.White.copy(0.6f)
                    )
                }
            }

            // Reset to Flat button
            Surface(
                onClick = {
                    for (i in 0 until eqBands.size.coerceAtMost(10)) {
                        onBandGainChanged(i, 0f)
                    }
                },
                enabled = isEnabled,
                shape = RoundedCornerShape(12.dp),
                color = Color(0x14FFFFFF),
                border = androidx.compose.foundation.BorderStroke(1.dp, glassBorder),
                modifier = Modifier.heightIn(min = 36.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.RestartAlt, null, tint = neonCyan, modifier = Modifier.size(16.dp))
                    Text("Flat", color = neonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Quick Studio Presets Carousel
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 2.dp)
        ) {
            items(STUDIO_EQ_PRESETS) { preset ->
                val isSelected = remember(eqBands.toList(), preset) {
                    val currentRounded = eqBands.take(10).map { it.roundToInt() }
                    val presetRounded = preset.gains.take(10).map { it.roundToInt() }
                    currentRounded == presetRounded
                }

                Surface(
                    onClick = {
                        if (isEnabled) {
                            preset.gains.forEachIndexed { index, gain ->
                                if (index < eqBands.size) {
                                    onBandGainChanged(index, gain)
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) Color(0x3300E5FF) else Color(0x0EFFFFFF),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) neonCyan else glassBorder
                    ),
                    modifier = Modifier.heightIn(min = 38.dp)
                ) {
                    Text(
                        text = preset.name,
                        color = if (isSelected) neonCyan else Color.White.copy(0.75f),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Real-Time Studio Spline Curve Canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF090B10).copy(alpha = 0.95f))
                .border(1.dp, glassBorder, RoundedCornerShape(16.dp))
        ) {
            val width = size.width
            val height = size.height
            val midY = height / 2f
            val numBands = eqBands.size.coerceAtMost(10)
            if (numBands < 2) return@Canvas

            // Grid Lines: 0dB, +6dB, -6dB
            drawLine(
                color = Color.White.copy(alpha = 0.2f),
                start = Offset(0f, midY),
                end = Offset(width, midY),
                strokeWidth = 1.5f
            )

            val stepY = height / 4f
            drawLine(
                color = Color.White.copy(alpha = 0.08f),
                start = Offset(0f, midY - stepY),
                end = Offset(width, midY - stepY),
                strokeWidth = 1f
            )
            drawLine(
                color = Color.White.copy(alpha = 0.08f),
                start = Offset(0f, midY + stepY),
                end = Offset(width, midY + stepY),
                strokeWidth = 1f
            )

            // Frequency Vertical Guides
            for (i in 0 until numBands) {
                val x = (i.toFloat() / (numBands - 1)) * (width - 60f) + 30f
                drawLine(
                    color = Color.White.copy(alpha = 0.04f),
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1f
                )
            }

            // Calculate Bezier control points
            val points = mutableListOf<Offset>()
            for (i in 0 until numBands) {
                val x = (i.toFloat() / (numBands - 1)) * (width - 60f) + 30f
                val gain = eqBands[i].coerceIn(-12f, 12f)
                val normalizedY = midY - (gain / 12f) * (height * 0.42f)
                points.add(Offset(x, normalizedY))
            }

            // Smooth cubic Bezier spline
            val strokePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 0 until points.size - 1) {
                    val p0 = points[i]
                    val p1 = points[i + 1]
                    val controlX1 = (p0.x + p1.x) / 2f
                    val controlY1 = p0.y
                    val controlX2 = (p0.x + p1.x) / 2f
                    val controlY2 = p1.y
                    cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
                }
            }

            // Gradient Fill under curve
            val fillPath = Path().apply {
                addPath(strokePath)
                lineTo(points.last().x, height)
                lineTo(points.first().x, height)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        (if (isEnabled) neonCyan else Color.Gray).copy(alpha = 0.35f),
                        (if (isEnabled) neonPurple else Color.DarkGray).copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = height
                )
            )

            // Neon glowing stroke
            drawPath(
                path = strokePath,
                brush = Brush.horizontalGradient(
                    colors = if (isEnabled) {
                        listOf(neonCyan, Color(0xFF00B0FF), neonPurple, Color(0xFFFF4081))
                    } else {
                        listOf(Color.Gray, Color.DarkGray)
                    }
                ),
                style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // Interactive glowing band nodes
            for (pt in points) {
                drawCircle(
                    color = Color.Black,
                    radius = 6.dp.toPx(),
                    center = pt
                )
                drawCircle(
                    color = if (isEnabled) neonCyan else Color.Gray,
                    radius = 4.dp.toPx(),
                    center = pt
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.dp.toPx(),
                    center = pt
                )
            }
        }

        // 10-Band Tactile Studio Faders
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            for (i in 0 until eqBands.size.coerceAtMost(10)) {
                val freqLabel = EQ_FREQUENCIES.getOrElse(i) { "${i}k" }
                val currentGain = eqBands[i]

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    // dB readout badge
                    val isBoost = currentGain > 0.4f
                    val isCut = currentGain < -0.4f
                    val formattedGain = if (currentGain >= 0.4f) {
                        "+${currentGain.roundToInt()}"
                    } else if (currentGain <= -0.4f) {
                        "${currentGain.roundToInt()}"
                    } else {
                        "0"
                    }

                    Text(
                        text = formattedGain,
                        fontSize = 11.sp,
                        color = when {
                            !isEnabled -> Color.Gray
                            isBoost -> neonCyan
                            isCut -> Color(0xFFFF9100)
                            else -> Color.White.copy(0.7f)
                        },
                        fontWeight = FontWeight.Black
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Tactile Vertical Fader Track (Automotive width 28.dp x height 120.dp)
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(120.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF090B10))
                            .border(1.dp, glassBorder, RoundedCornerShape(14.dp))
                            .pointerInput(isEnabled, i) {
                                if (!isEnabled) return@pointerInput
                                detectVerticalDragGestures(
                                    onDragStart = { offset ->
                                        val trackHeight = size.height.toFloat()
                                        val fraction = (1f - (offset.y / trackHeight)).coerceIn(0f, 1f)
                                        val targetGain = (fraction * 24f - 12f).coerceIn(-12f, 12f)
                                        onBandGainChanged(i, targetGain)
                                    },
                                    onVerticalDrag = { change, dragAmount ->
                                        change.consume()
                                        val deltaGain = -dragAmount * (24f / size.height.toFloat())
                                        val newGain = (eqBands[i] + deltaGain).coerceIn(-12f, 12f)
                                        onBandGainChanged(i, newGain)
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Center 0dB line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.5.dp)
                                .background(Color.White.copy(alpha = 0.25f))
                        )

                        // Fader active gain indicator
                        val normGain = (currentGain / 12f).coerceIn(-1f, 1f)
                        val faderHeight = 120f
                        val midTrackY = faderHeight / 2f
                        val thumbOffsetY = (midTrackY - normGain * (midTrackY - 14f) - 12f).coerceIn(4f, faderHeight - 24f)

                        // Hardware Console Fader Knob Cap
                        Box(
                            modifier = Modifier
                                .offset(y = (thumbOffsetY - midTrackY + 12f).dp)
                                .width(24.dp)
                                .height(24.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isEnabled) {
                                        Brush.verticalGradient(listOf(Color(0xFF263238), Color(0xFF102027)))
                                    } else {
                                        Brush.verticalGradient(listOf(Color(0xFF1E1E1E), Color(0xFF121212)))
                                    }
                                )
                                .border(
                                    1.dp,
                                    if (isEnabled) neonCyan.copy(alpha = 0.8f) else Color.White.copy(0.2f),
                                    RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            // Center Glowing Indicator Line
                            Box(
                                modifier = Modifier
                                    .width(14.dp)
                                    .height(2.5.dp)
                                    .clip(RoundedCornerShape(1.dp))
                                    .background(if (isEnabled) neonCyan else Color.Gray)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Frequency Label
                    Text(
                        text = freqLabel,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
