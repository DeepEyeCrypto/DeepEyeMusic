// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.repository

import android.net.Uri
import android.util.Log
import com.deepeye.musicpro.account.AccountSession
import com.deepeye.musicpro.account.AccountSessionManager
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

    /** Atomic generation counter incremented on each account session change.
     * Used to discard stale buildFeed results that complete after a switch. */
    private val _feedGeneration = java.util.concurrent.atomic.AtomicLong(0)

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
                // Increment generation on account session change to fence stale results
                val gen = _feedGeneration.incrementAndGet()
                buildFeed(forceRefresh = false, feedGeneration = gen)
            }
        }
    }

    override suspend fun refreshFeed(forceRefresh: Boolean) {
        val gen = _feedGeneration.incrementAndGet()
        buildFeed(forceRefresh = forceRefresh, feedGeneration = gen)
    }

    override suspend fun refreshSection(sectionType: PersonalizedSectionType) {
        withContext(ioDispatcher) {
            refreshMutex.withLock {
                val currentSections = _feedState.value.sections.toMutableList()
                val session = accountSessionManager.accountSession.value
                val gen = _feedGeneration.incrementAndGet()
                val updatedSection = when (sectionType) {
                    PersonalizedSectionType.CONTINUE_LISTENING -> buildContinueListeningSection()
                    PersonalizedSectionType.YOUR_QUEUE -> buildYourQueueSection()
                    PersonalizedSectionType.RECENTLY_PLAYED -> buildRecentlyPlayedSection()
                    PersonalizedSectionType.LIKED_MUSIC -> buildLikedMusicSection(session, forceRefresh = true)
                    PersonalizedSectionType.YOUR_PLAYLISTS -> buildYourPlaylistsSection(session, forceRefresh = true)
                    PersonalizedSectionType.NEW_FROM_SUBSCRIPTIONS -> buildSubscriptionsSection(session, forceRefresh = true)
                    PersonalizedSectionType.BASED_ON_LISTENING -> buildBasedOnListeningSection(forceRefresh = true)
                    PersonalizedSectionType.TRENDING_MUSIC -> buildTrendingSection(forceRefresh = true)
                    PersonalizedSectionType.YOUTUBE_MUSIC_SHELF -> buildYouTubeMusicShelvesSection(session, forceRefresh = true)
                }
                // Check generation after section build
                if (gen != _feedGeneration.get()) return@withContext

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

    /** Returns the section if generation still matches; otherwise returns an empty section of the same type to signal discard. */
    private fun checkGen(section: PersonalizedSection, feedGeneration: Long): PersonalizedSection {
        if (feedGeneration != -1L && feedGeneration != _feedGeneration.get()) {
            return section.copy(items = emptyList())
        }
        return section
    }

        override suspend fun invalidateAccountCache(accountKey: String) {
            _feedState.update { it.copy(sections = emptyList(), lastUpdatedMillis = 0L) }
            // Also clear any per-account caches if they exist
            // hiddenContentManager.switchAccount(null) // handled by AccountSessionManager
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
        buildFeed(forceRefresh = false)
    }

    private suspend fun buildFeed(forceRefresh: Boolean, feedGeneration: Long = -1) = withContext(ioDispatcher) {
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
                // Check generation early to avoid launching unnecessary work
                if (feedGeneration != -1L && feedGeneration != _feedGeneration.get()) {
                    return@withContext
                }

                supervisorScope {
                    val continueListeningDeferred = async { buildContinueListeningSection() }
                    val yourQueueDeferred = async { buildYourQueueSection() }
                    val recentlyPlayedDeferred = async { buildRecentlyPlayedSection() }
                    val likedMusicDeferred = async { buildLikedMusicSection(session, forceRefresh) }
                    val playlistsDeferred = async { buildYourPlaylistsSection(session, forceRefresh) }
                    val subscriptionsDeferred = async { buildSubscriptionsSection(session, forceRefresh) }
                    val basedOnListeningDeferred = async { buildBasedOnListeningSection(forceRefresh) }
                    val trendingDeferred = async { buildTrendingSection(forceRefresh) }
                    val ytMusicShelvesDeferred = async { buildYouTubeMusicShelves(session, forceRefresh) }

                    val continueListening = checkGen(continueListeningDeferred.await(), feedGeneration)
                    val yourQueue = checkGen(yourQueueDeferred.await(), feedGeneration)
                    val recentlyPlayed = checkGen(recentlyPlayedDeferred.await(), feedGeneration)
                    val likedMusic = checkGen(likedMusicDeferred.await(), feedGeneration)
                    val playlists = checkGen(playlistsDeferred.await(), feedGeneration)
                    val subscriptions = checkGen(subscriptionsDeferred.await(), feedGeneration)
                    val basedOnListening = checkGen(basedOnListeningDeferred.await(), feedGeneration)
                    val trending = checkGen(trendingDeferred.await(), feedGeneration)
                    val ytMusicShelvesRaw = ytMusicShelvesDeferred.await()

                    if (feedGeneration != -1L && feedGeneration != _feedGeneration.get()) {
                        // Generation changed during await — discard all results by throwing
                        // CancellationException which will cancel the supervisorScope
                        throw kotlinx.coroutines.CancellationException("Generation changed")
                    }

                    val ytMusicShelves = ytMusicShelvesRaw

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
                                item.itemType == PersonalizedItemType.VIDEO ||
                                (item.durationMs in 1..59_000L) ||
                                (item.mediaItem as? MediaItem.Remote)?.isVideo == true ||
                                !MusicFilter.isMusicTrack(item.title, item.artist, item.durationMs / 1000L)
                        }
                        val filteredSection = section.copy(items = filteredItems)
                        val processed = filteredSection

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

                    // YT-MUSIC-OMEGA: when the authenticated YouTube Music home returns live
                    // shelves, they replace the local "Continue Listening" / "Recently Played"
                    // rails (the "Local" / "Local History" sources). If unauthenticated or the
                    // API yields nothing, fall back to the local sections so the tab is never empty.
                    val useYouTubeMusicShelves = ytMusicShelves.isNotEmpty()

                    // 1. Continue Listening (local) — suppressed when live YT Music shelves exist
                    if (!useYouTubeMusicShelves && prefs.enableLocalListeningSections) {
                        addSection(continueListening)
                    }

                    // 2. Your Queue (always shown – local playback, not filtered by local pref)
                    addSection(yourQueue)

                    // 3. Recently Played (local) — suppressed when live YT Music shelves exist
                    if (!useYouTubeMusicShelves && prefs.enableLocalListeningSections) {
                        addSection(recentlyPlayed)
                    }

                    // 3b. Live YouTube Music home shelves (Listen again, Quick picks, …)
                    if (useYouTubeMusicShelves) {
                        ytMusicShelves.forEach { addSection(it) }
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

                    // 7. Based on Your Listening — suppressed when live YT Music shelves exist
                    //    (avoids duplicating algorithmic rails that YT Music already provides).
                    if (!useYouTubeMusicShelves && prefs.enableLocalListeningSections && prefs.enableLocalMix) {
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
            return try {
                val likedVideos = authenticatedYouTubeClient.getLikedVideos()
                val filtered = likedVideos
                    .filterNot { it.isShort }
                    .filter { it.duration == 0L || it.duration >= 60L }
                    .filter { MusicFilter.isMusicTrack(it.title, it.channelName, it.duration, it.isShort) }
                    .distinctBy { it.id }
                    .map {
                        it.toPersonalizedFeedItem(
                            sourceBadge = "LIKED",
                            explanation = "Added to your Liked Music in your connected YouTube account."
                        )
                    }

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
                PersonalizedSection(
                    id = "sec_liked_music",
                    type = PersonalizedSectionType.LIKED_MUSIC,
                    title = "Liked Music",
                    subtitle = "From your connected account",
                    sourceLabel = "your account",
                    explanation = "Songs and tracks you marked as Liked in your connected YouTube account.",
                    items = emptyList(),
                    isFromCache = false,
                    lastUpdatedMillis = System.currentTimeMillis(),
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

        return try {
            val subsVideos = authenticatedYouTubeClient.getSubscriptionsFeed()
            val filtered = subsVideos
                .filterNot { it.isShort }
                .filter { it.duration == 0L || it.duration >= 60L }
                .filter { MusicFilter.isMusicTrack(it.title, it.channelName, it.duration, it.isShort) }
                .distinctBy { it.id }
                .take(20)
                .map {
                    it.toPersonalizedFeedItem(
                        sourceBadge = "NEW",
                        explanation = "New release from subscribed channel: ${it.channelName}."
                    )
                }

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
            PersonalizedSection(
                id = "sec_subscriptions",
                type = PersonalizedSectionType.NEW_FROM_SUBSCRIPTIONS,
                title = "New From Subscriptions",
                subtitle = "Latest releases from your channels",
                sourceLabel = "your subscriptions",
                explanation = "New music releases from channels you follow.",
                items = emptyList(),
                isFromCache = false,
                lastUpdatedMillis = System.currentTimeMillis(),
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
            val subtitle = if (!topArtistSeed.isNullOrBlank()) "Based on $topArtistSeed" else "Curated for your taste"
            val explanation = if (!topArtistSeed.isNullOrBlank()) {
                "Recommended because you frequently listen to $topArtistSeed on this device."
            } else {
                "Curated from your overall on-device listening preferences."
            }

            val filtered = rawResults
                .filter { it.duration == 0L || it.duration >= 60L }
                .filter { MusicFilter.isMusicTrack(it.title, it.artist, it.duration) }
                .distinctBy { it.id }
                .take(15)
                .map {
                    it.toPersonalizedFeedItem(
                        sourceBadge = "RECOMMENDED",
                        explanation = explanation
                    )
                }

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
            PersonalizedSection(
                id = "sec_based_on_listening",
                type = PersonalizedSectionType.BASED_ON_LISTENING,
                title = "Based on Your Listening",
                subtitle = "Curated recommendations",
                sourceLabel = "your listening data",
                explanation = "Curated recommendations based on your listening history.",
                items = emptyList(),
                isFromCache = false,
                lastUpdatedMillis = System.currentTimeMillis(),
            )
        }
    }

    // ── 8. Trending Music (Public / Region-Aware) ───────────────────────────
    private suspend fun buildTrendingSection(forceRefresh: Boolean): PersonalizedSection {
        return try {
            val region = personalizationPrefs.current().trendingRegion.ifBlank { "US" }
            val query = "trending songs $region official audio video"
            val trendingItems = youtubeRemoteDataSource.searchMusic(query)
            val explanation = "Popular track currently trending in your region ($region)."
            val filtered = trendingItems
                .filter { it.duration == 0L || it.duration >= 60L }
                .filter { MusicFilter.isMusicTrack(it.title, it.artist, it.duration) }
                .distinctBy { it.id }
                .take(20)
                .map {
                    it.toPersonalizedFeedItem(
                        sourceBadge = "TRENDING",
                        explanation = explanation
                    )
                }

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
            PersonalizedSection(
                id = "sec_trending",
                type = PersonalizedSectionType.TRENDING_MUSIC,
                title = "Trending Music",
                subtitle = "Popular tracks right now",
                sourceLabel = "Trending",
                explanation = "Popular tracks currently trending in your region.",
                items = emptyList(),
                isFromCache = false,
                lastUpdatedMillis = System.currentTimeMillis(),
            )
        }
    }

    // ── 9. YouTube Music Home Shelves (Authenticated FEmusic_home) ───────────
    /**
     * YT-MUSIC-OMEGA: build one rail per authenticated YouTube Music home shelf
     * ("Listen again", "Quick picks", …). Pure InnerTube — no local history/library.
     * Returns an empty list when unauthenticated or when the API yields no shelves,
     * so the caller can fall back to the local sections.
     */
    private suspend fun buildYouTubeMusicShelves(
        session: AccountSession,
        forceRefresh: Boolean,
    ): List<PersonalizedSection> {
        // Req 3+caller contract: the account that seeded this build owns its
        // completion. A late response from a previous account generation is
        // discarded by the collector's collectLatest, but builder-level code
        // re-checks the captured session before returning anything.
        if (session !is AccountSession.Connected) return emptyList()
        return try {
            val shelves = authenticatedYouTubeClient.getMusicHomeShelves()
            shelves.mapIndexedNotNull { index, shelf ->
                val items = shelf.items
                    .filter { MusicFilter.isMusicTrack(it.title, it.channelName, it.duration) }
                    .distinctBy { it.id }
                    .take(20)
                    .map {
                        it.toPersonalizedFeedItem(
                            sourceBadge = "YOUTUBE MUSIC",
                            explanation = "From your YouTube Music ${shelf.title} shelf.",
                        )
                    }
                if (items.isEmpty()) {
                    null
                } else {
                    PersonalizedSection(
                        id = "sec_yt_music_${index}_${shelf.title.hashCode()}",
                        type = PersonalizedSectionType.YOUTUBE_MUSIC_SHELF,
                        title = shelf.title,
                        subtitle = null,
                        sourceLabel = "YouTube Music",
                        explanation = "Live from your connected YouTube Music account.",
                        items = items,
                        isFromCache = false,
                        lastUpdatedMillis = System.currentTimeMillis(),
                    )
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Req 1: account switch / screen exit cancels in-flight builds
            // through collectLatest. Never convert that into an error section.
            throw e
        } catch (e: com.deepeye.musicpro.data.source.remote.youtube.InnerTubeAuthException) {
            // Req 4: auth travels the refresh/reauth path — surface an
            // account-gated retry section, never unavailable, never filler.
            Log.w(TAG, "event=yt_music_shelves result=reauth_required")
            return listOf(
                PersonalizedSection(
                    id = "sec_yt_music_reauth",
                    type = PersonalizedSectionType.YOUTUBE_MUSIC_SHELF,
                    title = "YouTube Music",
                    subtitle = null,
                    sourceLabel = "YouTube Music",
                    explanation = "Sign in with your YouTube account to see your music.",
                    items = emptyList(),
                    error = "Sign in required",
                    failureReason = com.deepeye.musicpro.data.source.remote.youtube
                        .MusicShelfFailureReason.REAUTH_REQUIRED.name,
                    isAccountRequired = true,
                    canRetry = true,
                    isFromCache = false,
                    lastUpdatedMillis = System.currentTimeMillis(),
                ),
            )
        } catch (e: com.deepeye.musicpro.data.source.remote.youtube.InnerTubeMusicUnavailableException) {
            // Req 5 + terminal check: typed unavailable yields an error-only
            // section — retry eligibility follows the retained reason, and no
            // generic search/guest content can enter this shelf.
            Log.w(TAG, "event=yt_music_shelves result=unavailable reason=${e.reason}")
            return listOf(
                PersonalizedSection(
                    id = "sec_yt_music_unavailable",
                    type = PersonalizedSectionType.YOUTUBE_MUSIC_SHELF,
                    title = "YouTube Music",
                    subtitle = null,
                    sourceLabel = "YouTube Music",
                    explanation = "YouTube Music is unavailable right now.",
                    items = emptyList(),
                    error = "Unavailable",
                    failureReason = e.reason.name,
                    isAccountRequired = true,
                    canRetry = e.reason !=
                        com.deepeye.musicpro.data.source.remote.youtube
                            .MusicShelfFailureReason.EMPTY_ACCOUNT_CONTENT,
                    isFromCache = false,
                    lastUpdatedMillis = System.currentTimeMillis(),
                ),
            )
        } catch (e: Exception) {
            // Req 2: narrow residual — programming errors only. The Music tab
            // errors on emptyList (honest empty, its own UI state); other repo
            // surfaces must not learn about them via a generic fallback here.
            Log.e(TAG, "event=yt_music_shelves result=provider_unavailable", e)
            return emptyList()
        }
    }

    /** Single-shelf view used by [refreshSection] (returns the first live shelf). */
    private suspend fun buildYouTubeMusicShelvesSection(
        session: AccountSession,
        forceRefresh: Boolean,
    ): PersonalizedSection {
        val shelves = buildYouTubeMusicShelves(session, forceRefresh)
        return shelves.firstOrNull() ?: PersonalizedSection(
            id = "sec_yt_music_empty",
            type = PersonalizedSectionType.YOUTUBE_MUSIC_SHELF,
            title = "YouTube Music",
            subtitle = null,
            sourceLabel = "YouTube Music",
            explanation = "No shelves returned from your YouTube Music account.",
            items = emptyList(),
            isFromCache = false,
            lastUpdatedMillis = System.currentTimeMillis(),
        )
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
            itemType = if (isShort || duration in 1..59) PersonalizedItemType.VIDEO else PersonalizedItemType.SONG,
            sourceBadge = sourceBadge ?: "DSP READY",
            explanation = explanation,
            mediaItem = MediaItem.Remote(
                id = id,
                title = title,
                artist = channelName,
                artworkUri = Uri.parse(thumbnailUrl),
                duration = duration * 1000L,
                isVideo = false,
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

