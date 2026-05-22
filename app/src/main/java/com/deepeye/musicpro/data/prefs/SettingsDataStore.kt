// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "deepeye_settings")

enum class ThemeMode { DARK, LIGHT, SYSTEM }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val crossfadeDuration: Int = 0,       // seconds, 0 = disabled
    val sampleRate: Int = 44100,
    val bitDepth: Int = 16,
    val autoScanOnLaunch: Boolean = true,
    val showVisualizer: Boolean = true
)

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_THEME = stringPreferencesKey("theme_mode")
        private val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        private val KEY_CROSSFADE = intPreferencesKey("crossfade_duration")
        private val KEY_SAMPLE_RATE = intPreferencesKey("sample_rate")
        private val KEY_BIT_DEPTH = intPreferencesKey("bit_depth")
        private val KEY_AUTO_SCAN = booleanPreferencesKey("auto_scan")
        private val KEY_SHOW_VISUALIZER = booleanPreferencesKey("show_visualizer")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            themeMode = try { ThemeMode.valueOf(prefs[KEY_THEME] ?: "SYSTEM") } catch (_: Exception) { ThemeMode.SYSTEM },
            dynamicColor = prefs[KEY_DYNAMIC_COLOR] ?: true,
            crossfadeDuration = prefs[KEY_CROSSFADE] ?: 0,
            sampleRate = prefs[KEY_SAMPLE_RATE] ?: 44100,
            bitDepth = prefs[KEY_BIT_DEPTH] ?: 16,
            autoScanOnLaunch = prefs[KEY_AUTO_SCAN] ?: true,
            showVisualizer = prefs[KEY_SHOW_VISUALIZER] ?: true
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) { context.dataStore.edit { it[KEY_THEME] = mode.name } }
    suspend fun setDynamicColor(enabled: Boolean) { context.dataStore.edit { it[KEY_DYNAMIC_COLOR] = enabled } }
    suspend fun setCrossfadeDuration(seconds: Int) { context.dataStore.edit { it[KEY_CROSSFADE] = seconds } }
    suspend fun setSampleRate(rate: Int) { context.dataStore.edit { it[KEY_SAMPLE_RATE] = rate } }
    suspend fun setBitDepth(depth: Int) { context.dataStore.edit { it[KEY_BIT_DEPTH] = depth } }
    suspend fun setAutoScan(enabled: Boolean) { context.dataStore.edit { it[KEY_AUTO_SCAN] = enabled } }
    suspend fun setShowVisualizer(enabled: Boolean) { context.dataStore.edit { it[KEY_SHOW_VISUALIZER] = enabled } }
}
