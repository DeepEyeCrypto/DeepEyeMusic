// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.repository

import android.net.Uri
import android.util.Log
import com.deepeye.musicpro.account.AccountSession
import com.deepeye.musicpro.account.AccountSessionManager
import com.deepeye.musicpro.data.cache.AccountPersonalizationCache
import com.deepeye.musicpro.data.cache.HiddenContentManager
import com.deepeye.musicpro.data.db.HistoryDao
import com.deepeye.musicpro.data.db.RecommendationDao
import com.deepeye.musicpro.data.db.TasteDao
import com.deepeye.musicpro.data.prefs.PersonalizationPreferenceStore
import com.deepeye.musicpro.data.prefs.PersonalizationPreferences
import com.deepeye.musicpro.data.source.remote.youtube.AuthenticatedYouTubeClient
import com.deepeye.musicpro.data.source.remote.youtube.MusicFilter
import com.deepeye.musicpro.data.source.remote.youtube.YoutubeRemoteDataSource
import com.deepeye.musicpro.domain.model.MediaItem
import com.deepeye.musicpro.domain.model.home.HomeMusicItem
import com.deepeye.musicpro.domain.model.home.HomeVideoItem
import com.deepeye.musicpro.domain.model.personalization.*
import com.deepeye.musicpro.domain.personalization.DiversityRanker
import com.deepeye.musicpro.domain.personalization.SectionDiagnostics
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
    private val hiddenContentManager: HiddenContentManager,
    private val personalizationPrefs: PersonalizationPreferenceStore,
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
                val accountKey = (session as? AccountSession.Connected)?.accountKey
                hiddenContentManager.switchAccount(accountKey)
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

    // ── Phase 5: Preferences ────────────────────────────────────────────────

    override fun observePreferences() = personalizationPrefs.observe()

    override suspend fun updatePreferences(transform: (PersonalizationPreferences) -> PersonalizationPreferences) {
        personalizationPrefs.update(transform)
        // Trigger a feed rebuild so new preferences take effect immediately.
        buildFeed(forceRefresh = false)
    }

    // ── Phase 5: Hidden content controls ────────────────────────────────────

    override suspend fun hideItem(itemId: String, label: String, alsoHideArtist: String?): Boolean {
        val hidden = hiddenContentManager.hideItem(itemId, label, alsoHideArtist)
        if (hidden) buildFeed(forceRefresh = false)
        return hidden
    }

    override suspend fun undoHideItem(itemId: String) {
        hiddenContentManager.undoHide(itemId)
        buildFeed(forceRefresh = false)
    }

    override suspend fun unhideItem(itemId: String) {
        hiddenContentManager.unhideItem(itemId)
        buildFeed(forceRefresh = false)
    }

    override suspend fun unhideArtist(artistName: String) {
        hiddenContentManager.unhideArtist(artistName)
        buildFeed(forceRefresh = false)
    }

    override suspend fun resetAllHiddenContent() {
        hiddenContentManager.resetAll()
        buildFeed(forceRefresh = false)
    }

    // ── Phase 5: Clear data ─────────────────────────────────────────────────

    override suspend fun clearLocalHistory() {
        historyDao.clearPlaybackHistory()
        buildFeed(forceRefresh = false)
    }

    override suspend fun clearPersonalizationCache() {
        accountPersonalizationCache.clearAll()
        buildFeed(forceRefresh = false)
    }

    private suspend fun buildFeed(forceRefresh: Boolean) = withContext(ioDispatcher) {
        refreshMutex.withLock {
            _feedState.update { it.copy(isLoading = it.sections.isEmpty(), isRefreshing = it.sections.isNotEmpty()) }
            val session = accountSessionManager.accountSession.value

            val prefs = personalizationPrefs.current()
            if (!prefs.enablePersonalization) {
                _feedState.update {
                    it.copy(
                        sections = emptyList(),
                        diagnostics = emptyList(),
                        isLoading = false,
                        isRefreshing = false,
                        lastUpdatedMillis = System.currentTimeMillis(),
                    )
                }
            } else {
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

                    if (forceRefresh) {
                        personalizationPrefs.update { it.copy(lastRefreshMillis = System.currentTimeMillis()) }
                    }

                    val assembledSections = mutableListOf<PersonalizedSection>()
                    val allDiagnostics = mutableListOf<SectionDiagnostics>()
                    val currentlyPlayingId = queueManager.queue.value.getOrNull(queueManager.currentIndex.value)?.id

                    // ── Phase 5: Filter hidden content and apply DiversityRanker ──
                    fun addSection(section: PersonalizedSection) {
                        val filteredItems = section.items.filterNot { item ->
                            hiddenContentManager.isItemHidden(item.id) ||
                                hiddenContentManager.isArtistHidden(item.artist) ||
                                (prefs.hideNonMusicContent && (item.itemType == PersonalizedItemType.VIDEO || (item.mediaItem as? MediaItem.Remote)?.isVideo == true))
                        }
                        val filteredSection = section.copy(items = filteredItems)
                        val processed = DiversityRanker.diversify(
                            section = filteredSection,
                            maxRepeatedArtistPerSection = prefs.maxRepeatedArtistPerSection,
                            currentPlayingItemId = currentlyPlayingId,
                        )

                        if (processed.items.isNotEmpty() || processed.error != null) {
                            assembledSections.add(processed)

                            allDiagnostics.add(
                                SectionDiagnostics(
                                    sectionId = processed.id,
                                    sectionTitle = processed.title,
                                    source = if (processed.isFromCache) SectionDiagnostics.CacheSource.ROOM_CACHE else SectionDiagnostics.CacheSource.LIVE,
                                    accountScopeHash = if (processed.isAccountRequired) (session as? AccountSession.Connected)?.accountKey?.take(8) else null,
                                    cacheAgeMs = if (processed.lastUpdatedMillis > 0) System.currentTimeMillis() - processed.lastUpdatedMillis else 0L,
                                    refreshState = if (processed.isFromCache) SectionDiagnostics.RefreshState.STALE_WHILE_REVALIDATE else SectionDiagnostics.RefreshState.FRESH,
                                    itemCount = processed.items.size,
                                    reason = processed.explanation,
                                    rebuiltByDiversityRanker = processed.items != filteredItems,
                                    hiddenItemsPruned = section.items.size - filteredItems.size,
                                )
                            )
                        }
                    }

                    // 1. Continue Listening
                    if (prefs.enableLocalListeningSections) {
                        addSection(continueListening)
                    }

                    // 2. Your Queue (always shown – local playback, not filtered by local pref)
                    addSection(yourQueue)

                    // 3. Recently Played
                    if (prefs.enableLocalListeningSections) {
                        addSection(recentlyPlayed)
                    }

                    // 4. Liked Music
                    if (prefs.enableAccountSections && (session is AccountSession.Connected || likedMusic.items.isNotEmpty()) && prefs.enableLikedMusic) {
                        addSection(likedMusic)
                    }

                    // 5. Your Playlists
                    if (prefs.enableAccountSections) {
                        addSection(playlists)
                    }

                    // 6. New From Subscriptions
                    if (prefs.enableAccountSections && session is AccountSession.Connected && prefs.enableSubscriptions) {
                        addSection(subscriptions)
                    }

                    // 7. Based on Your Listening
                    if (prefs.enableLocalListeningSections && prefs.enableLocalMix) {
                        addSection(basedOnListening)
                    }

                    // 8. Trending Music
                    if (prefs.enableTrending) {
                        addSection(trending)
                    }

                    _feedState.update {
                        it.copy(
                            sections = assembledSections,
                            diagnostics = allDiagnostics,
                            isLoading = false,
                            isRefreshing = false,
                            lastUpdatedMillis = System.currentTimeMillis(),
                            globalError = null,
                        )
                    }
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
                        explanation = "You listened to ${playback.title} recently on this device.",
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
                explanation = "Tracks you were listening to recently with unplayed progress on this device.",
                items = items,
                lastUpdatedMillis = System.currentTimeMillis(),
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to build Continue Listening section", e)
            PersonalizedSection(
                id = "sec_continue_listening",
                type = PersonalizedSectionType.CONTINUE_LISTENING,
                title = "Continue Listening",
                subtitle = "Jump back in",
                sourceLabel = "Local",
                explanation = "Tracks with saved playback progress.",
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
                explanation = if (index == queueManager.currentIndex.value) "Currently playing in player." else "Queued in player at position #${index + 1}.",
                mediaItem = mediaItem,
            )
        }

        return PersonalizedSection(
            id = "sec_your_queue",
            type = PersonalizedSectionType.YOUR_QUEUE,
            title = "Your Queue",
            subtitle = if (items.isNotEmpty()) "${items.size} tracks queued" else "Queue is empty",
            sourceLabel = "Local Queue",
            explanation = "Active tracks in your local playback queue.",
            items = items,
            lastUpdatedMillis = System.currentTimeMillis(),
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
                            explanation = "Played recently on this device.",
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
                explanation = "Tracks you played on this device, preserved privately and locally.",
                items = items.take(15),
                lastUpdatedMillis = System.currentTimeMillis(),
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to build Recently Played section", e)
            PersonalizedSection(
                id = "sec_recently_played",
                type = PersonalizedSectionType.RECENTLY_PLAYED,
                title = "Recently Played",
                subtitle = "Your listening history",
                sourceLabel = "Local History",
                explanation = "Playback history on this device.",
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
            val cachedResult = accountPersonalizationCache.getFullSection(
                session.accountKey,
                PersonalizedSectionType.LIKED_MUSIC,
                allowStale = true
            )
            if (!forceRefresh && cachedResult != null && !cachedResult.isStale) {
                return PersonalizedSection(
                    id = "sec_liked_music",
                    type = PersonalizedSectionType.LIKED_MUSIC,
                    title = "Liked Music",
                    subtitle = "From your connected account",
                    sourceLabel = "your account",
                    explanation = "Songs and tracks you marked as Liked in your connected YouTube account.",
                    items = cachedResult.items,
                    isFromCache = cachedResult.isStale,
                    lastUpdatedMillis = cachedResult.cachedAt,
                )
            }

            return try {
                val likedVideos = authenticatedYouTubeClient.getLikedVideos()
                val filtered = likedVideos
                    .filter { MusicFilter.isMusicTrack(it.title, it.channelName, it.duration) }
                    .distinctBy { it.id }
                    .map {
                        it.toPersonalizedFeedItem(
                            sourceBadge = "LIKED",
                            explanation = "Added to your Liked Music in your connected YouTube account."
                        )
                    }

                accountPersonalizationCache.put(
                    accountKey = session.accountKey,
                    sectionType = PersonalizedSectionType.LIKED_MUSIC,
                    items = filtered,
                    title = "Liked Music",
                    subtitle = "From your connected account",
                    sourceLabel = "your account",
                    explanation = "Songs and tracks you marked as Liked in your connected YouTube account.",
                )

                PersonalizedSection(
                    id = "sec_liked_music",
                    type = PersonalizedSectionType.LIKED_MUSIC,
                    title = "Liked Music",
                    subtitle = "From your connected account",
                    sourceLabel = "your account",
                    explanation = "Songs and tracks you marked as Liked in your connected YouTube account.",
                    items = filtered,
                    isFromCache = false,
                    lastUpdatedMillis = System.currentTimeMillis(),
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch remote liked music", e)
                if (cachedResult != null && cachedResult.items.isNotEmpty()) {
                    PersonalizedSection(
                        id = "sec_liked_music",
                        type = PersonalizedSectionType.LIKED_MUSIC,
                        title = "Liked Music",
                        subtitle = "Updated earlier • Offline",
                        sourceLabel = "your account",
                        explanation = "Cached from your connected account.",
                        items = cachedResult.items,
                        isFromCache = true,
                        lastUpdatedMillis = cachedResult.cachedAt,
                        canRetry = true,
                    )
                } else {
                    PersonalizedSection(
                        id = "sec_liked_music",
                        type = PersonalizedSectionType.LIKED_MUSIC,
                        title = "Liked Music",
                        subtitle = "From your connected account",
                        sourceLabel = "your account",
                        explanation = "Songs and tracks you marked as Liked in your connected YouTube account.",
                        error = "Could not load liked music from account",
                        canRetry = true,
                    )
                }
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
                    explanation = "From your saved or created playlists.",
                )
            }

            PersonalizedSection(
                id = "sec_your_playlists",
                type = PersonalizedSectionType.YOUR_PLAYLISTS,
                title = "Your Playlists",
                subtitle = "Local & customized mixes",
                sourceLabel = "your playlists",
                explanation = "Custom and curated playlists from your device library.",
                items = items,
                lastUpdatedMillis = System.currentTimeMillis(),
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load playlists", e)
            PersonalizedSection(
                id = "sec_your_playlists",
                type = PersonalizedSectionType.YOUR_PLAYLISTS,
                title = "Your Playlists",
                subtitle = "Playlists",
                sourceLabel = "your playlists",
                explanation = "Your playlists collection.",
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
                sourceLabel = "your subscriptions",
                explanation = "New uploads and music from channels you subscribe to on YouTube.",
                items = emptyList(),
                isAccountRequired = true,
            )
        }

        val cachedResult = accountPersonalizationCache.getFullSection(
            session.accountKey,
            PersonalizedSectionType.NEW_FROM_SUBSCRIPTIONS,
            allowStale = true
        )

        if (!forceRefresh && cachedResult != null && !cachedResult.isStale) {
            return PersonalizedSection(
                id = "sec_subscriptions",
                type = PersonalizedSectionType.NEW_FROM_SUBSCRIPTIONS,
                title = "New From Subscriptions",
                subtitle = "Latest releases from your channels",
                sourceLabel = "your subscriptions",
                explanation = "New music releases from channels you follow.",
                items = cachedResult.items,
                isFromCache = cachedResult.isStale,
                lastUpdatedMillis = cachedResult.cachedAt,
            )
        }

        return try {
            val subsVideos = authenticatedYouTubeClient.getSubscriptionsFeed()
            val filtered = subsVideos
                .filter { MusicFilter.isMusicTrack(it.title, it.channelName, it.duration) }
                .distinctBy { it.id }
                .take(20)
                .map {
                    it.toPersonalizedFeedItem(
                        sourceBadge = "NEW",
                        explanation = "New release from subscribed channel: ${it.channelName}."
                    )
                }

            accountPersonalizationCache.put(
                accountKey = session.accountKey,
                sectionType = PersonalizedSectionType.NEW_FROM_SUBSCRIPTIONS,
                items = filtered,
                title = "New From Subscriptions",
                subtitle = "Latest releases from your channels",
                sourceLabel = "your subscriptions",
                explanation = "New music releases from channels you follow.",
            )

            PersonalizedSection(
                id = "sec_subscriptions",
                type = PersonalizedSectionType.NEW_FROM_SUBSCRIPTIONS,
                title = "New From Subscriptions",
                subtitle = "Latest releases from your channels",
                sourceLabel = "your subscriptions",
                explanation = "New music releases from channels you follow.",
                items = filtered,
                isFromCache = false,
                lastUpdatedMillis = System.currentTimeMillis(),
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch subscriptions feed", e)
            if (cachedResult != null && cachedResult.items.isNotEmpty()) {
                PersonalizedSection(
                    id = "sec_subscriptions",
                    type = PersonalizedSectionType.NEW_FROM_SUBSCRIPTIONS,
                    title = "New From Subscriptions",
                    subtitle = "Updated earlier • Offline",
                    sourceLabel = "your subscriptions",
                    explanation = "Cached releases from channels you follow.",
                    items = cachedResult.items,
                    isFromCache = true,
                    lastUpdatedMillis = cachedResult.cachedAt,
                    canRetry = true,
                )
            } else {
                PersonalizedSection(
                    id = "sec_subscriptions",
                    type = PersonalizedSectionType.NEW_FROM_SUBSCRIPTIONS,
                    title = "New From Subscriptions",
                    subtitle = "Latest releases from your channels",
                    sourceLabel = "your subscriptions",
                    explanation = "New music releases from channels you follow.",
                    error = "Could not load subscriptions feed",
                    canRetry = true,
                )
            }
        }
    }

    // ── 7. Based on Your Listening (On-Device Affinity Model) ───────────────
    private suspend fun buildBasedOnListeningSection(forceRefresh: Boolean): PersonalizedSection {
        val cacheKey = "__local_listening__"
        val cachedResult = accountPersonalizationCache.getFullSection(
            cacheKey,
            PersonalizedSectionType.BASED_ON_LISTENING,
            allowStale = true
        )

        if (!forceRefresh && cachedResult != null && !cachedResult.isStale) {
            return PersonalizedSection(
                id = "sec_based_on_listening",
                type = PersonalizedSectionType.BASED_ON_LISTENING,
                title = "Based on Your Listening",
                subtitle = cachedResult.subtitle ?: "Curated for your taste",
                sourceLabel = "your listening data",
                explanation = cachedResult.explanation ?: "Curated recommendations based on your listening history.",
                items = cachedResult.items,
                isFromCache = cachedResult.isStale,
                lastUpdatedMillis = cachedResult.cachedAt,
            )
        }

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
            val subtitle = if (!topArtistSeed.isNullOrBlank()) "Based on $topArtistSeed" else "Curated for your taste"
            val explanation = if (!topArtistSeed.isNullOrBlank()) {
                "Recommended because you frequently listen to $topArtistSeed on this device."
            } else {
                "Curated from your overall on-device listening preferences."
            }

            val filtered = rawResults
                .filter { MusicFilter.isMusicTrack(it.title, it.artist, it.duration) }
                .distinctBy { it.id }
                .take(15)
                .map {
                    it.toPersonalizedFeedItem(
                        sourceBadge = "RECOMMENDED",
                        explanation = explanation
                    )
                }

            accountPersonalizationCache.put(
                accountKey = cacheKey,
                sectionType = PersonalizedSectionType.BASED_ON_LISTENING,
                items = filtered,
                title = "Based on Your Listening",
                subtitle = subtitle,
                sourceLabel = "your listening data",
                explanation = explanation,
            )

            PersonalizedSection(
                id = "sec_based_on_listening",
                type = PersonalizedSectionType.BASED_ON_LISTENING,
                title = "Based on Your Listening",
                subtitle = subtitle,
                sourceLabel = "your listening data",
                explanation = explanation,
                items = filtered,
                isFromCache = false,
                lastUpdatedMillis = System.currentTimeMillis(),
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to compute Based on Listening section", e)
            if (cachedResult != null && cachedResult.items.isNotEmpty()) {
                PersonalizedSection(
                    id = "sec_based_on_listening",
                    type = PersonalizedSectionType.BASED_ON_LISTENING,
                    title = "Based on Your Listening",
                    subtitle = "Updated earlier • Offline",
                    sourceLabel = "your listening data",
                    explanation = cachedResult.explanation ?: "Curated recommendations based on your listening.",
                    items = cachedResult.items,
                    isFromCache = true,
                    lastUpdatedMillis = cachedResult.cachedAt,
                    canRetry = true,
                )
            } else {
                PersonalizedSection(
                    id = "sec_based_on_listening",
                    type = PersonalizedSectionType.BASED_ON_LISTENING,
                    title = "Based on Your Listening",
                    subtitle = "Curated recommendations",
                    sourceLabel = "your listening data",
                    explanation = "Curated recommendations based on your listening history.",
                    error = "Could not generate recommendations",
                    canRetry = true,
                )
            }
        }
    }

    // ── 8. Trending Music (Public / Region-Aware) ───────────────────────────
    private suspend fun buildTrendingSection(forceRefresh: Boolean): PersonalizedSection {
        val cacheKey = "__public_trending__"
        val cachedResult = accountPersonalizationCache.getFullSection(
            cacheKey,
            PersonalizedSectionType.TRENDING_MUSIC,
            allowStale = true
        )

        if (!forceRefresh && cachedResult != null && !cachedResult.isStale) {
            return PersonalizedSection(
                id = "sec_trending",
                type = PersonalizedSectionType.TRENDING_MUSIC,
                title = "Trending Music",
                subtitle = "Popular tracks right now",
                sourceLabel = "Trending",
                explanation = "Popular tracks currently trending in your region.",
                items = cachedResult.items,
                isFromCache = cachedResult.isStale,
                lastUpdatedMillis = cachedResult.cachedAt,
            )
        }

        return try {
            val region = personalizationPrefs.current().trendingRegion.ifBlank { "US" }
            val query = "trending songs $region official audio video"
            val trendingItems = youtubeRemoteDataSource.searchMusic(query)
            val explanation = "Popular track currently trending in your region ($region)."
            val filtered = trendingItems
                .filter { MusicFilter.isMusicTrack(it.title, it.artist, it.duration) }
                .distinctBy { it.id }
                .take(20)
                .map {
                    it.toPersonalizedFeedItem(
                        sourceBadge = "TRENDING",
                        explanation = explanation
                    )
                }

            accountPersonalizationCache.put(
                accountKey = cacheKey,
                sectionType = PersonalizedSectionType.TRENDING_MUSIC,
                items = filtered,
                title = "Trending Music",
                subtitle = "Popular tracks right now",
                sourceLabel = "Trending",
                explanation = "Popular tracks currently trending in your region.",
            )

            PersonalizedSection(
                id = "sec_trending",
                type = PersonalizedSectionType.TRENDING_MUSIC,
                title = "Trending Music",
                subtitle = "Popular tracks right now",
                sourceLabel = "Trending",
                explanation = "Popular tracks currently trending in your region.",
                items = filtered,
                isFromCache = false,
                lastUpdatedMillis = System.currentTimeMillis(),
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load Trending Music section", e)
            if (cachedResult != null && cachedResult.items.isNotEmpty()) {
                PersonalizedSection(
                    id = "sec_trending",
                    type = PersonalizedSectionType.TRENDING_MUSIC,
                    title = "Trending Music",
                    subtitle = "Updated earlier • Offline",
                    sourceLabel = "Trending",
                    explanation = "Popular tracks currently trending in your region.",
                    items = cachedResult.items,
                    isFromCache = true,
                    lastUpdatedMillis = cachedResult.cachedAt,
                    canRetry = true,
                )
            } else {
                PersonalizedSection(
                    id = "sec_trending",
                    type = PersonalizedSectionType.TRENDING_MUSIC,
                    title = "Trending Music",
                    subtitle = "Popular tracks right now",
                    sourceLabel = "Trending",
                    explanation = "Popular tracks currently trending in your region.",
                    error = "Could not load trending music",
                    canRetry = true,
                )
            }
        }
    }

    // ── Helper Mappers ───────────────────────────────────────────────────────
    private fun HomeVideoItem.toPersonalizedFeedItem(
        sourceBadge: String? = null,
        explanation: String? = null,
    ): PersonalizedFeedItem {
        return PersonalizedFeedItem(
            id = id,
            title = title,
            artist = channelName,
            channelId = channelId,
            artworkUrl = thumbnailUrl,
            durationMs = duration * 1000L,
            itemType = PersonalizedItemType.SONG,
            sourceBadge = sourceBadge ?: "DSP READY",
            explanation = explanation,
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

    private fun HomeMusicItem.toPersonalizedFeedItem(
        sourceBadge: String? = null,
        explanation: String? = null,
    ): PersonalizedFeedItem {
        return PersonalizedFeedItem(
            id = id,
            title = title,
            artist = artist,
            artworkUrl = thumbnailUrl,
            durationMs = duration * 1000L,
            itemType = PersonalizedItemType.SONG,
            sourceBadge = sourceBadge ?: "DSP READY",
            explanation = explanation,
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

