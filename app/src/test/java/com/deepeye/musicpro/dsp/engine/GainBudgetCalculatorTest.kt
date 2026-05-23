// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.dsp.engine

import com.deepeye.musicpro.dsp.model.DspParams
import com.deepeye.musicpro.dsp.model.RiskLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GainBudgetCalculatorTest {

    @Test
    fun testFlatParamsYieldsLowRisk() {
        val params = DspParams(
            pgcEnabled = false,
            pgcGain = 0f,
            eqEnabled = false,
            bassBoostEnabled = false,
            loudnessEnabled = false,
            masterGain = 0f
        )

        val budget = GainBudgetCalculator.calculate(params)
        assertEquals(0f, budget.totalDb)
        assertEquals(RiskLevel.SAFE, budget.risk)
    }

    @Test
    fun testBass10dBPlusEQPeak6dBYieldsDangerRisk() {
        val params = DspParams(
            pgcEnabled = false,
            pgcGain = 0f,
            eqEnabled = true,
            eqBands = floatArrayOf(6f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
            bassBoostEnabled = true,
            bassBoostStrength = 1000, // 8.0f gain
            viperBassEnabled = true,
            viperBassGain = 12f, // 6.0f gain
            loudnessEnabled = false,
            masterGain = 0f
        )

        // total = 6*0.4 + 8 + 6 = 16.4f
        val budget = GainBudgetCalculator.calculate(params)
        assertEquals(16.4f, budget.totalDb, 0.01f)
        assertEquals(RiskLevel.DANGER, budget.risk)
    }

    @Test
    fun testBass6dBPlusEQ4dBYieldsModerateRisk() {
        val params = DspParams(
            pgcEnabled = false,
            pgcGain = 0f,
            eqEnabled = true,
            eqBands = floatArrayOf(4f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
            bassBoostEnabled = true,
            bassBoostStrength = 900, // 7.2f gain
            loudnessEnabled = false,
            masterGain = 0f
        )

        // total = 4*0.4 + 7.2 = 8.8f
        val budget = GainBudgetCalculator.calculate(params)
        assertEquals(8.8f, budget.totalDb, 0.01f)
        assertEquals(RiskLevel.MODERATE, budget.risk)
    }

    @Test
    fun testAutoCorrectReducesParameters() {
        val params = DspParams(
            pgcEnabled = false,
            pgcGain = 0f,
            eqEnabled = true,
            eqBands = floatArrayOf(6f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
            bassBoostEnabled = true,
            bassBoostStrength = 1000, 
            viperBassEnabled = true,
            viperBassGain = 12f,
            loudnessEnabled = true,
            loudnessTargetGainMb = 1000,
            masterGain = 0f
        )

        // Verifying it is in DANGER zone first
        val budgetBefore = GainBudgetCalculator.calculate(params)
        assertEquals(RiskLevel.DANGER, budgetBefore.risk)

        // Auto-correcting
        val correctedParams = GainBudgetCalculator.autoCorrect(params)
        
        // Asserting that parameters were indeed reduced
        assertTrue(correctedParams.bassBoostStrength < params.bassBoostStrength)
        assertTrue(correctedParams.viperBassGain < params.viperBassGain)
        assertTrue(correctedParams.loudnessTargetGainMb < params.loudnessTargetGainMb)
    }
}
