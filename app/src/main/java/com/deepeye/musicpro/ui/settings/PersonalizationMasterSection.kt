// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deepeye.musicpro.ui.components.GlowCard
import com.deepeye.musicpro.ui.theme.*

@Composable
fun PersonalizationMasterSection(
    uiState: PersonalizationSettingsUiState,
    viewModel: PersonalizationSettingsViewModel,
) {
    val prefs = uiState.preferences
    GlowCard(
        modifier = Modifier.fillMaxWidth(),
        glowColor = ElectricViolet.copy(alpha = 0.35f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Personalization",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Controls recommendations across your Music feed.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(Modifier.height(12.dp))

            PersonalizationSwitchRow(
                title = "Enable Personalized Music",
                subtitle = "Master switch for personalized feeds and tailored shelves",
                icon = Icons.Default.AutoAwesome,
                checked = prefs.enablePersonalization,
                onCheckedChange = { viewModel.setPersonalizationEnabled(it) }
            )

            AnimatedVisibility(visible = prefs.enablePersonalization) {
                Column {
                    HorizontalDivider(
                        color = GlassBorderLight,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )

                    PersonalizationSwitchRow(
                        title = "Use Listening History",
                        subtitle = "Build Continue Listening, Recently Played, and Affinity shelves",
                        icon = Icons.Default.History,
                        checked = prefs.enableLocalListeningSections,
                        onCheckedChange = { viewModel.setUseLocalHistory(it) }
                    )

                    Spacer(Modifier.height(10.dp))

                    PersonalizationSwitchRow(
                        title = "Use Recent Searches",
                        subtitle = "Shape discovery suggestions based on recent search terms",
                        icon = Icons.Default.Search,
                        checked = prefs.recentSearchInfluence,
                        onCheckedChange = { viewModel.setUseRecentSearches(it) }
                    )

                    Spacer(Modifier.height(10.dp))

                    PersonalizationSwitchRow(
                        title = "Show \"Based on your listening\"",
                        subtitle = "Include algorithmic local mix based on frequent genres",
                        icon = Icons.Default.GraphicEq,
                        checked = prefs.enableLocalMix,
                        onCheckedChange = { viewModel.setLocalMixEnabled(it) }
                    )

                    Spacer(Modifier.height(10.dp))

                    PersonalizationSwitchRow(
                        title = "Show Trending Music",
                        subtitle = "Include regional trending music in your feed",
                        icon = Icons.Default.TrendingUp,
                        checked = prefs.enableTrending,
                        onCheckedChange = { viewModel.setTrendingEnabled(it) }
                    )

                    Spacer(Modifier.height(10.dp))

                    PersonalizationSwitchRow(
                        title = "Hide Non-Music Content",
                        subtitle = "Filter out videos, podcasts, and shorts from music sections",
                        icon = Icons.Default.MusicNote,
                        checked = prefs.hideNonMusicContent,
                        onCheckedChange = { viewModel.setHideNonMusic(it) }
                    )
                }
            }
        }
    }
}
