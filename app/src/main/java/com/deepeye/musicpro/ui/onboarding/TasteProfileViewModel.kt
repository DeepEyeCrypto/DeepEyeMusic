// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.musicpro.data.prefs.TasteProfile
import com.deepeye.musicpro.di.ApplicationScope
import com.deepeye.musicpro.domain.model.Artist
import com.deepeye.musicpro.domain.repository.MusicRepository
import com.deepeye.musicpro.domain.repository.TasteProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TasteProfileUiState(
    val tasteProfile: TasteProfile = TasteProfile(),
    val localArtists: List<Artist> = emptyList(),
    val curatedArtists: List<String> =
        listOf(
            "Arijit Singh", "Shreya Ghoshal", "AP Dhillon", "Diljit Dosanjh", "Badshah",
            "Atif Aslam", "Lata Mangeshkar", "Sidhu Moose Wala", "Kishore Kumar", "Neha Kakkar",
            "Karan Aujla", "Yo Yo Honey Singh", "Divine", "Naezy", "Kr\$na", "Emiway Bantai",
            "Raftaar", "MC Stan", "King", "Guru Randhawa", "Harrdy Sandhu", "Jubin Nautiyal",
            "B Praak", "Darshan Raval", "Armaan Malik", "Sonu Nigam", "Udit Narayan", 
            "Alka Yagnik", "Kumar Sanu", "A.R. Rahman", "Mika Singh", "Sunidhi Chauhan",
            "Mohit Chauhan", "KK", "Shaan", "Pritam", "Amit Trivedi", "Vishal-Shekhar",
            "Shankar Mahadevan", "Kailash Kher", "Rahat Fateh Ali Khan", "Nusrat Fateh Ali Khan",
            "Anirudh Ravichander", "S. P. Balasubrahmanyam", "Hariharan", "Asha Bhosle",
            "Mohammed Rafi", "Mukesh", "Jagjit Singh", "Papon", "Jasleen Royal", "Neeti Mohan",
            "Monali Thakur", "Jonita Gandhi", "Sachet Tandon", "Parampara Tandon", "Jass Manak",
            "Dino James", "Seedhe Maut", "Fotty Seven", "Bali", "Ikka",
            "Taylor Swift", "Ed Sheeran", "The Weeknd", "Drake", "Justin Bieber", "Post Malone",
        ),
)

@HiltViewModel
class TasteProfileViewModel
@Inject
constructor(
    private val tasteProfileRepository: TasteProfileRepository,
    private val musicRepository: MusicRepository,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TasteProfileUiState())
    val uiState: StateFlow<TasteProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                tasteProfileRepository.getTasteProfile(),
                musicRepository.getAllArtists(),
            ) { profile, localArtists ->
                TasteProfileUiState(
                    tasteProfile = profile,
                    localArtists = localArtists,
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

    /**
     * Persists the "onboarding completed" flag.
     *
     * Runs on [applicationScope], NOT `viewModelScope`: the caller navigates to
     * Home with `popUpTo(Onboarding) { inclusive = true }`, which destroys the
     * back-stack entry and clears this ViewModel. A `viewModelScope` launch
     * would be cancelled mid-write, and the user would be trapped in the
     * onboarding loop on next launch. The previous `GlobalScope` avoided that
     * but leaked an unowned, never-cancelled scope and swallowed failures.
     */
    fun completeOnboarding() {
        applicationScope.launch {
            try {
                tasteProfileRepository.updateOnboardingCompleted(true)
                android.util.Log.i(TAG, "onboarding completed flag persisted result=success")
            } catch (e: CancellationException) {
                // Only reachable if the process itself dies; rethrow to keep
                // structured-concurrency semantics intact.
                throw e
            } catch (e: Exception) {
                android.util.Log.e(
                    TAG,
                    "failed to persist onboarding completion; user may re-enter onboarding",
                    e,
                )
            }
        }
    }

    private companion object {
        const val TAG = "TasteProfileViewModel"
    }
}
