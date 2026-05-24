// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.domain.repository

import com.deepeye.musicpro.data.db.PlayEvent
import com.deepeye.musicpro.data.db.UserFeedback
import com.deepeye.musicpro.data.prefs.TasteProfile
import com.deepeye.musicpro.domain.model.MediaItem
import kotlinx.coroutines.flow.Flow

interface TasteProfileRepository {
    fun getTasteProfile(): Flow<TasteProfile>
    suspend fun updateOnboardingCompleted(completed: Boolean)
    suspend fun updatePreferredLanguages(langs: Set<String>)
    suspend fun updateFavoriteArtists(artists: Set<String>)
    suspend fun updatePreferredGenres(genres: Set<String>)
    suspend fun updatePreferredMood(mood: String)
    suspend fun updateAutoplayEnabled(enabled: Boolean)
    suspend fun updatePersonalizedMixEnabled(enabled: Boolean)
    suspend fun recordPlayEvent(event: PlayEvent)
    suspend fun recordFeedback(songId: String, liked: Boolean, dontPlayAgain: Boolean)
    suspend fun recordQuickSkip(songId: String)
    suspend fun getFeedback(songId: String): UserFeedback?
    fun getFeedbackFlow(songId: String): Flow<UserFeedback?>
    suspend fun getNextAutoPlayCandidate(currentSongId: String, candidates: List<MediaItem>): MediaItem?
}
