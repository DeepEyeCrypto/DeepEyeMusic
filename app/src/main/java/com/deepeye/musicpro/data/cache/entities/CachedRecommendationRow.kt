package com.deepeye.musicpro.data.cache.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_recommendation_rows")
data class CachedRecommendationRow(
    @PrimaryKey val rowId: String,
    val title: String,
    val subtitle: String,
    val category: String,
    val accentColor: Long,
    val cachedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 6 * 3600_000L,
)
