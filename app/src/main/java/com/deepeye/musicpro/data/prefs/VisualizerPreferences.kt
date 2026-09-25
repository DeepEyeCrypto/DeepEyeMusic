// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.deepeye.musicpro.ui.player.visualizer.VisualizerSceneId
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

val Context.visualizerDataStore: DataStore<Preferences> by preferencesDataStore(name = "visualizer_settings")

data class VisualizerPrefs(
    val sceneId: VisualizerSceneId = VisualizerSceneId.TRIANGLE_REACTIVE,
    val intensity: Float = 1.0f,
    val reducedMotion: Boolean = false
)

/**
 * Persists visualizer scene selection and per-instance render tuning.
 *
 * Kept in its own DataStore (rather than [SettingsDataStore]) because these are
 * high-frequency render parameters, not account/exportable app settings.
 */
@Singleton
class VisualizerPreferences @Inject
constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val MIN_INTENSITY = 0.5f
        const val MAX_INTENSITY = 1.5f

        private val KEY_SCENE = stringPreferencesKey("visualizer_scene")
        private val KEY_INTENSITY = floatPreferencesKey("visualizer_intensity")
        private val KEY_REDUCED_MOTION = booleanPreferencesKey("visualizer_reduced_motion")
    }

    val prefs: Flow<VisualizerPrefs> = context.visualizerDataStore.data
        // A corrupt/unreadable prefs file must not take the player down with it.
        .catch { cause ->
            if (cause is IOException) emit(emptyPreferences()) else throw cause
        }
        .map { p ->
            VisualizerPrefs(
                sceneId = runCatching { VisualizerSceneId.valueOf(p[KEY_SCENE].orEmpty()) }
                    .getOrDefault(VisualizerSceneId.TRIANGLE_REACTIVE),
                // Clamp on read: a hand-edited or out-of-range value must not
                // produce a degenerate scale factor in the render loop.
                intensity = (p[KEY_INTENSITY] ?: 1.0f).coerceIn(MIN_INTENSITY, MAX_INTENSITY),
                reducedMotion = p[KEY_REDUCED_MOTION] ?: false
            )
        }

    suspend fun setScene(id: VisualizerSceneId) {
        context.visualizerDataStore.edit { it[KEY_SCENE] = id.name }
    }

    suspend fun setIntensity(value: Float) {
        context.visualizerDataStore.edit { it[KEY_INTENSITY] = value.coerceIn(MIN_INTENSITY, MAX_INTENSITY) }
    }

    suspend fun setReducedMotion(enabled: Boolean) {
        context.visualizerDataStore.edit { it[KEY_REDUCED_MOTION] = enabled }
    }
}