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
        private val KEY_ENABLE_PERSONALIZATION = booleanPreferencesKey("enable_personalization")
        private val KEY_ENABLE_ACCOUNT_SECTIONS = booleanPreferencesKey("enable_account_sections")
        private val KEY_ENABLE_LOCAL_LISTENING = booleanPreferencesKey("enable_local_listening_sections")
        private val KEY_RECENT_SEARCH_INFLUENCE = booleanPreferencesKey("recent_search_influence")
        private val KEY_ENABLE_LIKED_MUSIC = booleanPreferencesKey("enable_liked_music")
        private val KEY_ENABLE_SUBSCRIPTIONS = booleanPreferencesKey("enable_subscriptions")
        private val KEY_ENABLE_LOCAL_MIX = booleanPreferencesKey("enable_local_mix")
        private val KEY_ENABLE_TRENDING = booleanPreferencesKey("enable_trending")
        private val KEY_TRENDING_REGION = stringPreferencesKey("trending_region")
        private val KEY_HIDE_NON_MUSIC = booleanPreferencesKey("hide_non_music_content")
        private val KEY_DISCOVERY_BLEND = intPreferencesKey("discovery_blend_count")
        private val KEY_MAX_REPEATED_ARTIST = intPreferencesKey("max_repeated_artist_per_section")
        private val KEY_SKIPPED_COOLDOWN_HOURS = intPreferencesKey("recently_skipped_cooldown_hours")
        private val KEY_LAST_REFRESH = longPreferencesKey("last_refresh_millis")
    }

    val preferences: Flow<PersonalizationPreferences> =
        context.personalizationDataStore.data.map { prefs ->
            PersonalizationPreferences(
                enablePersonalization = prefs[KEY_ENABLE_PERSONALIZATION] ?: true,
                enableAccountSections = prefs[KEY_ENABLE_ACCOUNT_SECTIONS] ?: true,
                enableLocalListeningSections = prefs[KEY_ENABLE_LOCAL_LISTENING] ?: true,
                recentSearchInfluence = prefs[KEY_RECENT_SEARCH_INFLUENCE] ?: true,
                enableLikedMusic = prefs[KEY_ENABLE_LIKED_MUSIC] ?: true,
                enableSubscriptions = prefs[KEY_ENABLE_SUBSCRIPTIONS] ?: true,
                enableLocalMix = prefs[KEY_ENABLE_LOCAL_MIX] ?: true,
                enableTrending = prefs[KEY_ENABLE_TRENDING] ?: true,
                trendingRegion = prefs[KEY_TRENDING_REGION] ?: "US",
                hideNonMusicContent = prefs[KEY_HIDE_NON_MUSIC] ?: true,
                discoveryBlendCount = prefs[KEY_DISCOVERY_BLEND] ?: 2,
                maxRepeatedArtistPerSection = prefs[KEY_MAX_REPEATED_ARTIST] ?: 2,
                recentlySkippedCooldownHours = prefs[KEY_SKIPPED_COOLDOWN_HOURS] ?: 24,
                lastRefreshMillis = prefs[KEY_LAST_REFRESH] ?: 0L,
            )
        }

    suspend fun setEnablePersonalization(enabled: Boolean) {
        context.personalizationDataStore.edit { it[KEY_ENABLE_PERSONALIZATION] = enabled }
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

    suspend fun setEnableLikedMusic(enabled: Boolean) {
        context.personalizationDataStore.edit { it[KEY_ENABLE_LIKED_MUSIC] = enabled }
    }

    suspend fun setEnableSubscriptions(enabled: Boolean) {
        context.personalizationDataStore.edit { it[KEY_ENABLE_SUBSCRIPTIONS] = enabled }
    }

    suspend fun setEnableLocalMix(enabled: Boolean) {
        context.personalizationDataStore.edit { it[KEY_ENABLE_LOCAL_MIX] = enabled }
    }

    suspend fun setEnableTrending(enabled: Boolean) {
        context.personalizationDataStore.edit { it[KEY_ENABLE_TRENDING] = enabled }
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

    suspend fun setRecentlySkippedCooldownHours(hours: Int) {
        context.personalizationDataStore.edit { it[KEY_SKIPPED_COOLDOWN_HOURS] = hours.coerceIn(1, 720) }
    }

    suspend fun setLastRefreshMillis(millis: Long) {
        context.personalizationDataStore.edit { it[KEY_LAST_REFRESH] = millis }
    }

    // ── PersonalizationPreferenceStore interface ──
    override fun observe(): Flow<PersonalizationPreferences> = preferences

    override suspend fun current(): PersonalizationPreferences = preferences.first()

    override suspend fun update(transform: (PersonalizationPreferences) -> PersonalizationPreferences) {
        val curr = current()
        val updated = transform(curr)
        if (updated.enablePersonalization != curr.enablePersonalization)
            setEnablePersonalization(updated.enablePersonalization)
        if (updated.enableAccountSections != curr.enableAccountSections)
            setEnableAccountSections(updated.enableAccountSections)
        if (updated.enableLocalListeningSections != curr.enableLocalListeningSections)
            setEnableLocalListeningSections(updated.enableLocalListeningSections)
        if (updated.recentSearchInfluence != curr.recentSearchInfluence)
            setRecentSearchInfluence(updated.recentSearchInfluence)
        if (updated.enableLikedMusic != curr.enableLikedMusic)
            setEnableLikedMusic(updated.enableLikedMusic)
        if (updated.enableSubscriptions != curr.enableSubscriptions)
            setEnableSubscriptions(updated.enableSubscriptions)
        if (updated.enableLocalMix != curr.enableLocalMix)
            setEnableLocalMix(updated.enableLocalMix)
        if (updated.enableTrending != curr.enableTrending)
            setEnableTrending(updated.enableTrending)
        if (updated.trendingRegion != curr.trendingRegion)
            setTrendingRegion(updated.trendingRegion)
        if (updated.hideNonMusicContent != curr.hideNonMusicContent)
            setHideNonMusicContent(updated.hideNonMusicContent)
        if (updated.discoveryBlendCount != curr.discoveryBlendCount)
            setDiscoveryBlendCount(updated.discoveryBlendCount)
        if (updated.maxRepeatedArtistPerSection != curr.maxRepeatedArtistPerSection)
            setMaxRepeatedArtistPerSection(updated.maxRepeatedArtistPerSection)
        if (updated.recentlySkippedCooldownHours != curr.recentlySkippedCooldownHours)
            setRecentlySkippedCooldownHours(updated.recentlySkippedCooldownHours)
        if (updated.lastRefreshMillis != curr.lastRefreshMillis)
            setLastRefreshMillis(updated.lastRefreshMillis)
    }
}