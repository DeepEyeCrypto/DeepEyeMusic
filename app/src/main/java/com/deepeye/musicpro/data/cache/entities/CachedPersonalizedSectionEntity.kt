// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.cache.entities

import androidx.room.Entity

@Entity(
    tableName = "cached_personalized_sections",
    primaryKeys = ["accountKey", "sectionType"],
)
data class CachedPersonalizedSectionEntity(
    val accountKey: String,
    val sectionType: String,
    val title: String,
    val subtitle: String?,
    val sourceLabel: String,
    val explanation: String?,
    val cachedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 6 * 3600_000L, // 6 hours TTL
)
