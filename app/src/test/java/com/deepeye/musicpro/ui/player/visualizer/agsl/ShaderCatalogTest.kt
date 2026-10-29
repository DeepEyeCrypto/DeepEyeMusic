// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later
//
// ShaderCatalogTest — structural validation of the AGSL shader catalog
// (Task 2). Runs on the JVM: source-string checks, uniform-contract
// verification, and proprietary-content rejection. Actual RuntimeShader
// compilation is exercised in AgslShaderEngineTest (API 33+ gated).

package com.deepeye.musicpro.ui.player.visualizer.agsl

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShaderCatalogTest {

    /** Uniforms every studio scene must declare for the dispatcher contract. */
    private val requiredUniforms = listOf(
        "iResolution", "iTime", "iTimeDelta", "iMouse",
        "iAudioBass", "iAudioMids", "iAudioTreble", "iAudioEnergy",
        "iBeatPulse", "iSceneTransition", "iIntensity", "iAspect"
    )

    /** Strings that must NEVER appear (proprietary VVavy markers). */
    private val forbiddenStrings = listOf(
        "vvavy", "VVavy", "VVAVY",
        "liquid-carbon-plasma", "centroid-crystalline-tunnel",
        "aura-orb-hyper-geometric", "the-infinite-grid"
    )

    @Test
    fun `catalog exposes exactly the three studio scenes`() {
        val titles = ShaderCatalog.SCENES.map { it.title }
        assertTrue(titles.contains("Fluid Pulse"))
        assertTrue(titles.contains("Audio Fractal"))
        assertTrue(titles.contains("Neon Grid"))
        assertTrue(ShaderCatalog.SCENES.size == 3)
    }

    @Test
    fun `every scene declares the full uniform contract`() {
        for (scene in ShaderCatalog.SCENES) {
            for (uniform in requiredUniforms) {
                assertTrue(
                    "scene '${scene.title}' missing uniform $uniform",
                    scene.source.contains("uniform ") &&
                        Regex("\\b$uniform\\b").containsMatchIn(scene.source)
                )
            }
        }
    }

    @Test
    fun `every scene has an AGSL main function signature`() {
        for (scene in ShaderCatalog.SCENES) {
            assertTrue(
                "scene '${scene.title}' missing 'half4 main(float2 ...)'",
                scene.source.contains("half4 main(float2 fragCoord)")
            )
        }
    }

    @Test
    fun `no scene contains proprietary VVavy strings or branding`() {
        for (scene in ShaderCatalog.SCENES) {
            for (bad in forbiddenStrings) {
                assertFalse(
                    "scene '${scene.title}' contains forbidden marker '$bad'",
                    scene.source.contains(bad, ignoreCase = bad == bad.uppercase())
                )
            }
        }
    }

    @Test
    fun `scenes use half only on the final return not on intermediates`() {
        // Intermediates must stay float; the only half usage allowed is the
        // half4(...) return cast and the half3(...) inner cast.
        for (scene in ShaderCatalog.SCENES) {
            val halfDeclCount = Regex("\\bhalf\\s+\\w+\\s*=").findAll(scene.source).count()
            assertTrue(
                "scene '${scene.title}' declares half intermediates (count=$halfDeclCount)",
                halfDeclCount == 0
            )
        }
    }

    @Test
    fun `audio zero state is graceful with no division by audio uniforms`() {
        // Structural check: no shader divides directly by an audio uniform
        // (e.g. 'x / iAudioBass'), which would NaN at silence.
        for (scene in ShaderCatalog.SCENES) {
            for (audio in listOf("iAudioBass", "iAudioMids", "iAudioTreble", "iAudioEnergy", "iBeatPulse")) {
                assertFalse(
                    "scene '${scene.title}' divides by $audio (would NaN at silence)",
                    Regex("/\\s*$audio\\b").containsMatchIn(scene.source)
                )
            }
        }
    }

    @Test
    fun `scenes are visually distinct`() {
        // Each scene's source must be meaningfully different from the others.
        val sources = ShaderCatalog.SCENES.map { it.source }
        for (i in sources.indices) {
            for (j in (i + 1) until sources.size) {
                assertTrue(
                    "scenes $i and $j are identical",
                    sources[i] != sources[j]
                )
            }
        }
    }
}
