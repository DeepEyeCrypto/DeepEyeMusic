// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore

val Context.gamificationDataStore: DataStore<Preferences> by preferencesDataStore(name = "deepeye_gamification")

object GamificationPreferences {
    val CURRENT_STREAK = intPreferencesKey("current_streak")
    val LONGEST_STREAK = intPreferencesKey("longest_streak")
    val LAST_LISTENING_DATE = longPreferencesKey("last_listening_date") // Stored as epoch epoch milli
    val TOTAL_SONGS_LISTENED = intPreferencesKey("total_songs_listened")
    val TOTAL_LISTENING_HOURS = intPreferencesKey("total_listening_hours")
    val PLAYLISTS_CREATED = intPreferencesKey("playlists_created")
    val SONGS_SHARED = intPreferencesKey("songs_shared")
    val GENRES_EXPLORED = intPreferencesKey("genres_explored")
    val UNLOCKED_BADGES = stringSetPreferencesKey("unlocked_badges")
    val REWARD_POINTS = intPreferencesKey("reward_points")
    val DAILY_GOAL_COMPLETED = booleanPreferencesKey("daily_goal_completed")
    val DAILY_LISTENING_MINUTES = intPreferencesKey("daily_listening_minutes")
    val DAILY_GOAL_COMPLETED_COUNT = intPreferencesKey("daily_goal_completed_count")
}
