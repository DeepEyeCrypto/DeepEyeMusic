// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.cache.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.deepeye.musicpro.data.cache.entities.HiddenContentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HiddenContentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: HiddenContentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<HiddenContentEntity>)

    @Query("SELECT * FROM hidden_content WHERE (accountKey = :accountKey OR (accountKey IS NULL AND :accountKey IS NULL)) ORDER BY hiddenAt DESC")
    fun observeHiddenContent(accountKey: String?): Flow<List<HiddenContentEntity>>

    @Query("SELECT * FROM hidden_content WHERE (accountKey = :accountKey OR (accountKey IS NULL AND :accountKey IS NULL)) ORDER BY hiddenAt DESC")
    suspend fun getAllHiddenContent(accountKey: String?): List<HiddenContentEntity>

    @Query("SELECT * FROM hidden_content WHERE (accountKey = :accountKey OR (accountKey IS NULL AND :accountKey IS NULL)) AND itemType = :itemType ORDER BY hiddenAt DESC")
    suspend fun getHiddenContentByType(accountKey: String?, itemType: String): List<HiddenContentEntity>

    @Query("DELETE FROM hidden_content WHERE itemId = :itemId AND (accountKey = :accountKey OR (accountKey IS NULL AND :accountKey IS NULL))")
    suspend fun deleteByItemId(itemId: String, accountKey: String?)

    @Query("DELETE FROM hidden_content WHERE itemId IN (:itemIds) AND (accountKey = :accountKey OR (accountKey IS NULL AND :accountKey IS NULL))")
    suspend fun deleteByItemIds(itemIds: List<String>, accountKey: String?)

    @Query("DELETE FROM hidden_content WHERE (accountKey = :accountKey OR (accountKey IS NULL AND :accountKey IS NULL))")
    suspend fun deleteAllForAccount(accountKey: String?)

    @Query("DELETE FROM hidden_content")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM hidden_content WHERE (accountKey = :accountKey OR (accountKey IS NULL AND :accountKey IS NULL))")
    suspend fun count(accountKey: String?): Int
}
