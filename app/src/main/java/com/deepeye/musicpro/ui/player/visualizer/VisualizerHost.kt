// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.player.visualizer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.StateFlow

/**
 * Renders the selected [VisualizerSceneId].
 *
 * The `when` is exhaustive over the enum, so adding a scene to [VisualizerSceneId]
 * without providing a renderer here is a compile error. This is what keeps
 * [AvailableVisualizerScenes] honest: the catalog cannot advertise a scene that
 * has no implementation, because the compiler refuses the build.
 */
@Composable
fun VisualizerHost(
    sceneId: VisualizerSceneId,
    fftSpectrum: StateFlow<FloatArray>,
    frequencyBands: StateFlow<FloatArray>,
    accentColor: Color = VvavyCyan,
    primaryColor: Color = accentColor,
    secondaryColor: Color = Color(0xFFFF007F),
    intensity: Float = 1f,
    reducedMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
    // `when` is exhaustive over the enum: adding a scene without a renderer here
    // is a compile error, which is what keeps the catalog honest.
    when (sceneId) {
        VisualizerSceneId.TRIANGLE_REACTIVE -> VvavyTriangleVisualizer(
            fftSpectrum = fftSpectrum,
            frequencyBands = frequencyBands,
            accentColor = primaryColor,
            intensity = intensity,
            reducedMotion = reducedMotion,
            modifier = modifier
        )

        VisualizerSceneId.SPECTRUM_BARS -> SpectrumBarsVisualizer(
            fftSpectrum = fftSpectrum,
            frequencyBands = frequencyBands,
            accentColor = accentColor,
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
            intensity = intensity,
            reducedMotion = reducedMotion,
            modifier = modifier
        )

        VisualizerSceneId.WAVEFORM -> WaveformVisualizer(
            fftSpectrum = fftSpectrum,
            frequencyBands = frequencyBands,
            accentColor = accentColor,
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
            intensity = intensity,
            reducedMotion = reducedMotion,
            modifier = modifier
        )

        VisualizerSceneId.RADIAL_PULSE -> RadialPulseVisualizer(
            fftSpectrum = fftSpectrum,
            frequencyBands = frequencyBands,
            accentColor = accentColor,
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
            intensity = intensity,
            reducedMotion = reducedMotion,
            modifier = modifier
        )

        VisualizerSceneId.PARTICLE_FIELD -> ParticleFieldVisualizer(
            fftSpectrum = fftSpectrum,
            frequencyBands = frequencyBands,
            accentColor = accentColor,
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
            intensity = intensity,
            reducedMotion = reducedMotion,
            modifier = modifier
        )

        VisualizerSceneId.LIQUID_PLASMA -> com.deepeye.musicpro.ui.player.visualizer.agsl.AgslVisualizer(
            fftSpectrum = fftSpectrum,
            frequencyBands = frequencyBands,
            scene = com.deepeye.musicpro.ui.player.visualizer.agsl.AgslScene.LIQUID_PLASMA,
            accentColor = accentColor,
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
            modifier = modifier
        )

        VisualizerSceneId.CRYSTAL_TUNNEL -> com.deepeye.musicpro.ui.player.visualizer.agsl.AgslVisualizer(
            fftSpectrum = fftSpectrum,
            frequencyBands = frequencyBands,
            scene = com.deepeye.musicpro.ui.player.visualizer.agsl.AgslScene.CRYSTAL_TUNNEL,
            accentColor = accentColor,
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
            modifier = modifier
        )

        VisualizerSceneId.CYBER_GRID -> com.deepeye.musicpro.ui.player.visualizer.agsl.AgslVisualizer(
            fftSpectrum = fftSpectrum,
            frequencyBands = frequencyBands,
            scene = com.deepeye.musicpro.ui.player.visualizer.agsl.AgslScene.CYBER_GRID,
            accentColor = accentColor,
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
            modifier = modifier
        )
    }
}