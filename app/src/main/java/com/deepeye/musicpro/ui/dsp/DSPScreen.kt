// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.dsp

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.scale
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.deepeye.musicpro.dsp.engine.DSPViewModel
import com.deepeye.musicpro.dsp.model.*
import com.deepeye.musicpro.ui.theme.ContentBounds
import com.deepeye.musicpro.ui.dsp.components.DspDebugCard
import com.deepeye.musicpro.ui.dsp.components.GainBudgetCard
import com.deepeye.musicpro.ui.dsp.components.StudioKnob
import com.deepeye.musicpro.ui.dsp.components.StudioVisualizer
import com.deepeye.musicpro.ui.theme.ProvideAppZoom

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DSPScreen(
    windowSizeClass: WindowSizeClass,
    viewModel: DSPViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val fftData by viewModel.fftData.collectAsStateWithLifecycle()
    val userRank by viewModel.userRankFlow.collectAsStateWithLifecycle()
    val isEnabled = uiState.params.enabled
    val isWideScreen = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

    var showSaveDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("V4A DSP Engine", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(56.dp)
                            .background(Color(0x1AFFFFFF), androidx.compose.foundation.shape.CircleShape)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 16.dp),
                    ) {
                        Text(
                            text = if (isEnabled) "ACTIVE" else "DISABLED",
                            color = if (isEnabled) Color(0xFF00E5FF) else Color.White.copy(0.6f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 12.dp),
                        )
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { viewModel.toggleMasterEnabled() },
                            modifier = Modifier
                                .testTag("dsp_toggle")
                                .scale(1.35f),
                            colors =
                            SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF00E5FF),
                                checkedTrackColor = Color(0x4400E5FF),
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0x1AFFFFFF),
                            ),
                        )
                    }
                },
                colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
    ) { innerPadding ->
        Box(
            modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .padding(innerPadding),
        ) {
            if (isWideScreen) {
                // Dual Pane Layout for Tablet / Landscape
                Row(
                    modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    // Left Pane: Sticky Dashboard
                    Column(
                        modifier =
                        Modifier
                            .width(320.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        StudioVisualizer(
                            fftData = fftData,
                            barColor = if (isEnabled) Color(0xFF00E5FF) else Color.Gray.copy(alpha = 0.5f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        GainBudgetCard(gainBudget = uiState.gainBudget)

                        com.deepeye.musicpro.ui.dsp.components.DSPPresetSelector(
                            currentPreset = uiState.activePreset,
                            userRank = userRank,
                            onPresetChanged = viewModel::applyDSPPreset
                        )

                        PresetsCard(
                            presets = uiState.presets,
                            selectedPresetId = uiState.selectedPresetId,
                            onPresetSelect = viewModel::loadPreset,
                            onPresetDelete = viewModel::deletePreset,
                            onSaveClick = { showSaveDialog = true },
                            onSaveForTrackClick = viewModel::saveProfileForCurrentTrack,
                        )

                        DspDebugCard(
                            sessionId = uiState.sessionId,
                            gainBudget = uiState.gainBudget,
                            activeModules = viewModel.activeModuleNames(),
                            currentRoute = uiState.currentRoute,
                            activePresetName = uiState.activePreset.presetName,
                        )
                    }

                    // Right Pane: Collapsible Grid of Modules
                    ModulesGrid(
                        params = uiState.params,
                        isEnabled = isEnabled,
                        onUpdateParams = viewModel::updateParams,
                        onUpdateEqBand = viewModel::updateEqBand,
                        onCommitEqBands = viewModel::commitEqBands,
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                // Single Column Layout for Phones
                Column(
                    modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    StudioVisualizer(
                        fftData = fftData,
                        barColor = if (isEnabled) Color(0xFF00E5FF) else Color.Gray.copy(alpha = 0.5f)
                    )

                    GainBudgetCard(gainBudget = uiState.gainBudget)

                    com.deepeye.musicpro.ui.dsp.components.DSPPresetSelector(
                        currentPreset = uiState.activePreset,
                        userRank = userRank,
                        onPresetChanged = viewModel::applyDSPPreset
                    )

                    PresetsRow(
                        presets = uiState.presets,
                        selectedPresetId = uiState.selectedPresetId,
                        onPresetSelect = viewModel::loadPreset,
                        onSaveClick = { showSaveDialog = true },
                        onSaveForTrackClick = viewModel::saveProfileForCurrentTrack,
                    )

                    ModulesColumn(
                        params = uiState.params,
                        isEnabled = isEnabled,
                        onUpdateParams = viewModel::updateParams,
                        onUpdateEqBand = viewModel::updateEqBand,
                        onCommitEqBands = viewModel::commitEqBands,
                    )

                    DspDebugCard(
                        sessionId = uiState.sessionId,
                        gainBudget = uiState.gainBudget,
                        activeModules = viewModel.activeModuleNames(),
                        currentRoute = uiState.currentRoute,
                        activePresetName = uiState.activePreset.presetName,
                    )
                }
            }
        }
    }

    if (showSaveDialog) {
        SavePresetDialog(
            onDismiss = { showSaveDialog = false },
            onSave = { name ->
                viewModel.savePreset(name)
                showSaveDialog = false
            },
        )
    }
}

@Composable
fun PresetsCard(
    presets: List<Pair<Long, String>>,
    selectedPresetId: Long?,
    onPresetSelect: (Long) -> Unit,
    onPresetDelete: (Long) -> Unit,
    onSaveClick: () -> Unit,
    onSaveForTrackClick: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131722).copy(alpha = 0.85f)),
        border = BorderStroke(1.dp, Color(0x22FFFFFF)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Presets", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Row {
                    IconButton(onClick = onSaveForTrackClick, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Default.Star, contentDescription = "Link to Track", tint = Color(0xFFFFD700), modifier = Modifier.size(22.dp))
                    }
                    IconButton(onClick = onSaveClick, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "Save Custom Preset", tint = Color(0xFF00E5FF), modifier = Modifier.size(24.dp))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.forEach { (id, name) ->
                    val isSelected = id == selectedPresetId
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .clickable { onPresetSelect(id) }
                            .background(
                                color = if (isSelected) Color(0x3300E5FF) else Color(0x0AFFFFFF),
                                shape = RoundedCornerShape(12.dp),
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) Color(0xFF00E5FF) else Color(0x14FFFFFF),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = name,
                            color = if (isSelected) Color(0xFF00E5FF) else Color.White,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        )
                        // Allow deletion of custom user presets (id > 7, built-ins are 1-7)
                        if (id > 7) {
                            IconButton(onClick = { onPresetDelete(id) }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PresetsRow(
    presets: List<Pair<Long, String>>,
    selectedPresetId: Long?,
    onPresetSelect: (Long) -> Unit,
    onSaveClick: () -> Unit,
    onSaveForTrackClick: () -> Unit,
) {
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        presets.forEach { (id, name) ->
            val isSelected = id == selectedPresetId
            PresetChip(
                label = name,
                isSelected = isSelected,
                onClick = { onPresetSelect(id) },
            )
        }

        IconButton(onClick = onSaveForTrackClick) {
            Icon(Icons.Default.Star, contentDescription = "Link to Track", tint = Color(0xFFFFD700))
        }
        IconButton(onClick = onSaveClick) {
            Icon(Icons.Default.Add, contentDescription = "Save Preset", tint = Color(0xFF00E5FF))
        }
    }
}

@Composable
fun SavePresetDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        // A Dialog is its own window with its own density, so it does NOT
        // inherit the root zoom. Without this it renders at system scale
        // while the screen behind it is zoomed. See ProvideAppZoom.
        ProvideAppZoom {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.padding(16.dp),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Save Preset", style = MaterialTheme.typography.titleMedium, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Preset Name") },
                    colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(0.2f),
                    ),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(0.6f)) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { if (text.isNotBlank()) onSave(text) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    ) {
                        Text("Save", color = Color.Black)
                    }
                }
            }
        }
        }
    }
}

