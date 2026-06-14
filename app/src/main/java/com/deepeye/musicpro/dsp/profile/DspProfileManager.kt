// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.dsp.profile

import android.util.Log
import com.deepeye.musicpro.dsp.data.DspProfileDao
import com.deepeye.musicpro.dsp.data.DspProfileEntity
import com.deepeye.musicpro.dsp.engine.DSPEngine
import com.deepeye.musicpro.dsp.model.DspParams
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages DSP profile resolution with priority order:
 * per-track → per-device → global.
 *
 * Loads profiles within 100ms target by using Room suspend queries.
 */
@Singleton
class DspProfileManager @Inject constructor(
    private val dao: DspProfileDao,
    private val audioRouteDetector: AudioRouteDetector,
    private val dspEngine: DSPEngine,
) {
    companion object {
        private const val TAG = "DspProfileManager"
        private const val GLOBAL_KEY = "*"
    }

    private val gson = Gson()

    /**
     * Resolves and applies the best DSP profile for the given track.
     * Resolution order: per-track → per-device → global → current engine params.
     */
    suspend fun loadAndApplyProfile(trackId: String) {
        val deviceId = audioRouteDetector.currentDevice.value.id
        val profile = resolveProfile(trackId, deviceId)

        if (profile != null) {
            val params = entityToParams(profile)
            dspEngine.updateParams(params, "Loaded Profile")
            Log.d(TAG, "Applied profile: track=$trackId, device=$deviceId")
        }
    }

    /**
     * Resolves the best profile without applying it.
     * Returns null if no saved profile exists (use current/global).
     */
    suspend fun resolveProfile(trackId: String, deviceId: String): DspProfileEntity? {
        return withContext(Dispatchers.IO) {
            // 1. Per-track + per-device (most specific)
            dao.getProfile(trackId, deviceId)
                // 2. Per-track, any device
                ?: dao.getProfile(trackId, GLOBAL_KEY)
                // 3. Per-device (global track)
                ?: dao.getDeviceProfile(deviceId)
                // 4. Global profile
                ?: dao.getGlobalProfile()
        }
    }

    /**
     * Saves the current DSP params as a per-track profile.
     */
    suspend fun saveProfileForTrack(trackId: String, params: DspParams) {
        val deviceId = audioRouteDetector.currentDevice.value.id
        val entity = paramsToEntity(trackId, deviceId, params)
        withContext(Dispatchers.IO) {
            dao.upsert(entity)
        }
        Log.d(TAG, "Saved per-track profile: track=$trackId, device=$deviceId")
    }

    /**
     * Saves the current DSP params as a per-device profile (applies to all tracks on this device).
     */
    suspend fun saveProfileForDevice(params: DspParams) {
        val deviceId = audioRouteDetector.currentDevice.value.id
        val entity = paramsToEntity(GLOBAL_KEY, deviceId, params)
        withContext(Dispatchers.IO) {
            dao.upsert(entity)
        }
        Log.d(TAG, "Saved per-device profile: device=$deviceId")
    }

    /**
     * Deletes the per-track profile, reverting to device/global.
     */
    suspend fun deleteProfileForTrack(trackId: String) {
        val deviceId = audioRouteDetector.currentDevice.value.id
        withContext(Dispatchers.IO) {
            dao.delete(trackId, deviceId)
            dao.delete(trackId, GLOBAL_KEY)
        }
        Log.d(TAG, "Deleted per-track profile: track=$trackId")
    }

    /**
     * Checks if a per-track profile exists for the given track.
     */
    suspend fun hasProfileForTrack(trackId: String): Boolean {
        return withContext(Dispatchers.IO) {
            val deviceId = audioRouteDetector.currentDevice.value.id
            dao.getProfile(trackId, deviceId) != null || dao.getProfile(trackId, GLOBAL_KEY) != null
        }
    }

    /**
     * Called when audio output device changes — reloads the appropriate profile.
     */
    suspend fun onDeviceChanged(trackId: String?) {
        if (trackId != null) {
            loadAndApplyProfile(trackId)
        }
    }

    // ═══════════════════════════════════════════════════
    // Conversion helpers
    // ═══════════════════════════════════════════════════

    private fun paramsToEntity(trackId: String, deviceId: String, params: DspParams): DspProfileEntity {
        return DspProfileEntity(
            trackId = trackId,
            deviceId = deviceId,
            eqBands = gson.toJson(params.eqBands.toList()),
            bassBoostStrength = params.bassBoostStrength,
            subBassGain = 0f, // Extended bass fields (future use)
            midBassGain = 0f,
            virtualizerStrength = params.virtualizerStrength,
            reverbPreset = params.reverbPreset.ordinal,
            dynamicsEnabled = params.dynamicsEnabled,
            limiterThreshold = params.limiterThreshold,
            updatedAt = System.currentTimeMillis(),
        )
    }

    private fun entityToParams(entity: DspProfileEntity): DspParams {
        val eqBands: FloatArray = try {
            val type = object : TypeToken<List<Float>>() {}.type
            val list: List<Float> = gson.fromJson(entity.eqBands, type)
            list.toFloatArray()
        } catch (e: Exception) {
            FloatArray(10) { 0f }
        }

        return DspParams(
            enabled = true,
            eqEnabled = true,
            eqBands = eqBands,
            bassBoostEnabled = entity.bassBoostStrength > 0,
            bassBoostStrength = entity.bassBoostStrength,
            virtualizerEnabled = entity.virtualizerStrength > 0,
            virtualizerStrength = entity.virtualizerStrength,
            dynamicsEnabled = entity.dynamicsEnabled,
            limiterEnabled = entity.dynamicsEnabled,
            limiterThreshold = entity.limiterThreshold,
        )
    }
}
