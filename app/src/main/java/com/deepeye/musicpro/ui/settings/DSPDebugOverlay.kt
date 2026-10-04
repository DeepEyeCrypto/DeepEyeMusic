// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.musicpro.dsp.engine.DSPViewModel

@Composable
fun DSPDebugOverlay(
    viewModel: DSPViewModel,
    modifier: Modifier = Modifier
) {
    val diagnostics by viewModel.diagnostics.collectAsState()

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "V4A DSP Telemetry",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            // State & Session
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TelemetryPoint("Session ID", "${diagnostics.sessionId.takeIf { it > 0 } ?: "DISCONNECTED"}")
                TelemetryPoint("Engine State", diagnostics.state.name)
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Routing & Headroom
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TelemetryPoint("Audio Route", diagnostics.route.name)
                TelemetryPoint("Gain Ctrl", "${String.format("%.1f", diagnostics.gainBudget.totalDb)} dB")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TelemetryPoint("Clip Risk", diagnostics.gainBudget.risk.name)
                TelemetryPoint("Active Preset", diagnostics.preset)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Verbose Logging", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text("Reroutes buffer telemetry to Logcat under [V4A DEBUG]", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = diagnostics.isVerboseLoggingEnabled,
                    onCheckedChange = { viewModel.toggleVerboseLogging() }
                )
            }
        }
    }
}

@Composable
private fun TelemetryPoint(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)
    }
}
