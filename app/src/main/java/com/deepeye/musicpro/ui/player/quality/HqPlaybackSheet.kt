// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.player.quality

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.musicpro.domain.model.PlayerState
import com.deepeye.musicpro.player.format.BufferProfile
import com.deepeye.musicpro.player.format.DeepEyeFormat
import com.deepeye.musicpro.player.format.PlaybackDiagnostics
import com.deepeye.musicpro.player.format.QualityPreset

enum class HqSheetTab(val title: String, val icon: ImageVector) {
    VIDEO("Video", Icons.Default.HighQuality),
    AUDIO("Audio", Icons.Default.Audiotrack),
    BUFFER("Buffer", Icons.Default.Speed),
    STATS("Stats HUD", Icons.Default.Analytics)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HqPlaybackSheet(
    playerState: PlayerState,
    diagnostics: PlaybackDiagnostics,
    accentColor: Color,
    initialTab: HqSheetTab = HqSheetTab.VIDEO,
    onSelectQualityPreset: (QualityPreset) -> Unit,
    onSelectVideoFormat: (DeepEyeFormat) -> Unit,
    onSelectAudioFormat: (DeepEyeFormat) -> Unit,
    onSelectBufferProfile: (BufferProfile) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(initialTab) }

    LaunchedEffect(playerState.availableVideoFormats.size, playerState.availableAudioFormats.size) {
        val vCount = playerState.availableVideoFormats.count { !it.isAuto }
        val aCount = playerState.availableAudioFormats.count { !it.isAuto }
        android.util.Log.d("DeepEyeHQ", "event=sheet_render videoRows=$vCount audioRows=$aCount emptyReason=${if (vCount == 0 && aCount == 0) "No formats loaded" else null}")
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF121218),
        tonalElevation = 8.dp,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.3f))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "HQ Playback Engine",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "SmartTube Codec & Stream Pipeline",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.7f))
                }
            }

            if (playerState.isRecovering || playerState.recoveryMessage != null) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accentColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (playerState.isRecovering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = accentColor,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            text = playerState.recoveryMessage ?: "Refreshing stream…",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            HqTabBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                accentColor = accentColor
            )

            Spacer(Modifier.height(16.dp))

            Crossfade(targetState = selectedTab, label = "HqSheetContent") { tab ->
                when (tab) {
                    HqSheetTab.VIDEO -> VideoQualityTab(
                        playerState = playerState,
                        accentColor = accentColor,
                        onSelectQualityPreset = onSelectQualityPreset,
                        onSelectVideoFormat = onSelectVideoFormat
                    )
                    HqSheetTab.AUDIO -> AudioFormatTab(
                        playerState = playerState,
                        accentColor = accentColor,
                        onSelectAudioFormat = onSelectAudioFormat
                    )
                    HqSheetTab.BUFFER -> BufferProfileTab(
                        playerState = playerState,
                        diagnostics = diagnostics,
                        accentColor = accentColor,
                        onSelectBufferProfile = onSelectBufferProfile
                    )
                    HqSheetTab.STATS -> StatsForNerdsTab(
                        diagnostics = diagnostics,
                        accentColor = accentColor
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HqTabBar(
    selectedTab: HqSheetTab,
    onTabSelected: (HqSheetTab) -> Unit,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        HqSheetTab.values().forEach { tab ->
            val isSelected = tab == selectedTab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) accentColor else Color.Transparent)
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = null,
                        tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.sp
                        ),
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoQualityTab(
    playerState: PlayerState,
    accentColor: Color,
    onSelectQualityPreset: (QualityPreset) -> Unit,
    onSelectVideoFormat: (DeepEyeFormat) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Quick Presets
        item {
            Text(
                text = "STREAMING PRESET",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = Color.White.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(QualityPreset.AUTO, QualityPreset.HIGH_QUALITY, QualityPreset.BALANCED, QualityPreset.DATA_SAVER).forEach { preset ->
                    val isSelected = playerState.qualityPreset == preset
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectQualityPreset(preset) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) accentColor.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) accentColor else Color.White.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = preset.name.replace("_", " "),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = if (isSelected) accentColor else Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Available Video Tracks
        item {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "AVAILABLE VIDEO STREAMS",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = Color.White.copy(alpha = 0.5f)
            )
        }

        if (playerState.availableVideoFormats.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.03f)
                ) {
                    Text(
                        text = "Auto Track Selection Active (Media3 Adaptive Bitrate Engine)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(playerState.availableVideoFormats) { format ->
                val isSelected = (format.isAuto && playerState.selectedVideoFormat == null) ||
                    (playerState.selectedVideoFormat?.id == format.id)

                FormatSelectionCard(
                    format = format,
                    isSelected = isSelected,
                    accentColor = accentColor,
                    onClick = { onSelectVideoFormat(format) }
                )
            }
        }
    }
}
@Composable
private fun AudioFormatTab(
    playerState: PlayerState,
    accentColor: Color,
    onSelectAudioFormat: (DeepEyeFormat) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "AVAILABLE AUDIO STREAMS",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = Color.White.copy(alpha = 0.5f)
            )
        }

        if (playerState.availableAudioFormats.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.03f)
                ) {
                    Text(
                        text = "Auto High-Bitrate Opus Stream Active",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(playerState.availableAudioFormats) { format ->
                val isSelected = (format.isAuto && playerState.selectedAudioFormat == null) ||
                    (playerState.selectedAudioFormat?.id == format.id)

                FormatSelectionCard(
                    format = format,
                    isSelected = isSelected,
                    accentColor = accentColor,
                    onClick = { onSelectAudioFormat(format) }
                )
            }
        }
    }
}

