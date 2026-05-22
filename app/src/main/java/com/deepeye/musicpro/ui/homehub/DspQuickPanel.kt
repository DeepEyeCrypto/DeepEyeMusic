// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.homehub

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.deepeye.musicpro.dsp.model.AudioRoute
import com.deepeye.musicpro.dsp.model.GainBudget
import com.deepeye.musicpro.dsp.model.RiskLevel
import com.deepeye.musicpro.ui.components.MiniVisualizerBar

@Composable
fun DspQuickPanel(
    currentPreset: String,
    gainBudget: GainBudget,
    audioRoute: AudioRoute,
    fftData: FloatArray,
    onPresetClick: () -> Unit,
    onV4AOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val riskColor = when (gainBudget.risk) {
        RiskLevel.SAFE     -> Color(0xFF00E676)
        RiskLevel.MODERATE -> Color(0xFFFFD600)
        RiskLevel.DANGER   -> Color(0xFFFF4B4B)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F0F1A)
        ),
        border = BorderStroke(1.dp, Color(0xFF7B3FE4).copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Pulsing dot
                    PulsingDot(color = riskColor)
                    Text(
                        "DeepEye DSP",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFFE0E0E0),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                IconButton(
                    onClick = onV4AOpen,
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            Color(0xFF7B3FE4).copy(alpha = 0.15f),
                            RoundedCornerShape(8.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = "Open V4A",
                        tint = Color(0xFF7B3FE4),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Mini Visualizer
            MiniVisualizerBar(
                fftData = fftData,
                barColor = Color(0xFF7B3FE4),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
            )

            Spacer(Modifier.height(12.dp))

            // Info row: preset + route + gain
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Current preset chip
                QuickChip(
                    icon = Icons.Rounded.GraphicEq,
                    label = currentPreset,
                    color = Color(0xFF7B3FE4),
                    onClick = onPresetClick,
                    modifier = Modifier.weight(1f)
                )

                // Audio route chip
                QuickChip(
                    icon = when (audioRoute) {
                        AudioRoute.WIRED_HEADSET   -> Icons.Rounded.Headset
                        AudioRoute.BLUETOOTH_A2DP  -> Icons.Rounded.BluetoothAudio
                        AudioRoute.SPEAKER         -> Icons.Rounded.VolumeUp
                        AudioRoute.USB_AUDIO       -> Icons.Rounded.Usb
                        AudioRoute.UNKNOWN         -> Icons.Rounded.DeviceUnknown
                    },
                    label = audioRoute.name
                        .replace("_", " ")
                        .lowercase()
                        .replaceFirstChar { it.uppercase() },
                    color = Color(0xFF00BCD4),
                    onClick = {},
                    modifier = Modifier.wrapContentWidth()
                )

                // GainBudget chip
                QuickChip(
                    icon = Icons.Rounded.Shield,
                    label = "${"%.0f".format(gainBudget.totalDb)}dB",
                    color = riskColor,
                    onClick = onV4AOpen,
                    modifier = Modifier.wrapContentWidth()
                )
            }

            // Quick toggle row
            Spacer(Modifier.height(12.dp))
            QuickToggleRow(onV4AOpen = onV4AOpen)
        }
    }
}

@Composable
private fun QuickChip(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, null, tint = color,
                modifier = Modifier.size(14.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun QuickToggleRow(onV4AOpen: () -> Unit) {
    val quickLabels = listOf("Bass", "Clarity", "Surround", "Night", "Flat")
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        items(quickLabels) { label ->
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF7B3FE4).copy(alpha = 0.1f),
                border = BorderStroke(1.dp, Color(0xFF7B3FE4).copy(alpha = 0.3f)),
                modifier = Modifier.clickable { onV4AOpen() }
            ) {
                Text(
                    label,
                    modifier = Modifier.padding(
                        horizontal = 12.dp, vertical = 6.dp
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF9E9E9E)
                )
            }
        }
    }
}

@Composable
private fun PulsingDot(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )
    Box(
        modifier = Modifier
            .size(8.dp)
            .background(color.copy(alpha = alpha), CircleShape)
    )
}
