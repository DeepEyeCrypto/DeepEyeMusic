// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.dsp.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.musicpro.BuildConfig
import com.deepeye.musicpro.dsp.model.AudioRoute
import com.deepeye.musicpro.dsp.model.GainBudget
import com.deepeye.musicpro.dsp.model.RiskLevel

/**
 * A developer debug HUD component that displays real-time internal DSP engine telemetry.
 * Shows active master preset, session ID, audio route, gain headroom budget, and active modules.
 */
@Composable
fun DspDebugCard(
    sessionId: Int,
    gainBudget: GainBudget,
    activeModules: List<String>,
    currentRoute: AudioRoute,
    activePresetName: String = "",
    modifier: Modifier = Modifier,
) {
    if (!BuildConfig.DEBUG) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors =
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        ),
        border = BorderStroke(1.dp, Color(0xFFFF5900).copy(alpha = 0.5f)),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "⚙️ DSP DEBUG HUD",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFF5900),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
                if (activePresetName.isNotBlank()) {
                    Text(
                        "🎛️ $activePresetName",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF00E5FF),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFFF5900).copy(alpha = 0.2f))

            // Session ID
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Session:",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    if (sessionId != 0) {
                        "✅ $sessionId (V4A Engine Attached)"
                    } else {
                        "❌ NOT ATTACHED"
                    },
                    color =
                    if (sessionId != 0) {
                        Color(0xFF00E676)
                    } else {
                        Color(0xFFFF4B4B)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                )
            }

            // Route
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Route:",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    currentRoute.name,
                    color = Color(0xFF69F0AE),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            // Headroom / Gain Budget
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Gain Headroom:",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "${"%.1f".format(gainBudget.totalDb)} dB — ${gainBudget.risk}",
                    color =
                    when (gainBudget.risk) {
                        RiskLevel.SAFE -> Color(0xFF00E676)
                        RiskLevel.MODERATE -> Color(0xFFFFD600)
                        RiskLevel.DANGER -> Color(0xFFFF4B4B)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                )
            }

            // Active modules
            if (activeModules.isNotEmpty()) {
                Text(
                    "Active Modules: ${activeModules.joinToString(" · ")}",
                    color = Color(0xFF69F0AE),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
