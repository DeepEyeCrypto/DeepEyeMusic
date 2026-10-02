// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later
//
// VisualizerTheme — Registry of all visual scenes inspired by vvavy.io
// Each theme maps to a distinct Canvas draw pipeline in VisualizerCanvas.kt.
//
package com.deepeye.musicpro.ui.player.visualizer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Waves
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Sealed class for all available visualizer themes.
 * Each scene carries metadata for the picker BottomSheet.
 */
sealed class VisualizerTheme(
    val id: String,
    val displayName: String,
    val description: String,
    val icon: ImageVector,
    val isPremium: Boolean = false
) {
    // ── Core vvavy.io scenes ── (natively ported)

    data object Triangle : VisualizerTheme(
        id = "triangle",
        displayName = "Triangle",
        description = "Reactive 3D quadtree triangle mesh — bass kicks vertices outward",
        icon = Icons.Default.Category
    )

    data object SpectrumBars : VisualizerTheme(
        id = "spectrum-bars",
        displayName = "Spectrum Bars",
        description = "Frequency bar chart with vertical cyan→magenta gradient",
        icon = Icons.Default.BarChart
    )

    data object Waveform : VisualizerTheme(
        id = "waveform",
        displayName = "Waveform",
        description = "Oscilloscope waveform line across full width",
        icon = Icons.Default.ShowChart
    )

    data object AuraOrb : VisualizerTheme(
        id = "aura-orb",
        displayName = "Aura Orb",
        description = "Glowing radial orb pulsing with bass and treble halos",
        icon = Icons.Default.RadioButtonChecked
    )

    data object VvavyTriangle : VisualizerTheme(
        id = "vvavy-triangle",
        displayName = "Vvavy Triangle",
        description = "Full vvavy.io triangle — quadtree subdivision + chromatic aberration + 3D tumble",
        icon = Icons.Default.Biotech
    )

    data object BeatDropGrid : VisualizerTheme(
        id = "beat-drop-grid",
        displayName = "Beat Grid",
        description = "Glowing dot matrix grid that pulses on beat drops",
        icon = Icons.Default.GridView
    )

    data object FrequencyWaves : VisualizerTheme(
        id = "frequency-waves",
        displayName = "Frequency Waves",
        description = "Stacked undulating frequency band curves",
        icon = Icons.Default.Waves
    )

    data object CircularEQ : VisualizerTheme(
        id = "circular-eq",
        displayName = "Circular EQ",
        description = "Radial equalizer bars arranged in a circle, reacting to each frequency band",
        icon = Icons.Default.GraphicEq
    )

    companion object {
        /** All available themes in display order */
        val all: List<VisualizerTheme> = listOf(
            Triangle,
            VvavyTriangle,
            SpectrumBars,
            Waveform,
            AuraOrb,
            BeatDropGrid,
            FrequencyWaves,
            CircularEQ
        )

        /** Default theme */
        val default: VisualizerTheme = VvavyTriangle

        fun fromId(id: String): VisualizerTheme =
            all.firstOrNull { it.id == id } ?: default
    }
}
