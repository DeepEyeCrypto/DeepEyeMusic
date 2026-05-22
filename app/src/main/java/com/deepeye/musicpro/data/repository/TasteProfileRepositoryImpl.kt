// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.repository

import com.deepeye.musicpro.data.db.PlayEvent
import com.deepeye.musicpro.data.db.TasteDao
import com.deepeye.musicpro.data.db.UserFeedback
import com.deepeye.musicpro.data.prefs.TasteProfile
import com.deepeye.musicpro.data.prefs.TasteProfileDataStore
import com.deepeye.musicpro.domain.model.MediaItem
import com.deepeye.musicpro.domain.repository.TasteProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TasteProfileRepositoryImpl @Inject constructor(
    private val tasteProfileDataStore: TasteProfileDataStore,
    private val tasteDao: TasteDao
) : TasteProfileRepository {

    override fun getTasteProfile(): Flow<TasteProfile> =
        tasteProfileDataStore.tasteProfile

    override suspend fun updateOnboardingCompleted(completed: Boolean) {
        tasteProfileDataStore.setOnboardingCompleted(completed)
    }

    override suspend fun updatePreferredLanguages(langs: Set<String>) {
        tasteProfileDataStore.setPreferredLanguages(langs)
    }

    override suspend fun updateFavoriteArtists(artists: Set<String>) {
        tasteProfileDataStore.setFavoriteArtists(artists)
    }

    override suspend fun recordPlayEvent(event: PlayEvent) {
        tasteDao.insertPlayEvent(event)
    }

    override suspend fun recordFeedback(songId: String, liked: Boolean, dontPlayAgain: Boolean) {
        val existing = tasteDao.getFeedback(songId)
        val updated = existing?.copy(liked = liked, dontPlayAgain = dontPlayAgain)
            ?: UserFeedback(songId = songId, liked = liked, dontPlayAgain = dontPlayAgain)
        tasteDao.insertFeedback(updated)
    }

    // Helper for quick skips
    override suspend fun recordQuickSkip(songId: String) {
        val existing = tasteDao.getFeedback(songId)
        val events = tasteDao.getPlayEventsForSong(songId)
        
        // Count historical quick skips (playedMs < 10 seconds and duration > 15s to be safe)
        val quickSkipCount = events.count { it.playedMs < 10000 && it.durationMs > 15000 }
        val shouldBlock = (quickSkipCount >= 2) // Auto-block on 2 or more quick skips!
        
        val updated = existing?.copy(
            skippedQuickly = true,
            dontPlayAgain = shouldBlock || existing.dontPlayAgain
        ) ?: UserFeedback(
            songId = songId,
            skippedQuickly = true,
            dontPlayAgain = shouldBlock
        )
        tasteDao.insertFeedback(updated)
    }

    override suspend fun getFeedback(songId: String): UserFeedback? =
        tasteDao.getFeedback(songId)

    override fun getFeedbackFlow(songId: String): Flow<UserFeedback?> =
        tasteDao.getFeedbackFlow(songId)

    override suspend fun getNextAutoPlayCandidate(currentSongId: String, candidates: List<MediaItem>): MediaItem? {
        if (candidates.isEmpty()) return null

        val profile = tasteProfileDataStore.tasteProfile.first()
        val preferredLanguages = profile.preferredLanguages
        val favoriteArtists = profile.favoriteArtists

        val recentEvents = try {
            tasteDao.getAllPlayEvents().first().take(20)
        } catch (e: Exception) {
            emptyList()
        }

        // Calculate language and artist historical weights
        val favoriteLanguagesFromHistory = recentEvents
            .groupBy { it.language }
            .mapValues { it.value.size }

        val favoriteArtistsFromHistory = recentEvents
            .groupBy { it.artistId }
            .mapValues { it.value.size }

        val last5Ids = recentEvents.take(5).map { it.songId }.toSet()
        val history6to20Ids = recentEvents.drop(5).map { it.songId }.toSet()

        val scoredCandidates = candidates
            .filter { it.id != currentSongId }
            .map { item ->
                val feedback = tasteDao.getFeedback(item.id)
                if (feedback?.dontPlayAgain == true) {
                    return@map item to -9999
                }

                var score = 0

                // 1. Language check
                val detectedLanguage = getLanguageForMedia(item)
                if (preferredLanguages.contains(detectedLanguage)) {
                    score += 3
                }
                val historicalLanguageCount = favoriteLanguagesFromHistory[detectedLanguage] ?: 0
                score += (historicalLanguageCount * 1).coerceAtMost(5)

                // 2. Artist check
                val isPrefArtist = favoriteArtists.any { it.equals(item.artist, ignoreCase = true) }
                if (isPrefArtist) {
                    score += 4
                }
                val artistKey = when (item) {
                    is MediaItem.Local -> item.song.artistId.toString()
                    is MediaItem.Remote -> item.artist
                }
                val historicalArtistCount = favoriteArtistsFromHistory[artistKey] ?: 0
                score += (historicalArtistCount * 2).coerceAtMost(6)

                // 3. User feedback check
                if (feedback != null) {
                    if (feedback.liked) score += 5
                    if (feedback.skippedQuickly) score -= 6
                }

                // 4. Play History check
                if (last5Ids.contains(item.id)) {
                    score -= 4
                } else if (history6to20Ids.contains(item.id)) {
                    score += 2
                }

                item to score
            }
            .filter { it.second > -5000 } // Exclude blocked

        if (scoredCandidates.isEmpty()) return null

        // Pick randomly from the top scored items to keep autoplay dynamic and diverse
        val maxScore = scoredCandidates.maxOf { it.second }
        val threshold = maxScore - 2 // Include any candidate within 2 points of the top score
        val topCandidates = scoredCandidates.filter { it.second >= threshold }.map { it.first }

        return topCandidates.randomOrNull() ?: scoredCandidates.maxByOrNull { it.second }?.first
    }

    private fun getLanguageForMedia(item: MediaItem): String {
        val titleLower = item.title.lowercase()
        val langs = listOf("hindi", "punjabi", "bhojpuri", "tamil", "telugu", "english", "haryanvi", "bengali", "korean")
        for (lang in langs) {
            if (titleLower.contains(lang)) return lang.replaceFirstChar { it.uppercase() }
        }
        if (item is MediaItem.Local) {
            val genreLower = item.song.genre.lowercase()
            for (lang in langs) {
                if (genreLower.contains(lang)) return lang.replaceFirstChar { it.uppercase() }
            }
            return if (item.song.genre.isNotEmpty()) item.song.genre else "Local"
        }
        return "English"
    }
}
