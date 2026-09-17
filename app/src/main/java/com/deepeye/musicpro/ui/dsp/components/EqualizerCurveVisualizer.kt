// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.dsp.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.deepeye.musicpro.ui.theme.NeonCyan

val EQ_FREQUENCIES = listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")

/**
 * High-end Studio 10-Band Equalizer with interactive Bezier Spline Curve and responsive touch adjustments.
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
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0F101E).copy(alpha = 0.85f))
            .border(
                1.dp,
                Brush.linearGradient(listOf(NeonCyan.copy(alpha = 0.4f), Color.White.copy(alpha = 0.1f))),
                RoundedCornerShape(20.dp)
            )
            .padding(16.dp)
    ) {
        // Curve Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "10-Band Studio EQ Response",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isEnabled) NeonCyan else Color.Gray
            )
            Text(
                text = "-12dB ... +12dB",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Real-Time Bezier Response Curve Canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
        ) {
            val width = size.width
            val height = size.height
            val midY = height / 2f
            val numBands = eqBands.size
            if (numBands < 2) return@Canvas

            // 0dB Center reference grid line
            drawLine(
                color = Color.White.copy(alpha = 0.15f),
                start = Offset(0f, midY),
                end = Offset(width, midY),
                strokeWidth = 1.5f
            )

            // +6dB / -6dB Grid guides
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

            // Calculate control points for each frequency band
            val points = mutableListOf<Offset>()
            for (i in 0 until numBands) {
                val x = (i.toFloat() / (numBands - 1)) * (width - 40f) + 20f
                val gain = eqBands[i].coerceIn(-12f, 12f)
                // Normalize gain from [-12, +12] to [height, 0]
                val normalizedY = midY - (gain / 12f) * (height * 0.45f)
                points.add(Offset(x, normalizedY))
            }

            // Build smooth Bezier path
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

            // Filled gradient under curve
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
                        (if (isEnabled) NeonCyan else Color.Gray).copy(alpha = 0.35f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = height
                )
            )

            // Draw line curve
            drawPath(
                path = strokePath,
                color = if (isEnabled) NeonCyan else Color.Gray,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw band nodes
            for (pt in points) {
                drawCircle(
                    color = Color.Black,
                    radius = 5.dp.toPx(),
                    center = pt
                )
                drawCircle(
                    color = if (isEnabled) Color.White else Color.Gray,
                    radius = 3.dp.toPx(),
                    center = pt
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Interactive Band Sliders Row
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
                    Text(
                        text = if (currentGain >= 0) "+${currentGain.toInt()}" else "${currentGain.toInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = if (isEnabled) NeonCyan else Color.Gray,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Vertical slider touch track
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .height(100.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .pointerInput(isEnabled, i) {
                                if (!isEnabled) return@pointerInput
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    // Dragging up increases gain (-y = +gain)
                                    val deltaGain = -dragAmount.y * 0.25f
                                    val newGain = (eqBands[i] + deltaGain).coerceIn(-12f, 12f)
                                    onBandGainChanged(i, newGain)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Center 0 line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color.White.copy(alpha = 0.2f))
                        )

                        // Gain fill bar
                        val normGain = currentGain / 12f // [-1f..1f]
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .fillMaxHeight(Math.abs(normGain) * 0.5f)
                                .align(if (normGain >= 0) Alignment.TopCenter else Alignment.BottomCenter)
                                .offset(y = if (normGain >= 0) 50.dp * (1f - normGain) else 50.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (isEnabled) NeonCyan else Color.Gray)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = freqLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