@Composable
private fun BufferProfileTab(
    playerState: PlayerState,
    diagnostics: PlaybackDiagnostics,
    accentColor: Color,
    onSelectBufferProfile: (BufferProfile) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Live Buffer Health Meter
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Color.White.copy(alpha = 0.04f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = Color(0xFF00E676), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Real-Time Buffer Health", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Text(
                            text = diagnostics.formattedBufferHealth,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = Color(0xFF00E676)
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { (diagnostics.bufferedPercentage / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF00E676),
                        trackColor = Color.White.copy(alpha = 0.1f),
                    )

                    Spacer(Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Current: ${(diagnostics.bufferedDurationMs / 1000.0).toInt()}s buffered", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
                        Text("Target: ${playerState.bufferProfile.maxBufferMs / 1000}s cap", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
                    }
                }
            }
        }

        // Buffer Profiles Selection
        item {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "BUFFER & LATENCY PROFILE",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = Color.White.copy(alpha = 0.5f)
            )
        }

        items(BufferProfile.values()) { profile ->
            val isSelected = playerState.bufferProfile == profile
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectBufferProfile(profile) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) accentColor.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.03f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSelected) accentColor.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.06f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profile.displayName,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) accentColor else Color.White
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = profile.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
@Composable
private fun StatsForNerdsTab(
    diagnostics: PlaybackDiagnostics,
    accentColor: Color
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF0A0A0F),
                border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SMARTTUBE STATS HUD",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = accentColor
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF00E676).copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "LIVE TELEMETRY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color(0xFF00E676)
                            )
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 0.5.dp)

                    val videoFormat = diagnostics.activeVideoFormat
                    val audioFormat = diagnostics.activeAudioFormat

                    StatHudRow(
                        label = "Video Codec / Res",
                        value = if (videoFormat != null) "${videoFormat.codecName} @ ${videoFormat.formattedVideoResolution}" else "Adaptive (1080p)"
                    )
                    StatHudRow(
                        label = "Video Decoder",
                        value = diagnostics.activeVideoDecoder.ifEmpty { "c2.android.avc.decoder" },
                        highlight = diagnostics.isHardwareAccelerated
                    )
                    StatHudRow(
                        label = "Audio Codec / Stream",
                        value = if (audioFormat != null) audioFormat.formattedAudioSpec else "Opus 160 kbps • 48 kHz"
                    )
                    StatHudRow(
                        label = "Audio Decoder",
                        value = diagnostics.activeAudioDecoder.ifEmpty { "c2.android.opus.decoder" },
                        highlight = diagnostics.isHardwareAccelerated
                    )
                    StatHudRow(label = "Network Bandwidth", value = diagnostics.formattedBandwidth)
                    StatHudRow(
                        label = "Buffer Duration / Health",
                        value = "${diagnostics.formattedBufferHealth} (${(diagnostics.bufferedDurationMs / 1000.0).toInt()}s)"
                    )
                    StatHudRow(label = "Dropped Frames", value = "${diagnostics.droppedFrames}")
                    StatHudRow(label = "DSP Sink Pipeline", value = diagnostics.audioSinkSpec)
                }
            }
        }
    }
}

@Composable
private fun StatHudRow(
    label: String,
    value: String,
    highlight: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            color = Color.White.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            ),
            color = if (highlight) Color(0xFF00E676) else Color.White
        )
    }
}

@Composable
private fun FormatSelectionCard(
    format: DeepEyeFormat,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val title = if (format.type == com.deepeye.musicpro.player.format.FormatType.VIDEO) {
        format.formattedVideoResolution
    } else {
        format.formattedAudioSpec
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) accentColor.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.03f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) accentColor.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.06f)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isSelected) accentColor else Color.White
                    )
                    if (format.isHardwareAccelerated && !format.isAuto) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF00E676).copy(alpha = 0.15f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "HW ACCEL",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color(0xFF00E676)
                            )
                        }
                    }
                    if (format.isHdr) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFFFB300).copy(alpha = 0.15f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "HDR",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color(0xFFFFB300)
                            )
                        }
                    }
                }
                if (!format.isAuto) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = listOf(format.codecName, format.container, format.formattedBitrate)
                            .filter { it.isNotEmpty() }
                            .joinToString(" • "),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
