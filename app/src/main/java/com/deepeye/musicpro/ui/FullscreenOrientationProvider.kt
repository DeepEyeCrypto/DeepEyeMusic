// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * CompositionLocal to share fullscreen orientation state across the composable tree.
 * Allows HybridPlayerCard to request fullscreen enter/exit from the hosting Activity.
 */
val LocalFullscreenMode = compositionLocalOf { FullscreenMode() }

class FullscreenMode {
    var isFullscreen by mutableStateOf(false)
        private set

    /** When true, fullscreen exit is blocked (VLC-style lock) */
    var isGestureLocked by mutableStateOf(false)

    /** Callback interface for the Activity to implement */
    var onEnterFullscreen: (Boolean) -> Unit = {}
    var onExitFullscreen: () -> Unit = {}

    fun enter(forceLandscape: Boolean = true) {
        if (isFullscreen) return
        isFullscreen = true
        onEnterFullscreen(forceLandscape)
    }

    fun exit() {
        if (!isFullscreen || isGestureLocked) return
        isFullscreen = false
        onExitFullscreen()
    }

    fun toggle() {
        if (isFullscreen) exit() else enter()
    }
}
