// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.orchestrator

import android.content.Context
import android.os.PowerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Non-Compose bridge from Android's [PowerManager] thermal status into a
 * [StateFlow] the orchestrator can collect from any coroutine context.
 *
 * Maps the platform's coarse [PowerManager] status onto the orchestrator's
 * 4-tier [ThermalTier] enum (COOL/WARM/HOT/CRITICAL), which is what the
 * bounded-concurrency and prefetch gates consume.
 */
@Singleton
class ThermalGovernor @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val powerManager: PowerManager by lazy {
        context.applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
    }

    private val _tier = MutableStateFlow(currentTier)
    val tier: StateFlow<ThermalTier> = _tier.asStateFlow()

    val currentTier: ThermalTier get() = mapStatus(powerManager.currentThermalStatus)

    fun observeThermalTier(): StateFlow<ThermalTier> = tier

    fun refresh() {
        _tier.value = mapStatus(powerManager.currentThermalStatus)
    }

    private fun mapStatus(status: Int): ThermalTier = when (status) {
        PowerManager.THERMAL_STATUS_NONE,
        PowerManager.THERMAL_STATUS_LIGHT -> ThermalTier.COOL
        PowerManager.THERMAL_STATUS_MODERATE -> ThermalTier.WARM
        PowerManager.THERMAL_STATUS_SEVERE,
        PowerManager.THERMAL_STATUS_CRITICAL,
        PowerManager.THERMAL_STATUS_EMERGENCY,
        PowerManager.THERMAL_STATUS_SHUTDOWN -> ThermalTier.HOT
        else -> ThermalTier.COOL
    }
}