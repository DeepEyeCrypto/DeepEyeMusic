// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.features.visualizer.studio

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
 * VisualizerStudioScreen — Three-layer AGSL visualizer studio.
 *
 * Layer 1 (Base): [AgslVisualizer] draws the active AGSL shader scene.
 * Layer 2 (Transition): [AnimatedContent] crossfades between scenes (500ms).
 * Layer 3 (Tint): reserved for future RenderEffect bloom/vignette/tint chain.
 */
@Composable
fun VisualizerStudioScreen(
    viewModel: VisualizerStudioViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val activeScene by viewModel.activeScene.collectAsState()

    // Map domain scene IDs to AGSL scenes (new catalog scenes default to
    // NEON_TRIANGLE_GRID so existing visualizer library screen behavior
    // remains unchanged until the selector is expanded).
    val agslScene =
        when (activeScene) {
            VisualizerSceneId.NEON_TRIANGLE_GRID -> AgslScene.NEON_TRIANGLE_GRID
            VisualizerSceneId.LIQUID_PLASMA -> AgslScene.LIQUID_PLASMA
            VisualizerSceneId.CRYSTAL_TUNNEL -> AgslScene.CRYSTAL_TUNNEL
            VisualizerSceneId.CYBER_GRID -> AgslScene.CYBER_GRID
            VisualizerSceneId.AURA_ORB -> AgslScene.AURA_ORB
            VisualizerSceneId.FLUID_PULSE -> AgslScene.FLUID_PULSE
            VisualizerSceneId.AUDIO_FRACTAL -> AgslScene.AUDIO_FRACTAL
            VisualizerSceneId.NEON_GRID -> AgslScene.NEON_GRID
            else -> AgslScene.NEON_TRIANGLE_GRID
        }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = { VisualizerTopBar(onBackClick) }
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            // ── Layer 1 + 2: Base + Transition (crossfade) ────────────────
            AnimatedContent(
                targetState = agslScene,
                transitionSpec = {
                    // Slide in from right for forward, left for backward.
                    val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                    val spec =
                        slideInHorizontally(
                            initialOffsetX = { it * direction },
                            animationSpec = tween(durationMillis = 500)
                        ) togetherWith
                            slideOutHorizontally(
                                targetOffsetX = { it * direction },
                                animationSpec = tween(durationMillis = 500)
                            )
                    spec
                },
                label = "visualizerSceneCrossfade"
            ) { scene ->
                // ── Layer 1: Base shader layer ──────────────────────────────
                AgslVisualizer(
                    fftSpectrum = viewModel.fftSpectrum,
                    frequencyBands = viewModel.frequencyBands,
                    scene = scene,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // ── Layer 3: Tint / post-processing placeholder ──────────────
            // TODO (Task 4): integrate RenderEffect bloom/vignette chain here.
            // Currently a no-op dark-tint overlay to preserve the canvas.
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0f)),
            ) {}
        }

        // ── Interaction Overlay ─────────────────────────────────────────────────
        // Interaction overlay panel aligned to bottom-start
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomStart
        ) {
            Column {
                EffectsTunerPanel(viewModel)
                VisualSelectorPanel(viewModel)
            }
        }
    }
}
