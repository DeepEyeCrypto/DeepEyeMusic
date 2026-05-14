package com.deepeye.musicpro.dsp.session

import android.util.Log
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.deepeye.musicpro.dsp.engine.V4AEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the audio session lifecycle between ExoPlayer and the V4A DSP engine.
 *
 * Re-attaches the DSP engine whenever the audio session ID changes
 * (e.g., on format switch, output device change).
 */
@Singleton
class AudioSessionManager @Inject constructor(
    private val v4aEngine: V4AEngine
) {
    companion object {
        private const val TAG = "AudioSessionManager"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var currentSessionId: Int = 0

    /**
     * Registers a listener on the ExoPlayer to track audio session ID changes.
     */
    fun attachToPlayer(player: ExoPlayer) {
        // Get initial session ID
        val sessionId = player.audioSessionId
        if (sessionId != 0) {
            onSessionIdChanged(sessionId)
        }

        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                onSessionIdChanged(audioSessionId)
            }
        })

        Log.i(TAG, "Attached to ExoPlayer, initial session: $sessionId")
    }

    /**
     * Detaches from the ExoPlayer and releases the DSP session.
     */
    fun detach() {
        scope.launch {
            v4aEngine.releaseSession()
            currentSessionId = 0
        }
    }

    private fun onSessionIdChanged(newSessionId: Int) {
        if (newSessionId == currentSessionId || newSessionId == 0) return

        currentSessionId = newSessionId
        scope.launch {
            Log.i(TAG, "Audio session changed to $newSessionId — re-attaching DSP")
            v4aEngine.attachSession(newSessionId)
        }
    }
}