@Composable
fun ModulesGrid(
    params: DspParams,
    isEnabled: Boolean,
    onUpdateParams: ((DspParams) -> DspParams) -> Unit,
    onUpdateEqBand: (Int, Float) -> Unit,
    onCommitEqBands: (FloatArray) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            EqualizerCard(params, isEnabled, onToggle = { enabled ->
                onUpdateParams {
                    it.copy(eqEnabled = enabled)
                }
            }, onUpdateEqBand, onCommitEqBands)
        }
        item { PgcCard(params, isEnabled, onUpdateParams) }
        item { ViperBassCard(params, isEnabled, onUpdateParams) }
        item { SurroundCard(params, isEnabled, onUpdateParams) }
        item { ReverbCard(params, isEnabled, onUpdateParams) }
        item { LoudnessCard(params, isEnabled, onUpdateParams) }
        item { DynamicsCard(params, isEnabled, onUpdateParams) }
        item { ConvolverCard(params, isEnabled, onUpdateParams) }
        item { TubeCard(params, isEnabled, onUpdateParams) }
        item { ClarityCard(params, isEnabled, onUpdateParams) }
        item { HrtfCard(params, isEnabled, onUpdateParams) }
        item { ProtectionCard(params, isEnabled, onUpdateParams) }
        item { NoiseGateCard(params, isEnabled, onUpdateParams) }
        item { KaraokeCard(params, isEnabled, onUpdateParams) }
        item { CrossfeedCard(params, isEnabled, onUpdateParams) }
        item { SpeedPitchCard(params, isEnabled, onUpdateParams) }
    }
}

