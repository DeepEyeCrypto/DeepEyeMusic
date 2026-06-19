// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.repository

import com.deepeye.musicpro.data.db.RecommendationDao
import com.deepeye.musicpro.data.source.remote.youtube.YoutubeRemoteDataSource
import com.deepeye.musicpro.domain.model.home.HomeFeedState
import com.deepeye.musicpro.domain.model.home.HomeMusicItem
import com.deepeye.musicpro.domain.model.home.HomeVideoItem
import com.deepeye.musicpro.domain.model.home.MoodCategory
import com.deepeye.musicpro.domain.model.home.MoodMix
import com.deepeye.musicpro.domain.repository.MusicRepository
import com.deepeye.musicpro.dsp.engine.DSPEngine
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeFeedRepository
@Inject
constructor(
    private val youtubeDs: YoutubeRemoteDataSource,
    private val localRepo: MusicRepository,
    private val recommendationDao: RecommendationDao,
    private val dspEngine: DSPEngine,
    private val libraryRepo: com.deepeye.musicpro.domain.repository.library.LibraryRepository,
) {
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    suspend fun getHomeFeed(): HomeFeedState =
        withContext(ioDispatcher) {
            // Run all in parallel and catch individual failures to keep the screen partially functional
            val subscriptions = libraryRepo.getAllSubscribedChannels()

            val trendingDeferred =
                async {
                    try {
                        if (subscriptions.isNotEmpty()) {
                            val channel = subscriptions.random()
                            youtubeDs.searchVideos("${channel.channelName} new").take(15)
                        } else {
                            kotlinx.coroutines.withTimeoutOrNull(3000L) { youtubeDs.getTrending() } ?: emptyList()
                        }
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
            val shortsDeferred =
                async {
                    try {
                        if (subscriptions.isNotEmpty()) {
                            val channel = subscriptions.shuffled().first()
                            youtubeDs.searchVideos("${channel.channelName} shorts").filter { it.duration < 65 }.take(15)
                        } else {
                            kotlinx.coroutines.withTimeoutOrNull(3000L) { youtubeDs.getShorts() } ?: emptyList()
                        }
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
            val musicDeferred =
                async {
                    try {
                        if (subscriptions.isNotEmpty()) {
                            val channel = subscriptions.shuffled().first()
                            youtubeDs.searchMusic("${channel.channelName} official music video").take(15)
                        } else {
                            kotlinx.coroutines.withTimeoutOrNull(3000L) { youtubeDs.searchMusic("top hits 2025") } ?: emptyList()
                        }
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
            val localDeferred =
                async {
                    try {
                        localRepo.getRecentlyAdded(limit = 10).first()
                    } catch (e: Exception) {
                        emptyList()
                    }
                }

            // Continue Watching — recent videos with partial progress
            val continueWatchingDeferred =
                async {
                    try {
                        val recentSongs = recommendationDao.getTopSongsSince(
                            System.currentTimeMillis() - 7L * 24 * 3600 * 1000,
                            10
                        )
                        recentSongs.filter { it.avgCompletion < 0.9f && it.avgCompletion > 0.1f }
                            .take(6)
                            .map { stats ->
                                HomeVideoItem(
                                    id = stats.videoId,
                                    title = stats.title,
                                    channelName = stats.artist,
                                    channelId = stats.channelId,
                                    thumbnailUrl = "https://i.ytimg.com/vi/${stats.videoId}/hqdefault.jpg",
                                    progressPercent = stats.avgCompletion,
                                )
                            }
                    } catch (e: Exception) {
                        emptyList()
                    }
                }

            // Continue Listening — recently played music tracks
            val continueListeningDeferred =
                async {
                    try {
                        val recentSongs = recommendationDao.getTopSongsSince(
                            System.currentTimeMillis() - 3L * 24 * 3600 * 1000,
                            10
                        )
                        recentSongs.filter { it.avgCompletion > 0.5f }
                            .take(8)
                            .map { stats ->
                                HomeMusicItem(
                                    id = stats.videoId,
                                    title = stats.title,
                                    artist = stats.artist,
                                    thumbnailUrl = "https://i.ytimg.com/vi/${stats.videoId}/hqdefault.jpg",
                                    lastPlayedAt = stats.lastPlayed,
                                )
                            }
                    } catch (e: Exception) {
                        emptyList()
                    }
                }

            // Local resume — recently played local songs
            val localResumeDeferred =
                async {
                    try {
                        localRepo.getRecentlyPlayed(limit = 8).first().map { song ->
                            HomeMusicItem(
                                id = song.id.toString(),
                                title = song.title,
                                artist = song.artist,
                                thumbnailUrl = song.artUri?.toString() ?: "",
                                duration = song.duration,
                                lastPlayedAt = song.dateModified,
                            )
                        }
                    } catch (e: Exception) {
                        emptyList()
                    }
                }

            val trending = trendingDeferred.await()
            val shorts = shortsDeferred.await()
            val music = musicDeferred.await()
            val local = localDeferred.await()
            val continueWatching = continueWatchingDeferred.await()
            val continueListening = continueListeningDeferred.await()
            val localResume = localResumeDeferred.await()

            android.util.Log.d(
                "HomeFeed",
                "Trending: ${trending.size}, Shorts: ${shorts.size}, Music: ${music.size}, Local: ${local.size}, CW: ${continueWatching.size}, CL: ${continueListening.size}",
            )

            HomeFeedState(
                featuredVideo = trending.firstOrNull(),
                featuredMusic = music.firstOrNull(),
                trending = trending.drop(1).take(12),
                shorts = shorts,
                quickPicks = music.drop(1).take(10),
                continueWatching = continueWatching,
                continueListening = continueListening,
                localResume = localResume,
                moodMixes = buildMoodMixes(),
                activeDspPreset = dspEngine.currentPresetName.value,
                isLoading = false,
                isOffline = trending.isEmpty() && music.isEmpty(),
            )
        }

    private fun buildMoodMixes(): List<MoodMix> {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        
        val baseMoods = listOf(
            MoodMix(MoodCategory.CHILL, "Chill Vibes", "lofi chill beats relax", "🧊", 0xFF00BCD4),
            MoodMix(MoodCategory.ENERGETIC, "Energy Boost", "upbeat workout motivation", "⚡", 0xFFFF5722),
            MoodMix(MoodCategory.ROMANTIC, "Romance", "romantic hindi bollywood", "❤️", 0xFFE91E63),
            MoodMix(MoodCategory.FOCUS, "Deep Focus", "instrumental focus study", "🎯", 0xFF2196F3),
            MoodMix(MoodCategory.SAD, "In My Feels", "sad emotional hindi songs", "🥺", 0xFF607D8B),
            MoodMix(MoodCategory.PARTY, "Party Mode", "party dance punjabi hindi", "🎉", 0xFFFF9800),
            MoodMix(MoodCategory.WORKOUT, "Gym Beast", "gym workout bass heavy", "💪", 0xFF4CAF50),
            MoodMix(MoodCategory.SLEEP, "Sleep", "sleep ambient calm piano", "😴", 0xFF3F51B5),
        )
        
        // Prioritize moods based on time of day
        return baseMoods.sortedByDescending { mood ->
            when (mood.mood) {
                MoodCategory.ENERGETIC, MoodCategory.WORKOUT -> if (hour in 6..10) 10 else 0
                MoodCategory.FOCUS -> if (hour in 10..17) 10 else 0
                MoodCategory.PARTY -> if (hour in 18..23) 10 else 0
                MoodCategory.CHILL, MoodCategory.ROMANTIC -> if (hour in 19..23) 5 else 0
                MoodCategory.SLEEP -> if (hour >= 22 || hour <= 4) 10 else 0
                else -> 1
            }
        }
    }
}
