// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.domain.gamification

import com.deepeye.musicpro.data.prefs.GamificationPreferences
import com.deepeye.musicpro.domain.repository.GamificationRepository

import java.time.Instant
import java.time.LocalDate

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

import com.deepeye.musicpro.domain.ranking.RankingRepository
import com.deepeye.musicpro.domain.ranking.RankingEngine

@Singleton
class GamificationEngine @Inject constructor(
    private val repository: GamificationRepository,
    private val rankingRepository: RankingRepository,
    private val rankingEngine: RankingEngine
) {
    private val _achievementEvents = MutableSharedFlow<AchievementUnlockedEvent>()
    val achievementEvents = _achievementEvents.asSharedFlow()

    // GHOST-OMEGA (non-destructive): gamification disabled — no streak/XP/level writes.
    // Playback pipeline (history/library) is preserved; only reward loops are silenced.
    // All bodies below are intentional no-ops to keep DI/call sites compiling.
    suspend fun checkAndUpdateStreak() {
        return
    }

    suspend fun updateSongCompletion(durationMs: Long, completionRatio: Float) {
        return
    }

    suspend fun updateDailyListeningMinutes(minutesAdded: Int) {
        return
    }

    suspend fun restoreFromFirestore() {
        return
    }

    suspend fun forceSyncToFirestore() {
        return
    }

    private fun unlockBadgeInternalNoOp() {
        // GHOST-OMEGA: badge events disabled.
        return
    }
}
