// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.player.overlay

import com.deepeye.musicpro.domain.model.RepeatMode

/**
 * UI action adapter for the SmartTube/Kodi/VLC-inspired video player overlay.
 * Every method delegates to an existing current-tree callback.
 * Unsupported actions are hidden or rendered disabled — never pretended to succeed.
 */
interface VideoPlayerOverlayActions {
    fun playPause()
    fun previous()
    fun next()
    fun toggleRepeat()
    fun openSpeed()
    fun openPipOrBackgroundPlay()
    fun skipSegment()
    fun openQueue()
    fun openSearch()
    fun seekStarted()
    fun seekChanged(positionMs: Long)
    fun seekFinished(positionMs: Long)
    fun openQuality()
    fun openAudioTrack()
    fun toggleLike()
    fun toggleDislike()
    fun toggleCaptions()
    fun addToPlaylist()
    fun openChannel()
    fun openInfo()
    fun openStats()
    fun dismiss()

    // Additional VLC / Kodi / SmartTube actions
    fun rewind10() {}
    fun forward10() {}
    fun toggleMute() {}
    fun cycleAspectRatio() {}
    fun toggleLock() {}
    fun openSleepTimer() {}
    fun openAudioEqualizer() {}

    companion object {
        fun fromLambdas(
            playPause: () -> Unit = {},
            previous: () -> Unit = {},
            next: () -> Unit = {},
            toggleRepeat: () -> Unit = {},
            openSpeed: () -> Unit = {},
            openPipOrBackgroundPlay: () -> Unit = {},
            skipSegment: () -> Unit = {},
            openQueue: () -> Unit = {},
            openSearch: () -> Unit = {},
            seekStarted: () -> Unit = {},
            seekChanged: (Long) -> Unit = {},
            seekFinished: (Long) -> Unit = {},
            openQuality: () -> Unit = {},
            openAudioTrack: () -> Unit = {},
            toggleLike: () -> Unit = {},
            toggleDislike: () -> Unit = {},
            toggleCaptions: () -> Unit = {},
            addToPlaylist: () -> Unit = {},
            openChannel: () -> Unit = {},
            openInfo: () -> Unit = {},
            openStats: () -> Unit = {},
            dismiss: () -> Unit = {},
            rewind10: () -> Unit = {},
            forward10: () -> Unit = {},
            toggleMute: () -> Unit = {},
            cycleAspectRatio: () -> Unit = {},
            toggleLock: () -> Unit = {},
            openSleepTimer: () -> Unit = {},
            openAudioEqualizer: () -> Unit = {}
        ): VideoPlayerOverlayActions = object : VideoPlayerOverlayActions {
            override fun playPause() = playPause()
            override fun previous() = previous()
            override fun next() = next()
            override fun toggleRepeat() = toggleRepeat()
            override fun openSpeed() = openSpeed()
            override fun openPipOrBackgroundPlay() = openPipOrBackgroundPlay()
            override fun skipSegment() = skipSegment()
            override fun openQueue() = openQueue()
            override fun openSearch() = openSearch()
            override fun seekStarted() = seekStarted()
            override fun seekChanged(positionMs: Long) = seekChanged(positionMs)
            override fun seekFinished(positionMs: Long) = seekFinished(positionMs)
            override fun openQuality() = openQuality()
            override fun openAudioTrack() = openAudioTrack()
            override fun toggleLike() = toggleLike()
            override fun toggleDislike() = toggleDislike()
            override fun toggleCaptions() = toggleCaptions()
            override fun addToPlaylist() = addToPlaylist()
            override fun openChannel() = openChannel()
            override fun openInfo() = openInfo()
            override fun openStats() = openStats()
            override fun dismiss() = dismiss()
            override fun rewind10() = rewind10()
            override fun forward10() = forward10()
            override fun toggleMute() = toggleMute()
            override fun cycleAspectRatio() = cycleAspectRatio()
            override fun toggleLock() = toggleLock()
            override fun openSleepTimer() = openSleepTimer()
            override fun openAudioEqualizer() = openAudioEqualizer()
        }
    }
}
