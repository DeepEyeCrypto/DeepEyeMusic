// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deepeye.musicpro.ui.components.GlowCard
import com.deepeye.musicpro.ui.theme.*

@Composable
fun PrivacyAndDataSection(
    uiState: PersonalizationSettingsUiState,
    viewModel: PersonalizationSettingsViewModel,
    onNavigateToHiddenContent: () -> Unit,
) {
    val totalHidden = uiState.hiddenItemCount + uiState.hiddenArtistCount

    GlowCard(
        modifier = Modifier.fillMaxWidth(),
        glowColor = GlowPink.copy(alpha = 0.25f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Privacy and Data", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
            Spacer(Modifier.height(4.dp))
            Text("Listening-based suggestions are processed locally on this device.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(Modifier.height(14.dp))

            Surface(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onNavigateToHiddenContent() },
                color = GraphiteGlassElevated
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Manage Hidden Songs & Artists", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, color = TextPrimary))
                        Text("$totalHidden hidden item(s) \u2022 Process-local", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Open hidden content manager", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = { viewModel.clearPersonalizationCacheRequested() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderLight)
            ) {
                Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Clear Personalized Cache", fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = { viewModel.clearLocalHistoryRequested() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderLight)
            ) {
                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Clear Local Listening History", fontWeight = FontWeight.SemiBold)
            }

            if (totalHidden > 0) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { viewModel.clearHiddenContentRequested() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Restore All Hidden Content ($totalHidden)", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun RefreshSection(
    uiState: PersonalizationSettingsUiState,
    viewModel: PersonalizationSettingsViewModel,
) {
    GlowCard(
        modifier = Modifier.fillMaxWidth(),
        glowColor = GlowTeal.copy(alpha = 0.25f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Refresh and Offline Cache", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
            Spacer(Modifier.height(4.dp))
            Text("Cached sections remain available when you are offline.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)

            uiState.cacheSummary?.let { summary ->
                Spacer(Modifier.height(8.dp))
                Text(text = "Sections: ${summary.cachedSectionCount} \u2022 Items: ${summary.totalCachedItems}", style = MaterialTheme.typography.bodySmall, color = NeonCyan)
            }

            Spacer(Modifier.height(14.dp))

            Button(
                onClick = { viewModel.refreshPersonalizedFeed() },
                enabled = !uiState.isRefreshing,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GlowTeal, contentColor = Color.Black)
            ) {
                if (uiState.isRefreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.Black)
                    Spacer(Modifier.width(8.dp))
                    Text("Refreshing Feed...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Refresh Personalized Feed", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DeveloperDiagnosticsSection(
    onNavigateToDiagnostics: () -> Unit,
) {
    GlowCard(
        modifier = Modifier.fillMaxWidth(),
        glowColor = ElectricViolet.copy(alpha = 0.2f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.BugReport, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Developer Diagnostics", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
            }
            Spacer(Modifier.height(4.dp))
            Text("Debug-only tool for observing cache tiers, TTL states, and diversity metrics.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onNavigateToDiagnostics,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Open Personalization Diagnostics", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
