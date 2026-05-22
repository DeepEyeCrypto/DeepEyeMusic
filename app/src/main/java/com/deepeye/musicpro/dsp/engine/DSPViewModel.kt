package com.deepeye.musicpro.dsp.engine

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.musicpro.data.prefs.DSPKeys
import com.deepeye.musicpro.data.prefs.dspDataStore
import com.deepeye.musicpro.dsp.model.DSPPreset
import com.deepeye.musicpro.dsp.model.DSPPresets
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.datastore.preferences.core.edit

@HiltViewModel
class DSPViewModel @Inject constructor(
    private val application: Application,
    val dspEngine: DSPEngine
) : ViewModel() {

    private val dataStore = application.dspDataStore

    // States
    private val _currentPreset = MutableStateFlow(DSPPreset.PREMIUM_BASS)
    val currentPreset = _currentPreset.asStateFlow()

    private val _isEnabled = MutableStateFlow(false)
    val isEnabled = _isEnabled.asStateFlow()

    private val _eqBands = MutableStateFlow(DSPPresets.PREMIUM_BASS.eqBands)
    val eqBands = _eqBands.asStateFlow()

    private val _bassStrength = MutableStateFlow(DSPPresets.PREMIUM_BASS.bassStrength.toInt())
    val bassStrength = _bassStrength.asStateFlow()

    private val _virtualizerStrength = MutableStateFlow(DSPPresets.PREMIUM_BASS.virtualizerStrength.toInt())
    val virtualizerStrength = _virtualizerStrength.asStateFlow()

    init {
        // Load initial state from DataStore
        viewModelScope.launch {
            val prefs = dataStore.data.first()
            
            val enabled = prefs[DSPKeys.ENABLED] ?: false
            val presetName = prefs[DSPKeys.PRESET] ?: DSPPreset.PREMIUM_BASS.name
            val savedPreset = runCatching { DSPPreset.valueOf(presetName) }.getOrDefault(DSPPreset.PREMIUM_BASS)
            
            _isEnabled.value = enabled
            dspEngine.setEnabled(enabled)

            if (savedPreset == DSPPreset.CUSTOM) {
                // Load custom bands
                val bandsString = prefs[DSPKeys.CUSTOM_BANDS] ?: ""
                val customBands = if (bandsString.isNotBlank()) {
                    bandsString.split(",").mapNotNull { it.toIntOrNull() }.toIntArray()
                } else {
                    DSPPresets.FLAT.eqBands
                }
                
                val bass = prefs[DSPKeys.BASS_STRENGTH] ?: 0
                val virt = prefs[DSPKeys.VIRTUALIZER] ?: 0

                _eqBands.value = customBands
                _bassStrength.value = bass
                _virtualizerStrength.value = virt
                _currentPreset.value = DSPPreset.CUSTOM

                dspEngine.setCustomEqBands(customBands)
                dspEngine.setBassBoost(bass.toShort())
                dspEngine.setVirtualizer(virt.toShort())
            } else {
                setPreset(savedPreset)
            }
        }
    }

    fun setPreset(preset: DSPPreset) {
        if (preset == DSPPreset.CUSTOM) return // Custom is set indirectly by modifying bands

        dspEngine.applyPreset(preset)
        _currentPreset.value = preset
        
        val settings = DSPPresets.fromPreset(preset)
        _eqBands.value = settings.eqBands
        _bassStrength.value = settings.bassStrength.toInt()
        _virtualizerStrength.value = settings.virtualizerStrength.toInt()

        saveState()
    }

    fun setEnabled(enabled: Boolean) {
        dspEngine.setEnabled(enabled)
        _isEnabled.value = enabled
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[DSPKeys.ENABLED] = enabled
            }
        }
    }

    fun setBassStrength(strength: Int) {
        dspEngine.setBassBoost(strength.toShort())
        _bassStrength.value = strength
        markAsCustomAndSave()
    }

    fun setVirtualizer(strength: Int) {
        dspEngine.setVirtualizer(strength.toShort())
        _virtualizerStrength.value = strength
        markAsCustomAndSave()
    }

    fun setBandLevel(band: Int, level: Int) {
        dspEngine.setBandLevel(band.toShort(), level.toShort())
        
        val newBands = _eqBands.value.clone()
        if (band in newBands.indices) {
            newBands[band] = level
            _eqBands.value = newBands
        }
        
        markAsCustomAndSave()
    }

    private fun markAsCustomAndSave() {
        if (_currentPreset.value != DSPPreset.CUSTOM) {
            _currentPreset.value = DSPPreset.CUSTOM
        }
        saveState()
    }

    private fun saveState() {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[DSPKeys.PRESET] = _currentPreset.value.name
                if (_currentPreset.value == DSPPreset.CUSTOM) {
                    prefs[DSPKeys.BASS_STRENGTH] = _bassStrength.value
                    prefs[DSPKeys.VIRTUALIZER] = _virtualizerStrength.value
                    prefs[DSPKeys.CUSTOM_BANDS] = _eqBands.value.joinToString(",")
                }
            }
        }
    }
}
