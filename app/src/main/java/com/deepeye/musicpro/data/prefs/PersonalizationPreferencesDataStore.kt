// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.personalizationDataStore: DataStore<Preferences> by preferencesDataStore(name = "deepeye_personalization_prefs")

/**
 * Persistent store for user-facing personalization controls handled in Phase 5.
 * Backed by its own DataStore so it can evolve independently of taste/account settings.
 */
@Singleton
class PersonalizationPreferencesDataStore
@Inject
constructor(
    @ApplicationContext private val context: Context,
) : PersonalizationPreferenceStore {
    companion object {
        private val KEY_ENABLE_ACCOUNT_SECTIONS = booleanPreferencesKey("enable_account_sections")
        private val KEY_ENABLE_LOCAL_LISTENING = booleanPreferencesKey("enable_local_listening_sections")
        private val KEY_RECENT_SEARCH_INFLUENCE = booleanPreferencesKey("recent_search_influence")
        private val KEY_TRENDING_REGION = stringPreferencesKey("trending_region")
        private val KEY_HIDE_NON_MUSIC = booleanPreferencesKey("hide_non_music_content")
        private val KEY_DISCOVERY_BLEND = intPreferencesKey("discovery_blend_count")
        private val KEY_MAX_REPEATED_ARTIST = intPreferencesKey("max_repeated_artist_per_section")
        private val KEY_LAST_REFRESH = longPreferencesKey("last_refresh_millis")
    }

    val preferences: Flow<PersonalizationPreferences> =
        context.personalizationDataStore.data.map { prefs ->
            PersonalizationPreferences(
                enableAccountSections = prefs[KEY_ENABLE_ACCOUNT_SECTIONS] ?: true,
                enableLocalListeningSections = prefs[KEY_ENABLE_LOCAL_LISTENING] ?: true,
                recentSearchInfluence = prefs[KEY_RECENT_SEARCH_INFLUENCE] ?: true,
                trendingRegion = prefs[KEY_TRENDING_REGION] ?: "US",
                hideNonMusicContent = prefs[KEY_HIDE_NON_MUSIC] ?: false,
                discoveryBlendCount = prefs[KEY_DISCOVERY_BLEND] ?: 2,
                maxRepeatedArtistPerSection = prefs[KEY_MAX_REPEATED_ARTIST] ?: 2,
                lastRefreshMillis = prefs[KEY_LAST_REFRESH] ?: 0L,
            )
        }

    suspend fun setEnableAccountSections(enabled: Boolean) {
        context.personalizationDataStore.edit { it[KEY_ENABLE_ACCOUNT_SECTIONS] = enabled }
    }

    suspend fun setEnableLocalListeningSections(enabled: Boolean) {
        context.personalizationDataStore.edit { it[KEY_ENABLE_LOCAL_LISTENING] = enabled }
    }

    suspend fun setRecentSearchInfluence(enabled: Boolean) {
        context.personalizationDataStore.edit { it[KEY_RECENT_SEARCH_INFLUENCE] = enabled }
    }

    suspend fun setTrendingRegion(region: String) {
        context.personalizationDataStore.edit { it[KEY_TRENDING_REGION] = region }
    }

    suspend fun setHideNonMusicContent(enabled: Boolean) {
        context.personalizationDataStore.edit { it[KEY_HIDE_NON_MUSIC] = enabled }
    }

    suspend fun setDiscoveryBlendCount(count: Int) {
        context.personalizationDataStore.edit { it[KEY_DISCOVERY_BLEND] = count.coerceIn(0, 8) }
    }

    suspend fun setMaxRepeatedArtistPerSection(count: Int) {
        context.personalizationDataStore.edit { it[KEY_MAX_REPEATED_ARTIST] = count.coerceIn(1, 5) }
    }

    suspend fun setLastRefreshMillis(millis: Long) {
        context.personalizationDataStore.edit { it[KEY_LAST_REFRESH] = millis }
    }

    // ── PersonalizationPreferenceStore interface ──
    override fun observe(): Flow<PersonalizationPreferences> = preferences

    override suspend fun current(): PersonalizationPreferences = preferences.first()

    override suspend fun update(transform: (PersonalizationPreferences) -> PersonalizationPreferences) {
        val updated = transform(current())
        if (updated.enableAccountSections != current().enableAccountSections)
            setEnableAccountSections(updated.enableAccountSections)
        if (updated.enableLocalListeningSections != current().enableLocalListeningSections)
            setEnableLocalListeningSections(updated.enableLocalListeningSections)
        if (updated.recentSearchInfluence != current().recentSearchInfluence)
            setRecentSearchInfluence(updated.recentSearchInfluence)
        if (updated.trendingRegion != current().trendingRegion)
            setTrendingRegion(updated.trendingRegion)
        if (updated.hideNonMusicContent != current().hideNonMusicContent)
            setHideNonMusicContent(updated.hideNonMusicContent)
        if (updated.discoveryBlendCount != current().discoveryBlendCount)
            setDiscoveryBlendCount(updated.discoveryBlendCount)
        if (updated.maxRepeatedArtistPerSection != current().maxRepeatedArtistPerSection)
            setMaxRepeatedArtistPerSection(updated.maxRepeatedArtistPerSection)
    }
}