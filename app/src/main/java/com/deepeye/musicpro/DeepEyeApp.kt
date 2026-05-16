package com.deepeye.musicpro

import android.app.Application
import com.deepeye.musicpro.dsp.data.PresetRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

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
