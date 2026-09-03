// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.personalization

import com.deepeye.musicpro.account.AccountSession
import com.deepeye.musicpro.account.AccountSessionManager
import com.deepeye.musicpro.data.cache.HiddenContentManager
import com.deepeye.musicpro.data.prefs.PersonalizationPreferences
import com.deepeye.musicpro.data.prefs.TasteProfile
import com.deepeye.musicpro.domain.model.personalization.PersonalizedFeedState
import com.deepeye.musicpro.domain.model.personalization.PersonalizedSection
import com.deepeye.musicpro.domain.model.personalization.PersonalizedSectionType
import com.deepeye.musicpro.domain.repository.PersonalizationRepository
import com.deepeye.musicpro.domain.repository.TasteProfileRepository
import com.deepeye.musicpro.ui.settings.PersonalizationSettingsEvent
import com.deepeye.musicpro.ui.settings.PersonalizationSettingsViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PersonalizationSettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val personalizationRepository = mockk<PersonalizationRepository>(relaxed = true)
    private val accountSessionManager = mockk<AccountSessionManager>(relaxed = true)
    private val tasteProfileRepository = mockk<TasteProfileRepository>(relaxed = true)
    private val hiddenContentManager = HiddenContentManager()

    private val preferencesFlow = MutableStateFlow(PersonalizationPreferences())
    private val accountSessionFlow = MutableStateFlow<AccountSession>(AccountSession.LoggedOut)
    private val tasteProfileFlow = MutableStateFlow(TasteProfile(preferredLanguages = setOf("English"), preferredGenres = setOf("Rock")))
    private val feedStateFlow = MutableStateFlow(
        PersonalizedFeedState(
            sections = listOf(
                PersonalizedSection(
                    id = "sec_1",
                    type = PersonalizedSectionType.YOUR_QUEUE,
                    title = "Queue",
                    sourceLabel = "Local",
                    isFromCache = true,
                    items = emptyList()
                )
            )
        )
    )

    private lateinit var viewModel: PersonalizationSettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        every { personalizationRepository.observePreferences() } returns preferencesFlow
        every { personalizationRepository.observePersonalizedFeed() } returns feedStateFlow
        every { accountSessionManager.accountSession } returns accountSessionFlow
        every { tasteProfileRepository.getTasteProfile() } returns tasteProfileFlow

        viewModel = PersonalizationSettingsViewModel(
            personalizationRepository = personalizationRepository,
            accountSessionManager = accountSessionManager,
            tasteProfileRepository = tasteProfileRepository,
            hiddenContentManager = hiddenContentManager,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialUiState_reflectsDependencies() = runTest {
        val state = viewModel.uiState.value
        assertEquals(preferencesFlow.value, state.preferences)
        assertEquals(AccountSession.LoggedOut, state.accountState)
        assertTrue(state.preferredLanguages.contains("English"))
        assertTrue(state.preferredGenres.contains("Rock"))
        assertEquals(1, state.cacheSummary?.cachedSectionCount)
        assertEquals(0, state.hiddenItemCount)
    }

    @Test
    fun testSetPersonalizationEnabled_invokesRepositoryAndRefreshes() = runTest {
        viewModel.onEvent(PersonalizationSettingsEvent.SetPersonalizationEnabled(false))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { personalizationRepository.updatePreferences(any()) }
        coVerify { personalizationRepository.refreshFeed(forceRefresh = true) }
    }

    @Test
    fun testSetUseLocalHistory_invokesRepository() = runTest {
        viewModel.onEvent(PersonalizationSettingsEvent.SetUseLocalHistory(false))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { personalizationRepository.updatePreferences(any()) }
    }

    @Test
    fun testSetArtistDiversityLimit_invokesRepository() = runTest {
        viewModel.onEvent(PersonalizationSettingsEvent.SetArtistDiversityLimit(4))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { personalizationRepository.updatePreferences(any()) }
        coVerify { personalizationRepository.refreshFeed(forceRefresh = true) }
    }

    @Test
    fun testToggleLanguage_updatesTasteProfileAndRefreshes() = runTest {
        viewModel.onEvent(PersonalizationSettingsEvent.ToggleLanguage("Hindi"))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { tasteProfileRepository.updatePreferredLanguages(setOf("English", "Hindi")) }
        coVerify { personalizationRepository.refreshFeed(forceRefresh = true) }
    }

    @Test
    fun testToggleGenre_updatesTasteProfileAndRefreshes() = runTest {
        viewModel.onEvent(PersonalizationSettingsEvent.ToggleGenre("Chill"))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { tasteProfileRepository.updatePreferredGenres(setOf("Rock", "Chill")) }
        coVerify { personalizationRepository.refreshFeed(forceRefresh = true) }
    }

    @Test
    fun testClearCache_requiresConfirmationFlow() = runTest {
        assertNull(viewModel.uiState.value.activeConfirmation)

        viewModel.onEvent(PersonalizationSettingsEvent.ClearPersonalizationCacheRequested)
        assertNotNull(viewModel.uiState.value.activeConfirmation)
        assertEquals("Clear Personalized Cache", viewModel.uiState.value.activeConfirmation?.title)

        // Dismissal test
        viewModel.onEvent(PersonalizationSettingsEvent.DismissConfirmation)
        assertNull(viewModel.uiState.value.activeConfirmation)
        coVerify(exactly = 0) { personalizationRepository.clearPersonalizationCache() }

        // Confirm execution test
        viewModel.onEvent(PersonalizationSettingsEvent.ClearPersonalizationCacheRequested)
        viewModel.uiState.value.activeConfirmation?.onConfirm?.invoke()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { personalizationRepository.clearPersonalizationCache() }
        coVerify { personalizationRepository.refreshFeed(forceRefresh = true) }
    }

    @Test
    fun testClearHistory_requiresConfirmationFlow() = runTest {
        viewModel.onEvent(PersonalizationSettingsEvent.ClearLocalHistoryRequested)
        assertNotNull(viewModel.uiState.value.activeConfirmation)
        assertEquals("Clear Local Listening History", viewModel.uiState.value.activeConfirmation?.title)

        viewModel.uiState.value.activeConfirmation?.onConfirm?.invoke()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { personalizationRepository.clearLocalHistory() }
        coVerify { personalizationRepository.refreshFeed(forceRefresh = true) }
    }

    @Test
    fun testClearHiddenContent_requiresConfirmationFlow() = runTest {
        viewModel.onEvent(PersonalizationSettingsEvent.ClearHiddenContentRequested)
        assertNotNull(viewModel.uiState.value.activeConfirmation)
        assertEquals("Restore All Hidden Content", viewModel.uiState.value.activeConfirmation?.title)

        viewModel.uiState.value.activeConfirmation?.onConfirm?.invoke()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { personalizationRepository.resetAllHiddenContent() }
        coVerify { personalizationRepository.refreshFeed(forceRefresh = true) }
    }

    @Test
    fun testRefreshPersonalizedFeed_handlesSuccess() = runTest {
        viewModel.onEvent(PersonalizationSettingsEvent.RefreshPersonalizedFeed)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { personalizationRepository.refreshFeed(forceRefresh = true) }
        assertEquals("Personalized feed refreshed", viewModel.uiState.value.message)
    }

    @Test
    fun testSetCountryOverride_invokesRepository() = runTest {
        viewModel.onEvent(PersonalizationSettingsEvent.SetCountryOverride("IN"))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { personalizationRepository.updatePreferences(any()) }
        coVerify { personalizationRepository.refreshFeed(forceRefresh = true) }
    }
}
