// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.dsp.session

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.util.Log
import com.deepeye.musicpro.dsp.engine.DSPEngine
import com.deepeye.musicpro.dsp.model.AudioRoute
import com.deepeye.musicpro.dsp.model.DSPPreset
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import androidx.datastore.preferences.core.edit
import com.deepeye.musicpro.data.prefs.DSPKeys
import com.deepeye.musicpro.data.prefs.dspDataStore

/**
 * Automatically switches DSP presets based on audio routing (Headphones, Bluetooth, Speaker).
 */
@AndroidEntryPoint
class AudioRouteReceiver : BroadcastReceiver() {

    @Inject lateinit var engine: DSPEngine
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onReceive(context: Context, intent: Intent) {
        val route = when (intent.action) {
            AudioManager.ACTION_HEADSET_PLUG -> {
                val state = intent.getIntExtra("state", 0)
                if (state == 1) AudioRoute.WIRED_HEADSET else AudioRoute.SPEAKER
            }
            BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED)
                if (state == BluetoothProfile.STATE_CONNECTED) AudioRoute.BLUETOOTH_A2DP else AudioRoute.SPEAKER
            }
            else -> AudioRoute.UNKNOWN
        }

        val preset = when (route) {
            AudioRoute.WIRED_HEADSET -> DSPPreset.HIFI_HEADPHONES
            AudioRoute.BLUETOOTH_A2DP -> DSPPreset.PREMIUM_BASS
            AudioRoute.SPEAKER -> DSPPreset.LOUDNESS_MAX
            else -> null
        }

        preset?.let {
            Log.i("AudioRoute", "✅ Route changed → applied preset: ${it.name}")
            // Apply immediately to engine
            engine.applyPreset(it)
            
            // Save to DataStore so UI updates
            scope.launch {
                context.dspDataStore.edit { prefs ->
                    prefs[DSPKeys.PRESET] = it.name
                }
            }
        }
    }
}

