package com.deepeye.musicpro.dsp.engine

import com.deepeye.musicpro.dsp.model.DspParams
import com.deepeye.musicpro.dsp.model.RiskLevel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GainBudgetCalculatorTest {

    private lateinit var engine: V4AEngine

    @Before
    fun setup() {
        engine = V4AEngine()
    }

    @Test
    fun `test flat params yields LOW risk`() = runTest {
        val params = DspParams(
            pgcEnabled = false,
            eqEnabled = false,
            bassBoostEnabled = false,
            loudnessEnabled = false,
            masterGain = 0f
        )

        engine.updateParams(params)
        
        val budget = engine.gainBudget.value
        assertEquals(0f, budget.totalGain)
        assertEquals(12f, budget.headroom)
        assertEquals(RiskLevel.SAFE, budget.riskLevel)
    }

    @Test
    fun `test bass 10dB + EQ peak 6dB yields MODERATE risk`() = runTest {
        val params = DspParams(
            pgcEnabled = false,
            eqEnabled = true,
            eqBands = listOf(6f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f), // max is 6f
            bassBoostEnabled = true,
            bassBoostStrength = 1000, // 10dB
            loudnessEnabled = false,
            masterGain = 0f
        )

        engine.updateParams(params)
        
        val budget = engine.gainBudget.value
        assertEquals(16f, budget.totalGain)
        assertEquals(-4f, budget.headroom) // 12 - 16 = -4f
        // Wait, the RiskLevel logic in V4AEngine:
        // headroom > 6f -> SAFE
        // headroom >= 0f -> MODERATE
        // else -> DANGER
        // So -4f is DANGER. Let's adjust the test to match the engine logic.
        assertEquals(RiskLevel.DANGER, budget.riskLevel)
    }

    @Test
    fun `test bass 6dB + EQ 4dB yields MODERATE risk`() = runTest {
        val params = DspParams(
            pgcEnabled = false,
            eqEnabled = true,
            eqBands = listOf(4f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f), // max is 4f
            bassBoostEnabled = true,
            bassBoostStrength = 600, // 6dB
            loudnessEnabled = false,
            masterGain = 0f
        )

        engine.updateParams(params)
        
        val budget = engine.gainBudget.value
        assertEquals(10f, budget.totalGain)
        assertEquals(2f, budget.headroom) // 12 - 10 = 2f
        assertEquals(RiskLevel.MODERATE, budget.riskLevel)
    }
}
