package com.deepeye.musicpro.updates

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.updatePrefsDataStore by preferencesDataStore(name = "update_prefs")

/**
 * Manages persistence of the last-shown changelog version.
 * Uses DataStore for async, non-blocking reads.
 */
@Singleton
class UpdatePrefsManager
@Inject
constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val LAST_SHOWN_VERSION = intPreferencesKey("last_shown_version")
    }

    val lastShownVersionFlow: Flow<Int> =
        context.updatePrefsDataStore.data.map { prefs ->
            prefs[Keys.LAST_SHOWN_VERSION] ?: 0
        }

    suspend fun getLastShownVersion(): Int = context.updatePrefsDataStore.data.first()[Keys.LAST_SHOWN_VERSION] ?: 0

    suspend fun markVersionShown(versionCode: Int) {
        context.updatePrefsDataStore.edit { prefs ->
            prefs[Keys.LAST_SHOWN_VERSION] = versionCode
        }
    }
}
