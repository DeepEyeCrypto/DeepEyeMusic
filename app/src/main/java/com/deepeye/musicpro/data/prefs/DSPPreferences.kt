package com.deepeye.musicpro.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.dspDataStore: DataStore<Preferences> by preferencesDataStore(name = "dsp_settings")

object DSPKeys {
    val PRESET = stringPreferencesKey("dsp_preset")
    val ENABLED = booleanPreferencesKey("dsp_enabled")
    val BASS_STRENGTH = intPreferencesKey("bass_strength")
    val VIRTUALIZER = intPreferencesKey("virtualizer_strength")
    val CUSTOM_BANDS = stringPreferencesKey("custom_bands") // Comma separated integers
}
