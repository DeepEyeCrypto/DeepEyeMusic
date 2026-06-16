// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.domain.ranking

data class UserRank(
    val userId: String = "",
    val displayName: String = "User",
    val photoUrl: String? = null,
    val points: Int = 0,
    val streak: Int = 0,
    val songsListened: Int = 0,
    val dailyActiveDays: Int = 0,
    val rank: Int = 999999,
    val score: Float = 0f,
    val badges: List<String> = emptyList()
)

data class LeaderboardSeason(
    val seasonId: String = "",
    val startDate: Long = 0L,
    val endDate: Long = 0L,
    val top100Bonus: Int = 500
)

data class RankNotification(
    val notificationId: String = "",
    val userId: String = "",
    val type: String = "RANK_UP", // e.g., RANK_UP, RANK_DOWN, SEASON_END
    val message: String = "",
    val timestamp: Long = 0L,
    val read: Boolean = false
)
