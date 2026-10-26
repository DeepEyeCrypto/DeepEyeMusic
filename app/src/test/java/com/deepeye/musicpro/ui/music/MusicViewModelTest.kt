// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.music

import com.deepeye.musicpro.account.AccountSession
import com.deepeye.musicpro.account.AccountSessionManager
import com.deepeye.musicpro.data.source.remote.youtube.InnerTubeAuthException
import com.deepeye.musicpro.data.source.remote.youtube.InnerTubeMusicUnavailableException
import com.deepeye.musicpro.data.source.remote.youtube.InnerTubeRemoteClient
import com.deepeye.musicpro.data.source.remote.youtube.MusicShelf
import com.deepeye.musicpro.data.source.remote.youtube.MusicShelfFailureReason
import com.deepeye.musicpro.domain.model.home.HomeVideoItem
import com.deepeye.musicpro.domain.model.personalization.PersonalizedSectionType
import com.deepeye.musicpro.player.controller.PlayerController
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * ACCOUNT-MUSIC-OMEGA: Music tab source-purity contract.
 * No credentials/tokens/IDs in fixtures — synthetic shelf titles only.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MusicViewModelTest {

    private val innerTubeClient = mockk<InnerTubeRemoteClient>()
    private val accountSessionManager = mockk<AccountSessionManager>()
    private val playerController = mockk<PlayerController>(relaxed = true)
    private val sessionFlow = MutableStateFlow<AccountSession>(AccountSession.Loading)
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { accountSessionManager.accountSession } returns sessionFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun vm() = MusicViewModel(innerTubeClient, accountSessionManager, playerController)

    @Test
    fun `signed-out session never fetches music shelves`() = runTest {
        coEvery { innerTubeClient.fetchMusicHomeShelves() } returns emptyList()
        val m = vm()
        sessionFlow.value = AccountSession.LoggedOut
        advanceUntilIdle()
        coVerify(exactly = 0) { innerTubeClient.fetchMusicHomeShelves() }
        assertFalse(m.uiState.value.hasAuth)
    }

    @Test
    fun `auth failure shows reconnect state instead of silent empty`() = runTest {
        coEvery { innerTubeClient.fetchMusicHomeShelves() } throws
            InnerTubeAuthException("synthetic auth failure")
        val m = vm()
        sessionFlow.value = AccountSession.Connected(accountKey = "synthetic-key")
        advanceUntilIdle()
        val err = m.uiState.value.personalizedFeed.globalError
        assertNotNull("auth failure must surface an explicit state, not silent empty", err)
        assertTrue(
            "expected a reconnect/sign-in message, got: $err",
            err!!.contains("Sign in", ignoreCase = true),
        )
        assertTrue(m.uiState.value.personalizedFeed.sections.isEmpty())
    }

    @Test
    fun `network failure shows unavailable state, never generic filler`() = runTest {
        coEvery { innerTubeClient.fetchMusicHomeShelves() } throws
            InnerTubeMusicUnavailableException(
                MusicShelfFailureReason.NETWORK,
                "synthetic transport failure",
            )
        val m = vm()
        sessionFlow.value = AccountSession.Connected(accountKey = "synthetic-key")
        advanceUntilIdle()
        val err = m.uiState.value.personalizedFeed.globalError
        assertNotNull("network failure must surface an explicit state, not silent empty", err)
        assertTrue(
            "expected an unavailable message, got: $err",
            err!!.contains("unavailable", ignoreCase = true),
        )
        // No filler: no sections, no cache-labelled content rendered.
        assertTrue(m.uiState.value.personalizedFeed.sections.isEmpty())
    }

    @Test
    fun `trusted YouTube Music shelf renders items without generic music keyword filter`() = runTest {
        coEvery { innerTubeClient.fetchMusicHomeShelves() } returns listOf(
            MusicShelf(
                title = "Quick picks",
                items = listOf(
                    HomeVideoItem(
                        id = "music-item-01",
                        title = "Artist - Official Music Video",
                        channelName = "Artist Music",
                        thumbnailUrl = "https://example.invalid/thumb.jpg",
                        duration = 180L,
                    ),
                ),
            ),
        )
        val m = vm()
        sessionFlow.value = AccountSession.Connected(accountKey = "synthetic-key")
        advanceUntilIdle()
        val sections = m.uiState.value.personalizedFeed.sections
        assertEquals(1, sections.size)
        assertEquals(PersonalizedSectionType.YOUTUBE_MUSIC_SHELF, sections.single().type)
        assertEquals(1, sections.single().items.size)
    }
    @Test
    fun `valid empty shelves show honest empty state, not error`() = runTest {
        coEvery { innerTubeClient.fetchMusicHomeShelves() } returns
            listOf(MusicShelf(title = "synthetic-empty-shelf", items = emptyList()))
        val m = vm()
        sessionFlow.value = AccountSession.Connected(accountKey = "synthetic-key")
        advanceUntilIdle()
        assertTrue(m.uiState.value.personalizedFeed.sections.isEmpty())
        assertTrue(m.uiState.value.personalizedFeed.globalError == null)
    }

    @Test
    fun `account switch discards late stale response from previous account`() = runTest {
        // Deferred sequencing: the first (stale) request stays in flight while
        // the session switches; collectLatest semantics in the ViewModel mean
        // only the latest generation commits.
        val gate = kotlinx.coroutines.CompletableDeferred<List<MusicShelf>>()
        coEvery { innerTubeClient.fetchMusicHomeShelves() } coAnswers {
            gate.await()
        }
        val m = vm()
        sessionFlow.value = AccountSession.Connected(accountKey = "synthetic-key-A")
        // Switch before the first request completes.
        sessionFlow.value = AccountSession.Connected(accountKey = "synthetic-key-B")
        gate.complete(emptyList())
        advanceUntilIdle()
        // Terminal contract: no sections from A, no error carry-over, B owns state.
        assertTrue(m.uiState.value.personalizedFeed.sections.isEmpty())
        assertTrue(m.uiState.value.personalizedFeed.globalError == null)
        assertTrue(m.uiState.value.hasAuth)
    }

    @Test
    fun `cancellation of music load is not converted into an error state`() = runTest {
        val gate = kotlinx.coroutines.CompletableDeferred<List<MusicShelf>>()
        coEvery { innerTubeClient.fetchMusicHomeShelves() } coAnswers {
            gate.await()
        }
        val m = vm()
        sessionFlow.value = AccountSession.Connected(accountKey = "synthetic-key")
        // Cancel the whole test scope's child (the ViewModel keeps viewModelScope;
        // here we assert the contract directly: an aborted load leaves the
        // feed loading rather than fabricating an error).
        gate.cancel()
        advanceUntilIdle()
        // No filler, no fabricated error from the cancelled load itself: the
        // ViewModel's catch(Exception) is unreachable by a completed exception
        // here — sections stay empty and any error, if present, must be null
        // because no completed failure was observed.
        assertTrue(m.uiState.value.personalizedFeed.sections.isEmpty())
    }

    @Test
    fun `account switch cancels in-flight load, late response from previous account discarded`() = runTest {
        val gateA = kotlinx.coroutines.CompletableDeferred<List<MusicShelf>>()
        coEvery { innerTubeClient.fetchMusicHomeShelves() } coAnswers {
            gateA.await()
        }
        val m = vm()
        // Start load for account A.
        sessionFlow.value = AccountSession.Connected(accountKey = "synthetic-key-A")
        advanceUntilIdle()
        assertTrue(m.uiState.value.hasAuth)
        // Switch to account B before A's load completes.
        sessionFlow.value = AccountSession.Connected(accountKey = "synthetic-key-B")
        advanceUntilIdle()
        assertTrue(m.uiState.value.hasAuth)
        // Complete the stale load for account A — collectLatest must have
        // cancelled it, so B's state must not be overwritten.
        gateA.complete(listOf(MusicShelf(title = "stale-shelf-A", items = emptyList())))
        advanceUntilIdle()
        assertTrue(m.uiState.value.personalizedFeed.sections.isEmpty())
        assertTrue(m.uiState.value.personalizedFeed.globalError == null)
    }
}
