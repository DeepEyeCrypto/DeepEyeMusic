// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.musicpro.account.AccountSessionManager
import com.deepeye.musicpro.data.cache.HiddenContentManager
import com.deepeye.musicpro.domain.repository.PersonalizationRepository
import com.deepeye.musicpro.domain.repository.TasteProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PersonalizationSettingsViewModel @Inject constructor(
    private val personalizationRepository: PersonalizationRepository,
    private val accountSessionManager: AccountSessionManager,
    private val tasteProfileRepository: TasteProfileRepository,
    private val hiddenContentManager: HiddenContentManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PersonalizationSettingsUiState())
    val uiState: StateFlow<PersonalizationSettingsUiState> = _uiState.asStateFlow()

    companion object {
        private const val TAG = "PersonalizationSettingsVM"
    }

    init {
        observePreferences()
        observeAccountSession()
        observeTasteProfile()
        observeHiddenContent()
        observeFeedForCacheSummary()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            personalizationRepository.observePreferences().collectLatest { prefs ->
                _uiState.update { it.copy(preferences = prefs) }
            }
        }
    }

    private fun observeAccountSession() {
        viewModelScope.launch {
            accountSessionManager.accountSession.collectLatest { session ->
                _uiState.update { it.copy(accountState = session) }
            }
        }
    }

    private fun observeTasteProfile() {
        viewModelScope.launch {
            tasteProfileRepository.getTasteProfile().collectLatest { profile ->
                _uiState.update {
                    it.copy(
                        preferredLanguages = profile.preferredLanguages,
                        preferredGenres = profile.preferredGenres,
                    )
                }
            }
        }
    }

    private fun observeHiddenContent() {
        viewModelScope.launch {
            hiddenContentManager.hiddenItemIds.collectLatest { items ->
                _uiState.update { it.copy(hiddenItemCount = items.size) }
            }
        }
        viewModelScope.launch {
            hiddenContentManager.hiddenArtistNames.collectLatest { artists ->
                _uiState.update { it.copy(hiddenArtistCount = artists.size) }
            }
        }
    }

    private fun observeFeedForCacheSummary() {
        viewModelScope.launch {
            personalizationRepository.observePersonalizedFeed().collectLatest { feed ->
                val cachedSections = feed.sections.filter { it.isFromCache }
                val totalItems = feed.sections.sumOf { it.items.size }
                val summary = CacheSummary(
                    cachedSectionCount = cachedSections.size,
                    totalCachedItems = totalItems,
                    lastRefreshMillis = feed.lastUpdatedMillis,
                    isStale = cachedSections.isNotEmpty(),
                )
                _uiState.update { it.copy(cacheSummary = summary) }
            }
        }
    }
    fun onEvent(event: PersonalizationSettingsEvent) {
        when (event) {
            is PersonalizationSettingsEvent.SetPersonalizationEnabled -> setPersonalizationEnabled(event.enabled)
            is PersonalizationSettingsEvent.SetUseLocalHistory -> setUseLocalHistory(event.enabled)
            is PersonalizationSettingsEvent.SetUseRecentSearches -> setUseRecentSearches(event.enabled)
            is PersonalizationSettingsEvent.SetAccountSectionsEnabled -> setAccountSectionsEnabled(event.enabled)
            is PersonalizationSettingsEvent.SetSubscriptionsEnabled -> setSubscriptionsEnabled(event.enabled)
            is PersonalizationSettingsEvent.SetLikedMusicEnabled -> setLikedMusicEnabled(event.enabled)
            is PersonalizationSettingsEvent.SetLocalMixEnabled -> setLocalMixEnabled(event.enabled)
            is PersonalizationSettingsEvent.SetTrendingEnabled -> setTrendingEnabled(event.enabled)
            is PersonalizationSettingsEvent.SetHideNonMusic -> setHideNonMusic(event.enabled)
            is PersonalizationSettingsEvent.SetCountryOverride -> setCountryOverride(event.region)
            is PersonalizationSettingsEvent.SetPreferredMusicLanguages -> setPreferredMusicLanguages(event.languages)
            is PersonalizationSettingsEvent.ToggleLanguage -> toggleLanguage(event.language)
            is PersonalizationSettingsEvent.SetGenreMoodPreferences -> setGenreMoodPreferences(event.genres)
            is PersonalizationSettingsEvent.ToggleGenre -> toggleGenre(event.genre)
            is PersonalizationSettingsEvent.SetArtistDiversityLimit -> setArtistDiversityLimit(event.limit)
            is PersonalizationSettingsEvent.SetRecentlySkippedCooldown -> setRecentlySkippedCooldown(event.hours)
            PersonalizationSettingsEvent.RefreshPersonalizedFeed -> refreshPersonalizedFeed()
            PersonalizationSettingsEvent.ClearPersonalizationCacheRequested -> clearPersonalizationCacheRequested()
            PersonalizationSettingsEvent.ClearPersonalizationCacheConfirmed -> clearPersonalizationCacheConfirmed()
            PersonalizationSettingsEvent.ClearLocalHistoryRequested -> clearLocalHistoryRequested()
            PersonalizationSettingsEvent.ClearLocalHistoryConfirmed -> clearLocalHistoryConfirmed()
            PersonalizationSettingsEvent.ClearHiddenContentRequested -> clearHiddenContentRequested()
            PersonalizationSettingsEvent.ClearHiddenContentConfirmed -> clearHiddenContentConfirmed()
            PersonalizationSettingsEvent.DismissConfirmation -> dismissConfirmation()
            PersonalizationSettingsEvent.DismissMessage -> dismissMessage()
        }
    }

    fun setPersonalizationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            personalizationRepository.updatePreferences { it.copy(enablePersonalization = enabled) }
            personalizationRepository.refreshFeed(forceRefresh = true)
        }
    }

    fun setUseLocalHistory(enabled: Boolean) {
        viewModelScope.launch {
            personalizationRepository.updatePreferences { it.copy(enableLocalListeningSections = enabled) }
            personalizationRepository.refreshFeed(forceRefresh = true)
        }
    }

    fun setUseRecentSearches(enabled: Boolean) {
        viewModelScope.launch {
            personalizationRepository.updatePreferences { it.copy(recentSearchInfluence = enabled) }
            personalizationRepository.refreshFeed(forceRefresh = true)
        }
    }

    fun setAccountSectionsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            personalizationRepository.updatePreferences { it.copy(enableAccountSections = enabled) }
            personalizationRepository.refreshFeed(forceRefresh = true)
        }
    }

    fun setSubscriptionsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            personalizationRepository.updatePreferences { it.copy(enableSubscriptions = enabled) }
            personalizationRepository.refreshFeed(forceRefresh = true)
        }
    }

    fun setLikedMusicEnabled(enabled: Boolean) {
        viewModelScope.launch {
            personalizationRepository.updatePreferences { it.copy(enableLikedMusic = enabled) }
            personalizationRepository.refreshFeed(forceRefresh = true)
        }
    }

    fun setLocalMixEnabled(enabled: Boolean) {
        viewModelScope.launch {
            personalizationRepository.updatePreferences { it.copy(enableLocalMix = enabled) }
            personalizationRepository.refreshFeed(forceRefresh = true)
        }
    }

    fun setTrendingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            personalizationRepository.updatePreferences { it.copy(enableTrending = enabled) }
            personalizationRepository.refreshFeed(forceRefresh = true)
        }
    }

    fun setHideNonMusic(enabled: Boolean) {
        viewModelScope.launch {
            personalizationRepository.updatePreferences { it.copy(hideNonMusicContent = enabled) }
            personalizationRepository.refreshFeed(forceRefresh = true)
        }
    }

    fun setCountryOverride(region: String) {
        viewModelScope.launch {
            personalizationRepository.updatePreferences { it.copy(trendingRegion = region) }
            personalizationRepository.refreshFeed(forceRefresh = true)
        }
    }

    fun setPreferredMusicLanguages(languages: Set<String>) {
        viewModelScope.launch {
            tasteProfileRepository.updatePreferredLanguages(languages)
            personalizationRepository.refreshFeed(forceRefresh = true)
        }
    }

    fun toggleLanguage(language: String) {
        val current = _uiState.value.preferredLanguages
        val updated = if (language in current) current - language else current + language
        setPreferredMusicLanguages(updated)
    }

    fun setGenreMoodPreferences(genres: Set<String>) {
        viewModelScope.launch {
            tasteProfileRepository.updatePreferredGenres(genres)
            personalizationRepository.refreshFeed(forceRefresh = true)
        }
    }

    fun toggleGenre(genre: String) {
        val current = _uiState.value.preferredGenres
        val updated = if (genre in current) current - genre else current + genre
        setGenreMoodPreferences(updated)
    }

    fun setArtistDiversityLimit(limit: Int) {
        viewModelScope.launch {
            personalizationRepository.updatePreferences { it.copy(maxRepeatedArtistPerSection = limit.coerceIn(1, 5)) }
            personalizationRepository.refreshFeed(forceRefresh = true)
        }
    }

    fun setRecentlySkippedCooldown(hours: Int) {
        viewModelScope.launch {
            personalizationRepository.updatePreferences { it.copy(recentlySkippedCooldownHours = hours.coerceIn(1, 720)) }
            personalizationRepository.refreshFeed(forceRefresh = true)
        }
    }

    fun refreshPersonalizedFeed() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            try {
                personalizationRepository.refreshFeed(forceRefresh = true)
                _uiState.update { it.copy(message = "Personalized feed refreshed") }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to refresh feed", e)
                _uiState.update { it.copy(message = "Failed to refresh feed") }
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun clearPersonalizationCacheRequested() {
        _uiState.update {
            it.copy(
                activeConfirmation = ConfirmationDialogState(
                    title = "Clear Personalized Cache",
                    message = "This removes cached personalized sections for the current account on this device. Your local queue and playback are not affected.",
                    confirmLabel = "Clear Cache",
                    isDestructive = true,
                    onConfirm = { clearPersonalizationCacheConfirmed() },
                )
            )
        }
    }

    fun clearPersonalizationCacheConfirmed() {
        _uiState.update { it.copy(activeConfirmation = null, isClearingCache = true) }
        viewModelScope.launch {
            try {
                personalizationRepository.clearPersonalizationCache()
                personalizationRepository.refreshFeed(forceRefresh = true)
                _uiState.update { it.copy(message = "Personalized cache cleared") }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear cache", e)
                _uiState.update { it.copy(message = "Could not clear cache") }
            } finally {
                _uiState.update { it.copy(isClearingCache = false) }
            }
        }
    }

    fun clearLocalHistoryRequested() {
        _uiState.update {
            it.copy(
                activeConfirmation = ConfirmationDialogState(
                    title = "Clear Local Listening History",
                    message = "This removes locally stored listening and resume history. It may reduce the quality of on-device recommendations. Your account data and queue are not changed.",
                    confirmLabel = "Clear History",
                    isDestructive = true,
                    onConfirm = { clearLocalHistoryConfirmed() },
                )
            )
        }
    }

    fun clearLocalHistoryConfirmed() {
        _uiState.update { it.copy(activeConfirmation = null, isClearingHistory = true) }
        viewModelScope.launch {
            try {
                personalizationRepository.clearLocalHistory()
                personalizationRepository.refreshFeed(forceRefresh = true)
                _uiState.update { it.copy(message = "Listening history cleared") }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear local history", e)
                _uiState.update { it.copy(message = "Could not clear history") }
            } finally {
                _uiState.update { it.copy(isClearingHistory = false) }
            }
        }
    }

    fun clearHiddenContentRequested() {
        _uiState.update {
            it.copy(
                activeConfirmation = ConfirmationDialogState(
                    title = "Restore All Hidden Content",
                    message = "This restores all songs and artists you hid locally. Nothing is changed in your connected account.",
                    confirmLabel = "Restore All",
                    isDestructive = false,
                    onConfirm = { clearHiddenContentConfirmed() },
                )
            )
        }
    }

    fun clearHiddenContentConfirmed() {
        _uiState.update { it.copy(activeConfirmation = null, isClearingHidden = true) }
        viewModelScope.launch {
            try {
                personalizationRepository.resetAllHiddenContent()
                personalizationRepository.refreshFeed(forceRefresh = true)
                _uiState.update { it.copy(message = "All hidden content restored") }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reset hidden content", e)
                _uiState.update { it.copy(message = "Could not restore hidden content") }
            } finally {
                _uiState.update { it.copy(isClearingHidden = false) }
            }
        }
    }

    fun dismissConfirmation() {
        _uiState.update { it.copy(activeConfirmation = null) }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }
}