// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.homehub

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Compact DSP Quick Panel for HomeHub — shows active preset and quick toggle.
 * One-tap opens the full V4A DSP screen.
 */
@Composable
fun DspQuickPanel(
    activePresetName: String?,
    isEngineAttached: Boolean,
    onOpenDsp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accentColor = Color(0xFF7B3FE4)
    val statusColor by animateColorAsState(
        targetValue = if (isEngineAttached) Color(0xFF4CAF50) else Color(0xFF9E9E9E),
        label = "dspStatusColor",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.12f),
                        Color(0xFF00E5FF).copy(alpha = 0.08f),
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.3f),
                        Color(0xFF00E5FF).copy(alpha = 0.15f),
                    )
                ),
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onOpenDsp)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // DSP Icon
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = accentColor.copy(alpha = 0.2f),
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "DSP Engine",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.padding(10.dp),
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "V4A DSP Engine",
                            style = MaterialTheme.typography.titleSmall,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                        )
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = if (isEngineAttached) "Active" else "Inactive",
                            tint = statusColor,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    Text(
                        text = activePresetName ?: "No preset active",
                        style = MaterialTheme.typography.bodySmall,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
            }

            // Open full DSP button
            Surface(
                modifier = Modifier.size(36.dp),
                shape = RoundedCornerShape(10.dp),
                color = accentColor.copy(alpha = 0.15f),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = "Open DSP",
                    tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }
    }
}
