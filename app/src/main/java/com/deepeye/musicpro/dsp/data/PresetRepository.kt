// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.dsp.data

import android.util.Log
import com.deepeye.musicpro.dsp.model.DspParams
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for DSP presets — handles serialization and seeding of built-in presets.
 */
@Singleton
class PresetRepository @Inject constructor(
    private val dao: DspPresetDao,
    private val gson: Gson
) {

    fun getAllPresets(): Flow<List<Pair<Long, String>>> =
        dao.getAllPresets().map { entities ->
            entities.map { it.id to it.name }
        }

    fun getPresetParams(id: Long): Flow<DspParams?> =
        dao.getPresetById(id).map { entity ->
            entity?.let { gson.fromJson(it.paramsJson, DspParams::class.java) }
        }

    suspend fun savePreset(name: String, params: DspParams): Long {
        val json = gson.toJson(params)
        return dao.insert(
            DspPresetEntity(
                name = name,
                paramsJson = json,
                isBuiltin = false
            )
        )
    }

    suspend fun updatePreset(id: Long, name: String, params: DspParams) {
        val json = gson.toJson(params)
        dao.update(
            DspPresetEntity(
                id = id,
                name = name,
                paramsJson = json,
                modifiedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deletePreset(id: Long) = dao.deleteUserPreset(id)

    /**
     * Finds a preset by name.
     */
    suspend fun findByName(name: String): DspParams? {
        val presets = dao.getBuiltinPresetsList()
        val entity = presets.find { it.name == name }
        return entity?.let { gson.fromJson(it.paramsJson, DspParams::class.java) }
    }

    /**
     * Seeds the database with the Phase D3 premium presets.
     */
    suspend fun seedBuiltinPresets() {
        if (dao.getBuiltinCount() > 0) return

        val builtins = listOf(
            "Flat" to DspParams.flat(),
            "Premium Headphone Bass" to DspParams.premiumHeadphoneBass(),
            "Bollywood Vocals" to DspParams.bollywoodVocals(),
            "Night Mode" to DspParams.nightMode(),
            "Bass Monster" to DspParams.bassMonster(),
            "Speaker Safe" to DspParams.speakerSafe(),
            "Bluetooth Optimized" to DspParams.bluetoothOptimized()
        )

        builtins.forEach { (name, params) ->
            dao.insert(
                DspPresetEntity(
                    name = name,
                    paramsJson = gson.toJson(params),
                    isBuiltin = true
                )
            )
        }
        Log.i("PresetRepository", "✅ Seeded ${builtins.size} premium built-in presets")
    }
}
