// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.dsp.engine

import android.util.Log
import com.deepeye.musicpro.dsp.model.DspParams
import com.deepeye.musicpro.dsp.model.GainBudget
import com.deepeye.musicpro.dsp.model.RiskLevel

/**
 * Calculates the estimated digital gain accumulated by various DSP modules.
 * Used to prevent digital clipping and provide real-time feedback.
 */
object GainBudgetCalculator {

    fun calculate(params: DspParams): GainBudget {
        var total = 0f

        if (params.eqEnabled) {
            // Estimated gain contribution from EQ bands (weighted positive gains)
            total += params.eqBands
                .filter { it > 0 }
                .sum() * 0.4f
        }
        if (params.bassBoostEnabled) {
            total += (params.bassBoostStrength / 1000f) * 8f
        }
        if (params.loudnessEnabled) {
            total += (params.loudnessTargetGainMb / 1000f) * 0.1f
        }
        if (params.viperBassEnabled) {
            total += params.viperBassGain * 0.5f
        }
        if (params.viperClarityEnabled) {
            total += params.viperClarityGain * 0.3f
        }
        if (params.dynamicSystemEnabled) {
            total += params.dynamicSystemStrength * 0.08f
        }
        if (params.tubeEnabled) {
            total += (params.tubeDrive / 100f) * 2f
        }

        // PGC headroom subtracts from total
        total += params.pgcGain // negative value reduces risk

        val risk = when {
            total < 8f  -> RiskLevel.SAFE
            total < 14f -> RiskLevel.MODERATE
            else        -> RiskLevel.DANGER
        }

        return GainBudget(totalDb = total.coerceAtLeast(0f), risk = risk)
    }

    /**
     * Automatically adjusts parameters if the gain budget enters the DANGER zone.
     */
    fun autoCorrect(params: DspParams): DspParams {
        val budget = calculate(params)
        return if (budget.risk == RiskLevel.DANGER) {
            params.copy(
                loudnessTargetGainMb = (params.loudnessTargetGainMb * 0.5f).toInt(),
                bassBoostStrength = (params.bassBoostStrength * 0.7f).toInt(),
                viperBassGain = params.viperBassGain * 0.8f
            ).also {
                Log.w("GainBudget", "⚠️ DANGER — auto-corrected: loudness + bassBoost reduced")
            }
        } else params
    }
}