@Composable
fun ModulesColumn(
    params: DspParams,
    isEnabled: Boolean,
    onUpdateParams: ((DspParams) -> DspParams) -> Unit,
    onUpdateEqBand: (Int, Float) -> Unit,
    onCommitEqBands: (FloatArray) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PgcCard(params, isEnabled, onUpdateParams)
        EqualizerCard(params, isEnabled, onToggle = { enabled ->
            onUpdateParams {
                it.copy(eqEnabled = enabled)
            }
        }, onUpdateEqBand, onCommitEqBands)
        ViperBassCard(params, isEnabled, onUpdateParams)
        SurroundCard(params, isEnabled, onUpdateParams)
        ReverbCard(params, isEnabled, onUpdateParams)
        LoudnessCard(params, isEnabled, onUpdateParams)
        DynamicsCard(params, isEnabled, onUpdateParams)
        ConvolverCard(params, isEnabled, onUpdateParams)
        TubeCard(params, isEnabled, onUpdateParams)
        ClarityCard(params, isEnabled, onUpdateParams)
        HrtfCard(params, isEnabled, onUpdateParams)
        ProtectionCard(params, isEnabled, onUpdateParams)
        NoiseGateCard(params, isEnabled, onUpdateParams)
        KaraokeCard(params, isEnabled, onUpdateParams)
        CrossfeedCard(params, isEnabled, onUpdateParams)
        SpeedPitchCard(params, isEnabled, onUpdateParams)
    }
}

// ── Collapsible Card Container ──

