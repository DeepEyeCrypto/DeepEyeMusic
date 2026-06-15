// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.domain.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.deepeye.musicpro.data.prefs.GamificationPreferences
import com.deepeye.musicpro.data.prefs.gamificationDataStore
import com.deepeye.musicpro.domain.gamification.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GamificationRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.gamificationDataStore

    // All predefined badges
    private val ALL_BADGES = listOf(
        UserAchievement(
            badgeId = AchievementRequirement.SONGS_LISTENED_100.name,
            title = "Music Guru",
            description = "Listened to 100 songs",
            iconResId = androidx.media3.ui.R.drawable.exo_ic_audiotrack, // Placeholder icon
            requirement = AchievementRequirement.SONGS_LISTENED_100,
            isUnlocked = false
        ),
        UserAchievement(
            badgeId = AchievementRequirement.STREAK_30_DAYS.name,
            title = "Super Listener",
            description = "Maintained a 30-day listening streak",
            iconResId = androidx.media3.ui.R.drawable.exo_ic_audiotrack,
            requirement = AchievementRequirement.STREAK_30_DAYS,
            isUnlocked = false
        ),
        UserAchievement(
            badgeId = AchievementRequirement.LISTENING_HOURS_50.name,
            title = "Music Enthusiast",
            description = "Listened for 50 total hours",
            iconResId = androidx.media3.ui.R.drawable.exo_ic_audiotrack,
            requirement = AchievementRequirement.LISTENING_HOURS_50,
            isUnlocked = false
        ),
        UserAchievement(
            badgeId = AchievementRequirement.DAILY_GOAL_7_DAYS.name,
            title = "Consistent Listener",
            description = "Hit daily goal 7 times",
            iconResId = androidx.media3.ui.R.drawable.exo_ic_audiotrack,
            requirement = AchievementRequirement.DAILY_GOAL_7_DAYS,
            isUnlocked = false
        )
    )

    fun getGamificationState(): Flow<GamificationState> {
        return dataStore.data.map { prefs ->
            val currentStreak = prefs[GamificationPreferences.CURRENT_STREAK] ?: 0
            val longestStreak = prefs[GamificationPreferences.LONGEST_STREAK] ?: 0
            val lastListeningDateMs = prefs[GamificationPreferences.LAST_LISTENING_DATE] ?: 0L
            val dailyMinutes = prefs[GamificationPreferences.DAILY_LISTENING_MINUTES] ?: 0
            
            val totalPoints = prefs[GamificationPreferences.REWARD_POINTS] ?: 0
            
            val unlockedBadgeIds = prefs[GamificationPreferences.UNLOCKED_BADGES] ?: emptySet()
            
            val unlockedBadges = mutableListOf<UserAchievement>()
            val lockedBadges = mutableListOf<UserAchievement>()
            
            for (badge in ALL_BADGES) {
                if (unlockedBadgeIds.contains(badge.badgeId)) {
                    unlockedBadges.add(badge.copy(isUnlocked = true, unlockedAt = Instant.now())) // Mock time for now
                } else {
                    lockedBadges.add(badge)
                }
            }

            val isDailyGoalCompleted = prefs[GamificationPreferences.DAILY_GOAL_COMPLETED] ?: false
            val dailyGoalCompletedCount = prefs[GamificationPreferences.DAILY_GOAL_COMPLETED_COUNT] ?: 0

            GamificationState(
                streak = ListeningStreak(currentStreak, longestStreak, if (lastListeningDateMs > 0) Instant.ofEpochMilli(lastListeningDateMs) else null, dailyMinutes),
                rewardPoints = RewardPoints(totalPoints, 0, 0, 0),
                unlockedBadges = unlockedBadges,
                lockedBadges = lockedBadges,
                isDailyGoalCompleted = isDailyGoalCompleted,
                dailyGoalCompletedCount = dailyGoalCompletedCount
            )
        }
    }

    suspend fun updatePreferences(action: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        dataStore.edit { prefs ->
            action(prefs)
        }
    }
}
