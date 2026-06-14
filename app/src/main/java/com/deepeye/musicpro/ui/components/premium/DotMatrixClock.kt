// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.components.premium

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

val DotMatrixDigits = mapOf(
    '0' to listOf(
        " 111 ",
        "1   1",
        "1  11",
        "1 1 1",
        "11  1",
        "1   1",
        " 111 "
    ),
    '1' to listOf(
        "  1  ",
        " 11  ",
        "1 1  ",
        "  1  ",
        "  1  ",
        "  1  ",
        "11111"
    ),
    '2' to listOf(
        " 111 ",
        "1   1",
        "    1",
        "   1 ",
        "  1  ",
        " 1   ",
        "11111"
    ),
    '3' to listOf(
        " 111 ",
        "1   1",
        "    1",
        "  11 ",
        "    1",
        "1   1",
        " 111 "
    ),
    '4' to listOf(
        "   1 ",
        "  11 ",
        " 1 1 ",
        "1  1 ",
        "11111",
        "   1 ",
        "   1 "
    ),
    '5' to listOf(
        "11111",
        "1    ",
        "1111 ",
        "    1",
        "    1",
        "1   1",
        " 111 "
    ),
    '6' to listOf(
        " 111 ",
        "1   1",
        "1    ",
        "1111 ",
        "1   1",
        "1   1",
        " 111 "
    ),
    '7' to listOf(
        "11111",
        "    1",
        "   1 ",
        "  1  ",
        " 1   ",
        " 1   ",
        " 1   "
    ),
    '8' to listOf(
        " 111 ",
        "1   1",
        "1   1",
        " 111 ",
        "1   1",
        "1   1",
        " 111 "
    ),
    '9' to listOf(
        " 111 ",
        "1   1",
        "1   1",
        " 1111",
        "    1",
        "1   1",
        " 111 "
    ),
    ':' to listOf(
        "   ",
        " 1 ",
        " 1 ",
        "   ",
        " 1 ",
        " 1 ",
        "   "
    ),
    ' ' to listOf(
        "   ",
        "   ",
        "   ",
        "   ",
        "   ",
        "   ",
        "   "
    ),
    'A' to listOf(
        " 111 ",
        "1   1",
        "11111",
        "1   1",
        "1   1",
        "1   1",
        "1   1"
    ),
    'P' to listOf(
        "1111 ",
        "1   1",
        "1   1",
        "1111 ",
        "1    ",
        "1    ",
        "1    "
    ),
    'M' to listOf(
        "1   1",
        "11 11",
        "1 1 1",
        "1   1",
        "1   1",
        "1   1",
        "1   1"
    ),
    'G' to listOf(
        " 111 ",
        "1   1",
        "1    ",
        "1 111",
        "1   1",
        "1   1",
        " 111 "
    ),
    'O' to listOf(
        " 111 ",
        "1   1",
        "1   1",
        "1   1",
        "1   1",
        "1   1",
        " 111 "
    ),
    'D' to listOf(
        "1111 ",
        "1   1",
        "1   1",
        "1   1",
        "1   1",
        "1   1",
        "1111 "
    ),
    'R' to listOf(
        "1111 ",
        "1   1",
        "1   1",
        "1111 ",
        "1 1  ",
        "1  1 ",
        "1   1"
    ),
    'N' to listOf(
        "1   1",
        "11  1",
        "1 1 1",
        "1  11",
        "1   1",
        "1   1",
        "1   1"
    ),
    'I' to listOf(
        " 111 ",
        "  1  ",
        "  1  ",
        "  1  ",
        "  1  ",
        "  1  ",
        " 111 "
    ),
    'F' to listOf(
        "11111",
        "1    ",
        "1    ",
        "1111 ",
        "1    ",
        "1    ",
        "1    "
    ),
    'T' to listOf(
        "11111",
        "  1  ",
        "  1  ",
        "  1  ",
        "  1  ",
        "  1  ",
        "  1  "
    ),
    'E' to listOf(
        "11111",
        "1    ",
        "1    ",
        "1111 ",
        "1    ",
        "1    ",
        "11111"
    ),
    'V' to listOf(
        "1   1",
        "1   1",
        "1   1",
        "1   1",
        " 1 1 ",
        " 1 1 ",
        "  1  "
    ),
    'H' to listOf(
        "1   1",
        "1   1",
        "1   1",
        "11111",
        "1   1",
        "1   1",
        "1   1"
    )
)

