package com.deepeye.musicpro.data.cache.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cached_tracks",
    foreignKeys = [
        ForeignKey(
            entity = CachedRecommendationRow::class,
            parentColumns = ["rowId"],
            childColumns = ["rowId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("rowId"), Index("videoId")],
)
data class CachedTrack(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rowId: String,
    val videoId: String,
    val title: String,
    val artist: String,
    val channelId: String,
    val thumbnailUrl: String,
    val duration: String,
    val viewCount: String,
    val rank: Int,
    val cachedAt: Long = System.currentTimeMillis(),
)
