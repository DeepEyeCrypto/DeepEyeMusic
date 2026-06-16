// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.components

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
        .scale(animatedScale)
        .graphicsLayer {
            // Brightness effect can be approximated using color filter or just alpha if preferred.
            // Using alpha here for simplicity, or we can leave it just scaling for better performance.
        }
}

fun hapticVibrate(view: View, type: Int = HapticFeedbackConstants.CONTEXT_CLICK) {
    view.performHapticFeedback(type, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
}
