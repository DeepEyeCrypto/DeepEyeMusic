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
    private val authClient: AuthenticatedYouTubeClient,
    private val settingsDataStore: SettingsDataStore,
    private val historyRepo: com.deepeye.musicpro.domain.repository.HistoryRepository,
) {
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    suspend fun getHomeFeed(): HomeFeedState =
        withContext(ioDispatcher) {
            val preferredLangs = emptySet<String>()
            val langsToInclude = "hindi punjabi english"
            val artists = ""
            
            // Build negative constraints for non-preferred languages to stop YouTube API bleed
            val negativeKeywords = com.deepeye.musicpro.domain.util.LanguageUtils.buildNegativeLanguageConstraints(preferredLangs)

            val personalQuerySuffix = "$langsToInclude $artists $negativeKeywords".trim()

            // Run all in parallel and catch individual failures to keep the screen partially functional
            val subscriptions = libraryRepo.getAllSubscribedChannels()
            val authSettings = try { settingsDataStore.settings.first() } catch (e: Exception) { null }
            val hasAuth = authSettings?.youtubeAccessToken != null
            android.util.Log.d("AuthYTClient", "hasAuth: $hasAuth, tokenPresent: ${authSettings?.youtubeAccessToken != null}")

            var authHistory: List<HomeVideoItem> = emptyList()
            var authHome: List<HomeVideoItem> = emptyList()
            var authSubscriptions: List<HomeVideoItem> = emptyList()
            var authLiked: List<HomeVideoItem> = emptyList()
            var authMusic: List<HomeVideoItem> = emptyList()

            if (hasAuth) {
                val authHistoryDeferred = async { try { authClient.getHistory() } catch (e: Exception) { emptyList() } }
                val authHomeDeferred = async { try { authClient.getHomeFeed() } catch (e: Exception) { emptyList() } }
                val authSubsDeferred = async { try { authClient.getSubscriptionsFeed() } catch (e: Exception) { emptyList() } }
                val authLikedDeferred = async { try { authClient.getLikedVideos() } catch (e: Exception) { emptyList() } }
                val authMusicDeferred = async { try { authClient.getMusicFeed() } catch (e: Exception) { emptyList() } }

                authHistory = authHistoryDeferred.await()
                authHome = authHomeDeferred.await()
                authSubscriptions = authSubsDeferred.await()
                authLiked = authLikedDeferred.await()
                authMusic = authMusicDeferred.await()
            }

            // Common async tasks that run regardless of auth state
            val localDeferred = async {
                try { localRepo.getRecentlyAdded(limit = 10).first() } catch (e: Exception) { emptyList() }
            }
            val continueWatchingDeferred = async {
                try {
                    val recentVideos = historyRepo.getRecentVideos(limit = 10).first()
                    recentVideos.filter { 
                        val p = if (it.completionPercent > 1f) it.completionPercent / 100f else it.completionPercent
                        p in 0.03f..0.97f 
                    }.take(6).map { v ->
                        val p = if (v.completionPercent > 1f) v.completionPercent / 100f else v.completionPercent
                        HomeVideoItem(
                            id = v.videoId,
                            title = v.title,
                            channelName = "",
                            channelId = "",
                            thumbnailUrl = v.thumbnailUri ?: "https://i.ytimg.com/vi/${v.videoId}/maxresdefault.jpg",
                            progressPercent = p.coerceIn(0f, 1f),
                            duration = v.durationMs / 1000
                        )
                    }
                } catch (e: Exception) { emptyList() }
            }
            val continueListeningDeferred = async {
                try {
                    val recentPlaybacks = historyRepo.getRecentPlaybacks(limit = 8).first()
                    recentPlaybacks.map { p ->
                        HomeMusicItem(
                            id = p.mediaId,
                            title = p.title,
                            artist = p.artist,
                            thumbnailUrl = p.artworkUri ?: "https://i.ytimg.com/vi/${p.mediaId}/maxresdefault.jpg",
                            lastPlayedAt = p.playedAt
                        )
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

            // When authenticated, use ONLY InnerTube data. Old search-based calls are skipped.
            val trending: List<HomeVideoItem>
            val shorts: List<HomeVideoItem> = emptyList()
            val music: List<HomeMusicItem>
            val discoverMix: List<HomeMusicItem>
            val supermix: List<HomeMusicItem>
            var topLikedArtist: String? = null
            val becauseYouLikedMix: List<HomeMusicItem>
            val newReleases: List<HomeMusicItem>

            if (hasAuth) {
                trending = authHome
                    .filterNot { MusicFilter.isShort(it.title, it.duration, it.isShort) }
                    .take(15)
                val filteredLiked = authLiked.filter { !it.isShort && (it.duration == 0L || it.duration >= 60L) && MusicFilter.isMusicTrack(it.title, it.channelName, it.duration, it.isShort) }
                val filteredSubs = authSubscriptions.filter { !it.isShort && (it.duration == 0L || it.duration >= 60L) && MusicFilter.isMusicTrack(it.title, it.channelName, it.duration, it.isShort) }
                val filteredHome = authHome.filter { !it.isShort && (it.duration == 0L || it.duration >= 60L) && MusicFilter.isMusicTrack(it.title, it.channelName, it.duration, it.isShort) }
                val musicItems = (authMusic + filteredLiked).map {
                    HomeMusicItem(id = it.id, title = it.title, artist = it.channelName, thumbnailUrl = it.thumbnailUrl, duration = it.duration)
                }.distinctBy { it.id }
                music = musicItems.take(15)
                discoverMix = if (authMusic.size > 5) {
                    authMusic.drop(5).take(15).map {
                        HomeMusicItem(id = it.id, title = it.title, artist = it.channelName, thumbnailUrl = it.thumbnailUrl, duration = it.duration)
                    }
                } else {
                    filteredHome.take(15).map {
                        HomeMusicItem(id = it.id, title = it.title, artist = it.channelName, thumbnailUrl = it.thumbnailUrl, duration = it.duration)
                    }
                }
                supermix = (filteredLiked + filteredSubs).map {
                    HomeMusicItem(id = it.id, title = it.title, artist = it.channelName, thumbnailUrl = it.thumbnailUrl, duration = it.duration)
                }.distinctBy { it.id }.take(20)
                becauseYouLikedMix = filteredLiked.drop(5).take(15).map {
                    HomeMusicItem(id = it.id, title = it.title, artist = it.channelName, thumbnailUrl = it.thumbnailUrl, duration = it.duration)
                }
                newReleases = filteredSubs.take(15).map {
                    HomeMusicItem(id = it.id, title = it.title, artist = it.channelName, thumbnailUrl = it.thumbnailUrl, duration = it.duration)
                }
            } else {
                // Fallback: old search-based calls for non-authenticated users
                val trendingDeferred = async {
                    try {
                        if (subscriptions.isNotEmpty()) {
                            val channel = subscriptions.random()
                            youtubeDs.searchVideos("${channel.channelName} new")
                                .filterNot { MusicFilter.isShort(it.title, it.duration, it.isShort) }
                                .take(15)
                        } else {
                            (kotlinx.coroutines.withTimeoutOrNull(3000L) { youtubeDs.getTrending() } ?: emptyList())
                                .filterNot { MusicFilter.isShort(it.title, it.duration, it.isShort) }
                        }
                    } catch (e: Exception) { emptyList() }
                }
                val musicDeferred = async {
                    try {
                        if (subscriptions.isNotEmpty()) {
                            val channel = subscriptions.shuffled().first()
                            youtubeDs.searchMusic("${channel.channelName} official music video").take(15)
                        } else {
                            val fallbackQuery = if (personalQuerySuffix.isNotBlank()) "top hits $personalQuerySuffix" else "top hits 2025"
                            com.deepeye.musicpro.util.Logger.i(com.deepeye.musicpro.util.Logger.Category.HOME_FEED, "Searching music with fallbackQuery: $fallbackQuery")
                            kotlinx.coroutines.withTimeoutOrNull(3000L) { youtubeDs.searchMusic(fallbackQuery) } ?: emptyList()
                        }
                    } catch (e: Exception) { emptyList() }
                }
                val discoverMixDeferred = async {
                    val result = try {
                        val topArtists = recommendationDao.getTopArtistsSince(0, 1)
                        if (topArtists.isNotEmpty()) {
                            val topArtist = topArtists.first().artistName
                            val searchResults = kotlinx.coroutines.withTimeoutOrNull(5000L) { youtubeDs.searchMusic("$topArtist discover new tracks") } ?: emptyList()
                            searchResults.take(15)
                        } else {
                            val fallback = kotlinx.coroutines.withTimeoutOrNull(5000L) { youtubeDs.searchMusic("new indie music discover") } ?: emptyList()
                            fallback.take(15)
                        }
                    } catch (e: Exception) { emptyList() }
                    if (result.isEmpty()) {
                        listOf(
                            HomeMusicItem("dummy4", "Heat Waves", "Glass Animals", "https://i.ytimg.com/vi/mRD0-GxqHVo/mqdefault.jpg"),
                            HomeMusicItem("dummy5", "As It Was", "Harry Styles", "https://i.ytimg.com/vi/H5v3kku4y6Q/mqdefault.jpg")
                        )
                    } else result
                }
                val supermixDeferred = async {
                    val result = try {
                        val topSongs = recommendationDao.getTopSongsSince(0, 5)
                        if (topSongs.isNotEmpty()) {
                            val topSong = topSongs.first()
                            val relatedRemote = kotlinx.coroutines.withTimeoutOrNull(5000L) { youtubeDs.getRelatedMusic(title = topSong.title, artist = topSong.artist) } ?: emptyList()
                            val related = relatedRemote.take(15).map { HomeMusicItem(id = it.id, title = it.title, artist = it.artist, thumbnailUrl = it.artworkUri?.toString() ?: "") }
                            val topMapped = topSongs.map { stats -> HomeMusicItem(id = stats.videoId, title = stats.title, artist = stats.artist, thumbnailUrl = "https://i.ytimg.com/vi/${stats.videoId}/maxresdefault.jpg") }
                            val mix = mutableListOf<HomeMusicItem>()
                            mix.addAll(topMapped)
                            mix.addAll(related)
                            mix.distinctBy { it.id }.take(20)
                        } else {
                            val fallback = kotlinx.coroutines.withTimeoutOrNull(5000L) { youtubeDs.searchMusic("latest hits 2026") } ?: emptyList()
                            fallback.take(15)
                        }
                    } catch (e: Exception) { emptyList() }
                    if (result.isEmpty()) {
                        listOf(
                            HomeMusicItem("dummy1", "Blinding Lights", "The Weeknd", "https://i.ytimg.com/vi/4NRXx6U8ABQ/mqdefault.jpg"),
                            HomeMusicItem("dummy2", "Levitating", "Dua Lipa", "https://i.ytimg.com/vi/TUVcZfQe-Kw/mqdefault.jpg")
                        )
                    } else result
                }
                val becauseYouLikedDeferred = async {
                    try {
                        val topArtists = recommendationDao.getTopArtistsSince(System.currentTimeMillis() - 7L * 24 * 3600 * 1000, 1)
                        if (topArtists.isNotEmpty()) {
                            topLikedArtist = topArtists.first().artistName
                            val searchResults = kotlinx.coroutines.withTimeoutOrNull(5000L) { youtubeDs.searchMusic("similar to $topLikedArtist $negativeKeywords".trim()) } ?: emptyList()
                            searchResults.take(15)
                        } else emptyList()
                    } catch (e: Exception) { emptyList() }
                }
                val newReleasesDeferred = async {
                    try {
                        val searchResults = kotlinx.coroutines.withTimeoutOrNull(5000L) { youtubeDs.searchMusic("latest new releases 2026 $langsToInclude $negativeKeywords".trim()) } ?: emptyList()
                        searchResults.take(15)
                    } catch (e: Exception) { emptyList() }
                }

                trending = trendingDeferred.await()
                music = musicDeferred.await()
                discoverMix = discoverMixDeferred.await()
                supermix = supermixDeferred.await()
                becauseYouLikedMix = becauseYouLikedDeferred.await()
                newReleases = newReleasesDeferred.await()
            }

            val local = localDeferred.await()
            val continueWatching = (if (hasAuth && authHistory.isNotEmpty()) authHistory.take(15) else continueWatchingDeferred.await())
                .filterNot { MusicFilter.isShort(it.title, it.duration, it.isShort) }
            val continueListening = continueListeningDeferred.await()
                .filterNot { MusicFilter.isShort(it.title, it.duration, false) }
            val localResume = localResumeDeferred.await()

            android.util.Log.d(
                "HomeFeed",
                "Auth=$hasAuth, Trending: ${trending.size}, Shorts: ${shorts.size}, CW: ${continueWatching.size}, Supermix: ${supermix.size}, Discover: ${discoverMix.size}",
            )

            HomeFeedState(
                featuredVideo = if (hasAuth) authHome.firstOrNull() else trending.firstOrNull(),
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
                hasAuth = hasAuth,
            )
        }
}