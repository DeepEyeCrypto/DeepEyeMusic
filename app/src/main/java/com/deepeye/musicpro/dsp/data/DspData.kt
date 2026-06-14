// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.dsp.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// ═══════════════════════════════════════════════════
// Entities
// ═══════════════════════════════════════════════════

/**
 * Room entity for per-track and per-device DSP profiles.
 * Composite key: (trackId, deviceId).
 * Use "*" for trackId to represent a device-level profile.
 * Use "*" for deviceId to represent a global profile.
 */
@Entity(
    tableName = "dsp_profiles",
    primaryKeys = ["track_id", "device_id"],
)
data class DspProfileEntity(
    @ColumnInfo(name = "track_id")
    val trackId: String,
    @ColumnInfo(name = "device_id")
    val deviceId: String,
    @ColumnInfo(name = "eq_bands")
    val eqBands: String, // JSON serialized float array
    @ColumnInfo(name = "bass_boost_strength")
    val bassBoostStrength: Int,
    @ColumnInfo(name = "sub_bass_gain")
    val subBassGain: Float = 0f,
    @ColumnInfo(name = "mid_bass_gain")
    val midBassGain: Float = 0f,
    @ColumnInfo(name = "virtualizer_strength")
    val virtualizerStrength: Int,
    @ColumnInfo(name = "reverb_preset")
    val reverbPreset: Int,
    @ColumnInfo(name = "dynamics_enabled")
    val dynamicsEnabled: Boolean = false,
    @ColumnInfo(name = "limiter_threshold")
    val limiterThreshold: Float = -0.5f,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
)

/**
 * Room entity for a saved DSP preset.
 * The JSON field stores serialized [DspParams].
 */
@Entity(tableName = "dsp_presets")
data class DspPresetEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "params_json")
    val paramsJson: String,
    @ColumnInfo(name = "is_builtin")
    val isBuiltin: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "modified_at")
    val modifiedAt: Long = System.currentTimeMillis(),
)

// ═══════════════════════════════════════════════════
// DAOs
// ═══════════════════════════════════════════════════

@Dao
interface DspProfileDao {
    @Query("SELECT * FROM dsp_profiles WHERE track_id = :trackId AND device_id = :deviceId")
    suspend fun getProfile(trackId: String, deviceId: String): DspProfileEntity?

    @Query("SELECT * FROM dsp_profiles WHERE track_id = :trackId")
    suspend fun getProfilesForTrack(trackId: String): List<DspProfileEntity>

    @Query("SELECT * FROM dsp_profiles WHERE device_id = :deviceId AND track_id = '*'")
    suspend fun getDeviceProfile(deviceId: String): DspProfileEntity?

    @Query("SELECT * FROM dsp_profiles WHERE track_id = '*' AND device_id = '*'")
    suspend fun getGlobalProfile(): DspProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: DspProfileEntity)

    @Query("DELETE FROM dsp_profiles WHERE track_id = :trackId AND device_id = :deviceId")
    suspend fun delete(trackId: String, deviceId: String)

    @Query("DELETE FROM dsp_profiles WHERE track_id = :trackId")
    suspend fun deleteAllForTrack(trackId: String)
}

@Dao
interface DspPresetDao {
    @Query("SELECT * FROM dsp_presets ORDER BY is_builtin DESC, name ASC")
    fun getAllPresets(): Flow<List<DspPresetEntity>>

    @Query("SELECT * FROM dsp_presets WHERE id = :id")
    fun getPresetById(id: Long): Flow<DspPresetEntity?>

    @Query("SELECT * FROM dsp_presets WHERE is_builtin = 1 ORDER BY name ASC")
    fun getBuiltinPresets(): Flow<List<DspPresetEntity>>

    @Query("SELECT * FROM dsp_presets WHERE is_builtin = 0 ORDER BY modified_at DESC")
    fun getUserPresets(): Flow<List<DspPresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(preset: DspPresetEntity): Long

    @Update
    suspend fun update(preset: DspPresetEntity)

    @Query("DELETE FROM dsp_presets WHERE id = :id AND is_builtin = 0")
    suspend fun deleteUserPreset(id: Long)

    @Query("SELECT COUNT(*) FROM dsp_presets WHERE is_builtin = 1")
    suspend fun getBuiltinCount(): Int

    @Query("SELECT * FROM dsp_presets WHERE is_builtin = 1")
    suspend fun getBuiltinPresetsList(): List<DspPresetEntity>
}

// ═══════════════════════════════════════════════════
// Database
// ═══════════════════════════════════════════════════

@Database(
    entities = [DspPresetEntity::class, DspProfileEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class DspDatabase : RoomDatabase() {
    abstract fun dspPresetDao(): DspPresetDao
    abstract fun dspProfileDao(): DspProfileDao
}
