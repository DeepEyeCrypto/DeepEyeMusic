package com.deepeye.musicpro.data.cache.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cached_search_results",
    indices = [Index("query")],
)
data class CachedSearchResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val videoId: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val duration: String,
    val rank: Int,
    val cachedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 30 * 60_000L, // 30 mins
)
