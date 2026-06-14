package com.deepeye.musicpro.player.controller

import android.app.Activity
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Rational
import androidx.media3.common.Player
import androidx.media3.common.VideoSize

/**
 * Premium Picture-in-Picture engine for DeepEye Music Pro.
 *
 * Features:
 * - Dynamic aspect ratio from video content
 * - Auto-enter on Android 12+ (seamless home button PiP)
 * - Custom remote actions: play/pause, next, previous
 * - Smooth source rect hint for enter/exit animation
 * - Playback state continuity across PiP transitions
 */
class PipEngine(
    private val activity: Activity,
    private val playerController: PlayerController
) {
    companion object {
        private const val ACTION_PLAY_PAUSE = "com.deepeye.musicpro.PIP_PLAY_PAUSE"
        private const val ACTION_NEXT = "com.deepeye.musicpro.PIP_NEXT"
        private const val ACTION_PREVIOUS = "com.deepeye.musicpro.PIP_PREVIOUS"
    }

    private var currentVideoSize: VideoSize? = null
    private var pipActionsReceiver: BroadcastReceiver? = null

    init {
        playerController.player.addListener(object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    currentVideoSize = videoSize
                    updatePipParams()
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    updatePipParams()
                }
            }
        })

        // Register broadcast receiver for PiP custom actions
        registerPipActions()
    }

    private fun registerPipActions() {
        pipActionsReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    ACTION_PLAY_PAUSE -> playerController.togglePlayPause()
                    ACTION_NEXT -> playerController.next()
                    ACTION_PREVIOUS -> playerController.previous()
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(ACTION_PLAY_PAUSE)
            addAction(ACTION_NEXT)
            addAction(ACTION_PREVIOUS)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.registerReceiver(pipActionsReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            activity.registerReceiver(pipActionsReceiver, filter)
        }
    }

    private fun getAspectRatio(): Rational {
        val size = currentVideoSize
        return if (size != null && size.width > 0 && size.height > 0) {
            val ratio = size.width.toFloat() / size.height.toFloat()
            if (ratio in 0.418f..2.39f) {
                Rational(size.width, size.height)
            } else {
                Rational(16, 9)
            }
        } else {
            Rational(16, 9)
        }
    }

    private fun buildRemoteActions(): List<RemoteAction> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return emptyList()

        val isPlaying = playerController.playerState.value.isPlaying

        val prevAction = RemoteAction(
            Icon.createWithResource(activity, android.R.drawable.ic_media_previous),
            "Previous",
            "Previous track",
            PendingIntent.getBroadcast(
                activity, 0,
                Intent(ACTION_PREVIOUS).setPackage(activity.packageName),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        )

        val playPauseAction = RemoteAction(
            Icon.createWithResource(
                activity,
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            ),
            if (isPlaying) "Pause" else "Play",
            if (isPlaying) "Pause playback" else "Resume playback",
            PendingIntent.getBroadcast(
                activity, 1,
                Intent(ACTION_PLAY_PAUSE).setPackage(activity.packageName),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        )

        val nextAction = RemoteAction(
            Icon.createWithResource(activity, android.R.drawable.ic_media_next),
            "Next",
            "Next track",
            PendingIntent.getBroadcast(
                activity, 2,
                Intent(ACTION_NEXT).setPackage(activity.packageName),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        )

        return listOf(prevAction, playPauseAction, nextAction)
    }

    fun enterPipMode() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val state = playerController.playerState.value
        if (!state.isVideo) return

        val params = PictureInPictureParams.Builder()
            .setAspectRatio(getAspectRatio())
            .setActions(buildRemoteActions())
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setAutoEnterEnabled(true)
                    setSeamlessResizeEnabled(true)
                }
            }
            .build()

        activity.enterPictureInPictureMode(params)
    }

    fun updatePipParams(sourceRect: Rect? = null) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(getAspectRatio())
            .setActions(buildRemoteActions())

        if (sourceRect != null) {
            builder.setSourceRectHint(sourceRect)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val state = playerController.playerState.value
            val shouldAutoEnter = state.isVideo && state.isPlaying
            builder.setAutoEnterEnabled(shouldAutoEnter)
            builder.setSeamlessResizeEnabled(true)
        }

        try {
            activity.setPictureInPictureParams(builder.build())
        } catch (e: Exception) {
            android.util.Log.e("PipEngine", "Failed to set PiP params", e)
        }
    }

    fun onUserLeaveHint() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            val state = playerController.playerState.value
            if (state.isVideo && state.isPlaying) {
                enterPipMode()
            }
        }
    }

    fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration?) {
        if (!isInPictureInPictureMode) {
            playerController.setAppInForeground(true)
        }
    }

    fun destroy() {
        try {
            pipActionsReceiver?.let { activity.unregisterReceiver(it) }
        } catch (_: Exception) {}
    }
}
