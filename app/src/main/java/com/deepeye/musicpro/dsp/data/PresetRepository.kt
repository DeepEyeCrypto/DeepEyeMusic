package com.deepeye.musicpro.dsp.data

import com.deepeye.musicpro.dsp.model.DspParams
import com.deepeye.musicpro.dsp.model.ReverbPreset
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
     * Seeds the database with 7 built-in presets if they don't exist yet.
     */
    suspend fun seedBuiltinPresets() {
        if (dao.getBuiltinCount() > 0) return

        val builtins = listOf(
            "Flat" to DspParams(enabled = true),

            "Bass Boost" to DspParams(
                enabled = true,
                eqEnabled = true,
                eqBands = listOf(6f, 5f, 4f, 2f, 0f, 0f, 0f, 0f, 0f, 0f),
                bassBoostEnabled = true,
                bassBoostStrength = 600
            ),

            "Vocal Clarity" to DspParams(
                enabled = true,
                eqEnabled = true,
                eqBands = listOf(-2f, -1f, 0f, 2f, 4f, 4f, 3f, 1f, 0f, -1f),
                clarityEnabled = true,
                clarityStrength = 0.7f
            ),

            "Concert Hall" to DspParams(
                enabled = true,
                reverbEnabled = true,
                reverbPreset = ReverbPreset.LARGE_HALL,
                reverbRoomLevel = -800,
                reverbDecayTime = 3000,
                surroundEnabled = true,
                surroundStrength = 700
            ),

            "Night Mode" to DspParams(
                enabled = true,
                dynamicsEnabled = true,
                compressorThreshold = -20f,
                compressorRatio = 6f,
                loudnessEnabled = true,
                loudnessGain = 5f,
                speakerProtectionEnabled = true,
                speakerMaxDb = -3f
            ),

            "Headphone Surround" to DspParams(
                enabled = true,
                virtualizerEnabled = true,
                virtualizerStrength = 800,
                hrtfEnabled = true,
                surroundEnabled = true,
                surroundStrength = 600
            ),

            "Electronic" to DspParams(
                enabled = true,
                eqEnabled = true,
                eqBands = listOf(4f, 3f, 1f, 0f, -2f, -1f, 1f, 3f, 4f, 5f),
                bassBoostEnabled = true,
                bassBoostStrength = 400,
                loudnessEnabled = true,
                loudnessGain = 3f
            )
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
    }
}
