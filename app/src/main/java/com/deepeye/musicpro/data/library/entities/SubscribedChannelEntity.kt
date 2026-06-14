// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.library.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subscribed_channels")
data class SubscribedChannelEntity(
    @PrimaryKey val channelId: String,
    val channelName: String,
    val lastSeenVideoId: String = "",
    val subscribedAt: Long = System.currentTimeMillis()
)
