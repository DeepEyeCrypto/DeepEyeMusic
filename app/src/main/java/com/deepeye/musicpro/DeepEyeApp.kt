// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

import com.deepeye.musicpro.dsp.data.PresetRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * DeepEyeMusicPro Application class.
 * Entry point for Hilt dependency injection.
 */
@HiltAndroidApp
class DeepEyeApp : Application() {
    @Inject lateinit var presetRepository: PresetRepository

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch {
            presetRepository.seedBuiltinPresets()
        }
    }
}
