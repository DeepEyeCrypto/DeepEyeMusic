// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.cache.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cached_personalized_items",
    indices = [
        Index("accountKey", "sectionType"),
        Index("itemId"),
    ],
)
data class CachedPersonalizedItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountKey: String,
    val sectionType: String,
    val itemId: String,
    val title: String,
    val artist: String,
    val channelId: String? = null,
    val artworkUrl: String? = null,
    val durationMs: Long = 0L,
    val itemType: String = "SONG",
    val sourceBadge: String? = null,
    val explanation: String? = null,
    val rank: Int = 0,
    val cachedAt: Long = System.currentTimeMillis(),
)
