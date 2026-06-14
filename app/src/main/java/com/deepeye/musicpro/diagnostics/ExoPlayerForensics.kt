// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.deepeye.musicpro.diagnostics

import android.util.Log
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.DecoderCounters
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime
import java.util.Collections
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExoPlayerForensics @Inject constructor() : AnalyticsListener {
    companion object {
        private const val TAG = "ExoPlayerForensics"
        private const val MAX_LOGS = 50
    }

    data class PlaybackEvent(
        val timestamp: Long = System.currentTimeMillis(),
        val type: String,
        val description: String
    )

    private val timelineEvents = CopyOnWriteArrayList<PlaybackEvent>()

    private var currentFormat: Format? = null
    private var currentBitrate: Int = 0

    fun getTimeline(): List<PlaybackEvent> = timelineEvents.toList()

    fun clearTimeline() {
        timelineEvents.clear()
    }

    private fun addEvent(type: String, description: String) {
        val event = PlaybackEvent(type = type, description = description)
        timelineEvents.add(event)
        if (timelineEvents.size > MAX_LOGS) {
            timelineEvents.removeAt(0)
        }
        Log.d(TAG, "[$type] $description")
    }

    /**
     * Generates a detailed playback timeline report for Module 106.
     */
    fun generateForensicsReport(): String {
        val sb = StringBuilder()
        sb.append("# ExoPlayer Playback Forensics Timeline\n\n")

        if (timelineEvents.isEmpty()) {
            sb.append("No playback events recorded yet.\n")
            return sb.toString()
        }

        sb.append("| Time | Event Type | Description |\n")
        sb.append("|---|---|---|\n")
        val sdf = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault())
        for (event in timelineEvents) {
            val timeStr = sdf.format(java.util.Date(event.timestamp))
            sb.append("| $timeStr | **${event.type}** | ${event.description} |\n")
        }

        sb.append("\n## Active Codec & Format Info\n")
        currentFormat?.let { fmt ->
            sb.append("- **Mime/Codec**: ${fmt.sampleMimeType ?: "Unknown"}\n")
            sb.append("- **Channels**: ${fmt.channelCount}\n")
            sb.append("- **Sample Rate**: ${fmt.sampleRate} Hz\n")
            sb.append("- **Bitrate**: ${if (currentBitrate > 0) "${currentBitrate / 1000} kbps" else "Unknown"}\n")
        } ?: sb.append("No active format decoded yet.\n")

        return sb.toString()
    }

    override fun onMediaItemTransition(
        eventTime: EventTime,
        mediaItem: MediaItem?,
        reason: Int
    ) {
        val id = mediaItem?.mediaId ?: "None"
        val title = mediaItem?.mediaMetadata?.title ?: "Unknown"
        val reasonStr = when (reason) {
            Player.MEDIA_ITEM_TRANSITION_REASON_AUTO -> "AUTO"
            Player.MEDIA_ITEM_TRANSITION_REASON_SEEK -> "SEEK"
            Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED -> "PLAYLIST_CHANGED"
            Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT -> "REPEAT"
            else -> "UNKNOWN"
        }
        addEvent("TRANSITION", "Switched to media item '$title' ($id), reason=$reasonStr")
    }

    override fun onPlaybackStateChanged(eventTime: EventTime, state: Int) {
        val stateStr = when (state) {
            Player.STATE_IDLE -> "IDLE"
            Player.STATE_BUFFERING -> "BUFFERING"
            Player.STATE_READY -> "READY"
            Player.STATE_ENDED -> "ENDED"
            else -> "UNKNOWN"
        }
        addEvent("STATE_CHANGE", "Player state changed to $stateStr")
    }

    override fun onAudioInputFormatChanged(
        eventTime: EventTime,
        format: Format,
        decoderReuseEvaluation: androidx.media3.exoplayer.DecoderReuseEvaluation?
    ) {
        currentFormat = format
        currentBitrate = format.bitrate
        addEvent("FORMAT_CHANGE", "Codec: ${format.sampleMimeType}, rate: ${format.sampleRate}Hz, channels: ${format.channelCount}")
    }

    override fun onAudioSessionIdChanged(eventTime: EventTime, audioSessionId: Int) {
        addEvent("SESSION_CHANGE", "Audio session ID changed to $audioSessionId")
    }

    override fun onPlayerError(eventTime: EventTime, error: androidx.media3.common.PlaybackException) {
        addEvent("ERROR", "Code: ${error.errorCodeName} (${error.errorCode}), msg: ${error.message}")
    }
}
