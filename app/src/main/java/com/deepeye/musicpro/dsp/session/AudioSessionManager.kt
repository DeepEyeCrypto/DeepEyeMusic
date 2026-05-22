// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.dsp.session
 
import android.util.Log
import androidx.media3.exoplayer.ExoPlayer
import com.deepeye.musicpro.dsp.engine.DSPEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime

/**
 * Manages the audio session lifecycle between ExoPlayer and the DSP engine.
 *
 * Re-attaches the DSP engine whenever the audio session ID changes
 * (e.g., on format switch, output device change).
 */
@Singleton
class AudioSessionManager @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val dspEngine: DSPEngine,
    private val visualizerEngine: com.deepeye.musicpro.player.visualizer.VisualizerEngine
) : AnalyticsListener {
    companion object {
        private const val TAG = "AudioSessionManager"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var currentSessionId: Int = 0

    /**
     * Registers a listener on the ExoPlayer to track audio session ID changes.
     */
    fun attachToPlayer(player: ExoPlayer) {
        Log.e(TAG, "attachToPlayer called with player: $player")
        
        // Register as AnalyticsListener for more robust session tracking
        player.addAnalyticsListener(this)
        
        // Initial check
        val sessionId = player.audioSessionId
        if (sessionId != 0) {
            handleSessionChange(sessionId)
        }
        
        Log.i(TAG, "Attached to ExoPlayer as AnalyticsListener, initial session: $sessionId")
    }

    override fun onAudioSessionIdChanged(eventTime: EventTime, audioSessionId: Int) {
        Log.e(TAG, "onAudioSessionIdChanged (Analytics): $audioSessionId")
        handleSessionChange(audioSessionId)
    }

    private fun handleSessionChange(newSessionId: Int) {
        if (newSessionId == 0 || newSessionId == currentSessionId) return
        
        Log.e(TAG, "Handling session change: $currentSessionId -> $newSessionId")
        currentSessionId = newSessionId
        
        // Broadcast session open to the system
        val intent = android.content.Intent(android.media.audiofx.AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
            putExtra(android.media.audiofx.AudioEffect.EXTRA_AUDIO_SESSION, newSessionId)
            putExtra(android.media.audiofx.AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
            putExtra(android.media.audiofx.AudioEffect.EXTRA_CONTENT_TYPE, android.media.audiofx.AudioEffect.CONTENT_TYPE_MUSIC)
        }
        context.sendBroadcast(intent)

        scope.launch {
            dspEngine.releaseSession()
            visualizerEngine.release()
            
            dspEngine.attachSession(newSessionId)
            visualizerEngine.start(newSessionId)
        }
    }

    /**
     * Detaches from the ExoPlayer and releases the DSP session.
     */
    fun detach() {
        scope.launch {
            dspEngine.releaseSession()
            visualizerEngine.release()
            currentSessionId = 0
        }
    }
}
