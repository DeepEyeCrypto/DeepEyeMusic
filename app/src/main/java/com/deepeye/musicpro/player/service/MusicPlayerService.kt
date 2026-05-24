// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.player.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.deepeye.musicpro.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MusicPlayerService"
private const val NOTIFICATION_ID = 1001
private const val CHANNEL_ID = "deepeye_playback_channel"

@AndroidEntryPoint
class MusicPlayerService : MediaSessionService() {

    @Inject lateinit var player: ExoPlayer
    @Inject lateinit var audioSessionManager: com.deepeye.musicpro.dsp.session.AudioSessionManager

    @Inject lateinit var playerController: com.deepeye.musicpro.player.controller.PlayerController

    private var mediaSession: MediaSession? = null
    private val serviceScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: Initializing MusicPlayerService")

        createNotificationChannel()

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val forwardingPlayer = object : androidx.media3.common.ForwardingPlayer(player) {
            private val listeners = java.util.concurrent.CopyOnWriteArraySet<Player.Listener>()
            private val self = this

            init {
                // Track playerState to notify MediaSession of manual play/pause in video mode
                serviceScope.launch {
                    playerController.playerState.collect { state ->
                        if (state.isVideo) {
                            val flags = androidx.media3.common.FlagSet.Builder()
                                .add(Player.EVENT_IS_PLAYING_CHANGED)
                                .add(Player.EVENT_PLAYBACK_STATE_CHANGED)
                                .add(Player.EVENT_PLAY_WHEN_READY_CHANGED)
                                .build()
                            val events = Player.Events(flags)

                            listeners.forEach { listener ->
                                listener.onIsPlayingChanged(state.isPlaying)
                                listener.onPlayWhenReadyChanged(
                                    state.isPlaying,
                                    Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST
                                )
                                listener.onPlaybackStateChanged(
                                    if (state.isPlaying) Player.STATE_READY else Player.STATE_IDLE
                                )
                                listener.onEvents(self, events)
                            }
                        }
                    }
                }
            }

            override fun addListener(listener: Player.Listener) {
                super.addListener(listener)
                listeners.add(listener)
            }

            override fun removeListener(listener: Player.Listener) {
                super.removeListener(listener)
                listeners.remove(listener)
            }

            override fun isPlaying(): Boolean {
                if (playerController.playerState.value.isVideo) {
                    return playerController.playerState.value.isPlaying
                }
                return super.isPlaying()
            }

            override fun getPlayWhenReady(): Boolean {
                if (playerController.playerState.value.isVideo) {
                    return playerController.playerState.value.isPlaying
                }
                return super.getPlayWhenReady()
            }

            override fun getPlaybackState(): Int {
                if (playerController.playerState.value.isVideo) {
                    return if (playerController.playerState.value.isPlaying) Player.STATE_READY else Player.STATE_IDLE
                }
                return super.getPlaybackState()
            }

            override fun play() {
                if (playerController.playerState.value.isVideo) {
                    if (!playerController.playerState.value.isPlaying) {
                        playerController.togglePlayPause()
                    }
                } else {
                    super.play()
                }
            }

            override fun pause() {
                if (playerController.playerState.value.isVideo) {
                    if (playerController.playerState.value.isPlaying) {
                        playerController.togglePlayPause()
                    }
                } else {
                    super.pause()
                }
            }

            override fun getAvailableCommands(): Player.Commands {
                if (playerController.playerState.value.isVideo) {
                    return super.getAvailableCommands().buildUpon()
                        .add(Player.COMMAND_PLAY_PAUSE)
                        .add(Player.COMMAND_SEEK_TO_NEXT)
                        .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                        .build()
                }
                return super.getAvailableCommands()
            }
        }

        mediaSession = MediaSession.Builder(this, forwardingPlayer)
            .setSessionActivity(pendingIntent)
            .build()

        audioSessionManager.attachToPlayer(player)

        player.addListener(object : Player.Listener {
            override fun onMediaMetadataChanged(mediaMetadata: androidx.media3.common.MediaMetadata) {
                showNotification()
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                showNotification()
            }
        })
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "DeepEye Playback",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Music playback controls"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    @OptIn(UnstableApi::class)
    private fun showNotification(title: String? = null, artist: String? = null) {
        val metadata = player.mediaMetadata
        val finalTitle = title ?: metadata.title ?: "DeepEye Music"
        val finalArtist = artist ?: metadata.artist ?: "Streaming..."

        val sessionToken = mediaSession?.sessionCompatToken

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(com.deepeye.musicpro.R.drawable.ic_music_note)
            .setContentTitle(finalTitle)
            .setContentText(finalArtist)
            .setOngoing(player.isPlaying)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (sessionToken != null) {
            notificationBuilder.setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(sessionToken)
            )
        }

        val notification = notificationBuilder.build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID, 
                notification, 
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun artistFromMetadata(metadata: androidx.media3.common.MediaMetadata): String {
        return metadata.artist?.toString() ?: metadata.albumArtist?.toString() ?: "Streaming..."
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showNotification()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        audioSessionManager.detach()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
