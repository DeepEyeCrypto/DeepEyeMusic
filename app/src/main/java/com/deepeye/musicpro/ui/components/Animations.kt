// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.components

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

fun Modifier.hoverable(
    scale: Float = 1.02f,
    brightness: Float = 1.1f,
    downScale: Float = 0.98f
): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) downScale else 1f, // We map hover to click since this is mostly touch, but we'll simulate 'press'
        animationSpec = tween(durationMillis = 200),
        label = "hover_scale"
    )

    val animatedBrightness by animateFloatAsState(
        targetValue = if (isPressed) brightness else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "hover_brightness"
    )

    this
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                isPressed = true
                waitForUpOrCancellation()
                isPressed = false
            }
        }
        .graphicsLayer {
            scaleX = animatedScale
            scaleY = animatedScale
            // Simulated brightness via alpha
            alpha = if (isPressed) 0.8f else 1f 
        }
}

fun Modifier.bouncyClickable(
    downScale: Float = 0.95f,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) downScale else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "bouncy_scale"
    )

    this
        .graphicsLayer {
            scaleX = animatedScale
            scaleY = animatedScale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}

@Suppress("DEPRECATION")
fun hapticVibrate(view: View, type: Int = HapticFeedbackConstants.CONTEXT_CLICK) {
    view.performHapticFeedback(type, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
}
