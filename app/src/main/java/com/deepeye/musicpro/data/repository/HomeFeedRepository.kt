// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.repository

import com.deepeye.musicpro.data.db.RecommendationDao
import com.deepeye.musicpro.data.source.remote.youtube.YoutubeRemoteDataSource
import com.deepeye.musicpro.domain.model.home.HomeFeedState
import com.deepeye.musicpro.domain.model.home.HomeMusicItem
import com.deepeye.musicpro.domain.model.home.HomeVideoItem
import com.deepeye.musicpro.domain.repository.MusicRepository
import com.deepeye.musicpro.dsp.engine.DSPEngine
import com.deepeye.musicpro.domain.repository.TasteProfileRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import com.deepeye.musicpro.data.source.remote.youtube.AuthenticatedYouTubeClient
import com.deepeye.musicpro.data.source.remote.youtube.MusicFilter
import com.deepeye.musicpro.data.prefs.SettingsDataStore
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
    private val tasteProfileRepo: TasteProfileRepository,
    private val authClient: AuthenticatedYouTubeClient,
) {
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    suspend fun getHomeFeed(): HomeFeedState =
        withContext(ioDispatcher) {
            val tasteProfile = try { tasteProfileRepo.getTasteProfile().first() } catch (e: Exception) { null }
            val preferredLangs = tasteProfile?.preferredLanguages ?: emptySet()
            val langsToInclude = preferredLangs.takeIf { it.isNotEmpty() }?.joinToString(" ") ?: "hindi punjabi english"
            val artists = tasteProfile?.favoriteArtists?.takeIf { it.isNotEmpty() }?.joinToString(" ") ?: ""
            
            // Build negative constraints for non-preferred languages to stop YouTube API bleed
            val negativeKeywords = com.deepeye.musicpro.domain.util.LanguageUtils.buildNegativeLanguageConstraints(preferredLangs)

            val personalQuerySuffix = "$langsToInclude $artists $negativeKeywords".trim()

            // Run all in parallel and catch individual failures to keep the screen partially functional
            val subscriptions = libraryRepo.getAllSubscribedChannels()

            var authHistory: List<HomeVideoItem> = emptyList()
            var authHome: List<HomeVideoItem> = emptyList()
            var authSubscriptions: List<HomeVideoItem> = emptyList()
            var authLiked: List<HomeVideoItem> = emptyList()
            var authMusic: List<HomeVideoItem> = emptyList()
            var authTrending: List<HomeVideoItem> = emptyList()

            val authHomeDeferred = async { authClient.getHomeFeed() }
            val authMusicDeferred = async { authClient.getMusicFeed() }
            val authTrendingDeferred = async { authClient.getTrending() }

            authHome = authHomeDeferred.await()
            authMusic = authMusicDeferred.await()
            authTrending = authTrendingDeferred.await()

            // Common async tasks that run regardless of auth state
            val localDeferred = async {
                try { localRepo.getRecentlyAdded(limit = 10).first() } catch (e: Exception) { emptyList() }
            }
            val continueWatchingDeferred = async {
                try {
                    val recentSongs = recommendationDao.getTopSongsSince(System.currentTimeMillis() - 7L * 24 * 3600 * 1000, 10)
                    recentSongs.filter { it.avgCompletion < 0.9f && it.avgCompletion > 0.1f }.take(6).map { stats ->
                        HomeVideoItem(id = stats.videoId, title = stats.title, channelName = stats.artist, channelId = stats.channelId, thumbnailUrl = "https://i.ytimg.com/vi/${stats.videoId}/maxresdefault.jpg", progressPercent = stats.avgCompletion)
                    }
                } catch (e: Exception) { emptyList() }
            }
            val continueListeningDeferred = async {
                try {
                    val recentSongs = recommendationDao.getTopSongsSince(System.currentTimeMillis() - 3L * 24 * 3600 * 1000, 10)
                    recentSongs.filter { it.avgCompletion > 0.5f }.take(8).map { stats ->
                        HomeMusicItem(id = stats.videoId, title = stats.title, artist = stats.artist, thumbnailUrl = "https://i.ytimg.com/vi/${stats.videoId}/maxresdefault.jpg", lastPlayedAt = stats.lastPlayed)
                    }
                } catch (e: Exception) { emptyList() }
            }
            val localResumeDeferred = async {
                try {
                    localRepo.getRecentlyPlayed(limit = 8).first().map { song ->
                        HomeMusicItem(id = song.id.toString(), title = song.title, artist = song.artist, thumbnailUrl = song.artUri?.toString() ?: "", duration = song.duration, lastPlayedAt = song.dateModified)
                    }
                } catch (e: Exception) { emptyList() }
            }

            val trending = (if (authTrending.isNotEmpty()) authTrending else authHome.take(15))
                .filterNot { MusicFilter.isShort(it.title, it.duration, it.isShort) }
            val filteredHome = authHome.filter { !it.isShort && (it.duration == 0L || it.duration >= 60L) && MusicFilter.isMusicTrack(it.title, it.channelName, it.duration, it.isShort) }
            val musicItems = (authMusic.ifEmpty { authHome }).map {
                HomeMusicItem(id = it.id, title = it.title, artist = it.channelName, thumbnailUrl = it.thumbnailUrl, duration = it.duration)
            }.distinctBy { it.id }
            val music = musicItems.take(15)
            val discoverMix = filteredHome.take(15).map {
                HomeMusicItem(id = it.id, title = it.title, artist = it.channelName, thumbnailUrl = it.thumbnailUrl, duration = it.duration)
            }
            val supermix = authMusic.take(15).map {
                HomeMusicItem(id = it.id, title = it.title, artist = it.channelName, thumbnailUrl = it.thumbnailUrl, duration = it.duration)
            }.distinctBy { it.id }.take(20)
            val becauseYouLikedMix: List<HomeMusicItem> = emptyList()
            val newReleases = authTrending.take(15).map {
                HomeMusicItem(id = it.id, title = it.title, artist = it.channelName, thumbnailUrl = it.thumbnailUrl, duration = it.duration)
            }
            val shorts: List<HomeVideoItem> = emptyList()
            var topLikedArtist: String? = null

            val local = localDeferred.await()
            val continueWatching = continueWatchingDeferred.await()
                .filterNot { MusicFilter.isShort(it.title, it.duration, it.isShort) }
            val continueListening = continueListeningDeferred.await()
                .filterNot { MusicFilter.isShort(it.title, it.duration, false) }
            val localResume = localResumeDeferred.await()

            HomeFeedState(
                featuredVideo = authHome.firstOrNull() ?: trending.firstOrNull(),
                featuredMusic = music.firstOrNull(),
                trending = trending,
                shorts = shorts,
                quickPicks = music.drop(1).take(10),
                continueWatching = continueWatching,
                continueListening = continueListening,
                localResume = localResume,
                supermix = supermix,
                discoverMix = discoverMix,
                activeDspPreset = dspEngine.currentPresetName.value,
                isLoading = false,
                isOffline = trending.isEmpty() && music.isEmpty() && authHome.isEmpty(),
                becauseYouLikedArtist = topLikedArtist,
                becauseYouLikedMix = becauseYouLikedMix,
                newReleases = newReleases,
                hasAuth = false,
            )
        }
}