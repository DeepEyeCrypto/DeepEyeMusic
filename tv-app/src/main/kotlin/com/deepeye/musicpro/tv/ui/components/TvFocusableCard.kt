// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.tv.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * The cyan used for focus indication across the TV surface.
 */
val TvFocusCyan = Color(0xFF00E5FF)

/**
 * 10-foot focusable container.
 *
 * Every element reachable with a D-pad must be individually focusable, so the
 * remote cursor is never ambiguous. Focus is communicated three ways at once,
 * because a single cue is easy to miss on a large screen at viewing distance:
 *  1. a 1.1x scale-up (spring, so it lands with weight rather than snapping),
 *  2. a glowing cyan border,
 *  3. a raised shadow tinted with the same cyan.
 *
 * Touch input is intentionally ignored: `focusable()` is used rather than
 * `clickable()` + `focusable()` so a TV build cannot rely on press-and-hold,
 * which has no equivalent on a directional remote.
 *
 * @param onFocus invoked when focus lands on this item, letting the screen react
 *   (e.g. swap the background art) before the user commits with a click.
 */
@Composable
fun Modifier.tvFocusable(
    enabled: Boolean = true,
    onFocus: () -> Unit = {},
    shape: androidx.compose.ui.graphics.Shape,
    onClick: () -> Unit,
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (focused) TvFocusScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "tvFocusScale"
    )
    val borderAlpha by animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = tween(durationMillis = 120),
        label = "tvFocusBorder"
    )

    return this
        .scale(scale)
        .graphicsLayer {
            if (borderAlpha > 0f) {
                shadowElevation = 16f
                ambientShadowColor = TvFocusCyan
                spotShadowColor = TvFocusCyan
            }
        }
        .clip(shape)
        .then(
            if (enabled) {
                Modifier
                    .focusable(enabled = true)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = enabled,
                        onClick = onClick
                    )
                    .onFocusChanged { state ->
                        if (state.isFocused) onFocus()
                    }
            } else {
                Modifier
            }
        )
        .background(Color.Transparent)
        .border(
            width = if (borderAlpha > 0f) TvFocusBorderWidth else 0.dp,
            color = TvFocusCyan.copy(alpha = borderAlpha),
            shape = shape
        )
}

/** Scale applied to a focused 10-foot card. */
const val TvFocusScale = 1.1f

/** Border thickness for the focused-state glow. */
val TvFocusBorderWidth = 2.dp
