package com.deepeye.musicpro.data.cache.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_autoplay_queue")
data class CachedAutoplayQueue(
    @PrimaryKey val videoId: String,
    val title: String,
    val artist: String,
    val channelId: String,
    val reason: String,
    val score: Float,
    val rank: Int,
    val seedVideoId: String,
    val cachedAt: Long = System.currentTimeMillis(),
)
