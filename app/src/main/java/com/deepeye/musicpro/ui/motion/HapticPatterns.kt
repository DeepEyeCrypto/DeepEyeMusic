// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.motion

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import android.os.Vibrator
import android.os.VibratorManager
import android.os.VibrationEffect
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf

/**
 * Premium haptic feedback patterns for DeepEye Music Pro.
 */
class PremiumHaptics(private val context: Context, private val isEnabled: Boolean) {
    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    fun lightClick() {
        if (!isEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(10)
            }
        } catch (e: Exception) {}
    }

    fun click() {
        if (!isEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(20)
            }
        } catch (e: Exception) {}
    }

    fun heavyClick() {
        if (!isEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(40)
            }
        } catch (e: Exception) {}
    }

    fun slideTick() {
        if (!isEnabled) return
        // Force hard click bypassing any view limits
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(30)
            }
        } catch (e: Exception) {}
    }
}

object HapticPatterns {
    @Suppress("DEPRECATION")
    fun isHapticsDisabled(context: Context): Boolean {
        return try {
            Settings.System.getInt(
                context.contentResolver,
                Settings.System.HAPTIC_FEEDBACK_ENABLED,
                1
            ) == 0
        } catch (e: Exception) {
            false
        }
    }
}

@Composable
fun rememberPremiumHaptics(): PremiumHaptics {
    val context = LocalContext.current
    val disabled = remember { HapticPatterns.isHapticsDisabled(context) }
    // Pass context instead of view
    return remember(context, disabled) { PremiumHaptics(context, !disabled) }
}

/**
 * Adds optimized, premium haptic feedback to a LazyList (LazyColumn/LazyRow) during scrolling.
 * It triggers a subtle slideTick haptic every time a new item crosses the viewport boundary,
 * but only if the user is actively scrolling.
 */
fun Modifier.premiumScrollHaptics(state: LazyListState, thresholdPx: Int = 150): Modifier = composed {
    val haptics = rememberPremiumHaptics()
    
    LaunchedEffect(state) {
        var lastOffset = state.firstVisibleItemScrollOffset
        var accumulated = 0
        snapshotFlow { state.firstVisibleItemScrollOffset }
            .collect { offset ->
                if (state.isScrollInProgress) {
                    val delta = kotlin.math.abs(offset - lastOffset)
                    // Ignore huge jumps caused by index changes
                    if (delta < 1000) {
                        accumulated += delta
                        if (accumulated > thresholdPx) {
                            haptics.slideTick()
                            accumulated = 0
                        }
                    }
                }
                lastOffset = offset
            }
    }
    this
}

/**
 * Adds optimized, premium haptic feedback to a LazyGrid during scrolling.
 */
fun Modifier.premiumScrollHaptics(state: LazyGridState, thresholdPx: Int = 150): Modifier = composed {
    val haptics = rememberPremiumHaptics()
    
    LaunchedEffect(state) {
        var lastOffset = state.firstVisibleItemScrollOffset
        var accumulated = 0
        snapshotFlow { state.firstVisibleItemScrollOffset }
            .collect { offset ->
                if (state.isScrollInProgress) {
                    val delta = kotlin.math.abs(offset - lastOffset)
                    if (delta < 1000) {
                        accumulated += delta
                        if (accumulated > thresholdPx) {
                            haptics.slideTick()
                            accumulated = 0
                        }
                    }
                }
                lastOffset = offset
            }
    }
    this
}

/**
 * Adds optimized, premium haptic feedback to a standard ScrollState.
 * It triggers a tick every time the user scrolls past [thresholdPx].
 */
fun Modifier.premiumScrollHaptics(state: ScrollState, thresholdPx: Int = 300): Modifier = composed {
    val haptics = rememberPremiumHaptics()
    
    LaunchedEffect(state) {
        var lastHapticValue = state.value
        snapshotFlow { state.value }
            .collect { value ->
                if (state.isScrollInProgress && kotlin.math.abs(value - lastHapticValue) > thresholdPx) {
                    haptics.slideTick()
                    lastHapticValue = value
                }
            }
    }
    this
}
