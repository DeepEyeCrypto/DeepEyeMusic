// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.domain.gamification

import java.time.Instant

enum class AchievementRequirement {
    SONGS_LISTENED_100,      // "Music Guru" badge
    STREAK_30_DAYS,          // "Super Listener" badge
    GENRES_EXPLORED_5,       // "Genre Explorer" badge
    PLAYLISTS_CREATED_10,    // "Playlist Master" badge
    SONGS_SHARED_10,         // "Social Sharer" badge
    LISTENING_HOURS_50,      // "Music Enthusiast" badge
    DAILY_GOAL_7_DAYS,       // "Consistent Listener" badge
}

data class UserAchievement(
    val badgeId: String,
    val title: String,
    val description: String,
    val iconResId: Int, // Vector drawable res ID
    val requirement: AchievementRequirement,
    val isUnlocked: Boolean,
    val unlockedAt: Instant? = null
)

data class ListeningStreak(
    val currentStreak: Int,
    val longestStreak: Int,
    val lastListeningDate: Instant?,
    val dailyMinutes: Int
)

data class RewardPoints(
    val totalPoints: Int,
    val dailyPoints: Int,
    val pointsFromStreak: Int,
    val pointsFromBadges: Int
)

data class GamificationState(
    val streak: ListeningStreak = ListeningStreak(0, 0, null, 0),
    val rewardPoints: RewardPoints = RewardPoints(0, 0, 0, 0),
    val unlockedBadges: List<UserAchievement> = emptyList(),
    val lockedBadges: List<UserAchievement> = emptyList(),
    val isDailyGoalCompleted: Boolean = false,
    val dailyGoalCompletedCount: Int = 0
)

data class AchievementUnlockedEvent(
    val requirement: AchievementRequirement
)
