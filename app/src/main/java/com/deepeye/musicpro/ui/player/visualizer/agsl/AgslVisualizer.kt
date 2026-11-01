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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.deepeye.musicpro.ui.player.visualizer.VisualizerCanvas
import com.deepeye.musicpro.ui.player.visualizer.VisualizerTheme
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlin.math.sqrt

enum class AgslScene {
    LIQUID_PLASMA,
    CRYSTAL_TUNNEL,
    AURA_ORB,
    CYBER_GRID,
    NEON_TRIANGLE_GRID,
    FLUID_PULSE,
    AUDIO_FRACTAL,
    NEON_GRID,

    // Batch 1 — new scenes (map to existing shaders for now)
    AURORA_FLUX,
    LIQUID_CHROME,
    NEBULA_BLOOM,
    PRISM_TUNNEL,
    KINETIC_MANDALA,
    PULSE_GEOMETRY
}

/**
 * Scenes whose AGSL source declares `iMouse`. Binding iMouse to a shader that
 * does NOT declare it makes libhwui throw (observed SIGABRT on some MT6835
 * ROMs), so the dispatcher only sets iMouse for this set.
 */
val AgslScene.declaresMouse: Boolean
    get() =
        when (this) {
            AgslScene.FLUID_PULSE, AgslScene.AUDIO_FRACTAL, AgslScene.NEON_GRID -> true
            else -> false
        }

/**
 * Scenes whose AGSL source declares `iColorPrimary` and `iColorSecondary`.
 * Binding these to a shader that does NOT declare them makes libhwui throw
 * SIGABRT on MT6835 ROMs. The legacy AgslShaders (LIQUID_PLASMA through
 * NEON_TRIANGLE_GRID) declare these; the Studio catalog shaders
 * (FLUID_PULSE, AUDIO_FRACTAL, NEON_GRID) do NOT.
 */
val AgslScene.declaresColorUniforms: Boolean
    get() =
        when (this) {
            AgslScene.LIQUID_PLASMA,
            AgslScene.CRYSTAL_TUNNEL,
            AgslScene.AURA_ORB,
            AgslScene.CYBER_GRID,
            AgslScene.NEON_TRIANGLE_GRID -> true
            else -> false
        }

