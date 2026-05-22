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
// Entity
// ═══════════════════════════════════════════════════

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
    val modifiedAt: Long = System.currentTimeMillis()
)

// ═══════════════════════════════════════════════════
// DAO
// ═══════════════════════════════════════════════════

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
    entities = [DspPresetEntity::class],
    version = 1,
    exportSchema = true
)
abstract class DspDatabase : RoomDatabase() {
    abstract fun dspPresetDao(): DspPresetDao
}
