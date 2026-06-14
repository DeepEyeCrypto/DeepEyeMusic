// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.deepeye.musicpro.diagnostics

import android.util.Log
import android.webkit.WebView
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.deepeye.musicpro.BuildConfig
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackPathEnforcer @Inject constructor() {
    companion object {
        private const val TAG = "PlaybackPathEnforcer"
    }

    private val activePlayers = Collections.newSetFromMap(ConcurrentHashMap<Player, Boolean>())

    private val pathListener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            Log.e(TAG, "ExoPlayer Source Error detected.", error)
        }
    }

    /**
     * Registers a new player instance and checks for duplicate instances.
     */
    fun registerPlayer(player: Player) {
        activePlayers.add(player)
        player.addListener(pathListener)
        Log.i(TAG, "Registered new Player instance: $player. Total active players: ${activePlayers.size}")
        if (activePlayers.size > 1) {
            Log.e(TAG, "🚨 DUPLICATE PLAYERS DETECTED! Active players in memory: $activePlayers")
        }
    }

    /**
     * Unregisters a player instance.
     */
    fun unregisterPlayer(player: Player) {
        player.removeListener(pathListener)
        activePlayers.remove(player)
        Log.i(TAG, "Unregistered Player instance: $player. Total active players: ${activePlayers.size}")
    }

    /**
     * Gets the number of currently active players.
     */
    fun getActivePlayerCount(): Int = activePlayers.size

    /**
     * Mutes any WebView instances to prevent rogue playback paths (WebView-owned audio).
     */
    fun enforceWebViewMuted(webView: WebView) {
        Log.d(TAG, "Enforcing WebView mute state via JavaScript injection")
        val jsMute = """
            (function() {
                try {
                    var mediaElements = document.querySelectorAll('video, audio');
                    mediaElements.forEach(function(el) {
                        el.muted = true;
                        el.volume = 0;
                    });
                    console.log("PlaybackPathEnforcer: Muted WebView media elements.");
                } catch(e) {
                    console.error("PlaybackPathEnforcer error: " + e.message);
                }
            })();
        """.trimIndent()
        webView.evaluateJavascript(jsMute, null)
    }
}
