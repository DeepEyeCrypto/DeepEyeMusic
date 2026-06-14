// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.dp

/**
 * Centralized motion design tokens for DeepEye Music Pro.
 *
 * All animations across the app should reference these constants
 * to ensure consistent, premium-feeling motion.
 */
object MotionTokens {

    // ═══════════════════════════════════════════════════
    // Easing Curves
    // ═══════════════════════════════════════════════════

    /** Standard Material easing — used for most transitions. */
    val StandardEasing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)

    /** Decelerate easing — used for elements entering the screen. */
    val DecelerateEasing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)

    /** Accelerate easing — used for elements leaving the screen. */
    val AccelerateEasing = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)

    /** Premium easing — slightly more dramatic for hero transitions. */
    val PremiumEasing = CubicBezierEasing(0.16f, 1.0f, 0.3f, 1.0f)

    // ═══════════════════════════════════════════════════
    // Spring Profiles
    // ═══════════════════════════════════════════════════

    /** Expand/collapse spring — used for sheet, mini-player, panels. */
    val ExpandCollapseSpring = spring<Float>(
        stiffness = 300f,
        dampingRatio = 0.7f,
    )

    /** Artwork transition spring — used for album art crossfade/scale. */
    val ArtworkTransitionSpring = spring<Float>(
        stiffness = 400f,
        dampingRatio = 0.8f,
    )

    /** Snappy spring — used for quick UI responses (buttons, toggles). */
    val SnappySpring = spring<Float>(
        stiffness = Spring.StiffnessMediumLow,
        dampingRatio = Spring.DampingRatioNoBouncy,
    )

    /** Bouncy spring — used for playful elements (bass ring, visualizer). */
    val BouncySpring = spring<Float>(
        stiffness = Spring.StiffnessLow,
        dampingRatio = 0.6f,
    )

    // ═══════════════════════════════════════════════════
    // Durations (ms)
    // ═══════════════════════════════════════════════════

    /** Palette color transition duration. */
    const val PaletteTransitionMs = 600

    /** Album artwork crossfade duration. */
    const val ArtworkCrossfadeMs = 400

    /** Video/audio control overlay fade duration. */
    const val ControlFadeMs = 200

    /** Screen navigation transition duration. */
    const val NavigationTransitionMs = 420

    /** Tab bar shrink/expand animation duration. */
    const val TabBarAnimationMs = 350

    /** Skeleton shimmer cycle duration. */
    const val ShimmerCycleMs = 1200

    // ═══════════════════════════════════════════════════
    // Thresholds
    // ═══════════════════════════════════════════════════

    /** Scroll offset (dp) at which tab bar starts shrinking. */
    val TabBarScrollThreshold = 100.dp

    /** Tab bar full height. */
    val TabBarHeight = 56.dp

    /** Mini player collapsed height. */
    val MiniPlayerHeight = 88.dp

    // ═══════════════════════════════════════════════════
    // Tween Specs (pre-built for common use)
    // ═══════════════════════════════════════════════════

    /** Standard tween with premium easing. */
    fun <T> standardTween(durationMs: Int = NavigationTransitionMs) = tween<T>(
        durationMillis = durationMs,
        easing = StandardEasing,
    )

    /** Fast tween for micro-interactions. */
    fun <T> fastTween() = tween<T>(
        durationMillis = ControlFadeMs,
        easing = StandardEasing,
    )

    /** Palette transition tween. */
    fun <T> paletteTween() = tween<T>(
        durationMillis = PaletteTransitionMs,
        easing = StandardEasing,
    )
}
