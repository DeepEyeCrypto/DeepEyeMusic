package com.deepeye.musicpro.data.cache.entities

import androidx.room.Embedded
import androidx.room.Relation

data class CachedRowWithTracks(
    @Embedded val row: CachedRecommendationRow,
    @Relation(
        parentColumn = "rowId",
        entityColumn = "rowId",
    )
    val tracks: List<CachedTrack>,
)
