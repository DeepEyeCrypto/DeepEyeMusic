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
import com.deepeye.musicpro.dsp.model.RiskLevel
import com.deepeye.musicpro.ui.theme.GainDanger
import com.deepeye.musicpro.ui.theme.GainModerate
import com.deepeye.musicpro.ui.theme.GainSafe

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
                    colors = CardDefaults.cardColors(containerColor = GainDanger.copy(alpha = 0.15f))
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, "Warning", tint = GainDanger)
                        Spacer(Modifier.width(8.dp))
                        Text("Field Surround + Convolver may cause phase conflicts", style = MaterialTheme.typography.bodySmall, color = GainDanger)
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
                totalGain = uiState.gainBudget.totalGain,
                headroom = uiState.gainBudget.headroom,
                riskLevel = uiState.gainBudget.riskLevel,
                breakdown = uiState.gainBudget.breakdown
            )
        }

        // ── Equalizer Card ──
        item {
            DspModuleCard(title = "10-Band Equalizer", enabled = params.eqEnabled, onToggle = { viewModel.updateParams { it.copy(eqEnabled = !it.eqEnabled) } }) {
                val bands = listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    params.eqBands.forEachIndexed { i, value ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(32.dp)) {
                            Text("${value.toInt()}", style = MaterialTheme.typography.labelSmall)
                            Slider(
                                value = value, onValueChange = { viewModel.updateEqBand(i, it) },
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
            DspModuleCard(title = "Bass Boost", enabled = params.bassBoostEnabled, onToggle = { viewModel.updateParams { it.copy(bassBoostEnabled = !it.bassBoostEnabled) } }) {
                SliderRow("Strength", params.bassBoostStrength.toFloat(), 0f, 1000f) { viewModel.updateParams { p -> p.copy(bassBoostStrength = it.toInt()) } }
            }
        }

        // ── Virtualizer ──
        item {
            DspModuleCard(title = "Virtualizer", enabled = params.virtualizerEnabled, onToggle = { viewModel.updateParams { it.copy(virtualizerEnabled = !it.virtualizerEnabled) } }) {
                SliderRow("Strength", params.virtualizerStrength.toFloat(), 0f, 1000f) { viewModel.updateParams { p -> p.copy(virtualizerStrength = it.toInt()) } }
            }
        }

        // ── Reverb ──
        item {
            DspModuleCard(title = "Reverb", enabled = params.reverbEnabled, onToggle = { viewModel.updateParams { it.copy(reverbEnabled = !it.reverbEnabled) } }) {
                Text("Preset: ${params.reverbPreset.name}", style = MaterialTheme.typography.bodySmall)
            }
        }

        // ── Loudness Enhancer ──
        item {
            DspModuleCard(title = "Loudness Enhancer", enabled = params.loudnessEnabled, onToggle = { viewModel.updateParams { it.copy(loudnessEnabled = !it.loudnessEnabled) } }) {
                SliderRow("Gain", params.loudnessGain, 0f, 15f) { viewModel.updateParams { p -> p.copy(loudnessGain = it) } }
            }
        }

        // ── Dynamics Processing ──
        item {
            DspModuleCard(title = "Dynamics Processing", enabled = params.dynamicsEnabled, onToggle = { viewModel.updateParams { it.copy(dynamicsEnabled = !it.dynamicsEnabled) } }) {
                SliderRow("Threshold", params.compressorThreshold, -60f, 0f) { viewModel.updateParams { p -> p.copy(compressorThreshold = it) } }
                SliderRow("Ratio", params.compressorRatio, 1f, 20f) { viewModel.updateParams { p -> p.copy(compressorRatio = it) } }
            }
        }

        // ── Field Surround ──
        item {
            DspModuleCard(title = "Field Surround", enabled = params.surroundEnabled, onToggle = { viewModel.updateParams { it.copy(surroundEnabled = !it.surroundEnabled) } }) {
                SliderRow("Strength", params.surroundStrength.toFloat(), 0f, 1000f) { viewModel.updateParams { p -> p.copy(surroundStrength = it.toInt()) } }
            }
        }

        // ── Convolver ──
        item {
            DspModuleCard(title = "Convolver (IRS)", enabled = params.convolverEnabled, onToggle = { viewModel.updateParams { it.copy(convolverEnabled = !it.convolverEnabled) } }) {
                SliderRow("Mix", params.convolverMix, 0f, 1f) { viewModel.updateParams { p -> p.copy(convolverMix = it) } }
            }
        }

        // ── Tube Simulator ──
        item {
            DspModuleCard(title = "Tube Simulator", enabled = params.tubeEnabled, onToggle = { viewModel.updateParams { it.copy(tubeEnabled = !it.tubeEnabled) } }) {
                SliderRow("Drive", params.tubeDrive, 0f, 1f) { viewModel.updateParams { p -> p.copy(tubeDrive = it) } }
            }
        }

        // ── Clarity ──
        item {
            DspModuleCard(title = "Clarity / Exciter", enabled = params.clarityEnabled, onToggle = { viewModel.updateParams { it.copy(clarityEnabled = !it.clarityEnabled) } }) {
                SliderRow("Strength", params.clarityStrength, 0f, 1f) { viewModel.updateParams { p -> p.copy(clarityStrength = it) } }
            }
        }

        // ── HRTF ──
        item {
            DspModuleCard(title = "HRTF", enabled = params.hrtfEnabled, onToggle = { viewModel.updateParams { it.copy(hrtfEnabled = !it.hrtfEnabled) } }) {
                Text("Preset: ${params.hrtfPreset.name}", style = MaterialTheme.typography.bodySmall)
            }
        }

        // ── Speaker Protection ──
        item {
            DspModuleCard(title = "Speaker Protection", enabled = params.speakerProtectionEnabled, onToggle = { viewModel.updateParams { it.copy(speakerProtectionEnabled = !it.speakerProtectionEnabled) } }) {
                SliderRow("Max dB", params.speakerMaxDb, -12f, 0f) { viewModel.updateParams { p -> p.copy(speakerMaxDb = it) } }
            }
        }

        // ── Noise Gate ──
        item {
            DspModuleCard(title = "Noise Gate", enabled = params.noiseGateEnabled, onToggle = { viewModel.updateParams { it.copy(noiseGateEnabled = !it.noiseGateEnabled) } }) {
                SliderRow("Threshold", params.noiseGateThreshold, -80f, -20f) { viewModel.updateParams { p -> p.copy(noiseGateThreshold = it) } }
            }
        }

        // ── PGC ──
        item {
            DspModuleCard(title = "Pre-Gain Control", enabled = params.pgcEnabled, onToggle = { viewModel.updateParams { it.copy(pgcEnabled = !it.pgcEnabled) } }) {
                SliderRow("Gain", params.pgcGain, -12f, 12f) { viewModel.updateParams { p -> p.copy(pgcGain = it) } }
            }
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

@Composable
private fun GainBudgetCard(totalGain: Float, headroom: Float, riskLevel: RiskLevel, breakdown: Map<String, Float>) {
    val riskColor = when (riskLevel) { RiskLevel.SAFE -> GainSafe; RiskLevel.MODERATE -> GainModerate; RiskLevel.DANGER -> GainDanger }
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Gain Budget", style = MaterialTheme.typography.titleSmall)
                Text(riskLevel.name, style = MaterialTheme.typography.labelMedium, color = riskColor)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (totalGain / 12f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = riskColor,
                trackColor = MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.height(4.dp))
            Text("Total: %.1f dB | Headroom: %.1f dB".format(totalGain, headroom), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
