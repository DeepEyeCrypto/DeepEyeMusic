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
import com.deepeye.musicpro.dsp.data.PresetRepository
import com.deepeye.musicpro.dsp.engine.DSPEngine
import com.deepeye.musicpro.dsp.model.AudioRoute
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Automatically switches DSP presets based on audio routing (Headphones, Bluetooth, Speaker).
 */
@AndroidEntryPoint
class AudioRouteReceiver : BroadcastReceiver() {
    @Inject lateinit var engine: DSPEngine

    @Inject lateinit var presetRepository: PresetRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        var route = AudioRoute.SPEAKER

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            val hasUsb = devices.any {
                it.type == android.media.AudioDeviceInfo.TYPE_USB_DEVICE ||
                it.type == android.media.AudioDeviceInfo.TYPE_USB_ACCESSORY ||
                it.type == android.media.AudioDeviceInfo.TYPE_USB_HEADSET
            }
            val hasWired = devices.any {
                it.type == android.media.AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                it.type == android.media.AudioDeviceInfo.TYPE_WIRED_HEADPHONES
            }
            val hasBluetooth = devices.any {
                it.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
            }

            route = when {
                hasUsb -> AudioRoute.USB_AUDIO
                hasWired -> AudioRoute.WIRED_HEADSET
                hasBluetooth -> AudioRoute.BLUETOOTH_A2DP
                else -> AudioRoute.SPEAKER
            }
        } else {
            route = when (intent.action) {
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
        }

        val presetName =
            when (route) {
                AudioRoute.USB_AUDIO -> "Audiophile USB DAC"
                AudioRoute.WIRED_HEADSET -> "Premium Headphone Bass"
                AudioRoute.BLUETOOTH_A2DP -> "Bluetooth Optimized"
                AudioRoute.SPEAKER -> "Speaker Safe"
                else -> null
            }

        engine.updateRoute(route)

        presetName?.let { name ->
            scope.launch {
                val params = presetRepository.findByName(name)
                params?.let {
                    engine.updateParams(it, name)
                    Log.i("AudioRoute", "✅ Route changed → applied preset: $name")
                }
            }
        }
    }
}
