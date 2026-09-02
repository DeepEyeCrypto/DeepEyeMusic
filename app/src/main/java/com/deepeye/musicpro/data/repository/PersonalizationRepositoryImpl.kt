// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.repository

import android.net.Uri
import android.util.Log
import com.deepeye.musicpro.account.AccountSession
import com.deepeye.musicpro.account.AccountSessionManager
import com.deepeye.musicpro.data.cache.AccountPersonalizationCache
import com.deepeye.musicpro.data.db.HistoryDao
import com.deepeye.musicpro.data.db.RecommendationDao
import com.deepeye.musicpro.data.db.TasteDao
import com.deepeye.musicpro.data.source.remote.youtube.AuthenticatedYouTubeClient
import com.deepeye.musicpro.data.source.remote.youtube.MusicFilter
import com.deepeye.musicpro.data.source.remote.youtube.YoutubeRemoteDataSource
import com.deepeye.musicpro.domain.model.MediaItem
import com.deepeye.musicpro.domain.model.home.HomeMusicItem
import com.deepeye.musicpro.domain.model.home.HomeVideoItem
import com.deepeye.musicpro.domain.model.personalization.*
import com.deepeye.musicpro.domain.repository.PersonalizationRepository
import com.deepeye.musicpro.domain.repository.library.LibraryRepository
import com.deepeye.musicpro.player.queue.QueueManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production-ready implementation of [PersonalizationRepository].
 *
 * Coordinates local playback history, local queues, on-device affinity models,
 * and authenticated account feeds with strict privacy and account-isolation guarantees.
 */
