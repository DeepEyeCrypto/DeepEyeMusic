// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.diagnostics

import android.content.Context
import android.media.audiofx.AudioEffect
import com.deepeye.musicpro.dsp.engine.DSPEngine
import com.deepeye.musicpro.dsp.profile.AudioDeviceType
import com.deepeye.musicpro.dsp.profile.AudioRouteDetector
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class V4ACompatibilityEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dspEngine: DSPEngine,
    private val audioRouteDetector: AudioRouteDetector
) {
    /**
     * Checks if a package name exists on the device.
     */
    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks if Viper4Android or a similar external equalizer package is present on the device.
     */
    fun isV4AInstalled(): Boolean {
        val v4aPackages = listOf(
            "com.vipercn.viper4android_v2",
            "com.pittvandewitt.viper4android",
            "com.vipercn.viper4android"
        )
        return v4aPackages.any { isPackageInstalled(it) }
    }

    /**
     * Generates a compatibility matrix as a markdown report.
     */
    fun generateCompatibilityMatrix(): String {
        val currentSession = dspEngine.getCurrentSessionId()
        val currentDevice = audioRouteDetector.currentDevice.value
        val dspActive = dspEngine.isAttached()
        val hasV4A = isV4AInstalled()

        val sb = StringBuilder()
        sb.append("# V4A/DSP Route Compatibility Matrix\n\n")
        sb.append("| Route | Session Active? | DSP Active? | V4A Attached? | Status |\n")
        sb.append("|---|---|---|---|---|\n")

        val routes = listOf(
            AudioDeviceType.SPEAKER to "Built-in Speaker",
            AudioDeviceType.BLUETOOTH to "Bluetooth A2DP/SCO",
            AudioDeviceType.WIRED_HEADSET to "Wired Headphones/Jack",
            AudioDeviceType.USB to "USB DAC/Headset"
        )

        for ((type, name) in routes) {
            val isActiveRoute = currentDevice.type == type
            val sessionActive = if (isActiveRoute && currentSession != 0) "✅ YES" else "❌ NO"
            val dspState = if (isActiveRoute && dspActive) "✅ YES" else "❌ NO"
            val v4aState = if (isActiveRoute && currentSession != 0 && hasV4A) "✅ YES" else "❌ NO"
            val statusStr = when {
                isActiveRoute && dspActive && hasV4A -> "🟢 ACTIVE (V4A)"
                isActiveRoute && dspActive -> "🟡 ACTIVE (Native)"
                isActiveRoute -> "🔴 INACTIVE"
                else -> "⚪ IDLE"
            }

            sb.append("| $name | $sessionActive | $dspState | $v4aState | $statusStr |\n")
        }

        sb.append("\n**Current Route**: ${currentDevice.name} (${currentDevice.type.name})\n")
        sb.append("**Viper4Android App Installed**: ${if (hasV4A) "✅ Yes" else "❌ No"}\n")

        return sb.toString()
    }
}
