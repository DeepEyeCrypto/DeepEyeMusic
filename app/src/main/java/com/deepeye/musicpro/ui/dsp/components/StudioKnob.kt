// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.dsp.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * A studio-grade rotary knob that replaces standard sliders.
 * Emits haptic ticks when rotated.
 */
@Composable
fun StudioKnob(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    activeColor: Color = Color(0xFF00E5FF),
) {
    val view = LocalView.current
    var lastHapticValue by remember { mutableStateOf(value) }
    
    // Convert value to angle (-135 to +135 degrees)
    val startAngle = 135f
    val sweepAngle = 270f
    
    val normalizedValue = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
    val currentAngle = startAngle + (normalizedValue * sweepAngle)

    Box(
        modifier = modifier
            .size(72.dp)
            .pointerInput(Unit) {
                var dragAngle = 0f
                detectDragGestures(
                    onDragStart = { offset ->
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        dragAngle = atan2(offset.y - centerY, offset.x - centerX) * (180f / PI.toFloat())
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val newAngle = atan2(change.position.y - centerY, change.position.x - centerX) * (180f / PI.toFloat())
                        
                        var angleDiff = newAngle - dragAngle
                        if (angleDiff < -180f) angleDiff += 360f
                        if (angleDiff > 180f) angleDiff -= 360f
                        
                        dragAngle = newAngle
                        
                        // Map angle change to value change
                        val valueChange = (angleDiff / sweepAngle) * (valueRange.endInclusive - valueRange.start)
                        val newValue = (value + valueChange).coerceIn(valueRange.start, valueRange.endInclusive)
                        
                        onValueChange(newValue)
                        
                        // Haptic tick every ~5% change
                        val tickThreshold = (valueRange.endInclusive - valueRange.start) * 0.05f
                        if (Math.abs(newValue - lastHapticValue) >= tickThreshold) {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            lastHapticValue = newValue
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.width / 2.5f
            val arcRadius = radius * 1.2f

            // 1. Draw outer glowing arc (Track)
            drawArc(
                color = Color.White.copy(alpha = 0.05f),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
                size = androidx.compose.ui.geometry.Size(arcRadius * 2, arcRadius * 2),
                topLeft = Offset(center.x - arcRadius, center.y - arcRadius)
            )

            // 2. Draw active arc (Progress)
            if (normalizedValue > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(activeColor.copy(alpha = 0.5f), activeColor),
                        center = center
                    ),
                    startAngle = startAngle,
                    sweepAngle = normalizedValue * sweepAngle,
                    useCenter = false,
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
                    size = androidx.compose.ui.geometry.Size(arcRadius * 2, arcRadius * 2),
                    topLeft = Offset(center.x - arcRadius, center.y - arcRadius)
                )
            }

            // 3. Draw Knob Base (Metallic Gradient)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF2A2A35),
                        Color(0xFF1E1E24)
                    ),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
            // Knob inner border
            drawCircle(
                color = Color.White.copy(alpha = 0.1f),
                radius = radius,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // 4. Draw Indicator Dot
            val indicatorRadius = radius * 0.6f
            val indicatorAngleRad = currentAngle * (PI / 180f).toFloat()
            val indicatorX = center.x + indicatorRadius * cos(indicatorAngleRad)
            val indicatorY = center.y + indicatorRadius * sin(indicatorAngleRad)

            drawCircle(
                color = activeColor,
                radius = 4.dp.toPx(),
                center = Offset(indicatorX, indicatorY)
            )
            
            // Glowing effect on the dot
            drawCircle(
                color = activeColor.copy(alpha = 0.5f),
                radius = 8.dp.toPx(),
                center = Offset(indicatorX, indicatorY)
            )
        }
    }
}
