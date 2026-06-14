// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.diagnostics

import android.util.Log
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.deepeye.musicpro.player.controller.PlayerController
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NowPlayingGuardian @Inject constructor() {
    companion object {
        private const val TAG = "NowPlayingGuardian"
        private const val WATCH_INTERVAL_MS = 4000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    private var playerController: PlayerController? = null
    private var mediaSession: MediaSession? = null
    private var onRepairCallback: (() -> Unit)? = null

    fun initialize(
        controller: PlayerController,
        session: MediaSession,
        repairCallback: () -> Unit
    ) {
        this.playerController = controller
        this.mediaSession = session
        this.onRepairCallback = repairCallback
        startMonitoring()
    }

    private fun startMonitoring() {
        job?.cancel()
        job = scope.launch {
            while (isActive) {
                try {
                    verifyAndRepair()
                } catch (e: Exception) {
                    Log.e(TAG, "Error in NowPlayingGuardian verification", e)
                }
                delay(WATCH_INTERVAL_MS)
            }
        }
        Log.i(TAG, "NowPlayingGuardian monitoring started.")
    }

    fun stopMonitoring() {
        job?.cancel()
        job = null
        Log.i(TAG, "NowPlayingGuardian monitoring stopped.")
    }

    /**
     * Checks if all Now Playing layers match, and auto-repairs any mismatch.
     */
    fun verifyAndRepair() {
        val controller = playerController ?: return
        val session = mediaSession ?: return

        scope.launch(Dispatchers.Main) {
            val isVideo = controller.playerState.value.isVideo
            val uiItem = controller.playerState.value.currentItem
            val exoItem = controller.player.currentMediaItem
            val sessionItem = session.player.currentMediaItem

            if (uiItem == null && exoItem == null) return@launch

            val uiId = uiItem?.id ?: ""
            val exoId = exoItem?.mediaId ?: ""
            val sessionId = sessionItem?.mediaId ?: ""

            val mismatch = uiId != exoId || uiId != sessionId

            if (mismatch) {
                Log.w(TAG, "⚠️ Now Playing metadata mismatch detected! UI: '$uiId', ExoPlayer: '$exoId', MediaSession: '$sessionId'")
                repair(uiId, exoId, sessionId)
            } else {
                Log.d(TAG, "✅ Now Playing metadata matches on all layers: UI/ExoPlayer/MediaSession = '$uiId'")
            }
        }
    }

    private fun repair(uiId: String, exoId: String, sessionId: String) {
        Log.i(TAG, "Running metadata mismatch auto-repair...")
        // Call the service's repair callback to update media session / notification
        onRepairCallback?.invoke()
    }
}
