// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.diagnostics.research

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Debug-only YT research probe card (plan Task 4).
 *
 * Renders nothing in release builds. Trigger status is non-sensitive:
 * endpoint names, capture counts, and the seed videoId only.
 */
@Composable
fun YtResearchDebugCard(
    viewModel: YtResearchDebugViewModel,
    modifier: Modifier = Modifier
) {
    val status by viewModel.status.collectAsState()
    val running by viewModel.running.collectAsState()

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "YT Research Probe (debug)",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                status,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Row {
                Button(
                    onClick = { viewModel.triggerResearch("dQw4w9WgXcQ") },
                    enabled = !running,
                ) {
                    Text("Run Probe")
                }
            }
        }
    }
}
