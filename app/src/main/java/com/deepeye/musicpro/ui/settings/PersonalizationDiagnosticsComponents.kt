// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deepeye.musicpro.domain.model.personalization.PersonalizedFeedState
import com.deepeye.musicpro.domain.personalization.SectionDiagnostics
import com.deepeye.musicpro.ui.components.GlowCard
import com.deepeye.musicpro.ui.theme.*

@Composable
fun DiagnosticsSummaryCard(
    feedState: PersonalizedFeedState,
    diagnostics: List<SectionDiagnostics>,
) {
    GlowCard(
        modifier = Modifier.fillMaxWidth(),
        glowColor = ElectricViolet.copy(alpha = 0.25f)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                "Feed State Summary",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Sections: ${feedState.sections.size}", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                Text("Total Diagnostics: ${diagnostics.size}", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val status = when {
                    feedState.isLoading -> "Loading"
                    feedState.sections.any { it.error != null } -> "Error"
                    else -> "Fresh"
                }
                Text("Feed Status: $status", color = NeonCyan, style = MaterialTheme.typography.bodyMedium)
                val errorCount = feedState.sections.count { it.error != null }
                Text("Errors: $errorCount", color = if (errorCount > 0) GlowOrange else TextSecondary, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun DiagnosticsSectionCard(diag: SectionDiagnostics) {
    GlowCard(
        modifier = Modifier.fillMaxWidth(),
        glowColor = when (diag.source) {
            SectionDiagnostics.CacheSource.LIVE -> NeonCyan.copy(alpha = 0.25f)
            SectionDiagnostics.CacheSource.MEMORY_CACHE -> ElectricViolet.copy(alpha = 0.25f)
            SectionDiagnostics.CacheSource.ROOM_CACHE -> GlowTeal.copy(alpha = 0.25f)
            SectionDiagnostics.CacheSource.LOCAL_ONLY -> GlowOrange.copy(alpha = 0.25f)
        }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = diag.sectionTitle, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                Surface(
                    color = when (diag.source) {
                        SectionDiagnostics.CacheSource.LIVE -> NeonCyan.copy(alpha = 0.2f)
                        SectionDiagnostics.CacheSource.MEMORY_CACHE -> ElectricViolet.copy(alpha = 0.2f)
                        SectionDiagnostics.CacheSource.ROOM_CACHE -> GlowTeal.copy(alpha = 0.2f)
                        SectionDiagnostics.CacheSource.LOCAL_ONLY -> GlowOrange.copy(alpha = 0.2f)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = diag.source.name,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = when (diag.source) {
                            SectionDiagnostics.CacheSource.LIVE -> NeonCyan
                            SectionDiagnostics.CacheSource.MEMORY_CACHE -> ElectricViolet
                            SectionDiagnostics.CacheSource.ROOM_CACHE -> GlowTeal
                            SectionDiagnostics.CacheSource.LOCAL_ONLY -> GlowOrange
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Section ID: ${diag.sectionId}", color = TextTertiary, style = MaterialTheme.typography.bodySmall)
                Text("Items: ${diag.itemCount}", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(4.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Cache Age: ${diag.formatCacheAge()}", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                Text("State: ${diag.refreshState.name}", color = if (diag.refreshState == SectionDiagnostics.RefreshState.FRESH) GlowTeal else GlowOrange, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(4.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Diversity Reranked: ${if (diag.rebuiltByDiversityRanker) "Yes" else "No"}", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                Text("Pruned Hidden: ${diag.hiddenItemsPruned}", color = if (diag.hiddenItemsPruned > 0) GlowPink else TextSecondary, style = MaterialTheme.typography.bodySmall)
            }

            diag.reason?.let { reason ->
                Spacer(Modifier.height(6.dp))
                Text(text = "Reason: $reason", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
            }
        }
    }
}
