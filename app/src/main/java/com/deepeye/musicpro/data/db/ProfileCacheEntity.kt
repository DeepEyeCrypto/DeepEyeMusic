// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Per-account snapshot of the library counters shown on the profile dashboard.
 *
 * Written only when the live [com.deepeye.musicpro.domain.repository.library.LibraryRepository]
 * counters are already flowing (i.e. the user is signed in and the library has
 * been observed at least once); it is read back only when the live flow is not
 * available (e.g. offline first launch) so the profile can show the last
 * known counts instead of zeros.
 */
@Entity(tableName = "profile_cache")
data class ProfileCacheEntity(
    @PrimaryKey val accountKey: String,
    val subscribedCount: Int = 0,
    val likedCount: Int = 0,
    val playlistCount: Int = 0,
    val downloadCount: Int = 0,
    val lastSyncedAt: Long = 0L,
)
