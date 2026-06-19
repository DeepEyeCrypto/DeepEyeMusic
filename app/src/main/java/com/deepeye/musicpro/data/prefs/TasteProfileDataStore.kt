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
import kotlinx.coroutines.flow.first
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
        private val KEY_RECENT_SEARCHES = stringPreferencesKey("recent_searches")
    }

    private val gson = Gson()

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
                recentSearches = prefs[KEY_RECENT_SEARCHES]?.let { 
                    try {
                        val type = object : TypeToken<List<String>>() {}.type
                        gson.fromJson(it, type)
                    } catch (e: Exception) {
                        emptyList()
                    }
                } ?: emptyList(),
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

    suspend fun setRecentSearches(searches: List<String>) {
        context.tasteDataStore.edit { it[KEY_RECENT_SEARCHES] = gson.toJson(searches) }
    }

    suspend fun exportToJson(): String {
        val currentProfile = tasteProfile.first()
        return gson.toJson(currentProfile)
    }

    suspend fun importFromJson(json: String) {
        try {
            val importedProfile = gson.fromJson(json, TasteProfile::class.java)
            if (importedProfile != null) {
                context.tasteDataStore.edit { prefs ->
                    prefs[KEY_ONBOARDING_COMPLETED] = importedProfile.hasCompletedOnboarding
                    prefs[KEY_PREFERRED_LANGUAGES] = importedProfile.preferredLanguages
                    prefs[KEY_FAVORITE_ARTISTS] = importedProfile.favoriteArtists
                    prefs[KEY_PREFERRED_GENRES] = importedProfile.preferredGenres
                    prefs[KEY_PREFERRED_MOOD] = importedProfile.preferredMood
                    prefs[KEY_AUTOPLAY_ENABLED] = importedProfile.autoplayEnabled
                    prefs[KEY_PERSONALIZED_MIX_ENABLED] = importedProfile.personalizedMixEnabled
                    prefs[KEY_RECENT_SEARCHES] = gson.toJson(importedProfile.recentSearches)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("TasteProfileDataStore", "Failed to import from JSON", e)
        }
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
    val recentSearches: List<String> = emptyList(),
)
