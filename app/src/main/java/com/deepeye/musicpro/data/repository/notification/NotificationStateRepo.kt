// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.repository.notification

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lightweight, persistent state tracker for Background YouTube Upload Notifications.
 * Prevents alert duplication and tracks timestamp markers on Dispatchers.IO.
 */
@Singleton
class NotificationStateRepo @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("deepeye_notification_state", Context.MODE_PRIVATE)
    }

    suspend fun isVideoNotified(videoId: String): Boolean = withContext(Dispatchers.IO) {
        val notifiedSet = prefs.getStringSet(KEY_NOTIFIED_VIDEO_IDS, emptySet()) ?: emptySet()
        notifiedSet.contains(videoId)
    }

    suspend fun markVideoNotified(videoId: String, channelName: String, title: String) = withContext(Dispatchers.IO) {
        val currentSet = prefs.getStringSet(KEY_NOTIFIED_VIDEO_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
        // Maintain a sliding window of the last 300 notified video IDs to avoid unbounded growth
        if (currentSet.size > 300) {
            val list = currentSet.toList()
            currentSet.clear()
            currentSet.addAll(list.takeLast(200))
        }
        currentSet.add(videoId)

        prefs.edit()
            .putStringSet(KEY_NOTIFIED_VIDEO_IDS, currentSet)
            .putLong(KEY_LAST_CHECKED_TIMESTAMP, System.currentTimeMillis())
            .apply()
    }

    suspend fun getLastCheckedTimestamp(): Long = withContext(Dispatchers.IO) {
        prefs.getLong(KEY_LAST_CHECKED_TIMESTAMP, 0L)
    }

    suspend fun setLastCheckedTimestamp(timestamp: Long) = withContext(Dispatchers.IO) {
        prefs.edit().putLong(KEY_LAST_CHECKED_TIMESTAMP, timestamp).apply()
    }

    suspend fun isNotificationsEnabled(): Boolean = withContext(Dispatchers.IO) {
        prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    companion object {
        private const val KEY_NOTIFIED_VIDEO_IDS = "notified_video_ids"
        private const val KEY_LAST_CHECKED_TIMESTAMP = "last_checked_timestamp"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
    }
}
