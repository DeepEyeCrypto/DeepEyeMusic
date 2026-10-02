// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later
//
// AgslVisualizer — High-Performance GPU Shader Renderer using AGSL / RuntimeShader (API 33+)
// Zero-recomposition uniform dispatch with SkSL execution directly on Mali/Adreno GPU.
//
package com.deepeye.musicpro.ui.player.visualizer.agsl

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.toArgb
import com.deepeye.musicpro.ui.player.visualizer.VisualizerCanvas
import com.deepeye.musicpro.ui.player.visualizer.VisualizerTheme
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive

enum class AgslScene {
    LIQUID_PLASMA,
    CRYSTAL_TUNNEL,
    AURA_ORB,
    CYBER_GRID
}

@Composable
fun AgslVisualizer(
    fftSpectrum: StateFlow<FloatArray>,
    frequencyBands: StateFlow<FloatArray>,
    scene: AgslScene = AgslScene.LIQUID_PLASMA,
    themeState: StateFlow<VisualizerTheme>? = null,
    accentColor: Color = Color(0xFF00E5FF),
    modifier: Modifier = Modifier
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        AgslRuntimeShaderRenderer(
            fftSpectrum = fftSpectrum,
            frequencyBands = frequencyBands,
            scene = scene,
            themeState = themeState,
            accentColor = accentColor,
            modifier = modifier
        )
    } else {
        // Fallback for API < 33
        VisualizerCanvas(
            fftSpectrum = fftSpectrum,
            frequencyBands = frequencyBands,
            themeState = themeState ?: kotlinx.coroutines.flow.MutableStateFlow(VisualizerTheme.default),
            accentColor = accentColor,
            modifier = modifier
        )
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun AgslRuntimeShaderRenderer(
    fftSpectrum: StateFlow<FloatArray>,
    frequencyBands: StateFlow<FloatArray>,
    scene: AgslScene,
    themeState: StateFlow<VisualizerTheme>?,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    // ── Continuous 60fps clock driven via withFrameMillis ──
    // MutableFloatState is updated every frame, but read ONLY inside DrawScope
    val timeState = remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        var startNanos = 0L
        while (isActive) {
            withFrameNanos { frameNanos ->
                if (startNanos == 0L) startNanos = frameNanos
                timeState.floatValue = (frameNanos - startNanos) / 1_000_000_000f
            }
        }
    }

    // Pre-instantiate RuntimeShader per scene
    val liquidShader = remember { RuntimeShader(AgslShaders.LIQUID_PLASMA) }
    val crystalShader = remember { RuntimeShader(AgslShaders.CRYSTAL_TUNNEL) }
    val auraShader = remember { RuntimeShader(AgslShaders.AURA_ORB) }
    val cyberGridShader = remember { RuntimeShader(AgslShaders.CYBER_GRID) }

    // Pre-instantiate ShaderBrushes
    val liquidBrush = remember(liquidShader) { ShaderBrush(liquidShader) }
    val crystalBrush = remember(crystalShader) { ShaderBrush(crystalShader) }
    val auraBrush = remember(auraShader) { ShaderBrush(auraShader) }
    val cyberGridBrush = remember(cyberGridShader) { ShaderBrush(cyberGridShader) }

    Canvas(modifier = modifier.fillMaxSize()) {
        // ── ZERO-RECOMPOSITION DRAW PHASE UNIFORM DISPATCH ──
        val time = timeState.floatValue
        val bands = frequencyBands.value
        val currentTheme = themeState?.value

        val bass   = if (bands.isNotEmpty()) bands[0].coerceIn(0f, 2.5f) else 0f
        val mids   = if (bands.size > 2) bands[2].coerceIn(0f, 2f) else 0f
        val treble = if (bands.size > 4) bands[4].coerceIn(0f, 2f) else 0f
        val peak   = if (bands.size > 5) bands[5].coerceIn(0f, 2.5f) else 0f

        val activeShader: RuntimeShader
        val activeBrush: ShaderBrush

        if (currentTheme != null) {
            when (currentTheme) {
                is VisualizerTheme.Triangle,
                is VisualizerTheme.VvavyTriangle -> {
                    activeShader = crystalShader
                    activeBrush = crystalBrush
                }
                is VisualizerTheme.SpectrumBars,
                is VisualizerTheme.CircularEQ -> {
                    activeShader = cyberGridShader
                    activeBrush = cyberGridBrush
                }
                is VisualizerTheme.AuraOrb,
                is VisualizerTheme.BeatDropGrid -> {
                    activeShader = auraShader
                    activeBrush = auraBrush
                }
                else -> {
                    activeShader = liquidShader
                    activeBrush = liquidBrush
                }
            }
        } else {
            when (scene) {
                AgslScene.LIQUID_PLASMA  -> { activeShader = liquidShader; activeBrush = liquidBrush }
                AgslScene.CRYSTAL_TUNNEL -> { activeShader = crystalShader; activeBrush = crystalBrush }
                AgslScene.AURA_ORB       -> { activeShader = auraShader; activeBrush = auraBrush }
                AgslScene.CYBER_GRID     -> { activeShader = cyberGridShader; activeBrush = cyberGridBrush }
            }
        }

        // Set float uniforms
        activeShader.setFloatUniform("iResolution", size.width, size.height)
        activeShader.setFloatUniform("iTime", time)
        activeShader.setFloatUniform("iBass", bass)
        activeShader.setFloatUniform("iMid", mids)
        activeShader.setFloatUniform("iTreble", treble)
        activeShader.setFloatUniform("iPeak", peak)
        activeShader.setFloatUniform(
            "iAccentColor",
            accentColor.red,
            accentColor.green,
            accentColor.blue,
            accentColor.alpha
        )

        // Draw shader directly onto GPU surface
        drawRect(brush = activeBrush)
    }
}
