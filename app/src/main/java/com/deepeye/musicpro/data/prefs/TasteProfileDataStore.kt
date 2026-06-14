// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.tasteDataStore: DataStore<Preferences> by preferencesDataStore(name = "deepeye_taste_profile")

@Singleton
class TasteProfileDataStore
@Inject
constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val KEY_PREFERRED_LANGUAGES = stringSetPreferencesKey("preferred_languages")
        private val KEY_FAVORITE_ARTISTS = stringSetPreferencesKey("favorite_artists")
        private val KEY_PREFERRED_GENRES = stringSetPreferencesKey("preferred_genres")
        private val KEY_PREFERRED_MOOD = stringPreferencesKey("preferred_mood")
        private val KEY_AUTOPLAY_ENABLED = booleanPreferencesKey("autoplay_enabled")
        private val KEY_PERSONALIZED_MIX_ENABLED = booleanPreferencesKey("personalized_mix_enabled")
    }

    val tasteProfile: Flow<TasteProfile> =
        context.tasteDataStore.data.map { prefs ->
            TasteProfile(
                hasCompletedOnboarding = prefs[KEY_ONBOARDING_COMPLETED] ?: false,
                preferredLanguages = prefs[KEY_PREFERRED_LANGUAGES] ?: emptySet(),
                favoriteArtists = prefs[KEY_FAVORITE_ARTISTS] ?: emptySet(),
                preferredGenres = prefs[KEY_PREFERRED_GENRES] ?: emptySet(),
                preferredMood = prefs[KEY_PREFERRED_MOOD] ?: "Balanced",
                autoplayEnabled = prefs[KEY_AUTOPLAY_ENABLED] ?: true,
                personalizedMixEnabled = prefs[KEY_PERSONALIZED_MIX_ENABLED] ?: true,
            )
        }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.tasteDataStore.edit { it[KEY_ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setPreferredLanguages(languages: Set<String>) {
        context.tasteDataStore.edit { it[KEY_PREFERRED_LANGUAGES] = languages }
    }

    suspend fun setFavoriteArtists(artists: Set<String>) {
        context.tasteDataStore.edit { it[KEY_FAVORITE_ARTISTS] = artists }
    }

    suspend fun setPreferredGenres(genres: Set<String>) {
        context.tasteDataStore.edit { it[KEY_PREFERRED_GENRES] = genres }
    }

    suspend fun setPreferredMood(mood: String) {
        context.tasteDataStore.edit { it[KEY_PREFERRED_MOOD] = mood }
    }

    suspend fun setAutoplayEnabled(enabled: Boolean) {
        context.tasteDataStore.edit { it[KEY_AUTOPLAY_ENABLED] = enabled }
    }

    suspend fun setPersonalizedMixEnabled(enabled: Boolean) {
        context.tasteDataStore.edit { it[KEY_PERSONALIZED_MIX_ENABLED] = enabled }
    }
}

data class TasteProfile(
    val hasCompletedOnboarding: Boolean = false,
    val preferredLanguages: Set<String> = emptySet(),
    val favoriteArtists: Set<String> = emptySet(),
    val preferredGenres: Set<String> = emptySet(),
    val preferredMood: String = "Balanced",
    val autoplayEnabled: Boolean = true,
    val personalizedMixEnabled: Boolean = true,
)
