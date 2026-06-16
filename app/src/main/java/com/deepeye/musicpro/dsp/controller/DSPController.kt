// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.dsp.controller

import com.deepeye.musicpro.dsp.engine.DSPEngine
import com.deepeye.musicpro.dsp.engine.GainBudgetCalculator
import com.deepeye.musicpro.dsp.model.DSPPreset
import com.deepeye.musicpro.dsp.model.GainBudget
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DSPController @Inject constructor(
    val engine: DSPEngine
) {
    fun enableDSP(enabled: Boolean) {
        engine.updateParams(
            engine.currentParams.value.copy(enabled = enabled)
        )
    }
    
    fun applyPreset(preset: DSPPreset) {
        engine.updateParams(
            preset.params.copy(enabled = true),
            presetName = preset.presetName
        )
    }
    
    fun getGainBudget(): GainBudget {
        return GainBudgetCalculator.calculate(engine.currentParams.value)
    }
}
