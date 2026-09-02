// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.cache.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.deepeye.musicpro.data.cache.entities.CachedPersonalizedItemEntity
import com.deepeye.musicpro.data.cache.entities.CachedPersonalizedSectionEntity

@Dao
interface PersonalizedSectionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSection(section: CachedPersonalizedSectionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItems(items: List<CachedPersonalizedItemEntity>)

    @Query("SELECT * FROM cached_personalized_sections WHERE accountKey = :accountKey AND sectionType = :sectionType LIMIT 1")
    suspend fun getSection(accountKey: String, sectionType: String): CachedPersonalizedSectionEntity?

    @Query("SELECT * FROM cached_personalized_items WHERE accountKey = :accountKey AND sectionType = :sectionType ORDER BY rank ASC")
    suspend fun getItemsForSection(accountKey: String, sectionType: String): List<CachedPersonalizedItemEntity>

    @Query("SELECT * FROM cached_personalized_sections WHERE accountKey = :accountKey")
    suspend fun getAllSectionsForAccount(accountKey: String): List<CachedPersonalizedSectionEntity>

    @Query("DELETE FROM cached_personalized_items WHERE accountKey = :accountKey AND sectionType = :sectionType")
    suspend fun deleteItemsForSection(accountKey: String, sectionType: String)

    @Query("DELETE FROM cached_personalized_sections WHERE accountKey = :accountKey AND sectionType = :sectionType")
    suspend fun deleteSection(accountKey: String, sectionType: String)

    @Query("DELETE FROM cached_personalized_sections WHERE accountKey = :accountKey")
    suspend fun deleteAccountSections(accountKey: String)

    @Query("DELETE FROM cached_personalized_items WHERE accountKey = :accountKey")
    suspend fun deleteAccountItems(accountKey: String)

    @Query("DELETE FROM cached_personalized_sections WHERE expiresAt < :now")
    suspend fun deleteExpiredSections(now: Long = System.currentTimeMillis())

    @Query("DELETE FROM cached_personalized_items WHERE cachedAt < :cutoff")
    suspend fun deleteOldItems(cutoff: Long)

    @Query("DELETE FROM cached_personalized_sections")
    suspend fun clearAllSections()

    @Query("DELETE FROM cached_personalized_items")
    suspend fun clearAllItems()

    @Transaction
    suspend fun saveSectionWithItems(
        section: CachedPersonalizedSectionEntity,
        items: List<CachedPersonalizedItemEntity>,
    ) {
        upsertSection(section)
        deleteItemsForSection(section.accountKey, section.sectionType)
        if (items.isNotEmpty()) {
            upsertItems(items)
        }
    }

    @Transaction
    suspend fun invalidateAccountCache(accountKey: String) {
        deleteAccountSections(accountKey)
        deleteAccountItems(accountKey)
    }

    @Transaction
    suspend fun clearAll() {
        clearAllSections()
        clearAllItems()
    }
}
