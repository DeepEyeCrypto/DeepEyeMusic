// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.personalization

import com.deepeye.musicpro.data.cache.HiddenContentManager
import com.deepeye.musicpro.domain.repository.PersonalizationRepository
import com.deepeye.musicpro.ui.settings.HiddenContentTab
import com.deepeye.musicpro.ui.settings.HiddenContentViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HiddenContentViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val hiddenContentManager = HiddenContentManager()
    private val personalizationRepository = mockk<PersonalizationRepository>(relaxed = true)

    private lateinit var viewModel: HiddenContentViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        viewModel = HiddenContentViewModel(
            hiddenContentManager = hiddenContentManager,
            personalizationRepository = personalizationRepository,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testTabSelectionAndSearchQuery() = runTest {
        assertEquals(HiddenContentTab.SONGS, viewModel.uiState.value.selectedTab)
        assertEquals("", viewModel.uiState.value.searchQuery)

        viewModel.selectTab(HiddenContentTab.ARTISTS)
        assertEquals(HiddenContentTab.ARTISTS, viewModel.uiState.value.selectedTab)

        viewModel.setSearchQuery("Coldplay")
        assertEquals("Coldplay", viewModel.uiState.value.searchQuery)
    }

    @Test
    fun testObserveHiddenItems_populatesUiState() = runTest {
        hiddenContentManager.hideItem("s1", "Song One", "Artist A")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.hiddenSongs.size)
        assertEquals("s1", state.hiddenSongs[0].id)
        assertEquals("Song One", state.hiddenSongs[0].title)
        assertEquals(1, state.hiddenArtists.size)
        assertEquals("Artist A", state.hiddenArtists[0])
    }

    @Test
    fun testRestoreItem_callsUnhideAndRefreshesFeed() = runTest {
        hiddenContentManager.hideItem("s1", "Song One")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.restoreItem("s1")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { personalizationRepository.unhideItem("s1") }
        coVerify { personalizationRepository.refreshFeed(forceRefresh = true) }
        assertEquals("Song restored to recommendations", viewModel.uiState.value.message)
    }

    @Test
    fun testRestoreArtist_callsUnhideAndRefreshesFeed() = runTest {
        hiddenContentManager.hideItem("s1", "Song One", "Artist A")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.restoreArtist("Artist A")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { personalizationRepository.unhideArtist("Artist A") }
        coVerify { personalizationRepository.refreshFeed(forceRefresh = true) }
        assertEquals("Artist restored to recommendations", viewModel.uiState.value.message)
    }

    @Test
    fun testRestoreAll_requiresConfirmation() = runTest {
        hiddenContentManager.hideItem("s1", "Song One", "Artist A")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.requestRestoreAll()
        val conf = viewModel.uiState.value.activeConfirmation
        assertNotNull(conf)
        assertEquals("Restore All Hidden Content", conf?.title)

        conf?.onConfirm?.invoke()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { personalizationRepository.resetAllHiddenContent() }
        coVerify { personalizationRepository.refreshFeed(forceRefresh = true) }
        assertEquals("All hidden items restored", viewModel.uiState.value.message)
    }
}
