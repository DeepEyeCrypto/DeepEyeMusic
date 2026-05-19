package com.deepeye.musicpro.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.tasteDataStore: DataStore<Preferences> by preferencesDataStore(name = "deepeye_taste_profile")

@Singleton
class TasteProfileDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val KEY_PREFERRED_LANGUAGES = stringSetPreferencesKey("preferred_languages")
        private val KEY_FAVORITE_ARTISTS = stringSetPreferencesKey("favorite_artists")
    }

    val tasteProfile: Flow<TasteProfile> = context.tasteDataStore.data.map { prefs ->
        TasteProfile(
            hasCompletedOnboarding = prefs[KEY_ONBOARDING_COMPLETED] ?: false,
            preferredLanguages = prefs[KEY_PREFERRED_LANGUAGES] ?: emptySet(),
            favoriteArtists = prefs[KEY_FAVORITE_ARTISTS] ?: emptySet()
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
}

data class TasteProfile(
    val hasCompletedOnboarding: Boolean = false,
    val preferredLanguages: Set<String> = emptySet(),
    val favoriteArtists: Set<String> = emptySet()
)
