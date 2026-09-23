// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.player.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer

private val AmbientCyan = Color(0xFF00E5FF)
private val AmbientBlue = Color(0xFF0051FF)
private val AmbientPurple = Color(0xFF7000FF)

/**
 * Real-time Palette API-driven Ambilight Ambient Blur.
 * Eliminates solid black letterboxing behind the video player.
 */
@Composable
fun AmbilightBackground(
    primaryColor: Color = AmbientCyan,
    secondaryColor: Color = AmbientBlue,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "AmbilightTransition")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val driftX by infiniteTransition.animateFloat(
        initialValue = -60f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "DriftX"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // ── AMBILIGHT GLOW LAYER ──
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        renderEffect = RenderEffect
                            .createBlurEffect(140f, 140f, Shader.TileMode.DECAL)
                            .asComposeRenderEffect()
                    }
                    alpha = 0.55f
                    scaleX = pulseScale
                    scaleY = pulseScale
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Left Ambient Lobe
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.85f), Color.Transparent),
                    center = Offset(canvasWidth * 0.15f + driftX, canvasHeight * 0.5f),
                    radius = canvasWidth * 0.50f
                )
            )

            // Right Ambient Lobe
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(secondaryColor.copy(alpha = 0.85f), Color.Transparent),
                    center = Offset(canvasWidth * 0.85f - driftX, canvasHeight * 0.5f),
                    radius = canvasWidth * 0.50f
                )
            )

            // Center Ambient Fill
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.40f), Color.Transparent),
                    center = Offset(canvasWidth * 0.5f, canvasHeight * 0.5f),
                    radius = canvasWidth * 0.60f
                )
            )
        }

        // ── FOREGROUND VIDEO CONTENT ──
        content()
    }
}
