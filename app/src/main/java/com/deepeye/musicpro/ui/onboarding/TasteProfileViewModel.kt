// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.musicpro.data.prefs.TasteProfile
import com.deepeye.musicpro.domain.model.Artist
import com.deepeye.musicpro.domain.repository.MusicRepository
import com.deepeye.musicpro.domain.repository.TasteProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TasteProfileUiState(
    val tasteProfile: TasteProfile = TasteProfile(),
    val localArtists: List<Artist> = emptyList(),
    val curatedArtists: List<String> = listOf(
        "Arijit Singh", "Shreya Ghoshal", "AP Dhillon", "Diljit Dosanjh", "Badshah",
        "Atif Aslam", "Lata Mangeshkar", "Sidhu Moose Wala", "Kishore Kumar", "Neha Kakkar",
        "Taylor Swift", "Ed Sheeran", "The Weeknd", "Drake", "Justin Bieber"
    )
)

@HiltViewModel
class TasteProfileViewModel @Inject constructor(
    private val tasteProfileRepository: TasteProfileRepository,
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TasteProfileUiState())
    val uiState: StateFlow<TasteProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                tasteProfileRepository.getTasteProfile(),
                musicRepository.getAllArtists()
            ) { profile, localArtists ->
                TasteProfileUiState(
                    tasteProfile = profile,
                    localArtists = localArtists
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun setLanguages(languages: Set<String>) {
        viewModelScope.launch {
            tasteProfileRepository.updatePreferredLanguages(languages)
        }
    }

    fun setArtists(artists: Set<String>) {
        viewModelScope.launch {
            tasteProfileRepository.updateFavoriteArtists(artists)
        }
    }

    fun setGenres(genres: Set<String>) {
        viewModelScope.launch {
            tasteProfileRepository.updatePreferredGenres(genres)
        }
    }

    fun setMood(mood: String) {
        viewModelScope.launch {
            tasteProfileRepository.updatePreferredMood(mood)
        }
    }

    fun setAutoplay(enabled: Boolean) {
        viewModelScope.launch {
            tasteProfileRepository.updateAutoplayEnabled(enabled)
        }
    }

    fun setPersonalizedMix(enabled: Boolean) {
        viewModelScope.launch {
            tasteProfileRepository.updatePersonalizedMixEnabled(enabled)
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            tasteProfileRepository.updateOnboardingCompleted(true)
        }
    }
}
