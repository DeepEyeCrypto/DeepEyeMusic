// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.deepeye.musicpro.dsp.session

import android.util.Log
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime
import com.deepeye.musicpro.dsp.engine.DSPEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the audio session lifecycle between ExoPlayer and the DSP engine.
 *
 * Re-attaches the DSP engine whenever the audio session ID changes
 * (e.g., on format switch, output device change).
 */
@Singleton
class AudioSessionManager
@Inject
constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val dspEngine: DSPEngine,
    private val visualizerEngine: com.deepeye.musicpro.player.visualizer.VisualizerEngine,
) : AnalyticsListener {
    companion object {
        private const val TAG = "AudioSessionManager"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var currentSessionId: Int = 0
    private var debounceJob: kotlinx.coroutines.Job? = null

    /**
     * Registers a listener on the ExoPlayer to track audio session ID changes.
     */
    fun attachToPlayer(player: ExoPlayer) {
        Log.d(TAG, "attachToPlayer called with player: $player")

        // Register as AnalyticsListener for more robust session tracking
        player.addAnalyticsListener(this)

        // Initial check
        val sessionId = player.audioSessionId
        if (sessionId != 0) {
            handleSessionChange(sessionId)
        }

        Log.i(TAG, "Attached to ExoPlayer as AnalyticsListener, initial session: $sessionId")
    }

    override fun onAudioSessionIdChanged(
        eventTime: EventTime,
        audioSessionId: Int,
    ) {
        Log.d(TAG, "onAudioSessionIdChanged (Analytics): $audioSessionId")
        handleSessionChange(audioSessionId)
    }

    fun handleSessionChange(newSessionId: Int, force: Boolean = false) {
        if (newSessionId == 0) return
        
        val isSameSession = (newSessionId == currentSessionId)
        if (isSameSession && !force) return

        debounceJob?.cancel()
        debounceJob = scope.launch {
            kotlinx.coroutines.delay(100) // Debounce rapid flashes
            
            // If session is unchanged and already attached to the DSP engine, skip redundant re-attach
            if (isSameSession && currentSessionId != 0 && dspEngine.getCurrentSessionId() == currentSessionId) {
                Log.d(TAG, "Session remains unchanged ($newSessionId). Skipping redundant re-attach.")
                return@launch
            }

            Log.d(TAG, "Handling session change: $currentSessionId -> $newSessionId")
            
            // Broadcast CLOSE for the old session before opening the new one
            if (currentSessionId != 0 && currentSessionId != newSessionId) {
                val closeIntent = android.content.Intent(
                    android.media.audiofx.AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION
                ).apply {
                    putExtra(android.media.audiofx.AudioEffect.EXTRA_AUDIO_SESSION, currentSessionId)
                    putExtra(android.media.audiofx.AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                }
                context.sendBroadcast(closeIntent)
            }

            val sessionChanged = (currentSessionId != newSessionId)
            currentSessionId = newSessionId

            if (sessionChanged) {
                // Broadcast session open to the system
                val intent =
                    android.content.Intent(
                        android.media.audiofx.AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION
                    ).apply {
                        putExtra(android.media.audiofx.AudioEffect.EXTRA_AUDIO_SESSION, newSessionId)
                        putExtra(android.media.audiofx.AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                        putExtra(
                            android.media.audiofx.AudioEffect.EXTRA_CONTENT_TYPE,
                            android.media.audiofx.AudioEffect.CONTENT_TYPE_MUSIC
                        )
                    }
                context.sendBroadcast(intent)
            }

            if (sessionChanged || dspEngine.getCurrentSessionId() != newSessionId) {
                dspEngine.releaseSession()
                visualizerEngine.release()

                dspEngine.attachSession(newSessionId, force = force)
                visualizerEngine.start(newSessionId)
            }
        }
    }

    /**
     * Forces the DSP to re-attach to the current session ID, ignoring any caching.
     * Use this when the AudioTrack is recreated but the session ID remains the same.
     */
    fun forceReattach() {
        if (currentSessionId != 0) {
            Log.d(TAG, "forceReattach() called for session $currentSessionId")
            handleSessionChange(currentSessionId, force = true)
        }
    }

    /**
     * Detaches from the ExoPlayer and releases the DSP session.
     */
    fun detach() {
        debounceJob?.cancel()
        scope.launch {
            if (currentSessionId != 0) {
                val closeIntent = android.content.Intent(
                    android.media.audiofx.AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION
                ).apply {
                    putExtra(android.media.audiofx.AudioEffect.EXTRA_AUDIO_SESSION, currentSessionId)
                    putExtra(android.media.audiofx.AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                }
                context.sendBroadcast(closeIntent)
            }
            dspEngine.releaseSession()
            visualizerEngine.release()
            currentSessionId = 0
        }
    }
}
