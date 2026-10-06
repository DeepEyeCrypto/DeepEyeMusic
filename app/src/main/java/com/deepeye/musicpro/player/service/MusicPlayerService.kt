// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.deepeye.musicpro.player.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionResult
import coil3.BitmapImage
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import com.deepeye.musicpro.MainActivity
import com.deepeye.musicpro.domain.model.MediaItem
import com.deepeye.musicpro.domain.model.toMedia3Item
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import javax.inject.Inject

private const val TAG = "MusicPlayerService"
private const val ACTION_PLAY = "com.deepeye.musicpro.ACTION_PLAY"
private const val ACTION_PAUSE = "com.deepeye.musicpro.ACTION_PAUSE"
private const val ACTION_PREVIOUS = "com.deepeye.musicpro.ACTION_PREVIOUS"
private const val ACTION_NEXT = "com.deepeye.musicpro.ACTION_NEXT"

private const val ROOT_ID = "DEEPEYE_MEDIA_ROOT"
private const val QUEUE_ID = "DEEPEYE_MEDIA_QUEUE"
private const val RECENT_ID = "DEEPEYE_MEDIA_RECENT"

/**
 * Media3 MediaLibraryService for DeepEye Music Pro.
 *
 * Architecture:
 * - Implements [MediaLibraryService] for full Android Auto, WearOS, Lockscreen, and Automotive head unit integration.
 * - Manages [MediaLibrarySession] with root browsing hierarchy (Queue, Recents, Algorithmic Mixes).
 * - ForwardingPlayer guarantees correct playback state, seek next/prev across audio and video modes.
 * - Hardware steering wheel media buttons are intercepted and routed to PlayerController.
 * - Album artwork is fetched via Coil, downscaled to 512x512, and cached into MediaMetadata artworkData.
 */
@AndroidEntryPoint
class MusicPlayerService : MediaLibraryService() {

    @Inject lateinit var player: ExoPlayer
    @Inject lateinit var audioSessionManager: com.deepeye.musicpro.dsp.session.AudioSessionManager
    @Inject lateinit var playerController: com.deepeye.musicpro.player.controller.PlayerController
    @Inject lateinit var nowPlayingGuardian: com.deepeye.musicpro.diagnostics.NowPlayingGuardian
    @Inject lateinit var playbackPathEnforcer: com.deepeye.musicpro.diagnostics.PlaybackPathEnforcer
    @Inject lateinit var dspEngine: com.deepeye.musicpro.dsp.engine.DSPEngine

    private var mediaLibrarySession: MediaLibrarySession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var currentArtworkUri: Uri? = null
    private var currentArtworkBytes: ByteArray? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: Initializing MediaLibraryService for Android Auto & Media3 Ecosystem")

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val forwardingPlayer = object : androidx.media3.common.ForwardingPlayer(player) {
            override fun getAvailableCommands(): Player.Commands {
                return super.getAvailableCommands().buildUpon()
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .build()
            }

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

        val librarySessionCallback = object : MediaLibrarySession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                val availableSessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                    .build()
                return MediaSession.ConnectionResult.accept(
                    availableSessionCommands,
                    session.player.availableCommands
                )
            }

            override fun onGetLibraryRoot(
                session: MediaLibrarySession,
                controller: MediaSession.ControllerInfo,
                params: MediaLibraryService.LibraryParams?
            ): ListenableFuture<LibraryResult<Media3Item>> {
                val rootMetadata = MediaMetadata.Builder()
                    .setTitle("DeepEye Music")
                    .setIsPlayable(false)
                    .setIsBrowsable(true)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                    .build()

                val rootItem = Media3Item.Builder()
                    .setMediaId(ROOT_ID)
                    .setMediaMetadata(rootMetadata)
                    .build()

                return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
            }

