package com.deepeye.musicpro.ui.dsp

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deepeye.musicpro.dsp.engine.DSPViewModel
import com.deepeye.musicpro.dsp.model.DSPPreset

@Composable
fun DSPScreen(
    viewModel: DSPViewModel = hiltViewModel()
) {
    val preset by viewModel.currentPreset.collectAsState()
    val isEnabled by viewModel.isEnabled.collectAsState()
    val eqBands by viewModel.eqBands.collectAsState()
    val bassStrength by viewModel.bassStrength.collectAsState()
    val virtualizerStrength by viewModel.virtualizerStrength.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0A0A1A), Color(0xFF1A0A2E))
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // ─── DSP Toggle Header ───────────────────
        DSPToggleHeader(
            isEnabled = isEnabled,
            onToggle = viewModel::setEnabled
        )

        Spacer(Modifier.height(16.dp))

        // ─── Preset Grid ─────────────────────────
        PresetGrid(
            currentPreset = preset,
            onPresetSelected = viewModel::setPreset
        )

        Spacer(Modifier.height(20.dp))

        // ─── EQ Visualizer + Bands ───────────────
        Text(
            "10-Band Equalizer",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        EQBandsSliders(
            bands = eqBands,
            onBandChange = viewModel::setBandLevel
        )

        Spacer(Modifier.height(20.dp))

        // ─── Bass Control ─────────────────────────
        GlassCard {
            DSPSlider(
                label = "🔊 Bass Boost",
                value = bassStrength.toFloat(),
                range = 0f..1000f,
                onValueChange = { viewModel.setBassStrength(it.toInt()) },
                color = Color(0xFFFF6B35)
            )
        }

        Spacer(Modifier.height(12.dp))

        // ─── Virtualizer / 3D Sound ───────────────
        GlassCard {
            DSPSlider(
                label = "🌐 3D Surround",
                value = virtualizerStrength.toFloat(),
                range = 0f..1000f,
                onValueChange = { viewModel.setVirtualizer(it.toInt()) },
                color = Color(0xFF00E5FF)
            )
        }
        
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun DSPToggleHeader(isEnabled: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Premium DSP",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )
        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF00E5FF)
            )
        )
    }
}

@Composable
fun PresetGrid(
    currentPreset: DSPPreset,
    onPresetSelected: (DSPPreset) -> Unit
) {
    val presets = listOf(
        DSPPreset.PREMIUM_BASS to "🎵 Premium Bass",
        DSPPreset.BASS_BOOSTER_MAX to "💥 Bass MAX",
        DSPPreset.HIFI_HEADPHONES to "🎧 HiFi",
        DSPPreset.VOCAL_CLARITY to "🎤 Vocal",
        DSPPreset.DEEP_HOUSE to "🏠 Deep House",
        DSPPreset.LOUDNESS_MAX to "📢 Loud MAX",
        DSPPreset.FLAT to "⬜ Flat",
        DSPPreset.CUSTOM to "🎛️ Custom"
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.height(200.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        userScrollEnabled = false
    ) {
        items(presets) { (preset, label) ->
            PresetChip(
                label = label,
                isSelected = currentPreset == preset,
                onClick = { onPresetSelected(preset) }
            )
        }
    }
}

@Composable
fun PresetChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected)
            Color(0xFF00E5FF).copy(alpha = 0.2f)
        else
            Color.White.copy(alpha = 0.05f),
        border = BorderStroke(
            1.dp,
            if (isSelected) Color(0xFF00E5FF)
            else Color.White.copy(0.1f)
        )
    ) {
        Box(
            Modifier.padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) Color(0xFF00E5FF) else Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun EQBandsSliders(
    bands: IntArray,
    onBandChange: (Int, Int) -> Unit
) {
    val bandLabels = listOf(
        "60Hz", "170Hz", "310Hz", "600Hz", "1kHz",
        "3kHz", "6kHz", "12kHz", "14kHz", "16kHz"
    )
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        bands.forEachIndexed { i, level ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // To display slider vertically, we use graphicsLayer rotation.
                // Size mapping can be tricky, so we use a Box
                Box(
                    modifier = Modifier
                        .height(140.dp)
                        .width(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Slider(
                        value = level.toFloat(),
                        onValueChange = { onBandChange(i, it.toInt()) },
                        valueRange = -1500f..1500f,
                        modifier = Modifier
                            .requiredWidth(140.dp)
                            .graphicsLayer {
                                rotationZ = -90f
                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.5f)
                            },
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00E5FF),
                            activeTrackColor = Color(0xFF00E5FF),
                            inactiveTrackColor = Color.White.copy(0.2f)
                        )
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    bandLabels.getOrElse(i) { "${i}band" },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(0.6f)
                )
            }
        }
    }
}

@Composable
fun GlassCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun DSPSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    color: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = Color.White, style = MaterialTheme.typography.bodyLarge)
            Text(text = value.toInt().toString(), color = color, style = MaterialTheme.typography.bodyLarge)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
            )
        )
    }
}
