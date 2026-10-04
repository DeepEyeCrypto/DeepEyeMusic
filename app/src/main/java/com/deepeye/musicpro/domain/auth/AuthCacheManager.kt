// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.domain.auth

import android.util.Log
import com.deepeye.musicpro.data.repository.YouTubeRepository
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Manages global cache eradication upon YouTube OAuth lifecycle events.
 * Guarantees that guest / anonymous search and rail caches never leak into authenticated user sessions.
 */
@Singleton
class AuthCacheManager @Inject constructor(
    private val youTubeRepositoryProvider: Provider<YouTubeRepository>
) {
    companion object {
        private const val TAG = "AuthCacheManager"
    }

    suspend fun purgeAllCaches() {
        Log.i(TAG, "[AuthCacheManager] Purging in-memory rail and search caches on auth state transition")
        try {
            youTubeRepositoryProvider.get().clearCache()
        } catch (e: Exception) {
            Log.e(TAG, "Error purging caches", e)
        }
    }
}
