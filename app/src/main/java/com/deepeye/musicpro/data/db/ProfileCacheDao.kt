// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * DAO for the per-account profile counters cache. One row per [AccountSession]
 * account key; upserted whenever the live library counts change and read back
 * only when the live flow is unavailable.
 */
@Dao
interface ProfileCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: ProfileCacheEntity)

    @Query("SELECT * FROM profile_cache WHERE accountKey = :accountKey")
    suspend fun get(accountKey: String): ProfileCacheEntity?
}
