// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.debug.recovery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.deepeye.musicpro.player.controller.PlayerController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.security.MessageDigest
import javax.inject.Inject

private const val TAG = "RecoveryProbe"

/**
 * Debug-only receiver for recovery test actions.
 * Only active in debug builds via AndroidManifest.xml.
 */
@AndroidEntryPoint
class RecoveryProbeReceiver : BroadcastReceiver() {

    @Inject
    lateinit var playerController: PlayerController

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        const val ACTION_SIMULATE_STREAM_EXPIRY = "com.deepeye.musicpro.action.SIMULATE_STREAM_EXPIRY"
        const val ACTION_START_PROBE = "com.deepeye.musicpro.debug.action.START_RECOVERY_PROBE"
        const val ACTION_DUMP_PROBE = "com.deepeye.musicpro.debug.action.DUMP_RECOVERY_PROBE"
        const val ACTION_TRIGGER_DUPLICATE = "com.deepeye.musicpro.debug.action.TRIGGER_DUPLICATE_EXPIRY"
        const val ACTION_TRIGGER_BUDGET_EXHAUSTION = "com.deepeye.musicpro.debug.action.TRIGGER_BUDGET_EXHAUSTION"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val action = intent.action ?: return
        Log.i(TAG, "Debug action received: $action")

        when (action) {
            ACTION_SIMULATE_STREAM_EXPIRY -> handleSimulateExpiry()
            ACTION_START_PROBE -> handleStartProbe()
            ACTION_DUMP_PROBE -> handleDumpProbe()
            ACTION_TRIGGER_DUPLICATE -> handleDuplicateExpiry()
            ACTION_TRIGGER_BUDGET_EXHAUSTION -> handleBudgetExhaustion()
        }
    }

    private fun handleSimulateExpiry() {
        Log.i(TAG, "Simulating stream expiry (403)...")
        val success = playerController.simulateStreamExpiryForTesting()
        Log.i(TAG, "Simulated stream expiry triggered: $success")
    }

    private fun handleStartProbe() {
        Log.i(TAG, "Recovery probe mode started")
    }

    private fun handleDumpProbe() {
        Log.i(TAG, "=== RECOVERY PROBE DUMP ===")
    }

    private fun handleDuplicateExpiry() {
        Log.i(TAG, "Triggering duplicate expiry test (concurrent requests)")
        scope.launch {
            playerController.simulateStreamExpiryForTesting()
            delay(50)
            playerController.simulateStreamExpiryForTesting()
        }
    }

    private fun handleBudgetExhaustion() {
        Log.i(TAG, "Triggering budget exhaustion test (rapid retries)")
        scope.launch {
            playerController.simulateStreamExpiryForTesting()
            delay(500)
            playerController.simulateStreamExpiryForTesting()
            delay(500)
            playerController.simulateStreamExpiryForTesting()
        }
    }
}

/**
 * Safe hash for media IDs (SHA-256 prefix).
 */
fun String.toSafeHash(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(toByteArray())
    return hashBytes.take(8).joinToString("") { "%02x".format(it) }
}