@Composable
fun AgslVisualizer(
    fftSpectrum: StateFlow<FloatArray>,
    frequencyBands: StateFlow<FloatArray>,
    scene: AgslScene = AgslScene.LIQUID_PLASMA,
    themeState: StateFlow<VisualizerTheme>? = null,
    accentColor: Color = Color(0xFF00E5FF),
    primaryColor: Color = accentColor,
    secondaryColor: Color = Color(0xFFFF007F),
    modifier: Modifier = Modifier
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        AgslRuntimeShaderRenderer(
            fftSpectrum = fftSpectrum,
            frequencyBands = frequencyBands,
            scene = scene,
            themeState = themeState,
            accentColor = accentColor,
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
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
    primaryColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier
) {
    val dataBridge = remember { VisualizerDataBridge() }

    // ── Thermal governor: hardware heat → frame-rate / post-FX policy ──
    // rememberThermalTier() bridges PowerManager's OnThermalStatusChangedListener
    // (API 29+) into Compose; on API < 29 it stays FULL (existing 144 Hz path).
    val thermalTier by rememberThermalTier()

    // ── Background GPU kill-switch ──
    // The GPU must drop to 0% the instant the app is backgrounded, while the DSP
    // engine and ExoPlayer keep the audio stream alive. isRendering is read from
    // the frame loop and from the Canvas draw below, so setting it false on
    // ON_PAUSE stops both the clock and the shader redraw with zero GPU cost.
    val isRendering = remember { mutableStateOf(true) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                    isRendering.value = false
                } else if (event == Lifecycle.Event.ON_RESUME) {
                    isRendering.value = true
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // ── Adaptive clock driven via withFrameNanos ──
    // The loop throttles itself to the thermal tier's target FPS by dropping
    // frames whose deadline has not elapsed. When backgrounded (isRendering ==
    // false) the loop still awaits frames but advances neither the clock nor the
    // smoothing accumulator, and the Canvas returns early — so the compositor has
    // nothing new to draw and GPU usage falls to 0%.
    val timeState = remember { mutableFloatStateOf(0f) }
    val smoothingState = remember { mutableFloatStateOf(thermalTier.smoothing) }
    LaunchedEffect(thermalTier) {
        smoothingState.floatValue = thermalTier.smoothing
        var startNanos = 0L
        var lastDrawnNanos = 0L
        val frameBudgetNanos = 1_000_000_000L / thermalTier.targetFps
        while (isActive) {
            withFrameNanos { frameNanos ->
                if (startNanos == 0L) startNanos = frameNanos
                // Backgrounded: hold the clock, spend zero GPU.
                if (!isRendering.value) {
                    lastDrawnNanos = frameNanos
                    return@withFrameNanos
                }
                // Thermal frame cap: only advance on frames that land within the
                // tier's budget. timeState drives the shader clock and the redraw.
                if (frameNanos - lastDrawnNanos >= frameBudgetNanos) {
                    lastDrawnNanos = frameNanos
                    timeState.floatValue = (frameNanos - startNanos) / 1_000_000_000f
                }
            }
        }
    }

    // Reactive draw trigger: fires on the throttled clock, on a thermal-tier
    // change, and on background/foreground transitions so the kill-switch and
    // any tier shift take effect immediately.
    val renderTrigger by remember(thermalTier) {
        androidx.compose.runtime.derivedStateOf { timeState.floatValue to thermalTier to isRendering.value }
    }

    // Pre-instantiate RuntimeShader per scene
    val liquidShader = remember { RuntimeShader(AgslShaders.LIQUID_PLASMA) }
    val crystalShader = remember { RuntimeShader(AgslShaders.CRYSTAL_TUNNEL) }
    val auraShader = remember { RuntimeShader(AgslShaders.AURA_ORB) }
    val cyberGridShader = remember { RuntimeShader(AgslShaders.CYBER_GRID) }
    val neonTriangleShader = remember { RuntimeShader(AgslShaders.NEON_TRIANGLE_GRID) }
    // Studio catalog scenes (Task 2) — full uniform contract incl. iMouse.
    val fluidPulseShader = remember { RuntimeShader(ShaderCatalog.FLUID_PULSE) }
    val audioFractalShader = remember { RuntimeShader(ShaderCatalog.AUDIO_FRACTAL) }
    val neonGridShader = remember { RuntimeShader(ShaderCatalog.NEON_GRID) }

    // Pre-instantiate ShaderBrushes
    val liquidBrush = remember(liquidShader) { ShaderBrush(liquidShader) }
    val crystalBrush = remember(crystalShader) { ShaderBrush(crystalShader) }
    val auraBrush = remember(auraShader) { ShaderBrush(auraShader) }
    val cyberGridBrush = remember(cyberGridShader) { ShaderBrush(cyberGridShader) }
    val neonTriangleBrush = remember(neonTriangleShader) { ShaderBrush(neonTriangleShader) }
    val fluidPulseBrush = remember(fluidPulseShader) { ShaderBrush(fluidPulseShader) }
    val audioFractalBrush = remember(audioFractalShader) { ShaderBrush(audioFractalShader) }
    val neonGridBrush = remember(neonGridShader) { ShaderBrush(neonGridShader) }

    // ── Safe touch → iMouse mapping ─────────────────────────────────────
    // Normalized (0..1, y-down Compose convention) with pressed-state in
    // the zw components. Reused float/int state holders: no allocation in
    // the draw loop. `size` is captured by onGloballyPositioned so the
    // gesture handler (which runs outside the Canvas draw scope) can
    // normalize pointer coordinates without a division by zero.
    val mouseX = remember { mutableFloatStateOf(0.5f) }
    val mouseY = remember { mutableFloatStateOf(0.5f) }
    val mousePressed = remember { mutableFloatStateOf(0f) }
    val viewWidth = remember { mutableFloatStateOf(1f) }
    val viewHeight = remember { mutableFloatStateOf(1f) }

    Canvas(
        modifier =
            modifier
                .fillMaxSize()
                .onGloballyPositioned { coords ->
                    viewWidth.floatValue = coords.size.width.toFloat().coerceAtLeast(1f)
                    viewHeight.floatValue = coords.size.height.toFloat().coerceAtLeast(1f)
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset: Offset ->
                            mousePressed.floatValue = 1f
                            mouseX.floatValue = (offset.x / viewWidth.floatValue).coerceIn(0f, 1f)
                            mouseY.floatValue = (offset.y / viewHeight.floatValue).coerceIn(0f, 1f)
                        },
                        onDrag = { change: PointerInputChange, _ ->
                            change.consume()
                            mouseX.floatValue = (change.position.x / viewWidth.floatValue).coerceIn(0f, 1f)
                            mouseY.floatValue = (change.position.y / viewHeight.floatValue).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            mousePressed.floatValue = 0f
                        },
                        onDragCancel = {
                            mousePressed.floatValue = 0f
                        }
                    )
                }
    ) {
        // ── ZERO-RECOMPOSITION DRAW PHASE UNIFORM DISPATCH ──
        // Background kill-switch: bail out before touching the GPU so the
        // compositor has nothing to redraw and GPU usage is 0% while backgrounded.
        renderTrigger
        if (!isRendering.value) return@Canvas

        val time = timeState.floatValue
        val spectrum = fftSpectrum.value
        val bands = frequencyBands.value
        val currentTheme = themeState?.value

        val bass = if (bands.isNotEmpty()) bands[0].coerceIn(0f, 2.5f) else 0f
        val mids = if (bands.size > 2) bands[2].coerceIn(0f, 2f) else 0f
        val treble = if (bands.size > 4) bands[4].coerceIn(0f, 2f) else 0f
        val peak = if (bands.size > 5) bands[5].coerceIn(0f, 2.5f) else 0f
        // Studio catalog contract (Task 3): derived, zero-allocation.
        val energy = sqrt((bass * bass + mids * mids + treble * treble) / 3f).coerceIn(0f, 1f)
        val beatPulse = ((peak - treble).coerceAtLeast(0f) * 0.5f).coerceIn(0f, 1f)
        val intensity = 1f

        val activeShader: RuntimeShader
        val activeBrush: ShaderBrush

        if (currentTheme != null) {
            when (currentTheme) {
                is VisualizerTheme.Triangle -> {
                    activeShader = crystalShader
                    activeBrush = crystalBrush
                }
                is VisualizerTheme.VvavyTriangle -> {
                    activeShader = neonTriangleShader
                    activeBrush = neonTriangleBrush
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
                AgslScene.LIQUID_PLASMA -> {
                    activeShader = liquidShader
                    activeBrush = liquidBrush
                }
                AgslScene.CRYSTAL_TUNNEL -> {
                    activeShader = crystalShader
                    activeBrush = crystalBrush
                }
                AgslScene.AURA_ORB -> {
                    activeShader = auraShader
                    activeBrush = auraBrush
                }
                AgslScene.CYBER_GRID -> {
                    activeShader = cyberGridShader
                    activeBrush = cyberGridBrush
                }
                AgslScene.NEON_TRIANGLE_GRID -> {
                    activeShader = neonTriangleShader
                    activeBrush = neonTriangleBrush
                }
                AgslScene.FLUID_PULSE -> {
                    activeShader = fluidPulseShader
                    activeBrush = fluidPulseBrush
                }
                AgslScene.AUDIO_FRACTAL -> {
                    activeShader = audioFractalShader
                    activeBrush = audioFractalBrush
                }
                AgslScene.NEON_GRID -> {
                    activeShader = neonGridShader
                    activeBrush = neonGridBrush
                }
                // Batch 1 — map new scenes to existing shaders for now
                AgslScene.AURORA_FLUX -> {
                    activeShader = liquidShader
                    activeBrush = liquidBrush
                }
                AgslScene.LIQUID_CHROME -> {
                    activeShader = crystalShader
                    activeBrush = crystalBrush
                }
                AgslScene.NEBULA_BLOOM -> {
                    activeShader = auraShader
                    activeBrush = auraBrush
                }
                AgslScene.PRISM_TUNNEL -> {
                    activeShader = cyberGridShader
                    activeBrush = cyberGridBrush
                }
                AgslScene.KINETIC_MANDALA -> {
                    activeShader = neonTriangleShader
                    activeBrush = neonTriangleBrush
                }
                AgslScene.PULSE_GEOMETRY -> {
                    activeShader = fluidPulseShader
                    activeBrush = fluidPulseBrush
                }
            }
        }

        // ── Studio catalog uniform dispatch (Task 3) ─────────────────────
        // The three ShaderCatalog scenes use the full uniform contract
        // (iAudioBass/iAudioMids/iAudioTreble/iAudioEnergy/iBeatPulse/
        //  iSceneTransition/iIntensity/iAspect, plus iMouse where declared)
        // instead of the legacy iBass/iMid/iTreble/iPeak set.
        val isCatalogScene =
            currentTheme == null && (
                scene == AgslScene.FLUID_PULSE ||
                    scene == AgslScene.AUDIO_FRACTAL ||
                    scene == AgslScene.NEON_GRID
            )

        if (isCatalogScene && currentTheme == null) {
            runCatching { activeShader.setFloatUniform("iResolution", size.width, size.height) }
            runCatching { activeShader.setFloatUniform("iTime", time) }
            runCatching { activeShader.setFloatUniform("iTimeDelta", 1f / 60f) }
            runCatching { activeShader.setFloatUniform("iAudioBass", bass) }
            runCatching { activeShader.setFloatUniform("iAudioMids", mids) }
            runCatching { activeShader.setFloatUniform("iAudioTreble", treble) }
            runCatching { activeShader.setFloatUniform("iAudioEnergy", energy.coerceIn(0f, 1f)) }
            runCatching { activeShader.setFloatUniform("iBeatPulse", beatPulse) }
            runCatching { activeShader.setFloatUniform("iSceneTransition", 1f) }
            runCatching { activeShader.setFloatUniform("iIntensity", intensity) }
            runCatching {
                activeShader.setFloatUniform(
                    "iAspect",
                    if (size.height > 0f) size.width / size.height else 1f
                )
            }
            // iMouse only for scenes that declare it (see AgslScene.declaresMouse).
            if (scene.declaresMouse) {
                runCatching {
                    activeShader.setFloatUniform(
                        "iMouse",
                        mouseX.floatValue,
                        mouseY.floatValue,
                        if (mousePressed.floatValue >= 0.5f) 1f else 0f,
                        if (mousePressed.floatValue >= 0.5f) 0f else 1f
                    )
                }
            }
        }

        // ── Uniform contract guard ─────────────────────────────────────────────
        // Only call setFloatUniform/setInputShader for uniforms the ACTIVE shader
        // actually declares. Setting an undeclared uniform makes libhwui throw an
        // IllegalArgumentException from native code while formatting the
        // "unable to find uniform" message; on some ROMs (observed: Realme RMX3945,
        // MT6835) that throw path reads a freed buffer, fails CheckJNI's Modified
        // UTF-8 validation and SIGABRTs the process — an abort runCatching cannot
        // intercept. Declared uniforms per AgslShaders.kt:
        //   LIQUID_PLASMA / CRYSTAL_TUNNEL / AURA_ORB / CYBER_GRID:
        //     iResolution, iTime, iBass, iMid, iTreble, iPeak,
        //     iChannel0, iAccentColor, iColorPrimary, iColorSecondary
        //   NEON_TRIANGLE_GRID: same minus iChannel0 and iAccentColor
        //   (no shader declares iTimeDelta — that call was removed.)
        val isNeonTriangle = activeShader === neonTriangleShader

        // ── Legacy AgslShaders uniform dispatch ───────────────────────────
        // Catalog scenes (FLUID_PULSE / AUDIO_FRACTAL / NEON_GRID) use the
        // full Studio contract above and do NOT declare iBass/iMid/iTreble/
        // iPeak/iChannel0/iColorPrimary — binding any of those on MT6835
        // SIGABRTs via the "unable to find uniform" native path. Skip the
        // whole legacy block for them; only draw the brush.
        if (!isCatalogScene) {
            // 1. Update and bind Audio FFT 256x1 Bitmap Texture Bridge to iChannel0 safely.
            //    On THROTTLED (severe/critical heat) the tier disables audio post-processing
            //    so we skip the per-frame texture upload — the biggest thermal saving once
            //    the frame rate is already capped.
            if (!isNeonTriangle && thermalTier.audioPostFx) {
                runCatching {
                    val audioShader = dataBridge.updateAudioTexture(spectrum, bands)
                    activeShader.setInputShader("iChannel0", audioShader)
                }
            }

            // 2. Set float uniforms safely (all declared by every active legacy shader)
            runCatching { activeShader.setFloatUniform("iResolution", size.width, size.height) }
            runCatching { activeShader.setFloatUniform("iTime", time) }
            runCatching { activeShader.setFloatUniform("iBass", bass) }
            runCatching { activeShader.setFloatUniform("iMid", mids) }
            runCatching { activeShader.setFloatUniform("iTreble", treble) }
            runCatching { activeShader.setFloatUniform("iPeak", peak) }
            if (!isNeonTriangle) {
                runCatching {
                    activeShader.setFloatUniform(
                        "iAccentColor",
                        accentColor.red,
                        accentColor.green,
                        accentColor.blue,
                        accentColor.alpha
                    )
                }
            }
            // iColorPrimary/iColorSecondary only for legacy shaders that declare them
            if (scene.declaresColorUniforms) {
                runCatching {
                    activeShader.setFloatUniform(
                        "iColorPrimary",
                        primaryColor.red,
                        primaryColor.green,
                        primaryColor.blue,
                        primaryColor.alpha
                    )
                }
                runCatching {
                    activeShader.setFloatUniform(
                        "iColorSecondary",
                        secondaryColor.red,
                        secondaryColor.green,
                        secondaryColor.blue,
                        secondaryColor.alpha
                    )
                }
            }
        }

        // Draw shader directly onto GPU surface
        runCatching {
            drawRect(brush = activeBrush)
        }
    }
}
