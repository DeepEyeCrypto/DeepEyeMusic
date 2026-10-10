// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.features.visualizer.input

import kotlinx.coroutines.flow.StateFlow

/**
 * VisualizerInputHub — Abstract gateway for visualizer data sources.
 */
interface VisualizerInputHub {
    val isMicActive: StateFlow<Boolean>
    fun setMicInput(enabled: Boolean)
    fun setFileInput(path: String?)
    fun setMidiInput(enabled: Boolean)
}