@Singleton
class PersonalizationRepositoryImpl @Inject constructor(
    private val accountSessionManager: AccountSessionManager,
    private val accountPersonalizationCache: AccountPersonalizationCache,
    private val queueManager: QueueManager,
    private val libraryRepository: LibraryRepository,
    private val historyDao: HistoryDao,
    private val tasteDao: TasteDao,
    private val recommendationDao: RecommendationDao,
    private val authenticatedYouTubeClient: AuthenticatedYouTubeClient,
    private val youtubeRemoteDataSource: YoutubeRemoteDataSource,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : PersonalizationRepository {

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val ioDispatcher = ioDispatcher
    private val refreshMutex = Mutex()

    private val _feedState = MutableStateFlow(PersonalizedFeedState(isLoading = true))
    val feedState: StateFlow<PersonalizedFeedState> = _feedState.asStateFlow()

    companion object {
        private const val TAG = "PersonalizationRepo"
    }

    init {
        observeAccountAndQueue()
    }

    override fun observePersonalizedFeed(): StateFlow<PersonalizedFeedState> = feedState

    private fun observeAccountAndQueue() {
        scope.launch {
            accountSessionManager.accountSession.collectLatest { session ->
                _feedState.update { it.copy(accountSession = session) }
                buildFeed(forceRefresh = false)
            }
        }
    }

    override suspend fun refreshFeed(forceRefresh: Boolean) {
        buildFeed(forceRefresh = forceRefresh)
    }

    override suspend fun refreshSection(sectionType: PersonalizedSectionType) {
        withContext(ioDispatcher) {
            refreshMutex.withLock {
                val currentSections = _feedState.value.sections.toMutableList()
                val session = accountSessionManager.accountSession.value
                val updatedSection = when (sectionType) {
                    PersonalizedSectionType.CONTINUE_LISTENING -> buildContinueListeningSection()
                    PersonalizedSectionType.YOUR_QUEUE -> buildYourQueueSection()
                    PersonalizedSectionType.RECENTLY_PLAYED -> buildRecentlyPlayedSection()
                    PersonalizedSectionType.LIKED_MUSIC -> buildLikedMusicSection(session, forceRefresh = true)
                    PersonalizedSectionType.YOUR_PLAYLISTS -> buildYourPlaylistsSection(session, forceRefresh = true)
                    PersonalizedSectionType.NEW_FROM_SUBSCRIPTIONS -> buildSubscriptionsSection(session, forceRefresh = true)
                    PersonalizedSectionType.BASED_ON_LISTENING -> buildBasedOnListeningSection(forceRefresh = true)
                    PersonalizedSectionType.TRENDING_MUSIC -> buildTrendingSection(forceRefresh = true)
                }

                val index = currentSections.indexOfFirst { it.type == sectionType }
                if (index >= 0) {
                    currentSections[index] = updatedSection
                } else {
                    currentSections.add(updatedSection)
                }
                _feedState.update { it.copy(sections = currentSections, lastUpdatedMillis = System.currentTimeMillis()) }
            }
        }
    }

    override suspend fun invalidateAccountCache(accountKey: String) {
        accountPersonalizationCache.invalidateAccount(accountKey)
    }

    private suspend fun buildFeed(forceRefresh: Boolean) = withContext(ioDispatcher) {
        refreshMutex.withLock {
            _feedState.update { it.copy(isLoading = it.sections.isEmpty(), isRefreshing = it.sections.isNotEmpty()) }
            val session = accountSessionManager.accountSession.value

            supervisorScope {
                val continueListeningDeferred = async { buildContinueListeningSection() }
                val yourQueueDeferred = async { buildYourQueueSection() }
                val recentlyPlayedDeferred = async { buildRecentlyPlayedSection() }
                val likedMusicDeferred = async { buildLikedMusicSection(session, forceRefresh) }
                val playlistsDeferred = async { buildYourPlaylistsSection(session, forceRefresh) }
                val subscriptionsDeferred = async { buildSubscriptionsSection(session, forceRefresh) }
                val basedOnListeningDeferred = async { buildBasedOnListeningSection(forceRefresh) }
                val trendingDeferred = async { buildTrendingSection(forceRefresh) }

                val continueListening = continueListeningDeferred.await()
                val yourQueue = yourQueueDeferred.await()
                val recentlyPlayed = recentlyPlayedDeferred.await()
                val likedMusic = likedMusicDeferred.await()
                val playlists = playlistsDeferred.await()
                val subscriptions = subscriptionsDeferred.await()
                val basedOnListening = basedOnListeningDeferred.await()
                val trending = trendingDeferred.await()

                val assembledSections = mutableListOf<PersonalizedSection>()

                // 1. Continue Listening (if non-empty)
                if (continueListening.items.isNotEmpty()) {
                    assembledSections.add(continueListening)
                }

                // 2. Your Queue (if items present)
                if (yourQueue.items.isNotEmpty()) {
                    assembledSections.add(yourQueue)
                }

                // 3. Recently Played (if items present)
                if (recentlyPlayed.items.isNotEmpty()) {
                    assembledSections.add(recentlyPlayed)
                }

                // 4. Liked Music
                if (session is AccountSession.Connected || likedMusic.items.isNotEmpty()) {
                    assembledSections.add(likedMusic)
                }

                // 5. Your Playlists
                if (playlists.items.isNotEmpty()) {
                    assembledSections.add(playlists)
                }

                // 6. New From Subscriptions (Connected only)
                if (session is AccountSession.Connected) {
                    assembledSections.add(subscriptions)
                }

                // 7. Based on Your Listening
                if (basedOnListening.items.isNotEmpty()) {
                    assembledSections.add(basedOnListening)
                }

                // 8. Trending Music
                if (trending.items.isNotEmpty() || trending.error != null) {
                    assembledSections.add(trending)
                }

                _feedState.update {
                    it.copy(
                        sections = assembledSections,
                        isLoading = false,
                        isRefreshing = false,
                        lastUpdatedMillis = System.currentTimeMillis(),
                        globalError = null,
                    )
                }
            }
        }
    }

    // ── 1. Continue Listening (Local History, Incomplete Progress) ───────────
    private suspend fun buildContinueListeningSection(): PersonalizedSection {
        return try {
            val recentPlaybacks = historyDao.getRecentPlaybacks(20).firstOrNull() ?: emptyList()
            val items = recentPlaybacks
                .filter { it.mediaId.isNotBlank() }
                .distinctBy { it.mediaId }
                .take(10)
                .map { playback ->
                    PersonalizedFeedItem(
                        id = playback.mediaId,
                        title = playback.title,
                        artist = playback.artist,
                        artworkUrl = playback.artworkUri,
                        durationMs = playback.totalDurationMs,
                        itemType = PersonalizedItemType.SONG,
                        sourceBadge = "RESUME",
                        mediaItem = MediaItem.Remote(
                            id = playback.mediaId,
                            title = playback.title,
                            artist = playback.artist,
                            artworkUri = playback.artworkUri?.let { Uri.parse(it) },
                            duration = playback.totalDurationMs,
                        ),
                        lastPlayedAt = playback.playedAt,
                    )
                }

            PersonalizedSection(
                id = "sec_continue_listening",
                type = PersonalizedSectionType.CONTINUE_LISTENING,
                title = "Continue Listening",
                subtitle = "Jump back in",
                sourceLabel = "Local",
                items = items,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to build Continue Listening section", e)
            PersonalizedSection(
                id = "sec_continue_listening",
                type = PersonalizedSectionType.CONTINUE_LISTENING,
                title = "Continue Listening",
                subtitle = "Jump back in",
                sourceLabel = "Local",
                error = "Could not load continue listening",
                canRetry = true,
            )
        }
    }

    // ── 2. Your Queue (Active Local Queue) ──────────────────────────────────
    private fun buildYourQueueSection(): PersonalizedSection {
        val queueItems = queueManager.queue.value
        val items = queueItems.mapIndexed { index, mediaItem ->
            PersonalizedFeedItem(
                id = mediaItem.id,
                title = mediaItem.title,
                artist = mediaItem.artist,
                artworkUrl = mediaItem.artworkUri?.toString(),
                durationMs = mediaItem.duration,
                itemType = PersonalizedItemType.SONG,
                sourceBadge = if (index == queueManager.currentIndex.value) "NOW PLAYING" else "QUEUED",
                mediaItem = mediaItem,
            )
        }

        return PersonalizedSection(
            id = "sec_your_queue",
            type = PersonalizedSectionType.YOUR_QUEUE,
            title = "Your Queue",
            subtitle = if (items.isNotEmpty()) "${items.size} tracks queued" else "Queue is empty",
            sourceLabel = "Local Queue",
            items = items,
        )
    }

    // ── 3. Recently Played (Deduplicated Local History) ─────────────────────
    private suspend fun buildRecentlyPlayedSection(): PersonalizedSection {
        return try {
            val playEvents = tasteDao.getRecentHistory(30).firstOrNull() ?: emptyList()
            // Deduplicate consecutive/duplicate tracks
            val seen = mutableSetOf<String>()
            val items = mutableListOf<PersonalizedFeedItem>()
            for (event in playEvents) {
                if (seen.add(event.songId)) {
                    items.add(
                        PersonalizedFeedItem(
                            id = event.songId,
                            title = event.title,
                            artist = event.artistId,
                            artworkUrl = event.artworkUri,
                            durationMs = event.playedMs,
                            itemType = PersonalizedItemType.SONG,
                            sourceBadge = "RECENT",
                            mediaItem = MediaItem.Remote(
                                id = event.songId,
                                title = event.title,
                                artist = event.artistId,
                                artworkUri = event.artworkUri?.let { Uri.parse(it) },
                                duration = event.playedMs,
                            ),
                            lastPlayedAt = event.timestamp,
                        )
                    )
                }
            }

            PersonalizedSection(
                id = "sec_recently_played",
                type = PersonalizedSectionType.RECENTLY_PLAYED,
                title = "Recently Played",
                subtitle = "Your listening history",
                sourceLabel = "Local History",
                items = items.take(15),
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to build Recently Played section", e)
            PersonalizedSection(
                id = "sec_recently_played",
                type = PersonalizedSectionType.RECENTLY_PLAYED,
                title = "Recently Played",
                subtitle = "Your listening history",
                sourceLabel = "Local History",
                error = "Could not load playback history",
                canRetry = true,
            )
        }
    }

    // ── 4. Liked Music (Account or Local Library) ───────────────────────────
    private suspend fun buildLikedMusicSection(
        session: AccountSession,
        forceRefresh: Boolean,
    ): PersonalizedSection {
        if (session is AccountSession.Connected) {
            if (!forceRefresh) {
                val cached = accountPersonalizationCache.get(session.accountKey, PersonalizedSectionType.LIKED_MUSIC)
                if (cached != null) {
                    return PersonalizedSection(
                        id = "sec_liked_music",
                        type = PersonalizedSectionType.LIKED_MUSIC,
                        title = "Liked Music",
                        subtitle = "From your connected account",
                        sourceLabel = "Account",
                        items = cached,
                    )
                }
            }

            return try {
                val likedVideos = authenticatedYouTubeClient.getLikedVideos()
                val filtered = likedVideos
                    .filter { MusicFilter.isMusicTrack(it.title, it.channelName, it.duration) }
                    .distinctBy { it.id }
                    .map { it.toPersonalizedFeedItem(sourceBadge = "LIKED") }

                accountPersonalizationCache.put(session.accountKey, PersonalizedSectionType.LIKED_MUSIC, filtered)

                PersonalizedSection(
                    id = "sec_liked_music",
                    type = PersonalizedSectionType.LIKED_MUSIC,
                    title = "Liked Music",
                    subtitle = "From your connected account",
                    sourceLabel = "From your account",
                    items = filtered,
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch remote liked music", e)
                PersonalizedSection(
                    id = "sec_liked_music",
                    type = PersonalizedSectionType.LIKED_MUSIC,
                    title = "Liked Music",
                    subtitle = "From your connected account",
                    sourceLabel = "From your account",
                    error = "Could not load liked music from account",
                    canRetry = true,
                )
            }
        } else {
            // Logged-out fallback: check local library liked tracks
            return try {
                val libraryHome = libraryRepository.observeLibraryHome().firstOrNull()
                val localLiked = libraryHome?.likedTracks ?: emptyList()
                val items = localLiked.map { item ->
                    PersonalizedFeedItem(
                        id = item.id,
                        title = item.title,
                        artist = item.subtitle.ifEmpty { "Unknown Artist" },
                        artworkUrl = item.artworkUrl,
                        itemType = PersonalizedItemType.SONG,
                        sourceBadge = "FAVORITE",
                        mediaItem = MediaItem.Remote(
                            id = item.id,
                            title = item.title,
                            artist = item.subtitle.ifEmpty { "Unknown Artist" },
                            artworkUri = item.artworkUrl?.let { Uri.parse(it) },
                            duration = 0L,
                        ),
                    )
                }

                PersonalizedSection(
                    id = "sec_liked_music",
                    type = PersonalizedSectionType.LIKED_MUSIC,
                    title = "Liked Music",
                    subtitle = if (items.isNotEmpty()) "Local favorites" else "Sign in to sync your likes",
                    sourceLabel = "Local Library",
                    items = items,
                    isAccountRequired = items.isEmpty(),
                )
            } catch (e: Exception) {
                PersonalizedSection(
                    id = "sec_liked_music",
                    type = PersonalizedSectionType.LIKED_MUSIC,
                    title = "Liked Music",
                    subtitle = "Favorites",
                    sourceLabel = "Local Library",
                    items = emptyList(),
                )
            }
        }
    }

    // ── 5. Your Playlists (Local & Account-Accessible) ──────────────────────
    private suspend fun buildYourPlaylistsSection(
        session: AccountSession,
        forceRefresh: Boolean,
    ): PersonalizedSection {
        return try {
            val libraryHome = libraryRepository.observeLibraryHome().firstOrNull()
            val localPlaylists = libraryHome?.playlists ?: emptyList()
            val items = localPlaylists.map { pl ->
                PersonalizedFeedItem(
                    id = pl.id,
                    title = pl.title,
                    artist = pl.subtitle.ifEmpty { "Playlist" },
                    artworkUrl = pl.artworkUrl,
                    itemType = PersonalizedItemType.PLAYLIST,
                    sourceBadge = "PLAYLIST",
                )
            }

            PersonalizedSection(
                id = "sec_your_playlists",
                type = PersonalizedSectionType.YOUR_PLAYLISTS,
                title = "Your Playlists",
                subtitle = "Local & customized mixes",
                sourceLabel = "From your playlists",
                items = items,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load playlists", e)
            PersonalizedSection(
                id = "sec_your_playlists",
                type = PersonalizedSectionType.YOUR_PLAYLISTS,
                title = "Your Playlists",
                subtitle = "Playlists",
                sourceLabel = "From your playlists",
                error = "Could not load playlists",
                canRetry = true,
            )
        }
    }

    // ── 6. New From Subscriptions (Connected Account) ───────────────────────
    private suspend fun buildSubscriptionsSection(
        session: AccountSession,
        forceRefresh: Boolean,
    ): PersonalizedSection {
        if (session !is AccountSession.Connected) {
            return PersonalizedSection(
                id = "sec_subscriptions",
                type = PersonalizedSectionType.NEW_FROM_SUBSCRIPTIONS,
                title = "New From Subscriptions",
                subtitle = "Sign in to see updates from your subscribed artists",
                sourceLabel = "From your subscriptions",
                items = emptyList(),
                isAccountRequired = true,
            )
        }

        if (!forceRefresh) {
            val cached = accountPersonalizationCache.get(session.accountKey, PersonalizedSectionType.NEW_FROM_SUBSCRIPTIONS)
            if (cached != null) {
                return PersonalizedSection(
                    id = "sec_subscriptions",
                    type = PersonalizedSectionType.NEW_FROM_SUBSCRIPTIONS,
                    title = "New From Subscriptions",
                    subtitle = "Latest releases from your channels",
                    sourceLabel = "From your subscriptions",
                    items = cached,
                )
            }
        }

        return try {
            val subsVideos = authenticatedYouTubeClient.getSubscriptionsFeed()
            val filtered = subsVideos
                .filter { MusicFilter.isMusicTrack(it.title, it.channelName, it.duration) }
                .distinctBy { it.id }
                .take(20)
                .map { it.toPersonalizedFeedItem(sourceBadge = "NEW") }

            accountPersonalizationCache.put(session.accountKey, PersonalizedSectionType.NEW_FROM_SUBSCRIPTIONS, filtered)

            PersonalizedSection(
                id = "sec_subscriptions",
                type = PersonalizedSectionType.NEW_FROM_SUBSCRIPTIONS,
                title = "New From Subscriptions",
                subtitle = "Latest releases from your channels",
                sourceLabel = "From your subscriptions",
                items = filtered,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch subscriptions feed", e)
            PersonalizedSection(
                id = "sec_subscriptions",
                type = PersonalizedSectionType.NEW_FROM_SUBSCRIPTIONS,
                title = "New From Subscriptions",
                subtitle = "Latest releases from your channels",
                sourceLabel = "From your subscriptions",
                error = "Could not load subscriptions feed",
                canRetry = true,
            )
        }
    }

    // ── 7. Based on Your Listening (On-Device Affinity Model) ───────────────
    private suspend fun buildBasedOnListeningSection(forceRefresh: Boolean): PersonalizedSection {
        return try {
            val now = System.currentTimeMillis()
            val last30Days = now - 30L * 24 * 3600 * 1000
            val topArtists = recommendationDao.getTopArtistsSince(last30Days, 3)
            val topArtistSeed = topArtists.firstOrNull()?.artistName

            val query = if (!topArtistSeed.isNullOrBlank()) {
                "$topArtistSeed hits official songs"
            } else {
                "top trending songs official audio"
            }

            val rawResults = youtubeRemoteDataSource.searchMusic(query)
            val filtered = rawResults
                .filter { MusicFilter.isMusicTrack(it.title, it.artist, it.duration) }
                .distinctBy { it.id }
                .take(15)
                .map { it.toPersonalizedFeedItem(sourceBadge = "RECOMMENDED") }

            val subtitle = if (!topArtistSeed.isNullOrBlank()) {
                "Based on $topArtistSeed"
            } else {
                "Curated for your taste"
            }

            PersonalizedSection(
                id = "sec_based_on_listening",
                type = PersonalizedSectionType.BASED_ON_LISTENING,
                title = "Based on Your Listening",
                subtitle = subtitle,
                sourceLabel = "Based on your listening",
                items = filtered,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to compute Based on Listening section", e)
            PersonalizedSection(
                id = "sec_based_on_listening",
                type = PersonalizedSectionType.BASED_ON_LISTENING,
                title = "Based on Your Listening",
                subtitle = "Curated recommendations",
                sourceLabel = "Based on your listening",
                error = "Could not generate recommendations",
                canRetry = true,
            )
        }
    }

    // ── 8. Trending Music (Public / Region-Aware) ───────────────────────────
    private suspend fun buildTrendingSection(forceRefresh: Boolean): PersonalizedSection {
        return try {
            val trendingItems = youtubeRemoteDataSource.searchMusic("trending songs official audio video")
            val filtered = trendingItems
                .filter { MusicFilter.isMusicTrack(it.title, it.artist, it.duration) }
                .distinctBy { it.id }
                .take(20)
                .map { it.toPersonalizedFeedItem(sourceBadge = "TRENDING") }

            PersonalizedSection(
                id = "sec_trending",
                type = PersonalizedSectionType.TRENDING_MUSIC,
                title = "Trending Music",
                subtitle = "Popular tracks right now",
                sourceLabel = "Trending",
                items = filtered,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load Trending Music section", e)
            PersonalizedSection(
                id = "sec_trending",
                type = PersonalizedSectionType.TRENDING_MUSIC,
                title = "Trending Music",
                subtitle = "Popular tracks right now",
                sourceLabel = "Trending",
                error = "Could not load trending music",
                canRetry = true,
            )
        }
    }

    // ── Helper Mappers ───────────────────────────────────────────────────────
    private fun HomeVideoItem.toPersonalizedFeedItem(sourceBadge: String? = null): PersonalizedFeedItem {
        return PersonalizedFeedItem(
            id = id,
            title = title,
            artist = channelName,
            channelId = channelId,
            artworkUrl = thumbnailUrl,
            durationMs = duration * 1000L,
            itemType = PersonalizedItemType.SONG,
            sourceBadge = sourceBadge ?: "DSP READY",
            mediaItem = MediaItem.Remote(
                id = id,
                title = title,
                artist = channelName,
                artworkUri = Uri.parse(thumbnailUrl),
                duration = duration * 1000L,
                isVideo = isShort,
            )
        )
    }

    private fun HomeMusicItem.toPersonalizedFeedItem(sourceBadge: String? = null): PersonalizedFeedItem {
        return PersonalizedFeedItem(
            id = id,
            title = title,
            artist = artist,
            artworkUrl = thumbnailUrl,
            durationMs = duration * 1000L,
            itemType = PersonalizedItemType.SONG,
            sourceBadge = sourceBadge ?: "DSP READY",
            mediaItem = MediaItem.Remote(
                id = id,
                title = title,
                artist = artist,
                artworkUri = Uri.parse(thumbnailUrl),
                duration = duration * 1000L,
            )
        )
    }
}
