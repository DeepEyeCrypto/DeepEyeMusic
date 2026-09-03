// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.debug.recovery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.deepeye.musicpro.player.controller.PlayerController
import com.deepeye.musicpro.player.recovery.PlaybackRecoveryCoordinator
import com.deepeye.musicpro.player.smarttube.SmartTubePlaybackFormatRepository
import com.deepeye.musicpro.dsp.profile.DspProfileManager
import com.deepeye.musicpro.dsp.engine.DSPEngine
import com.deepeye.musicpro.player.queue.QueueManager
import java.security.MessageDigest

private const val TAG = "RecoveryProbe"

/**
 * Debug-only receiver for recovery test actions.
 * Only active in debug builds via AndroidManifest.xml.
 */
class RecoveryProbeReceiver : BroadcastReceiver() {
    
    companion object {
        const val ACTION_START_PROBE = "com.deepeye.musicpro.debug.action.START_RECOVERY_PROBE"
        const val ACTION_DUMP_PROBE = "com.deepeye.musicpro.debug.action.DUMP_RECOVERY_PROBE"
        const val ACTION_TRIGGER_DUPLICATE = "com.deepeye.musicpro.debug.action.TRIGGER_DUPLICATE_EXPIRY"
        const val ACTION_TRIGGER_BUDGET_EXHAUSTION = "com.deepeye.musicpro.debug.action.TRIGGER_BUDGET_EXHAUSTION"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        
        val action = intent.action ?: return
        Log.d(TAG, "Debug action received: $action")
        
        when (action) {
            ACTION_START_PROBE -> handleStartProbe(context)
            ACTION_DUMP_PROBE -> handleDumpProbe(context)
            ACTION_TRIGGER_DUPLICATE -> handleDuplicateExpiry(context)
            ACTION_TRIGGER_BUDGET_EXHAUSTION -> handleBudgetExhaustion(context)
        }
    }
    
    private fun handleStartProbe(context: Context) {
        // Capture baseline state via dependency injection would happen here
        // For now, just log that probe mode is active
        Log.d(TAG, "Recovery probe mode started")
    }
    
    private fun handleDumpProbe(context: Context) {
        // Output would go to logcat for ADB capture
        Log.d(TAG, "=== RECOVERY PROBE DUMP ===")
        Log.d(TAG, "Probe dump available via dumpsys in production")
    }
    
    private fun handleDuplicateExpiry(context: Context) {
        Log.d(TAG, "Triggering duplicate expiry test")
        // Would send two broadcasts with minimal delay
        val duplicateIntent = Intent("com.deepeye.musicpro.action.SIMULATE_STREAM_EXPIRY")
        context.sendBroadcast(duplicateIntent)
    }
    
    private fun handleBudgetExhaustion(context: Context) {
        Log.d(TAG, "Triggering budget exhaustion test")
        // Would send second broadcast before 60s reset window
        val budgetIntent = Intent("com.deepeye.musicpro.action.SIMULATE_STREAM_EXPIRY")
        context.sendBroadcast(budgetIntent)
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