@Composable
fun CollapsibleCard(
    title: String,
    isEnabled: Boolean,
    isMasterEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val showContent = expanded && isEnabled && isMasterEnabled

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131722).copy(alpha = 0.85f)),
        border = BorderStroke(
            if (isEnabled && isMasterEnabled) 1.5.dp else 1.dp,
            if (isEnabled && isMasterEnabled) Color(0xFF00E5FF).copy(0.4f) else Color(0x22FFFFFF)
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).padding(end = 12.dp)
                ) {
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = onToggle,
                        enabled = isMasterEnabled,
                        modifier = Modifier
                            .scale(1.35f)
                            .padding(end = 16.dp),
                        colors =
                        SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF00E5FF),
                            checkedTrackColor = Color(0x4400E5FF),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0x1AFFFFFF),
                            disabledUncheckedThumbColor = Color.White.copy(alpha = 0.4f),
                            disabledUncheckedTrackColor = Color(0x1AFFFFFF),
                            disabledCheckedThumbColor = Color(0xFF00E5FF).copy(alpha = 0.4f),
                            disabledCheckedTrackColor = Color(0x2200E5FF),
                        ),
                    )
                    Text(
                        text = title,
                        fontSize = 17.sp,
                        color = if (isEnabled && isMasterEnabled) Color.White else Color.White.copy(0.5f),
                        fontWeight = FontWeight.Bold,
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = if (isEnabled && isMasterEnabled) Color(0xFF00E5FF) else Color.White.copy(0.5f),
                    modifier = Modifier.size(24.dp)
                )
            }

            if (showContent) {
                Column {
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0x1AFFFFFF), thickness = 1.dp)
                    Spacer(Modifier.height(16.dp))
                    content()
                }
            }
        }
    }
}

// ── Sliders & Controls Helpers ──

@Composable
fun DspSliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    valueFormatter: (Float) -> String = { "%.1f".format(it) },
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(valueFormatter(value), color = Color(0xFF00E5FF), fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors =
            SliderDefaults.colors(
                thumbColor = Color(0xFF00E5FF),
                activeTrackColor = Color(0xFF00E5FF),
                inactiveTrackColor = Color(0x33FFFFFF),
            ),
        )
    }
}

@Composable
fun DspKnobControl(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    valueFormatter: (Float) -> String = { "%.1f".format(it) },
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        StudioKnob(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(label, color = Color.White.copy(0.7f), fontSize = 13.sp)
        Text(valueFormatter(value), color = Color(0xFF00E5FF), fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

// ── Custom PresetChip (Alternative to version-dependent InputChip) ──

@Composable
fun PresetChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color =
        if (isSelected) {
            Color(0x3300E5FF)
        } else {
            Color(0x0EFFFFFF)
        },
        border =
        BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) {
                Color(0xFF00E5FF)
            } else {
                Color(0x14FFFFFF)
            },
        ),
        modifier = Modifier.heightIn(min = 48.dp)
    ) {
        Box(
            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color(0xFF00E5FF) else Color.White,
            )
        }
    }
}

// ── 14 Module Card Implementations ──

@Composable
fun PgcCard(
    params: DspParams,
    isMasterEnabled: Boolean,
    onUpdateParams: ((DspParams) -> DspParams) -> Unit,
) {
    CollapsibleCard(
        title = "Pre-Gain Control (PGC)",
        isEnabled = params.pgcEnabled,
        isMasterEnabled = isMasterEnabled,
        onToggle = { enabled -> onUpdateParams { it.copy(pgcEnabled = enabled) } },
    ) {
        DspSliderRow(
            label = "Headroom Gain",
            value = params.pgcGain,
            valueRange = -12f..12f,
            onValueChange = { v -> onUpdateParams { it.copy(pgcGain = v) } },
            valueFormatter = { "${"%.1f".format(it)} dB" },
        )
    }
}

