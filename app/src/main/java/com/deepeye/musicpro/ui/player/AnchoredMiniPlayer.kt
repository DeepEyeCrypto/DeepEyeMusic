// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.player

import android.util.Log
import androidx.compose.animation.core.spring
import androidx.compose.animation.splineBasedDecay
import androidx.compose.runtime.derivedStateOf
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Anchored mini-player sheet with hardened gesture routing.
 *
 * Gesture design:
 * - **Direction classification** uses a dead-zone ratio (1.5:1) to reject ambiguous diagonal swipes.
 * - **Vertical drags** always control the sheet anchor (COLLAPSED ↔ HALF_EXPANDED ↔ EXPANDED).
 * - **Horizontal drags** trigger skip next/prev ONLY when sheet is COLLAPSED.
 * - **Velocity fling** on horizontal end uses velocity threshold (600dp/s) for snappy skip detection.
 * - All gesture state resets cleanly on cancellation — no stuck anchors.
 * - Pointer events are intercepted at `PointerEventPass.Initial` to beat child composables.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnchoredMiniPlayer(
    sheetState: MiniPlayerSheetState,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
    onHalfExpand: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onPlayPause: () -> Unit,
    bottomBarHeight: androidx.compose.ui.unit.Dp = 0.dp,
    expandedContent: @Composable () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize().padding(bottom = bottomBarHeight)) {
        val density = LocalDensity.current
        val containerHeightPx = constraints.maxHeight.toFloat()

        val bottomBarHeightPx = 0f // No longer used for anchor calculations
        val collapsedAnchor = containerHeightPx - with(density) { 88.dp.toPx() }
        val halfAnchor = containerHeightPx * 0.45f
        val expandedAnchor = 0f

        val draggableState =
            remember {
                AnchoredDraggableState(
                    initialValue = sheetState.anchor,
                    positionalThreshold = { distance: Float -> distance * 0.3f },
                    velocityThreshold = { with(density) { 125.dp.toPx() } },
                    snapAnimationSpec = spring(),
                    decayAnimationSpec = splineBasedDecay(density),
                )
            }

        // Skip threshold for horizontal swipes (distance-based)
        val skipThreshold = with(density) { 80.dp.toPx() }

        // Velocity threshold for horizontal fling detection (dp/s converted to px/s)
        val horizontalFlingVelocityThreshold = with(density) { 600.dp.toPx() }

        // Dead-zone ratio: primary axis must be at least 1.5x the secondary axis
        // to classify as intentional directional movement (rejects ambiguous diagonals)
        val deadZoneRatio = 1.5f

        LaunchedEffect(collapsedAnchor, expandedAnchor) {
            draggableState.updateAnchors(
                DraggableAnchors {
                    MiniSheetAnchor.COLLAPSED at collapsedAnchor
                    MiniSheetAnchor.EXPANDED at expandedAnchor
                }
            )
        }


        // Sync programmatic state changes (from ViewModel e.g. collapse() on back press)
        // to the draggable state. This does NOT trigger the drag-settled callback below
        // because we guard against re-entering.
        var isAnimating by remember { mutableStateOf(false) }
        LaunchedEffect(sheetState.anchor) {
            if (draggableState.currentValue != sheetState.anchor) {
                isAnimating = true
                draggableState.animateTo(sheetState.anchor)
                isAnimating = false
            }
        }

        // Notify the ViewModel only when a user-driven drag settles at a new anchor.
        // Skip programmatic animations (guarded by isAnimating).
        LaunchedEffect(draggableState.currentValue, isAnimating) {
            if (!isAnimating && draggableState.currentValue != sheetState.anchor) {
                when (draggableState.currentValue) {
                    MiniSheetAnchor.COLLAPSED -> onCollapse()
                    MiniSheetAnchor.HALF_EXPANDED -> onCollapse() // Treat as collapse (anchor removed)
                    MiniSheetAnchor.EXPANDED -> onExpand()
                }
            }
        }

        val scope = rememberCoroutineScope()

        Box(
            Modifier
                .fillMaxWidth()
                // Dynamic height based on current drag offset:
                // - At collapsed anchor: height = 88dp (mini player only, doesn't overlap nav bar)
                // - During drag or at expanded/half anchors: fillMaxHeight (full overlay)
                // This prevents the gesture handler from blocking the bottom nav bar when
                // collapsed, while allowing smooth full-screen expansion during drag.
                .then(
                    run {
                        val isAtCollapsed by remember {
                            derivedStateOf {
                                draggableState.targetValue == MiniSheetAnchor.COLLAPSED &&
                                draggableState.currentValue == MiniSheetAnchor.COLLAPSED
                            }
                        }
                        if (isAtCollapsed) {
                            Modifier.height(with(density) { 88.dp })
                        } else {
                            Modifier.fillMaxHeight()
                        }
                    }
                )
                .offset {
                    val yOffset = draggableState.offset.takeIf { !it.isNaN() } ?: collapsedAnchor
                    IntOffset(0, yOffset.roundToInt())
                }
                .pointerInput(sheetState.isGestureLocked, bottomBarHeightPx) {
                    if (sheetState.isGestureLocked) return@pointerInput

                    // Calculate the content height (mini player visible area).
                    // When collapsed: 88dp. When expanded: full container.
                    // Touches below the content area (in the nav bar zone) must pass through
                    // so the bottom navigation bar remains tappable.
                    val miniPlayerHeightPx = with(density) { 88.dp.toPx() }

                    coroutineScope {
                        awaitEachGesture {
                            val down = awaitFirstDown(
                                requireUnconsumed = false,
                                pass = PointerEventPass.Initial
                            )

                            // Guard: if the touch is below the mini player content area
                            // (i.e., in the bottom nav bar zone), don't intercept it.
                            // The content height depends on the current anchor:
                            // - Collapsed: only 88dp of content is visible
                            // - Expanded: full height is content
                            val currentOffset = draggableState.offset.takeIf { !it.isNaN() } ?: collapsedAnchor
                            val contentHeight = containerHeightPx - currentOffset - bottomBarHeightPx
                            if (down.position.y > contentHeight) {
                                // Touch is in the nav bar zone — abort gesture, let it pass through
                                return@awaitEachGesture
                            }
                            var dragDirection: Orientation? = null
                            var dragX = 0f
                            val touchSlop = viewConfiguration.touchSlop
                            var overSlop = 0f

                            val velocityTracker = VelocityTracker()
                            velocityTracker.addPosition(down.uptimeMillis, down.position)
                            Log.d("MiniPlayerGesture", "Gesture started at: ${down.position}")

                            // 1. Determine direction — use Main pass during classification
                            // so taps pass through to children unblocked. Only switch to
                            // Initial pass AFTER we've committed to a drag direction.
                            do {
                                val event = awaitPointerEvent(PointerEventPass.Main)
                                val change = event.changes.firstOrNull { it.id == down.id }
                                if (change == null || !change.pressed) {
                                    // Pointer released before slop — this is a TAP.
                                    // Don't consume anything, let children handle it.
                                    dragDirection = null
                                    break
                                }

                                val dx = change.position.x - down.position.x
                                val dy = change.position.y - down.position.y
                                velocityTracker.addPosition(change.uptimeMillis, change.position)

                                val absDx = abs(dx)
                                val absDy = abs(dy)

                                if (absDx > touchSlop || absDy > touchSlop) {
                                    val isHorizontalDominant = absDx > absDy * deadZoneRatio
                                    val isVerticalDominant = absDy > absDx * deadZoneRatio

                                    when {
                                        isVerticalDominant -> {
                                            dragDirection = Orientation.Vertical
                                            overSlop = if (dy > 0) dy - touchSlop else dy + touchSlop
                                            change.consume()
                                            Log.d("MiniPlayerGesture", "Vertical slop crossed. Consuming.")
                                        }
                                        isHorizontalDominant -> {
                                            if (draggableState.currentValue == MiniSheetAnchor.COLLAPSED) {
                                                dragDirection = Orientation.Horizontal
                                                overSlop = if (dx > 0) dx - touchSlop else dx + touchSlop
                                                change.consume()
                                                Log.d("MiniPlayerGesture", "Horizontal slop crossed (COLLAPSED). Consuming.")
                                            } else {
                                                dragDirection = null
                                                Log.d("MiniPlayerGesture", "Horizontal slop crossed (EXPANDED). Yielding to children.")
                                            }
                                        }
                                        else -> {
                                            dragDirection = null
                                            Log.d("MiniPlayerGesture", "Ambiguous diagonal detected (dx=$absDx, dy=$absDy). Rejecting.")
                                        }
                                    }
                                    break
                                }
                            } while (true)

                            if (dragDirection == null) {
                                Log.d("MiniPlayerGesture", "Gesture aborted: no clear direction (tap passthrough).")
                                return@awaitEachGesture
                            }

                            // 2. Direction committed — now consume at Initial pass for the drag
                            if (dragDirection == Orientation.Vertical) {
                                val dragChannel = Channel<Float>(Channel.UNLIMITED)
                                val dragJob = scope.launch {
                                    draggableState.anchoredDrag(MutatePriority.UserInput) {
                                        var currentOffset = draggableState.offset
                                        if (!currentOffset.isNaN()) {
                                            dragTo(currentOffset + overSlop)
                                        }
                                        for (deltaY in dragChannel) {
                                            currentOffset = draggableState.offset
                                            if (!currentOffset.isNaN()) {
                                                dragTo(currentOffset + deltaY)
                                            }
                                        }
                                    }
                                }

                                try {
                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                        val change = event.changes.firstOrNull { it.id == down.id }
                                        if (change == null || !change.pressed) {
                                            dragChannel.close()
                                            scope.launch {
                                                dragJob.join()
                                                val velocity = velocityTracker.calculateVelocity().y
                                                draggableState.settle(0f)
                                            }
                                            break
                                        }

                                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                                        val delta = change.positionChange()
                                        change.consume()
                                        dragChannel.trySend(delta.y)
                                    }
                                } catch (e: Exception) {
                                    dragChannel.close()
                                    scope.launch {
                                        try {
                                            dragJob.join()
                                            draggableState.settle(0f) // velocity param deprecated; using 0f
                                        } catch (_: Exception) {}
                                    }
                                }
                            } else {
                                // Horizontal drag for skip next/prev
                                dragX = overSlop
                                try {
                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                        val change = event.changes.firstOrNull { it.id == down.id }
                                        if (change == null || !change.pressed) {
                                            val velocity = velocityTracker.calculateVelocity()
                                            val vx = velocity.x
                                            Log.d("MiniPlayerGesture", "Horizontal drag ended. DragX=$dragX, VelocityX=$vx")

                                            val skipByDistance = abs(dragX) > skipThreshold
                                            val skipByFling = abs(vx) > horizontalFlingVelocityThreshold

                                            if (skipByDistance || skipByFling) {
                                                val effectiveDirection = if (skipByDistance) dragX else vx
                                                if (effectiveDirection < 0) {
                                                    Log.d("MiniPlayerGesture", "Skip → NEXT")
                                                    onNext()
                                                } else {
                                                    Log.d("MiniPlayerGesture", "Skip → PREV")
                                                    onPrev()
                                                }
                                            }
                                            break
                                        }
                                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                                        val delta = change.positionChange()
                                        change.consume()
                                        dragX += delta.x
                                    }
                                } catch (_: Exception) {
                                    Log.d("MiniPlayerGesture", "Horizontal gesture cancelled. No action.")
                                }
                            }
                        }
                    }
                }
        ) {
            MiniPlayerContent(
                sheetState = sheetState,
                currentAnchor = draggableState.currentValue,
                progress =
                if (draggableState.offset.isNaN()) {
                    0f
                } else {
                    // Calculate progress from collapsed (0f) to expanded (1f)
                    val totalDist = collapsedAnchor - expandedAnchor
                    if (totalDist > 0) ((collapsedAnchor - draggableState.offset) / totalDist).coerceIn(0f, 1f) else 0f
                },
                onExpand = onExpand,
                onCollapse = onCollapse,
                onHalfExpand = onHalfExpand,
                onNext = onNext,
                onPrev = onPrev,
                onPlayPause = onPlayPause,
                expandedContent = expandedContent,
            )
        }
    }
}
