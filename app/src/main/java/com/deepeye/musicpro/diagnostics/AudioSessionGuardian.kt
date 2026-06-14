// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.diagnostics

import android.util.Log
import androidx.media3.exoplayer.ExoPlayer
import com.deepeye.musicpro.dsp.engine.DSPEngine
import com.deepeye.musicpro.dsp.session.AudioSessionManager
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioSessionGuardian @Inject constructor(
    private val dspEngine: DSPEngine,
    private val audioSessionManager: AudioSessionManager
) {
    companion object {
        private const val TAG = "AudioSessionGuardian"
        private const val WATCH_INTERVAL_MS = 3000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    /**
     * Starts continuous monitoring of the ExoPlayer audio session.
     */
    fun startMonitoring(player: ExoPlayer) {
        job?.cancel()
        job = scope.launch {
            while (isActive) {
                try {
                    verifySession(player)
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking audio session", e)
                }
                delay(WATCH_INTERVAL_MS)
            }
        }
        Log.i(TAG, "AudioSessionGuardian monitoring started.")
    }

    /**
     * Stop monitoring.
     */
    fun stopMonitoring() {
        job?.cancel()
        job = null
        Log.i(TAG, "AudioSessionGuardian monitoring stopped.")
    }

    private fun verifySession(player: ExoPlayer) {
        // Run verify on Main thread as ExoPlayer methods must be called on application main thread.
        scope.launch(Dispatchers.Main) {
            val isPlaying = player.isPlaying
            val sessionId = player.audioSessionId
            val dspSessionId = dspEngine.getCurrentSessionId()

            if (isPlaying) {
                if (sessionId == 0) {
                    Log.w(TAG, "⚠️ ExoPlayer is playing but AudioSessionId is 0!")
                    // Attempt soft recovery by re-preparing the player if session remains 0
                } else if (sessionId != dspSessionId) {
                    Log.w(TAG, "⚠️ DSP session mismatch! ExoPlayer: $sessionId, DSP Engine: $dspSessionId. Triggering reattach.")
                    // Reattach DSP engine to the current session ID
                    audioSessionManager.handleSessionChange(sessionId)
                } else {
                    Log.d(TAG, "✅ Audio session verified: $sessionId is attached to DSP.")
                }
            }
        }
    }
}
