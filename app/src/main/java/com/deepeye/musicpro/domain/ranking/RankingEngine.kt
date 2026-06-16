// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.domain.ranking

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RankingEngine @Inject constructor() {

    fun calculateTotalScore(
        points: Int,
        streak: Int,
        songsListened: Int,
        activeDays: Int
    ): Float {
        val pointsScore = points.toFloat()
        val streakBonus = streak * 0.1f
        val songsBonus = songsListened * 0.05f
        val activeBonus = activeDays * 0.15f
        return pointsScore + streakBonus + songsBonus + activeBonus
    }

    fun getUserTier(rank: Int): String {
        return when {
            rank <= 10 -> "Legend"
            rank <= 100 -> "Elite"
            rank <= 1000 -> "Rising Star"
            rank <= 10000 -> "Listener"
            else -> "Novice"
        }
    }

    fun getSeasonBadge(seasonWeek: Int, rank: Int): String? {
        val tier = getUserTier(rank)
        if (tier == "Novice" || tier == "Listener") return null
        return "Week $seasonWeek $tier"
    }
}
