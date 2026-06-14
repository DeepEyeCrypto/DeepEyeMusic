// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.diagnostics

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.media3.exoplayer.ExoPlayer
import com.deepeye.musicpro.dsp.engine.DSPEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutoRegressionShield @Inject constructor(
    @ApplicationContext private val context: Context,
    private val player: ExoPlayer,
    private val dspEngine: DSPEngine
) {
    companion object {
        private const val TAG = "AutoRegressionShield"
    }

    data class CheckResult(
        val name: String,
        val passed: Boolean,
        val details: String
    )

    /**
     * Executes self-diagnostics checks to prevent regression failures.
     */
    fun runValidation(): List<CheckResult> {
        val results = mutableListOf<CheckResult>()

        // 1. Playback Engine Validation
        try {
            val isPlaying = player.isPlaying
            results.add(CheckResult("Playback Engine", true, "ExoPlayer instance resolved. Current play state: isPlaying=$isPlaying"))
        } catch (e: Exception) {
            results.add(CheckResult("Playback Engine", false, "ExoPlayer failed: ${e.message}"))
        }

        // 2. DSP Engine Validation
        try {
            val dspSessionId = dspEngine.getCurrentSessionId()
            val state = dspEngine.engineState.value
            results.add(CheckResult("DSP Engine", true, "DSPEngine state: $state, active session: $dspSessionId"))
        } catch (e: Exception) {
            results.add(CheckResult("DSP Engine", false, "DSPEngine check failed: ${e.message}"))
        }

        // 3. Notification Validation
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channel = manager.getNotificationChannel("deepeye_music_playback")
                if (channel != null) {
                    results.add(CheckResult("Notifications", true, "Notification channel 'deepeye_music_playback' exists, importance=${channel.importance}"))
                } else {
                    results.add(CheckResult("Notifications", false, "Notification channel 'deepeye_music_playback' is missing!"))
                }
            } else {
                results.add(CheckResult("Notifications", true, "Notifications checked (Oreo SDK check skipped)."))
            }
        } catch (e: Exception) {
            results.add(CheckResult("Notifications", false, "Notification verification failed: ${e.message}"))
        }

        Log.i(TAG, "Validation complete. Passed: ${results.count { it.passed }}/${results.size}")
        return results
    }
}