@Composable
fun EqualizerCard(
    params: DspParams,
    isMasterEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onUpdateEqBand: (Int, Float) -> Unit,
    onCommitEqBands: (FloatArray) -> Unit,
) {
    val isEnabled = params.eqEnabled
    var expanded by remember(isEnabled) { mutableStateOf(isEnabled) }
    val showContent = expanded && isEnabled && isMasterEnabled

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131722).copy(alpha = 0.85f)),
        border = BorderStroke(
            if (isEnabled && isMasterEnabled) 1.5.dp else 1.dp,
            if (isEnabled && isMasterEnabled) Color(0xFF00E5FF).copy(0.4f) else Color(0x22FFFFFF)
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).padding(end = 12.dp)
                ) {
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = onToggle,
                        enabled = isMasterEnabled,
                        modifier = Modifier
                            .scale(1.35f)
                            .padding(end = 16.dp),
                        colors =
                        SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF00E5FF),
                            checkedTrackColor = Color(0x4400E5FF),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0x1AFFFFFF),
                        ),
                    )
                    Column {
                        Text(
                            text = "10-Band Equalizer",
                            fontSize = 17.sp,
                            color = if (isEnabled && isMasterEnabled) Color.White else Color.White.copy(0.5f),
                            fontWeight = FontWeight.Bold,
                        )
                        if (isEnabled && isMasterEnabled) {
                            Text(
                                text = "STUDIO PRO • 10 BANDS ACTIVE",
                                fontSize = 10.sp,
                                color = Color(0xFF00E5FF),
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.5.sp,
                            )
                        }
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = if (isEnabled && isMasterEnabled) Color(0xFF00E5FF) else Color.White.copy(0.5f),
                    modifier = Modifier.size(24.dp)
                )
            }

            if (showContent) {
                Column {
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0x1AFFFFFF), thickness = 1.dp)
                    Spacer(Modifier.height(16.dp))
                    com.deepeye.musicpro.ui.dsp.components.EqualizerCurveVisualizer(
                        eqBands = params.eqBands,
                        isEnabled = isEnabled && isMasterEnabled,
                        onBandGainChanged = onUpdateEqBand,
                        onCommitBands = onCommitEqBands,
                    )
                }
            }
        }
    }
}

