package com.deepeye.musicpro.player.service

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.deepeye.musicpro.MainActivity
import com.deepeye.musicpro.dsp.session.AudioSessionManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Foreground service for music playback using Media3 MediaSessionService.
 */
@AndroidEntryPoint
class MusicPlayerService : MediaSessionService() {

    @Inject lateinit var player: ExoPlayer
    @Inject lateinit var audioSessionManager: AudioSessionManager

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        // Create PendingIntent to open the app when notification is clicked
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(pendingIntent)
            .build()

        // Wire DSP engine to the player's audio session
        audioSessionManager.attachToPlayer(player)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        audioSessionManager.detach()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
