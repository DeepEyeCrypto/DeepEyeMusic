package com.deepeye.musicpro.ui.v4a

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepeye.musicpro.dsp.engine.V4AViewModel
import com.deepeye.musicpro.ui.v4a.components.DspDebugCard
import com.deepeye.musicpro.ui.v4a.components.GainBudgetCard

@Composable
fun V4AScreen(
    onNavigateBack: () -> Unit,
    viewModel: V4AViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val params = uiState.params

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // ── Top Bar ──
        item {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                Text("V4A DSP Engine", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f).padding(start = 8.dp))
                Switch(checked = params.enabled, onCheckedChange = { viewModel.toggleMasterEnabled() })
            }
        }

        // ── Phase Conflict Warning ──
        if (uiState.showConflictWarning) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f))
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, "Warning", tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text("Field Surround + Convolver may cause phase conflicts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // ── Preset Bar ──
        item {
            Text("Presets", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.presets) { (id, name) ->
                    FilterChip(
                        selected = uiState.selectedPresetId == id,
                        onClick = { viewModel.loadPreset(id) },
                        label = { Text(name) }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // ── Gain Budget Card ──
        item {
            GainBudgetCard(
                gainBudget = uiState.gainBudget,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // ── Equalizer Card ──
        item {
            DspModuleCard(title = "10-Band Equalizer", enabled = params.eqEnabled, onToggle = { viewModel.updateParams { p -> p.copy(eqEnabled = !p.eqEnabled) } }) {
                val bands = listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    params.eqBands.forEachIndexed { i, value ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(32.dp)) {
                            Text("${value.toInt()}", style = MaterialTheme.typography.labelSmall)
                            Slider(
                                value = value, onValueChange = { v -> viewModel.updateEqBand(i, v) },
                                valueRange = -12f..12f,
                                modifier = Modifier.height(100.dp).width(28.dp)
                            )
                            Text(bands[i], style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // ── Bass Boost ──
        item {
            DspModuleCard(title = "Bass Boost", enabled = params.bassBoostEnabled, onToggle = { viewModel.updateParams { p -> p.copy(bassBoostEnabled = !p.bassBoostEnabled) } }) {
                SliderRow("Strength", params.bassBoostStrength.toFloat(), 0f, 1000f) { v -> viewModel.updateParams { p -> p.copy(bassBoostStrength = v.toInt()) } }
            }
        }

        // ── Virtualizer ──
        item {
            DspModuleCard(title = "Virtualizer", enabled = params.virtualizerEnabled, onToggle = { viewModel.updateParams { p -> p.copy(virtualizerEnabled = !p.virtualizerEnabled) } }) {
                SliderRow("Strength", params.virtualizerStrength.toFloat(), 0f, 1000f) { v -> viewModel.updateParams { p -> p.copy(virtualizerStrength = v.toInt()) } }
            }
        }

        // ── Reverb ──
        item {
            DspModuleCard(title = "Reverb", enabled = params.reverbEnabled, onToggle = { viewModel.updateParams { p -> p.copy(reverbEnabled = !p.reverbEnabled) } }) {
                Text("Preset: ${params.reverbPreset.name}", style = MaterialTheme.typography.bodySmall)
            }
        }

        // ── Loudness Enhancer ──
        item {
            DspModuleCard(title = "Loudness Enhancer", enabled = params.loudnessEnabled, onToggle = { viewModel.updateParams { p -> p.copy(loudnessEnabled = !p.loudnessEnabled) } }) {
                SliderRow("Gain", params.loudnessGain, 0f, 15f) { v -> viewModel.updateParams { p -> p.copy(loudnessGain = v) } }
            }
        }

        // ── Dynamics Processing ──
        item {
            DspModuleCard(title = "Dynamics Processing", enabled = params.dynamicsEnabled, onToggle = { viewModel.updateParams { p -> p.copy(dynamicsEnabled = !p.dynamicsEnabled) } }) {
                SliderRow("Threshold", params.compressorThreshold, -60f, 0f) { v -> viewModel.updateParams { p -> p.copy(compressorThreshold = v) } }
                SliderRow("Ratio", params.compressorRatio, 1f, 20f) { v -> viewModel.updateParams { p -> p.copy(compressorRatio = v) } }
            }
        }

        // ── Field Surround ──
        item {
            DspModuleCard(title = "Field Surround", enabled = params.surroundEnabled, onToggle = { viewModel.updateParams { p -> p.copy(surroundEnabled = !p.surroundEnabled) } }) {
                SliderRow("Strength", params.surroundStrength.toFloat(), 0f, 1000f) { v -> viewModel.updateParams { p -> p.copy(surroundStrength = v.toInt()) } }
            }
        }

        // ── Convolver ──
        item {
            DspModuleCard(title = "Convolver (IRS)", enabled = params.convolverEnabled, onToggle = { viewModel.updateParams { p -> p.copy(convolverEnabled = !p.convolverEnabled) } }) {
                SliderRow("Mix", params.convolverMix, 0f, 1f) { v -> viewModel.updateParams { p -> p.copy(convolverMix = v) } }
            }
        }

        // ── Tube Simulator ──
        item {
            DspModuleCard(title = "Tube Simulator", enabled = params.tubeEnabled, onToggle = { viewModel.updateParams { p -> p.copy(tubeEnabled = !p.tubeEnabled) } }) {
                SliderRow("Drive", params.tubeDrive.toFloat(), 0f, 100f) { v -> viewModel.updateParams { p -> p.copy(tubeDrive = v.toInt()) } }
            }
        }

        // ── Clarity ──
        item {
            DspModuleCard(title = "Clarity / Exciter", enabled = params.clarityEnabled, onToggle = { viewModel.updateParams { p -> p.copy(clarityEnabled = !p.clarityEnabled) } }) {
                SliderRow("Strength", params.clarityStrength, 0f, 1f) { v -> viewModel.updateParams { p -> p.copy(clarityStrength = v) } }
            }
        }

        // ── HRTF ──
        item {
            DspModuleCard(title = "HRTF", enabled = params.hrtfEnabled, onToggle = { viewModel.updateParams { p -> p.copy(hrtfEnabled = !p.hrtfEnabled) } }) {
                Text("Preset: ${params.hrtfPreset.name}", style = MaterialTheme.typography.bodySmall)
            }
        }

        // ── Speaker Protection ──
        item {
            DspModuleCard(title = "Speaker Protection", enabled = params.speakerProtectionEnabled, onToggle = { viewModel.updateParams { p -> p.copy(speakerProtectionEnabled = !p.speakerProtectionEnabled) } }) {
                SliderRow("Max dB", params.speakerMaxDb, -12f, 0f) { v -> viewModel.updateParams { p -> p.copy(speakerMaxDb = v) } }
            }
        }

        // ── Noise Gate ──
        item {
            DspModuleCard(title = "Noise Gate", enabled = params.noiseGateEnabled, onToggle = { viewModel.updateParams { p -> p.copy(noiseGateEnabled = !p.noiseGateEnabled) } }) {
                SliderRow("Threshold", params.noiseGateThreshold, -80f, -20f) { v -> viewModel.updateParams { p -> p.copy(noiseGateThreshold = v) } }
            }
        }

        // ── PGC ──
        item {
            DspModuleCard(title = "Pre-Gain Control", enabled = params.pgcEnabled, onToggle = { viewModel.updateParams { p -> p.copy(pgcEnabled = !p.pgcEnabled) } }) {
                SliderRow("Gain", params.pgcGain, -12f, 12f) { v -> viewModel.updateParams { p -> p.copy(pgcGain = v) } }
            }
        }

        // ── DSP Debug Card ──
        item {
            DspDebugCard(
                sessionId = uiState.sessionId,
                gainBudget = uiState.gainBudget,
                activeModules = viewModel.activeModuleNames(),
                currentRoute = uiState.currentRoute,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            )
        }
    }
}

// ── Reusable Components ──

@Composable
private fun DspModuleCard(
    title: String,
    enabled: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Switch(checked = enabled, onCheckedChange = { onToggle() })
            }
            if (enabled) {
                Spacer(Modifier.height(8.dp))
                content()
            }
        }
    }
}

@Composable
private fun SliderRow(label: String, value: Float, min: Float, max: Float, onValueChange: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(72.dp))
        Slider(value = value, onValueChange = onValueChange, valueRange = min..max, modifier = Modifier.weight(1f))
        Text("%.1f".format(value), style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(40.dp))
    }
}
