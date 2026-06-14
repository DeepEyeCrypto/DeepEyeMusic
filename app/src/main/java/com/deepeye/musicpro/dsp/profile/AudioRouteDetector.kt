// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.dsp.profile

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Detects the current audio output route and emits device identifiers
 * when the output device changes (headphones plugged, Bluetooth connected, etc.).
 */
@Singleton
class AudioRouteDetector @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _currentDevice = MutableStateFlow(detectCurrentDevice())
    val currentDevice: StateFlow<AudioDeviceId> = _currentDevice.asStateFlow()

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            _currentDevice.value = detectCurrentDevice()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            _currentDevice.value = detectCurrentDevice()
        }
    }

    init {
        audioManager.registerAudioDeviceCallback(deviceCallback, Handler(Looper.getMainLooper()))
    }

    /**
     * Detects the current primary audio output device.
     * Returns a stable identifier for the device type + name.
     */
    private fun detectCurrentDevice(): AudioDeviceId {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)

        // Priority: Bluetooth A2DP > Wired Headset > USB > Speaker
        val activeDevice = devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP }
            ?: devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET || it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES }
            ?: devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_USB_DEVICE || it.type == AudioDeviceInfo.TYPE_USB_HEADSET }
            ?: devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }

        return if (activeDevice != null) {
            AudioDeviceId(
                id = "${activeDevice.type}_${activeDevice.productName}",
                type = mapDeviceType(activeDevice.type),
                name = activeDevice.productName?.toString() ?: "Unknown",
            )
        } else {
            AudioDeviceId.SPEAKER
        }
    }

    private fun mapDeviceType(type: Int): AudioDeviceType {
        return when (type) {
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> AudioDeviceType.BLUETOOTH

            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> AudioDeviceType.WIRED_HEADSET

            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_USB_HEADSET -> AudioDeviceType.USB

            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> AudioDeviceType.SPEAKER

            else -> AudioDeviceType.OTHER
        }
    }
}

/**
 * Stable identifier for an audio output device.
 */
data class AudioDeviceId(
    val id: String,
    val type: AudioDeviceType,
    val name: String,
) {
    companion object {
        val SPEAKER = AudioDeviceId(
            id = "builtin_speaker",
            type = AudioDeviceType.SPEAKER,
            name = "Speaker",
        )
        val GLOBAL = AudioDeviceId(id = "*", type = AudioDeviceType.OTHER, name = "Global")
    }
}

enum class AudioDeviceType {
    SPEAKER,
    WIRED_HEADSET,
    BLUETOOTH,
    USB,
    CAR,
    OTHER,
}
