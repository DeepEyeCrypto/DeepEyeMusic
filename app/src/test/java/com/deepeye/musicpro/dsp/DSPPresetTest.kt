// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.dsp

import com.deepeye.musicpro.dsp.model.DSPPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DSPPresetTest {

    @Test
    fun testAllTenPresetsPresent() {
        assertEquals(10, DSPPreset.entries.size)
    }

    @Test
    fun testPresetsHaveProperHeadroomAndAntiClip() {
        for (preset in DSPPreset.entries) {
            assertNotNull(preset.presetName)
            assertTrue(preset.presetName.isNotBlank())
            assertNotNull(preset.description)
            assertTrue(preset.description.isNotBlank())

            // Anti-clipping safety rule: PGC gain must be <= -0.5 dB
            assertTrue(
                "Preset ${preset.presetName} must have PGC attenuation headroom for anti-clipping",
                preset.params.pgcGain <= -0.5f
            )

            // Limiter must be enabled
            assertTrue(
                "Preset ${preset.presetName} must have limiter enabled",
                preset.params.limiterEnabled
            )

            // EQ band array must have exactly 10 bands
            assertEquals(
                "Preset ${preset.presetName} must have 10 EQ bands",
                10,
                preset.params.eqBands.size
            )

            // All presets must be unlocked for all users (requiredRank <= 0)
            assertTrue(
                "Preset ${preset.presetName} must be unlocked (requiredRank <= 0)",
                preset.requiredRank <= 0
            )
        }
    }

    @Test
    fun testDeepBassPresetsHaveCorrectCalibration() {
        val infra = DSPPreset.SUBWOOFER_30HZ_INFRA
        assertTrue(infra.params.viperBassEnabled)
        assertEquals(40, infra.params.viperBassFreq)
        assertTrue(infra.params.viperBassGain >= 8.0f)

        val monster = DSPPreset.EARTHQUAKE_BASS_MONSTER
        assertTrue(monster.params.viperBassEnabled)
        assertTrue(monster.params.bassBoostEnabled)

        val slap = DSPPreset.PUNCHY_808_SLAP
        assertTrue(slap.params.viperBassEnabled)
        assertEquals(60, slap.params.viperBassFreq)

        val lofi = DSPPreset.VELVET_WARM_LOFI_BASS
        assertTrue(lofi.params.tubeEnabled)
        assertTrue(lofi.params.viperBassEnabled)

        val subHarmonic = DSPPreset.SUB_HARMONIC_EXCITER
        assertTrue(subHarmonic.params.dynamicSystemEnabled)
        assertTrue(subHarmonic.params.viperBassEnabled)
    }
}