            override fun onGetChildren(
                session: MediaLibrarySession,
                controller: MediaSession.ControllerInfo,
                parentId: String,
                page: Int,
                pageSize: Int,
                params: MediaLibraryService.LibraryParams?
            ): ListenableFuture<LibraryResult<ImmutableList<Media3Item>>> {
                val items = mutableListOf<Media3Item>()

                when (parentId) {
                    ROOT_ID -> {
                        items.add(
                            Media3Item.Builder()
                                .setMediaId(QUEUE_ID)
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setTitle("Current Queue")
                                        .setIsPlayable(false)
                                        .setIsBrowsable(true)
                                        .setMediaType(MediaMetadata.MEDIA_TYPE_PLAYLIST)
                                        .build()
                                )
                                .build()
                        )
                        items.add(
                            Media3Item.Builder()
                                .setMediaId(RECENT_ID)
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setTitle("Recent History")
                                        .setIsPlayable(false)
                                        .setIsBrowsable(true)
                                        .setMediaType(MediaMetadata.MEDIA_TYPE_PLAYLIST)
                                        .build()
                                )
                                .build()
                        )
                    }
                    QUEUE_ID, RECENT_ID -> {
                        val currentItem = playerController.playerState.value.currentItem
                        if (currentItem != null) {
                            items.add(currentItem.toMedia3Item())
                        }
                    }
                }

                return Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.copyOf(items), params))
            }

            override fun onGetItem(
                session: MediaLibrarySession,
                controller: MediaSession.ControllerInfo,
                mediaId: String
            ): ListenableFuture<LibraryResult<Media3Item>> {
                val currentItem = playerController.playerState.value.currentItem
                if (currentItem != null && currentItem.id == mediaId) {
                    return Futures.immediateFuture(LibraryResult.ofItem(currentItem.toMedia3Item(), null))
                }

                val fallback = Media3Item.Builder()
                    .setMediaId(mediaId)
                    .setMediaMetadata(MediaMetadata.Builder().setTitle("Media").build())
                    .build()
                return Futures.immediateFuture(LibraryResult.ofItem(fallback, null))
            }

            override fun onPlaybackResumption(
                mediaSession: MediaSession,
                controller: MediaSession.ControllerInfo
            ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
                val currentItem = playerController.playerState.value.currentItem
                if (currentItem != null) {
                    return Futures.immediateFuture(
                        MediaSession.MediaItemsWithStartPosition(
                            listOf(currentItem.toMedia3Item()),
                            0,
                            playerController.playerState.value.position
                        )
                    )
                }
                return super.onPlaybackResumption(mediaSession, controller)
            }

            override fun onMediaButtonEvent(
                session: MediaSession,
                controllerInfo: MediaSession.ControllerInfo,
                intent: Intent
            ): Boolean {
                @Suppress("DEPRECATION")
                val keyEvent = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
                if (keyEvent != null && keyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.keyCode) {
                        KeyEvent.KEYCODE_MEDIA_NEXT, KeyEvent.KEYCODE_MEDIA_STEP_FORWARD -> {
                            playerController.next()
                            return true
                        }
                        KeyEvent.KEYCODE_MEDIA_PREVIOUS, KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD -> {
                            playerController.previous()
                            return true
                        }
                        KeyEvent.KEYCODE_MEDIA_PLAY -> {
                            if (!playerController.playerState.value.isPlaying) {
                                playerController.togglePlayPause()
                            }
                            return true
                        }
                        KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                            if (playerController.playerState.value.isPlaying) {
                                playerController.togglePlayPause()
                            }
                            return true
                        }
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_HEADSETHOOK -> {
                            playerController.togglePlayPause()
                            return true
                        }
                    }
                }
                return super.onMediaButtonEvent(session, controllerInfo, intent)
            }
        }

        mediaLibrarySession = MediaLibrarySession.Builder(this, forwardingPlayer, librarySessionCallback)
            .setSessionActivity(pendingIntent)
            .build()

        addSession(requireNotNull(mediaLibrarySession))

        // Initialize Now Playing Guardian
        nowPlayingGuardian.initialize(playerController, requireNotNull(mediaLibrarySession)) {
            Log.d(TAG, "NowPlayingGuardian repair callback: Syncing metadata and refreshing notification.")
            mediaLibrarySession?.let { session ->
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

        // Lockscreen & Android Auto Artwork Downloader (512x512 Coil pipeline)
        serviceScope.launch(Dispatchers.IO) {
            playerController.playerState
                .map { it.currentItem?.artworkUri }
                .distinctUntilChanged()
                .collect { artUri ->
                    if (artUri != null && artUri != currentArtworkUri) {
                        currentArtworkUri = artUri
                        currentArtworkBytes = fetchDownscaledArtworkBytes(applicationContext, artUri)
                    }
                }
        }

        // Set custom notification provider to use the app icon for notifications
        val notificationProvider = DefaultMediaNotificationProvider(this).apply {
            setSmallIcon(com.deepeye.musicpro.R.drawable.ic_notification)
        }
        setMediaNotificationProvider(notificationProvider)
    }

    private suspend fun fetchDownscaledArtworkBytes(context: Context, uri: Uri): ByteArray? {
        return try {
            val loader = SingletonImageLoader.get(context)
            val request = ImageRequest.Builder(context)
                .data(uri)
                .size(512, 512)
                .allowHardware(false)
                .build()

            val result = loader.execute(request)
            if (result is SuccessResult) {
                val bitmap = (result.image as? BitmapImage)?.bitmap
                if (bitmap != null) {
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                    stream.toByteArray()
                } else null
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to downscale artwork for Android Auto / Lockscreen: ${e.message}")
            null
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = mediaLibrarySession

    override fun onTaskRemoved(rootIntent: Intent?) {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        nowPlayingGuardian.stopMonitoring()
        audioSessionManager.detach()
        mediaLibrarySession?.run {
            release()
            mediaLibrarySession = null
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
}
