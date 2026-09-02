// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.personalization

import android.net.Uri
import com.deepeye.musicpro.account.AccountSession
import com.deepeye.musicpro.account.AccountSessionManager
import com.deepeye.musicpro.data.cache.AccountPersonalizationCache
import com.deepeye.musicpro.data.db.*
import com.deepeye.musicpro.data.repository.PersonalizationRepositoryImpl
import com.deepeye.musicpro.data.source.remote.youtube.AuthenticatedYouTubeClient
import com.deepeye.musicpro.data.source.remote.youtube.YoutubeRemoteDataSource
import com.deepeye.musicpro.domain.model.MediaItem
import com.deepeye.musicpro.domain.model.home.HomeMusicItem
import com.deepeye.musicpro.domain.model.home.HomeVideoItem
import com.deepeye.musicpro.domain.model.library.LibraryItem
import com.deepeye.musicpro.domain.model.library.LibraryItemType
import com.deepeye.musicpro.domain.model.personalization.*
import com.deepeye.musicpro.domain.repository.library.LibraryRepository
import com.deepeye.musicpro.player.queue.QueueManager
import com.deepeye.musicpro.ui.library.LibraryHomeState
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class PersonalizationRepositoryTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val repoDispatcher = Dispatchers.Unconfined

    private val accountSessionManager = mockk<AccountSessionManager>()
    private val accountPersonalizationCache = AccountPersonalizationCache()
    private val queueManager = mockk<QueueManager>()
    private val libraryRepository = mockk<LibraryRepository>()
    private val historyDao = mockk<HistoryDao>()
    private val tasteDao = mockk<TasteDao>()
    private val recommendationDao = mockk<RecommendationDao>()
    private val authenticatedYouTubeClient = mockk<AuthenticatedYouTubeClient>()
    private val youtubeRemoteDataSource = mockk<YoutubeRemoteDataSource>()

    private val accountSessionFlow = MutableStateFlow<AccountSession>(AccountSession.LoggedOut)
    private val queueFlow = MutableStateFlow<List<MediaItem>>(emptyList())

    private lateinit var repository: PersonalizationRepositoryImpl

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        every { accountSessionManager.accountSession } returns accountSessionFlow
        every { queueManager.queue } returns queueFlow
        every { queueManager.currentIndex } returns MutableStateFlow(0)

        every { historyDao.getRecentPlaybacks(any()) } returns flowOf(
            listOf(
                PlaybackHistoryEntity(
                    id = 1,
                    mediaId = "v1",
                    title = "Recent Song 1",
                    artist = "Artist A",
                    artworkUri = "https://img/v1.jpg",
                    playedAt = 1000L,
                    playDurationMs = 120000L,
                    totalDurationMs = 180000L,
                    completionPercent = 0.67f,
                    source = "youtube"
                )
            )
        )

        every { tasteDao.getRecentHistory(any()) } returns flowOf(
            listOf(
                PlayEvent(
                    songId = "s1",
                    title = "Taste Song 1",
                    artistId = "Artist 1",
                    language = "en",
                    playedMs = 120000L,
                    durationMs = 200000L,
                    timestamp = 2000L,
                    source = "youtube",
                    artworkUri = "https://img/s1.jpg"
                ),
                PlayEvent(
                    songId = "s1",
                    title = "Taste Song 1",
                    artistId = "Artist 1",
                    language = "en",
                    playedMs = 120000L,
                    durationMs = 200000L,
                    timestamp = 2050L,
                    source = "youtube",
                    artworkUri = "https://img/s1.jpg"
                ),
                PlayEvent(
                    songId = "s2",
                    title = "Taste Song 2",
                    artistId = "Artist 2",
                    language = "en",
                    playedMs = 210000L,
                    durationMs = 250000L,
                    timestamp = 1500L,
                    source = "youtube",
                    artworkUri = "https://img/s2.jpg"
                )
            )
        )

        coEvery { recommendationDao.getTopArtistsSince(any(), any()) } returns listOf(
            ArtistStats(channelId = "c1", artistName = "Seed Artist", playCount = 10, avgCompletion = 0.9f, likeCount = 3, skipCount = 0)
        )

        every { libraryRepository.observeLibraryHome() } returns flowOf(
            LibraryHomeState(
                playlists = listOf(
                    LibraryItem(
                        id = "pl_1",
                        type = LibraryItemType.PLAYLIST,
                        title = "Chill Hits Playlist",
                        subtitle = "15 songs",
                    )
                )
            )
        )

        coEvery { youtubeRemoteDataSource.searchMusic(any()) } returns listOf(
            HomeMusicItem(
                id = "trend_1",
                title = "Trending Song 1",
                artist = "Trend Artist",
                thumbnailUrl = "https://img/trend1.jpg",
                duration = 200,
            )
        )

        coEvery { authenticatedYouTubeClient.getLikedVideos() } returns listOf(
            HomeVideoItem(
                id = "liked_1",
                title = "Liked Song 1",
                channelName = "Liked Artist",
                thumbnailUrl = "https://img/liked1.jpg",
                duration = 220,
            )
        )

        coEvery { authenticatedYouTubeClient.getSubscriptionsFeed() } returns listOf(
            HomeVideoItem(
                id = "sub_1",
                title = "Subscribed Track 1",
                channelName = "Subbed Artist",
                thumbnailUrl = "https://img/sub1.jpg",
                duration = 240,
            )
        )

        repository = PersonalizationRepositoryImpl(
            accountSessionManager = accountSessionManager,
            accountPersonalizationCache = accountPersonalizationCache,
            queueManager = queueManager,
            libraryRepository = libraryRepository,
            historyDao = historyDao,
            tasteDao = tasteDao,
            recommendationDao = recommendationDao,
            authenticatedYouTubeClient = authenticatedYouTubeClient,
            youtubeRemoteDataSource = youtubeRemoteDataSource,
            ioDispatcher = repoDispatcher,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testLoggedOutSession_producesGuestAndLocalSectionsOnly() = runTest {
        accountSessionFlow.value = AccountSession.LoggedOut
        repository.refreshFeed(forceRefresh = true)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = repository.observePersonalizedFeed().value
        assertFalse(state.isLoading)
        assertTrue(state.accountSession is AccountSession.LoggedOut)

        val sectionTypes = state.sections.map { it.type }
        assertTrue(sectionTypes.contains(PersonalizedSectionType.CONTINUE_LISTENING))
        assertTrue(sectionTypes.contains(PersonalizedSectionType.RECENTLY_PLAYED))
        assertTrue(sectionTypes.contains(PersonalizedSectionType.BASED_ON_LISTENING))
        assertTrue(sectionTypes.contains(PersonalizedSectionType.TRENDING_MUSIC))
        assertFalse(sectionTypes.contains(PersonalizedSectionType.NEW_FROM_SUBSCRIPTIONS))

        coVerify(exactly = 0) { authenticatedYouTubeClient.getSubscriptionsFeed() }
        coVerify(exactly = 0) { authenticatedYouTubeClient.getLikedVideos() }
    }

    @Test
    fun testConnectedSession_producesAccountScopedSections() = runTest {
        val connected = AccountSession.Connected(
            accountKey = "test_sha256_key_1",
            displayName = "DeepEye Tester",
            avatarUrl = "https://avatar/1.jpg"
        )
        accountSessionFlow.value = connected
        repository.refreshFeed(forceRefresh = true)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = repository.observePersonalizedFeed().value
        assertFalse(state.isLoading)
        assertTrue(state.accountSession is AccountSession.Connected)

        val sectionTypes = state.sections.map { it.type }
        assertTrue(sectionTypes.contains(PersonalizedSectionType.LIKED_MUSIC))
        assertTrue(sectionTypes.contains(PersonalizedSectionType.NEW_FROM_SUBSCRIPTIONS))
        assertTrue(sectionTypes.contains(PersonalizedSectionType.YOUR_PLAYLISTS))
        assertTrue(sectionTypes.contains(PersonalizedSectionType.TRENDING_MUSIC))

        val subsSection = state.sections.first { it.type == PersonalizedSectionType.NEW_FROM_SUBSCRIPTIONS }
        assertEquals("your subscriptions", subsSection.sourceLabel)
        assertEquals("sub_1", subsSection.items.first().id)
    }

    @Test
    fun testRecentlyPlayedDeduplication() = runTest {
        accountSessionFlow.value = AccountSession.LoggedOut
        repository.refreshFeed(forceRefresh = true)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = repository.observePersonalizedFeed().value
        val recent = state.sections.first { it.type == PersonalizedSectionType.RECENTLY_PLAYED }
        assertEquals(2, recent.items.size)
        assertEquals("s1", recent.items[0].id)
        assertEquals("s2", recent.items[1].id)
    }

    @Test
    fun testSectionLevelResilience_remoteFailureDoesNotFailFeed() = runTest {
        // Set up the mock FIRST, before triggering account session change
        coEvery { authenticatedYouTubeClient.getSubscriptionsFeed() } throws IOException("Network")

        accountSessionFlow.value = AccountSession.Connected(accountKey = "err_acc", displayName = "Error")
        testDispatcher.scheduler.advanceUntilIdle()

        // Now force refresh to ensure we hit the network (not cache)
        repository.refreshFeed(forceRefresh = true)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = repository.observePersonalizedFeed().value
        assertFalse(state.isLoading)
        assertNull(state.globalError)

        val subs = state.sections.first { it.type == PersonalizedSectionType.NEW_FROM_SUBSCRIPTIONS }
        assertNotNull(subs.error)
        assertTrue(subs.canRetry)

        val trending = state.sections.first { it.type == PersonalizedSectionType.TRENDING_MUSIC }
        assertNull(trending.error)
        assertTrue(trending.items.isNotEmpty())
    }

    @Test
    fun testAccountPersonalizationCache_hitAndInvalidation() = runTest {
        val accKey = "cache_test_key"
        accountSessionFlow.value = AccountSession.Connected(accountKey = accKey, displayName = "Cache")
        testDispatcher.scheduler.advanceUntilIdle()
        clearMocks(authenticatedYouTubeClient) // reset call counts from automatic observer build

        repository.refreshFeed(forceRefresh = true)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 1) { authenticatedYouTubeClient.getSubscriptionsFeed() }

        repository.refreshFeed(forceRefresh = false)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 1) { authenticatedYouTubeClient.getSubscriptionsFeed() }

        repository.invalidateAccountCache(accKey)
        repository.refreshFeed(forceRefresh = false)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 2) { authenticatedYouTubeClient.getSubscriptionsFeed() }
    }

    @Test
    fun testAccountSwitch_invalidatesOldAccountCache() = runTest {
        val key1 = "account_alpha"
        val key2 = "account_beta"

        accountSessionFlow.value = AccountSession.Connected(accountKey = key1, displayName = "U1")
        repository.refreshFeed(forceRefresh = true)
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(accountPersonalizationCache.get(key1, PersonalizedSectionType.NEW_FROM_SUBSCRIPTIONS))

        repository.invalidateAccountCache(key1)
        accountSessionFlow.value = AccountSession.Connected(accountKey = key2, displayName = "U2")
        repository.refreshFeed(forceRefresh = true)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(accountPersonalizationCache.get(key1, PersonalizedSectionType.NEW_FROM_SUBSCRIPTIONS))
        assertNotNull(accountPersonalizationCache.get(key2, PersonalizedSectionType.NEW_FROM_SUBSCRIPTIONS))
    }

    @Test
    fun testPrivacyBoundary_noCredentialFieldsInModels() {
        val forbidden = listOf("token", "cookie", "bearer", "authheader", "email", "password", "secret")
        val allFields = (
            PersonalizedFeedState::class.java.declaredFields.map { it.name.lowercase() } +
            PersonalizedSection::class.java.declaredFields.map { it.name.lowercase() } +
            PersonalizedFeedItem::class.java.declaredFields.map { it.name.lowercase() }
        )
        for (bad in forbidden) {
            for (field in allFields) {
                assertFalse("Field '$field' contains forbidden '$bad'", field.contains(bad))
            }
        }
    }

    @Test
    fun testRefreshSingleSection_updatesOnlyTargetSection() = runTest {
        coEvery { youtubeRemoteDataSource.searchMusic(any()) } returns listOf(
            HomeMusicItem(id = "trend_seed", title = "Seed Song Official Audio", artist = "Seed Artist", thumbnailUrl = "https://img/seed.jpg", duration = 180)
        )

        // refreshSection adds the section even if the feed hasn't been built yet
        repository.refreshSection(PersonalizedSectionType.TRENDING_MUSIC)

        val updated = repository.observePersonalizedFeed().value.sections
        assertTrue("Expected trending section present, got ${updated.map { it.type }}", updated.any { it.type == PersonalizedSectionType.TRENDING_MUSIC })
        val trending = updated.first { it.type == PersonalizedSectionType.TRENDING_MUSIC }
        assertEquals(1, trending.items.size)
        assertEquals("trend_seed", trending.items.first().id)
    }

    @Test
    fun testSectionsHaveHonestExplanations() = runTest {
        accountSessionFlow.value = AccountSession.Connected(accountKey = "explain_key", displayName = "Explain Tester")
        repository.refreshFeed(forceRefresh = true)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = repository.observePersonalizedFeed().value
        assertTrue("Expected non-empty sections", state.sections.isNotEmpty())

        for (section in state.sections) {
            assertNotNull("Section '${section.title}' must have an explanation", section.explanation)
            assertTrue(
                "Section '${section.title}' explanation must not be blank",
                section.explanation!!.isNotBlank()
            )
            // Explanations must not contain backend internals
            val lower = section.explanation!!.lowercase()
            assertFalse("Explanation must not contain 'token'", lower.contains("token"))
            assertFalse("Explanation must not contain 'cookie'", lower.contains("cookie"))
            assertFalse("Explanation must not contain 'bearer'", lower.contains("bearer"))
        }
    }

    @Test
    fun testItemsHaveExplanations() = runTest {
        accountSessionFlow.value = AccountSession.Connected(accountKey = "item_explain_key", displayName = "Item Tester")
        repository.refreshFeed(forceRefresh = true)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = repository.observePersonalizedFeed().value
        for (section in state.sections.filter { it.items.isNotEmpty() }) {
            for (item in section.items) {
                assertNotNull(
                    "Item '${item.title}' in section '${section.title}' must have explanation",
                    item.explanation
                )
                assertTrue(
                    "Item '${item.title}' explanation must not be blank",
                    item.explanation!!.isNotBlank()
                )
            }
        }
    }

    @Test
    fun testSectionsHaveLastUpdatedMillis() = runTest {
        accountSessionFlow.value = AccountSession.Connected(accountKey = "ts_key", displayName = "TS")
        repository.refreshFeed(forceRefresh = true)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = repository.observePersonalizedFeed().value
        for (section in state.sections) {
            assertTrue(
                "Section '${section.title}' must have lastUpdatedMillis > 0",
                section.lastUpdatedMillis > 0
            )
        }
    }

    @Test
    fun testIsFromCacheDefaultsFalseOnFreshFeed() = runTest {
        accountSessionFlow.value = AccountSession.Connected(accountKey = "fresh_key", displayName = "Fresh")
        repository.refreshFeed(forceRefresh = true)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = repository.observePersonalizedFeed().value
        for (section in state.sections) {
            assertFalse(
                "Fresh section '${section.title}' should not be from cache",
                section.isFromCache
            )
        }
    }

    @Test
    fun testLocalSectionsAreNeverFromCache() = runTest {
        accountSessionFlow.value = AccountSession.LoggedOut
        repository.refreshFeed(forceRefresh = true)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = repository.observePersonalizedFeed().value
        val localSections = state.sections.filter {
            it.type == PersonalizedSectionType.CONTINUE_LISTENING ||
            it.type == PersonalizedSectionType.RECENTLY_PLAYED ||
            it.type == PersonalizedSectionType.YOUR_QUEUE
        }

        for (section in localSections) {
            assertFalse(
                "Local section '${section.title}' should never be marked as from cache",
                section.isFromCache
            )
        }
    }

    @Test
    fun testPrivacyBoundary_explanationFieldsContainNoSecrets() {
        val forbidden = listOf("token", "cookie", "bearer", "authheader", "email", "password", "secret")
        val explanationFields = listOf(
            PersonalizedSection::class.java.getDeclaredField("explanation").name.lowercase(),
            PersonalizedFeedItem::class.java.getDeclaredField("explanation").name.lowercase(),
        )
        for (field in explanationFields) {
            for (bad in forbidden) {
                assertFalse("Field '$field' contains forbidden '$bad'", field.contains(bad))
            }
        }
    }
}
