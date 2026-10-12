// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.player.visualizer.agsl

import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * ThermalTier — the render policy the AGSL visualizer adopts for a given
 * hardware thermal status.
 *
 * @property targetFps   hard cap on the shader frame rate.
 * @property smoothing   exponential-smoothing factor for the audio-reactive
 *                       uniforms. Smoothing is increased (lower factor) as the
 *                       frame rate drops so the larger per-frame delta does not
 *                       read as judder — this is what "hides the frame drop".
 * @property audioPostFx when false, the per-frame audio-texture upload
 *                       (post-processing) is disabled and a static input is
 *                       bound instead, trading audio reactivity for thermal
 *                       headroom on a hot device.
 */
enum class ThermalTier(
    val targetFps: Int,
    val smoothing: Float,
    val audioPostFx: Boolean
) {
    /** Cool: forced 144 Hz, full resolution, full post-processing. */
    FULL(144, 0.50f, true),

    /** Warm: 60 Hz cap, heavier smoothing to mask the frame drop. */
    CAPPED(60, 0.32f, true),

    /** Hot: 30 Hz cap, heaviest smoothing, audio post-processing disabled. */
    THROTTLED(30, 0.20f, false);

    companion object {
        /**
         * Maps a [PowerManager] thermal-status constant to a [ThermalTier].
         * Unknown / future statuses fall back to [THROTTLED] (fail safe: protect
         * the hardware when the platform reports something we do not model).
         */
        fun fromStatus(status: Int): ThermalTier = when (status) {
            PowerManager.THERMAL_STATUS_NONE,
            PowerManager.THERMAL_STATUS_LIGHT -> FULL
            PowerManager.THERMAL_STATUS_MODERATE -> CAPPED
            else -> THROTTLED
        }
    }
}

/**
 * Remembers the device's current [ThermalTier], bridging Android's
 * [PowerManager.OnThermalStatusChangedListener] (API 29+) into a Compose [State]
 * the render loop can collect.
 *
 * The listener is registered once per composition and removed on dispose; the
 * tier is re-seeded from [PowerManager.getCurrentThermalStatus] on
 * [Lifecycle.Event.ON_RESUME] so a foregrounding app reflects the live heat
 * immediately. On API &lt; 29 there is no thermal API, so the tier stays
 * [ThermalTier.FULL] (preserves the existing 144 Hz behaviour on older devices).
 */
@Composable
fun rememberThermalTier(): State<ThermalTier> {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val tierFlow: MutableStateFlow<ThermalTier> = remember { MutableStateFlow(ThermalTier.FULL) }

    DisposableEffect(context, lifecycleOwner) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        var listener: PowerManager.OnThermalStatusChangedListener? = null

        if (powerManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val thermalListener = PowerManager.OnThermalStatusChangedListener { status ->
                tierFlow.value = ThermalTier.fromStatus(status)
            }
            listener = thermalListener
            // Seed with the current status before subscribing so the first
            // composed frame already reflects real device heat.
            tierFlow.value = ThermalTier.fromStatus(powerManager.currentThermalStatus)
            powerManager.addThermalStatusListener(thermalListener)
        }

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME &&
                powerManager != null &&
                listener != null &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
            ) {
                tierFlow.value = ThermalTier.fromStatus(powerManager.currentThermalStatus)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            if (powerManager != null && listener != null) {
                powerManager.removeThermalStatusListener(listener)
            }
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    return tierFlow.collectAsState()
}
