package com.deepeye.musicpro.dsp.engine

import com.deepeye.musicpro.dsp.model.DspParams
import com.deepeye.musicpro.dsp.model.RiskLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class GainBudgetCalculatorTest {

    @Test
    fun testFlatParams_isSafe() {
        val params = DspParams.flat()
        val budget = GainBudgetCalculator.calculate(params)
        assertEquals(RiskLevel.SAFE, budget.risk)
    }

    @Test
    fun testModerateRisk() {
        val params = DspParams(
            pgcGain = 0f,
            eqEnabled = true,
            eqBands = floatArrayOf(6f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
            bassBoostEnabled = true,
            bassBoostStrength = 1000
        )
        val budget = GainBudgetCalculator.calculate(params)
        assertEquals(RiskLevel.MODERATE, budget.risk)
    }

    @Test
    fun testHighRisk_Danger() {
        val params = DspParams(
            pgcGain = 0f,
            eqEnabled = true,
            eqBands = floatArrayOf(9f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
            bassBoostEnabled = true,
            bassBoostStrength = 1000,
            viperBassEnabled = true,
            viperBassGain = 12f
        )
        val budget = GainBudgetCalculator.calculate(params)
        assertEquals(RiskLevel.DANGER, budget.risk)
    }
}