@Composable
fun ViperBassCard(
    params: DspParams,
    isMasterEnabled: Boolean,
    onUpdateParams: ((DspParams) -> DspParams) -> Unit,
) {
    CollapsibleCard(
        title = "Viper Bass & Boost",
        isEnabled = params.bassBoostEnabled || params.viperBassEnabled,
        isMasterEnabled = isMasterEnabled,
        onToggle = { enabled ->
            onUpdateParams { it.copy(bassBoostEnabled = enabled, viperBassEnabled = enabled) }
        },
    ) {
        // Mode Selector
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Bass Mode", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(0.7f))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ViperBassMode.values().forEach { mode ->
                    val isSelected = params.viperBassMode == mode
                    PresetChip(
                        label = mode.name,
                        isSelected = isSelected,
                        onClick = { onUpdateParams { it.copy(viperBassMode = mode) } },
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Spacer(Modifier.height(16.dp))

        // Hardware style side-by-side Knobs (Removed as per user request)
        DspSliderRow(
            label = "Sub Cutoff",
            value = params.viperBassFreq.toFloat(),
            valueRange = 30f..120f,
            onValueChange = { v -> onUpdateParams { it.copy(viperBassFreq = v.toInt()) } },
            valueFormatter = { "${it.toInt()} Hz" },
        )
        
        Spacer(Modifier.height(8.dp))
        
        DspSliderRow(
            label = "Sub-Bass Gain",
            value = params.viperBassGain,
            valueRange = 0f..18f,
            onValueChange = { v -> onUpdateParams { it.copy(viperBassGain = v) } },
            valueFormatter = { "${"%.1f".format(it)} dB" },
        )

        Spacer(Modifier.height(16.dp))

        // Mid-Bass Strength (Uses Bass Boost parameter internally)
        DspSliderRow(
            label = "Mid-Bass Punch",
            value = params.bassBoostStrength.toFloat(),
            valueRange = 0f..1000f,
            onValueChange = { v -> onUpdateParams { it.copy(bassBoostStrength = v.toInt()) } },
            valueFormatter = { "${(it / 10f).toInt()}%" },
        )
    }
}

@Composable
fun SurroundCard(
    params: DspParams,
    isMasterEnabled: Boolean,
    onUpdateParams: ((DspParams) -> DspParams) -> Unit,
) {
    CollapsibleCard(
        title = "Field Surround",
        isEnabled = params.surroundEnabled,
        isMasterEnabled = isMasterEnabled,
        onToggle = { enabled -> onUpdateParams { it.copy(surroundEnabled = enabled) } },
    ) {
        // Mode
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Surround Mode", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(0.7f))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SurroundMode.values().forEach { mode ->
                    val isSelected = params.surroundMode == mode
                    PresetChip(
                        label = mode.name,
                        isSelected = isSelected,
                        onClick = { onUpdateParams { it.copy(surroundMode = mode) } },
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Strength
        DspSliderRow(
            label = "Surround Strength",
            value = params.surroundStrength.toFloat(),
            valueRange = 0f..1000f,
            onValueChange = { v -> onUpdateParams { it.copy(surroundStrength = v.toInt()) } },
            valueFormatter = { "${it.toInt()}" },
        )
    }
}

@Composable
fun ReverbCard(
    params: DspParams,
    isMasterEnabled: Boolean,
    onUpdateParams: ((DspParams) -> DspParams) -> Unit,
) {
    CollapsibleCard(
        title = "Preset Reverb",
        isEnabled = params.reverbEnabled,
        isMasterEnabled = isMasterEnabled,
        onToggle = { enabled -> onUpdateParams { it.copy(reverbEnabled = enabled) } },
    ) {
        // Preset selector
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Reverb Room", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(0.7f))
            Box {
                var menuExpanded by remember { mutableStateOf(false) }
                Button(
                    onClick = { menuExpanded = true },
                    colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(0.08f)),
                ) {
                    Text(params.reverbPreset.name, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    ReverbPreset.values().forEach { preset ->
                        DropdownMenuItem(
                            text = { Text(preset.name) },
                            onClick = {
                                onUpdateParams { it.copy(reverbPreset = preset) }
                                menuExpanded = false
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LoudnessCard(
    params: DspParams,
    isMasterEnabled: Boolean,
    onUpdateParams: ((DspParams) -> DspParams) -> Unit,
) {
    CollapsibleCard(
        title = "Loudness Enhancer",
        isEnabled = params.loudnessEnabled,
        isMasterEnabled = isMasterEnabled,
        onToggle = { enabled -> onUpdateParams { it.copy(loudnessEnabled = enabled) } },
    ) {
        DspSliderRow(
            label = "Target Gain Boost",
            value = params.loudnessTargetGainMb.toFloat(),
            valueRange = 0f..2000f,
            onValueChange = { v -> onUpdateParams { it.copy(loudnessTargetGainMb = v.toInt()) } },
            valueFormatter = { "${it.toInt()} mB" },
        )
    }
}

@Composable
fun DynamicsCard(
    params: DspParams,
    isMasterEnabled: Boolean,
    onUpdateParams: ((DspParams) -> DspParams) -> Unit,
) {
    CollapsibleCard(
        title = "Dynamics Processing",
        isEnabled = params.dynamicsEnabled,
        isMasterEnabled = isMasterEnabled,
        onToggle = { enabled -> onUpdateParams { it.copy(dynamicsEnabled = enabled) } },
    ) {
        // Compressor parameters
        DspSliderRow(
            label = "Attack Time",
            value = params.compressorAttack,
            valueRange = 1f..100f,
            onValueChange = { v -> onUpdateParams { it.copy(compressorAttack = v) } },
            valueFormatter = { "${it.toInt()} ms" },
        )

        DspSliderRow(
            label = "Release Time",
            value = params.compressorRelease,
            valueRange = 10f..1000f,
            onValueChange = { v -> onUpdateParams { it.copy(compressorRelease = v) } },
            valueFormatter = { "${it.toInt()} ms" },
        )

        // Limiter
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Enable Peak Limiter", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(0.7f))
            Switch(
                checked = params.limiterEnabled,
                onCheckedChange = { v -> onUpdateParams { it.copy(limiterEnabled = v) } },
                colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF00E5FF)),
            )
        }

        DspSliderRow(
            label = "Limiter Threshold",
            value = params.limiterThreshold,
            valueRange = -12f..0f,
            onValueChange = { v -> onUpdateParams { it.copy(limiterThreshold = v) } },
            valueFormatter = { "${"%.1f".format(it)} dB" },
        )
    }
}

@Composable
fun ConvolverCard(
    params: DspParams,
    isMasterEnabled: Boolean,
    onUpdateParams: ((DspParams) -> DspParams) -> Unit,
) {
    CollapsibleCard(
        title = "Convolver (IRS)",
        isEnabled = params.convolverEnabled,
        isMasterEnabled = isMasterEnabled,
        onToggle = { enabled -> onUpdateParams { it.copy(convolverEnabled = enabled) } },
    ) {
        Text(
            text = "Convolution replicates acoustics via impulse response files (.irs).",
            style = MaterialTheme.typography.bodySmall,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(0.5f),
            modifier = Modifier.padding(bottom = 8.dp),
        )
        DspSliderRow(
            label = "Wet Mix Level",
            value = params.convolverMix,
            valueRange = 0f..1f,
            onValueChange = { v -> onUpdateParams { it.copy(convolverMix = v) } },
            valueFormatter = { "${(it * 100).toInt()}%" },
        )
    }
}

@Composable
fun TubeCard(
    params: DspParams,
    isMasterEnabled: Boolean,
    onUpdateParams: ((DspParams) -> DspParams) -> Unit,
) {
    CollapsibleCard(
        title = "Tube Simulator",
        isEnabled = params.tubeEnabled,
        isMasterEnabled = isMasterEnabled,
        onToggle = { enabled -> onUpdateParams { it.copy(tubeEnabled = enabled) } },
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Valve Tube Mode", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(0.7f))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TubeMode.values().forEach { mode ->
                    val isSelected = params.tubeMode == mode
                    PresetChip(
                        label = mode.name,
                        isSelected = isSelected,
                        onClick = { onUpdateParams { it.copy(tubeMode = mode) } },
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        DspSliderRow(
            label = "Vacuum Tube Drive",
            value = params.tubeDrive.toFloat(),
            valueRange = 0f..100f,
            onValueChange = { v -> onUpdateParams { it.copy(tubeDrive = v.toInt()) } },
            valueFormatter = { "${it.toInt()}%" },
        )
    }
}

@Composable
fun ClarityCard(
    params: DspParams,
    isMasterEnabled: Boolean,
    onUpdateParams: ((DspParams) -> DspParams) -> Unit,
) {
    CollapsibleCard(
        title = "Viper Clarity / Exciter",
        isEnabled = params.clarityEnabled || params.viperClarityEnabled,
        isMasterEnabled = isMasterEnabled,
        onToggle = { enabled ->
            onUpdateParams { it.copy(clarityEnabled = enabled, viperClarityEnabled = enabled) }
        },
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Clarity Exciter Mode", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(0.7f))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ViperClarityMode.values().forEach { mode ->
                    val isSelected = params.viperClarityMode == mode
                    PresetChip(
                        label = mode.name,
                        isSelected = isSelected,
                        onClick = { onUpdateParams { it.copy(viperClarityMode = mode) } },
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        DspSliderRow(
            label = "Clarity Excitation",
            value = params.viperClarityGain,
            valueRange = 0f..15f,
            onValueChange = { v -> onUpdateParams { it.copy(viperClarityGain = v) } },
            valueFormatter = { "${"%.1f".format(it)} dB" },
        )
    }
}

@Composable
fun HrtfCard(
    params: DspParams,
    isMasterEnabled: Boolean,
    onUpdateParams: ((DspParams) -> DspParams) -> Unit,
) {
    CollapsibleCard(
        title = "HRTF Spatialization",
        isEnabled = params.hrtfEnabled,
        isMasterEnabled = isMasterEnabled,
        onToggle = { enabled -> onUpdateParams { it.copy(hrtfEnabled = enabled) } },
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Binaural HRTF Filter", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(0.7f))
            Box {
                var menuExpanded by remember { mutableStateOf(false) }
                Button(
                    onClick = { menuExpanded = true },
                    colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(0.08f)),
                ) {
                    Text(params.hrtfPreset.name, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    HrtfPreset.values().forEach { preset ->
                        DropdownMenuItem(
                            text = { Text(preset.name) },
                            onClick = {
                                onUpdateParams { it.copy(hrtfPreset = preset) }
                                menuExpanded = false
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProtectionCard(
    params: DspParams,
    isMasterEnabled: Boolean,
    onUpdateParams: ((DspParams) -> DspParams) -> Unit,
) {
    CollapsibleCard(
        title = "Speaker Protection",
        isEnabled = params.speakerProtectionEnabled,
        isMasterEnabled = isMasterEnabled,
        onToggle = { enabled -> onUpdateParams { it.copy(speakerProtectionEnabled = enabled) } },
    ) {
        DspSliderRow(
            label = "Maximum Decibel Limit",
            value = params.speakerMaxDb,
            valueRange = -12f..0f,
            onValueChange = { v -> onUpdateParams { it.copy(speakerMaxDb = v) } },
            valueFormatter = { "${"%.1f".format(it)} dB" },
        )
    }
}

@Composable
fun NoiseGateCard(
    params: DspParams,
    isMasterEnabled: Boolean,
    onUpdateParams: ((DspParams) -> DspParams) -> Unit,
) {
    CollapsibleCard(
        title = "Noise Gate",
        isEnabled = params.noiseGateEnabled,
        isMasterEnabled = isMasterEnabled,
        onToggle = { enabled -> onUpdateParams { it.copy(noiseGateEnabled = enabled) } },
    ) {
        DspSliderRow(
            label = "Noise Floor Threshold",
            value = params.noiseGateThreshold,
            valueRange = -80f..-20f,
            onValueChange = { v -> onUpdateParams { it.copy(noiseGateThreshold = v) } },
            valueFormatter = { "${"%.1f".format(it)} dB" },
        )
    }
}

@Composable
fun KaraokeCard(
    params: DspParams,
    isMasterEnabled: Boolean,
    onUpdateParams: ((DspParams) -> DspParams) -> Unit,
) {
    CollapsibleCard(
        title = "Karaoke Mode (Vocal Remover)",
        isEnabled = params.karaokeModeEnabled,
        isMasterEnabled = isMasterEnabled,
        onToggle = { enabled -> onUpdateParams { it.copy(karaokeModeEnabled = enabled) } },
    ) {
        Text(
            "Uses OOPS phase cancellation to isolate center-panned vocals. Works best on older stereo mixes.",
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(0.7f),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun CrossfeedCard(
    params: DspParams,
    isMasterEnabled: Boolean,
    onUpdateParams: ((DspParams) -> DspParams) -> Unit,
) {
    CollapsibleCard(
        title = "Binaural Crossfeed",
        isEnabled = params.crossfeedEnabled,
        isMasterEnabled = isMasterEnabled,
        onToggle = { enabled -> onUpdateParams { it.copy(crossfeedEnabled = enabled) } },
    ) {
        Text(
            "Simulates natural room acoustics on headphones by blending a delayed and filtered signal from opposite channels. Reduces listening fatigue.",
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(0.7f),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun SpeedPitchCard(
    params: DspParams,
    isMasterEnabled: Boolean,
    onUpdateParams: ((DspParams) -> DspParams) -> Unit,
) {
    CollapsibleCard(
        title = "Speed & Pitch",
        isEnabled = true,
        isMasterEnabled = isMasterEnabled,
        onToggle = { }, // It's always active, no internal bypass switch for ExoPlayer speed
    ) {
        DspSliderRow(
            label = "Playback Speed",
            value = params.playbackSpeed,
            valueRange = 0.25f..2.5f,
            onValueChange = { v -> onUpdateParams { it.copy(playbackSpeed = v) } },
            valueFormatter = { "${"%.2f".format(it)}x" },
        )
        DspSliderRow(
            label = "Playback Pitch",
            value = params.playbackPitch,
            valueRange = 0.5f..2.0f,
            onValueChange = { v -> onUpdateParams { it.copy(playbackPitch = v) } },
            valueFormatter = { "${"%.2f".format(it)}x" },
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = { 
                onUpdateParams { it.copy(playbackSpeed = 1.0f, playbackPitch = 1.0f) } 
            }) {
                Text("Reset", color = Color(0xFF00E5FF))
            }
        }
    }
}
