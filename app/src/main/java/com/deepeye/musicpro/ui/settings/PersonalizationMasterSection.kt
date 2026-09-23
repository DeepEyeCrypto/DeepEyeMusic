// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.musicpro.ui.theme.*

private val NeonCyan = Color(0xFF00E5FF)
private val DarkSurfaceCard = Color(0xFF0C0F17)
private val GlassBorder = Color(0x22FFFFFF)

@Composable
fun PersonalizationMasterSection(
    uiState: PersonalizationSettingsUiState,
    viewModel: PersonalizationSettingsViewModel,
) {
    val prefs = uiState.preferences

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = DarkSurfaceCard,
        border = BorderStroke(1.2.dp, GlassBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(NeonCyan)
                )
                Text(
                    "Personalization Engine",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    letterSpacing = 0.5.sp
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Real-time dynamic feed ranking & tailored shelf discovery algorithms.",
                color = Color.White.copy(alpha = 0.60f),
                fontSize = 13.sp,
                lineHeight = 17.sp
            )

            Spacer(Modifier.height(16.dp))

            // Master Switch Highlight Capsule
            PersonalizationSwitchRow(
                title = "Enable Personalized Music",
                subtitle = "Master switch for neural ranking, custom shelves & smart recommendations",
                icon = Icons.Default.AutoAwesome,
                checked = prefs.enablePersonalization,
                onCheckedChange = { viewModel.setPersonalizationEnabled(it) }
            )

            AnimatedVisibility(
                visible = prefs.enablePersonalization,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(top = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.08f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    PersonalizationSwitchRow(
                        title = "Use Listening History",
                        subtitle = "Build Continue Listening, Recently Played, and Affinity shelves",
                        icon = Icons.Default.History,
                        checked = prefs.enableLocalListeningSections,
                        onCheckedChange = { viewModel.setUseLocalHistory(it) }
                    )

                    PersonalizationSwitchRow(
                        title = "Use Recent Searches",
                        subtitle = "Shape discovery suggestions based on recent search queries",
                        icon = Icons.Default.Search,
                        checked = prefs.recentSearchInfluence,
                        onCheckedChange = { viewModel.setUseRecentSearches(it) }
                    )

                    PersonalizationSwitchRow(
                        title = "Show \"Based on your listening\"",
                        subtitle = "Include algorithmic local mix based on frequent genres",
                        icon = Icons.Default.GraphicEq,
                        checked = prefs.enableLocalMix,
                        onCheckedChange = { viewModel.setLocalMixEnabled(it) }
                    )

                    PersonalizationSwitchRow(
                        title = "Show Trending Music",
                        subtitle = "Include regional and global trending charts in your feed",
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        checked = prefs.enableTrending,
                        onCheckedChange = { viewModel.setTrendingEnabled(it) }
                    )

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
