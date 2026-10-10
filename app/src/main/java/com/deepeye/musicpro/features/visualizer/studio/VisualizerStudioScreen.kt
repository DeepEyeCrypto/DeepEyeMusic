// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.features.visualizer.studio

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import com.deepeye.musicpro.features.visualizer.studio.components.EffectsTunerPanel
import com.deepeye.musicpro.features.visualizer.studio.components.VisualSelectorPanel
import com.deepeye.musicpro.features.visualizer.studio.components.VisualizerTopBar
import com.deepeye.musicpro.ui.player.visualizer.VisualizerSceneId
import com.deepeye.musicpro.ui.player.visualizer.agsl.AgslScene
import com.deepeye.musicpro.ui.player.visualizer.agsl.AgslVisualizer

/**
 * VisualizerStudioScreen — The immersive studio shell wrapper for AGSL visualizers.
 */
@Composable
fun VisualizerStudioScreen(
    viewModel: VisualizerStudioViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val activeScene by viewModel.activeScene.collectAsState()

    // Map domain scene IDs to AGSL scenes
    val agslScene = when (activeScene) {
        VisualizerSceneId.NEON_TRIANGLE_GRID -> AgslScene.NEON_TRIANGLE_GRID
        VisualizerSceneId.LIQUID_PLASMA -> AgslScene.LIQUID_PLASMA
        else -> AgslScene.NEON_TRIANGLE_GRID
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = { VisualizerTopBar(onBackClick) }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Background Canvas (Z-0) — live FFT bound to the ExoPlayer audio session.
            AgslVisualizer(
                fftSpectrum = viewModel.fftSpectrum,
                frequencyBands = viewModel.frequencyBands,
                scene = agslScene,
                modifier = Modifier.fillMaxSize()
            )
            
            // Interaction Overlay
            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                EffectsTunerPanel(viewModel)
                VisualSelectorPanel(viewModel)
            }
        }
    }
}
