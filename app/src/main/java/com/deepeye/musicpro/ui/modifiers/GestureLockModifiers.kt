// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.modifiers

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.unit.Velocity
import kotlin.math.abs

/**
 * Universal Gesture Interoperability Framework for DeepEyeMusicPro.
 *
 * Solves parent-child touch-slop race conditions by eagerly consuming pointer events
 * on [PointerEventPass.Initial], completely starving parent containers (LazyColumn,
 * HorizontalPager, ModalBottomSheet) of conflicting scroll deltas.
 */

/**
 * 1. EAGER HORIZONTAL DRAG INTERCEPTOR
 * Replaces detectHorizontalDragGestures with slop-free PointerEventPass.Initial interception.
 */
fun Modifier.consumeHorizontalDrags(
    enabled: Boolean = true,
    horizontalSlopPx: Float = 0.5f,
    onDragStart: (Offset) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDragCancel: () -> Unit = {},
    onHorizontalDrag: (change: PointerInputChange, dragAmount: Float) -> Unit
): Modifier = if (!enabled) this else this.pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val pointerId = down.id
        var isDragging = false

        onDragStart(down.position)

        while (true) {
            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == pointerId } ?: break

            if (change.changedToUp()) {
                if (isDragging) onDragEnd() else onDragCancel()
                break
            }

            val deltaX = change.position.x - change.previousPosition.x

            if (isDragging || abs(deltaX) > horizontalSlopPx) {
                isDragging = true
                change.consume() // Starve parent scrollable on Main pass
                onHorizontalDrag(change, deltaX)
            }
        }
    }
}

/**
 * 2. EAGER VERTICAL DRAG INTERCEPTOR
 * Intercepts vertical drags on [PointerEventPass.Initial] for Studio EQ Faders and volume sliders.
 */
fun Modifier.consumeVerticalDrags(
    enabled: Boolean = true,
    verticalSlopPx: Float = 0.5f,
    onDragStart: (Offset) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDragCancel: () -> Unit = {},
    onVerticalDrag: (change: PointerInputChange, dragAmount: Float) -> Unit
): Modifier = if (!enabled) this else this.pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val pointerId = down.id
        var isDragging = false

        onDragStart(down.position)

        while (true) {
            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == pointerId } ?: break

            if (change.changedToUp()) {
                if (isDragging) onDragEnd() else onDragCancel()
                break
            }

            val deltaY = change.position.y - change.previousPosition.y

            if (isDragging || abs(deltaY) > verticalSlopPx) {
                isDragging = true
                change.consume() // Starve parent scrollable on Main pass
                onVerticalDrag(change, deltaY)
            }
        }
    }
}

/**
 * 3. PREVENT PARENT SCROLL ON DRAG (Generic Hook)
 */
fun Modifier.preventParentScrollOnDrag(
    enabled: Boolean = true,
    slopPx: Float = 0.5f,
    onDragStart: (Offset) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDragCancel: () -> Unit = {},
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit = { _, _ -> }
): Modifier = if (!enabled) this else this.pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val pointerId = down.id
        var isDragging = false

        onDragStart(down.position)

        while (true) {
            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == pointerId } ?: break

            if (change.changedToUp()) {
                if (isDragging) onDragEnd() else onDragCancel()
                break
            }

            val delta = change.position - change.previousPosition

            if (isDragging || delta.getDistance() > slopPx) {
                isDragging = true
                change.consume()
                onDrag(change, delta)
            }
        }
    }
}

/**
 * 4. NESTED SCROLL PARENT LOCK CONNECTION
 * Consumes 100% of nested scroll and fling deltas when [isLocked] is true.
 */
@Composable
fun rememberParentScrollLockConnection(isLocked: Boolean): NestedScrollConnection {
    return remember(isLocked) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                return if (isLocked) available else Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                return if (isLocked) available else Velocity.Zero
            }
        }
    }
}