@Composable
fun DotMatrixClock(
    modifier: Modifier = Modifier,
    activeColor: Color = Color(0xFF00D2FF),
    inactiveColor: Color = Color.White.copy(alpha = 0.05f),
    dotRadius: Float = 4f,
    dotSpacing: Float = 12f
) {
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    
    // Animate the colon blinking
    val infiniteTransition = rememberInfiniteTransition()
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "blink"
    )

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val timeString = remember(currentTime) {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(currentTime)).uppercase()
    }

    // Calculate total width
    val totalCols = timeString.sumOf { char ->
        DotMatrixDigits[char]?.first()?.length ?: 0
    } + (timeString.length - 1) // +1 for spacing between chars
    
    val totalWidth = totalCols * dotSpacing
    val totalHeight = 7 * dotSpacing // 7 rows fixed

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .size(width = (totalWidth / LocalContext.current.resources.displayMetrics.density).dp, 
                      height = (totalHeight / LocalContext.current.resources.displayMetrics.density).dp)
                .padding(4.dp)
        ) {
            var currentX = 0f

            for (char in timeString) {
                val matrix = DotMatrixDigits[char] ?: DotMatrixDigits[' ']!!
                val cols = matrix.first().length

                for (row in 0 until 7) {
                    for (col in 0 until cols) {
                        val isLit = matrix[row][col] == '1'
                        
                        // Handle colon blinking
                        val finalAlpha = if (char == ':' && isLit) blinkAlpha else if (isLit) 1f else 1f
                        val color = if (isLit) activeColor.copy(alpha = finalAlpha) else inactiveColor

                        drawCircle(
                            color = color,
                            radius = dotRadius,
                            center = Offset(
                                x = currentX + (col * dotSpacing) + dotRadius,
                                y = (row * dotSpacing) + dotRadius
                            )
                        )
                    }
                }
                currentX += (cols + 1) * dotSpacing
            }
        }
    }
}

@Composable
fun DotMatrixText(
    text: String,
    modifier: Modifier = Modifier,
    activeColor: Color = Color(0xFF00D2FF),
    inactiveColor: Color = Color.White.copy(alpha = 0.05f),
    dotRadius: Float = 4f,
    dotSpacing: Float = 12f
) {
    // Calculate total width
    val totalCols = text.sumOf { char ->
        DotMatrixDigits[char]?.first()?.length ?: 0
    } + (text.length - 1).coerceAtLeast(0) // spacing
    
    val totalWidth = totalCols * dotSpacing
    val totalHeight = 7 * dotSpacing

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .size(
                    width = (totalWidth / LocalContext.current.resources.displayMetrics.density).dp, 
                    height = (totalHeight / LocalContext.current.resources.displayMetrics.density).dp
                )
                .padding(4.dp)
        ) {
            var currentX = 0f
            for (char in text) {
                val matrix = DotMatrixDigits[char] ?: DotMatrixDigits[' ']!!
                val cols = matrix.first().length

                for (row in 0 until 7) {
                    for (col in 0 until cols) {
                        val isLit = matrix[row][col] == '1'
                        val color = if (isLit) activeColor else inactiveColor

                        drawCircle(
                            color = color,
                            radius = dotRadius,
                            center = Offset(
                                x = currentX + (col * dotSpacing) + dotRadius,
                                y = (row * dotSpacing) + dotRadius
                            )
                        )
                    }
                }
                currentX += (cols + 1) * dotSpacing
            }
        }
    }
}
