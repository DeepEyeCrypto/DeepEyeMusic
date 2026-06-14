// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.deepeye.musicpro.player.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.MediaStyleNotificationHelper
import com.deepeye.musicpro.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

private const val TAG = "MusicPlayerService"
private const val CHANNEL_ID = "deepeye_music_playback"
private const val ACTION_PLAY = "com.deepeye.musicpro.ACTION_PLAY"
private const val ACTION_PAUSE = "com.deepeye.musicpro.ACTION_PAUSE"
private const val ACTION_PREVIOUS = "com.deepeye.musicpro.ACTION_PREVIOUS"
private const val ACTION_NEXT = "com.deepeye.musicpro.ACTION_NEXT"

/**
 * Media3 MediaSessionService for DeepEye Music Pro.
 *
 * Architecture:
 * - Uses a [ForwardingPlayer] that delegates to ExoPlayer for audio-only mode
 *   and to PlayerController's state for video mode (where ExoPlayer is stopped/idle).
 * - MediaSession is built with this forwarding player so the system UI (lock screen,
 *   notification shade, Android Auto) gets correct state regardless of playback mode.
 * - ForwardingPlayer overrides:
 *   - play/pause → routes to PlayerController in video mode
 *   - isPlaying/playbackState → reflects PlayerController state in video mode
 *   - getMediaMetadata/getCurrentMediaItem → returns metadata from PlayerController state
 *     in video mode (since ExoPlayer has no media loaded for YouTube WebView fallback)
 *   - seekToNext/seekToPrevious → always routes through PlayerController
 */
@AndroidEntryPoint
class MusicPlayerService : MediaSessionService() {
    @Inject lateinit var player: ExoPlayer

    @Inject lateinit var audioSessionManager: com.deepeye.musicpro.dsp.session.AudioSessionManager

    @Inject lateinit var playerController: com.deepeye.musicpro.player.controller.PlayerController
    @Inject lateinit var nowPlayingGuardian: com.deepeye.musicpro.diagnostics.NowPlayingGuardian
    @Inject lateinit var playbackPathEnforcer: com.deepeye.musicpro.diagnostics.PlaybackPathEnforcer
    @Inject lateinit var dspEngine: com.deepeye.musicpro.dsp.engine.DSPEngine

    private var mediaSession: MediaSession? = null
    private val serviceScope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main
    )

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: Initializing MusicPlayerService")

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent =
            android.app.PendingIntent.getActivity(
                this,
                0,
                intent,
                android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT,
            )

        val forwardingPlayer =
            object : androidx.media3.common.ForwardingPlayer(player) {
                override fun seekToNext() {
                    playerController.next()
                }

                override fun seekToPrevious() {
                    playerController.previous()
                }

                override fun seekToNextMediaItem() {
                    playerController.next()
                }

                override fun seekToPreviousMediaItem() {
                    playerController.previous()
                }
            }

        val sessionCallback = object : MediaSession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                return MediaSession.ConnectionResult.accept(
                    androidx.media3.session.SessionCommands.EMPTY,
                    session.player.availableCommands
                )
            }
        }

        mediaSession =
            MediaSession.Builder(this, forwardingPlayer)
                .setSessionActivity(pendingIntent)
                .setCallback(sessionCallback)
                .build()

        addSession(requireNotNull(mediaSession))

        // Initialize Now Playing Guardian
        nowPlayingGuardian.initialize(playerController, requireNotNull(mediaSession)) {
            Log.d(TAG, "NowPlayingGuardian repair callback: Syncing metadata and refreshing notification.")
            mediaSession?.let { session ->
                onUpdateNotification(session, true)
            }
        }

        // Apply Speed and Pitch from DSPEngine
        serviceScope.launch {
            dspEngine.currentParams.collect { params ->
                val currentPlaybackParams = player.playbackParameters
                if (currentPlaybackParams.speed != params.playbackSpeed || currentPlaybackParams.pitch != params.playbackPitch) {
                    player.playbackParameters = androidx.media3.common.PlaybackParameters(
                        params.playbackSpeed,
                        params.playbackPitch
                    )
                }
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        nowPlayingGuardian.stopMonitoring()
        audioSessionManager.detach()
        mediaSession?.run {
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> playerController.togglePlayPause()
            ACTION_PAUSE -> playerController.togglePlayPause()
            ACTION_PREVIOUS -> playerController.previous()
            ACTION_NEXT -> playerController.next()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        super.onUpdateNotification(session, startInForegroundRequired)
    }
}
