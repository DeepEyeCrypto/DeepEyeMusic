package com.deepeye.musicpro.dsp.engine

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.util.Log
import com.deepeye.musicpro.dsp.model.DSPPreset
import com.deepeye.musicpro.dsp.model.DSPPresets
import com.deepeye.musicpro.dsp.model.DSPSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DSPEngine @Inject constructor() {

    private val TAG = "DSPEngine"

    private var audioSessionId: Int = 0

    // 1. Equalizer — 10 band parametric
    private var equalizer: Equalizer? = null

    // 2. Bass Boost
    private var bassBoost: BassBoost? = null

    // 3. Virtualizer — 3D surround
    private var virtualizer: Virtualizer? = null

    // 4. Loudness Enhancer
    private var loudnessEnhancer: LoudnessEnhancer? = null

    // 5. PresetReverb — room effect
    private var reverb: PresetReverb? = null

    private val _isEnabled = MutableStateFlow(false)
    val isEnabled = _isEnabled.asStateFlow()

    private val _currentPreset = MutableStateFlow(DSPPreset.PREMIUM_BASS)
    val currentPreset = _currentPreset.asStateFlow()

    fun attachSession(sessionId: Int) {
        if (sessionId == 0 || this.audioSessionId == sessionId) return
        Log.d(TAG, "Attaching DSP Engine to Session: $sessionId")
        
        releaseSession()
        this.audioSessionId = sessionId

        try {
            // Priority 0, audioSessionId
            equalizer = Equalizer(0, sessionId).apply { enabled = _isEnabled.value }
            bassBoost = BassBoost(0, sessionId).apply { enabled = _isEnabled.value }
            virtualizer = Virtualizer(0, sessionId).apply { enabled = _isEnabled.value }
            loudnessEnhancer = LoudnessEnhancer(sessionId).apply { enabled = _isEnabled.value }
            reverb = PresetReverb(0, sessionId).apply { enabled = false } // Explicitly managed by preset

            // Reapply current preset to new session
            applyPreset(_currentPreset.value)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize DSP effects", e)
            _isEnabled.value = false
        }
    }

    fun releaseSession() {
        Log.d(TAG, "Releasing DSP Engine Session: $audioSessionId")
        equalizer?.release()
        bassBoost?.release()
        virtualizer?.release()
        loudnessEnhancer?.release()
        reverb?.release()

        equalizer = null
        bassBoost = null
        virtualizer = null
        loudnessEnhancer = null
        reverb = null
        audioSessionId = 0
    }

    fun applyPreset(preset: DSPPreset) {
        val settings = DSPPresets.fromPreset(preset)
        applySettings(settings)
        _currentPreset.value = preset
    }

    fun applySettings(settings: DSPSettings) {
        // Apply EQ bands safely
        equalizer?.let { eq ->
            try {
                val numBands = eq.numberOfBands.toInt()
                settings.eqBands.take(numBands).forEachIndexed { i, value ->
                    eq.setBandLevel(i.toShort(), value.toShort())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to apply EQ", e)
            }
        }

        // Apply Bass Boost
        bassBoost?.let { bb ->
            try {
                if (bb.strengthSupported) {
                    bb.setStrength(settings.bassStrength)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to apply BassBoost", e)
            }
        }

        // Apply Virtualizer
        virtualizer?.let { virt ->
            try {
                if (virt.strengthSupported) {
                    virt.setStrength(settings.virtualizerStrength)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to apply Virtualizer", e)
            }
        }

        // Apply Loudness Enhancer (convert Float dB to milliBel, max safe 600mB)
        loudnessEnhancer?.let { le ->
            try {
                val targetGainMb = (settings.loudnessGain * 100).toInt().coerceAtMost(600)
                le.setTargetGain(targetGainMb)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to apply LoudnessEnhancer", e)
            }
        }

        // Apply Reverb
        reverb?.let { rv ->
            try {
                if (settings.reverbPreset != PresetReverb.PRESET_NONE) {
                    rv.preset = settings.reverbPreset
                    rv.enabled = _isEnabled.value // Only enable if global DSP is enabled
                } else {
                    rv.enabled = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to apply Reverb", e)
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        _isEnabled.value = enabled
        equalizer?.enabled = enabled
        bassBoost?.enabled = enabled
        virtualizer?.enabled = enabled
        loudnessEnhancer?.enabled = enabled
        
        // Reverb is special, it should only be enabled if DSP is on AND preset is not NONE
        reverb?.let { rv ->
            val isReverbPresetActive = rv.preset != PresetReverb.PRESET_NONE
            rv.enabled = enabled && isReverbPresetActive
        }
    }

    fun setBassBoost(strength: Short) {
        bassBoost?.let {
            if (it.strengthSupported) it.setStrength(strength)
        }
    }

    fun setVirtualizer(strength: Short) {
        virtualizer?.let {
            if (it.strengthSupported) it.setStrength(strength)
        }
    }

    fun setBandLevel(band: Short, levelMilliBel: Short) {
        equalizer?.setBandLevel(band, levelMilliBel)
        _currentPreset.value = DSPPreset.CUSTOM
    }

    fun setCustomEqBands(bands: IntArray) {
        equalizer?.let { eq ->
            val numBands = eq.numberOfBands.toInt()
            bands.take(numBands).forEachIndexed { i, value ->
                eq.setBandLevel(i.toShort(), value.toShort())
            }
        }
        _currentPreset.value = DSPPreset.CUSTOM
    }
}
