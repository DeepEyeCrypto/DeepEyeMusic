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
    suspend fun recordPlayEvent(event: PlayEvent)
    suspend fun recordFeedback(songId: String, liked: Boolean, dontPlayAgain: Boolean)
    suspend fun recordQuickSkip(songId: String)
    suspend fun getFeedback(songId: String): UserFeedback?
    fun getFeedbackFlow(songId: String): Flow<UserFeedback?>
    suspend fun getNextAutoPlayCandidate(currentSongId: String, candidates: List<MediaItem>): MediaItem?
}
