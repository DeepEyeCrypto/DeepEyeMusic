// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.homehub.video

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun VideoRail(
    modifier: Modifier = Modifier,
    viewModel: VideoRailViewModel = hiltViewModel(),
    onNavigateToVideo: (String) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val expandedId by viewModel.expandedCardId.collectAsState()

    // Section tab pills (Trending / Because You Watched / Top Charts)
    val activeSection = state.activeSection
    val activeSections = state.sections

    Column(modifier.fillMaxWidth()) {

        // ── Section filter pills ────────────────
        if (activeSections.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                items(activeSections) { section ->
                    val isActive = section.category == activeSection
                    FilterChip(
                        selected = isActive,
                        onClick = { viewModel.switchSection(section.category) },
                        label = {
                            Text(
                                section.title,
                                fontSize = 12.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = section.accentColor.copy(alpha = 0.2f),
                            selectedLabelColor = section.accentColor,
                            selectedLeadingIconColor = section.accentColor
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isActive,
                            selectedBorderColor = section.accentColor.copy(0.4f),
                            borderColor = Color.White.copy(0.08f)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
        }

        // ── Active section row ──────────────────
        val currentSection = activeSections.find { it.category == activeSection }
            ?: activeSections.firstOrNull()

        currentSection?.let { section ->
            // Section header
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        section.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        section.subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(0.4f)
                    )
                }
                TextButton(onClick = { /* see all */ }) {
                    Text(
                        "See all",
                        color = section.accentColor,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Video cards
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    section.items,
                    key = { it.videoId }
                ) { item ->
                    VideoRailCard(
                        item = item,
                        isExpanded = expandedId == item.videoId,
                        onTap = { viewModel.onCardTap(item.videoId) },
                        onExpandedTap = {
                            viewModel.onExpandedCardTap(item.videoId)
                            onNavigateToVideo(item.videoId)
                        }
                    )
                }
            }
        }

        // Loading shimmer
        if (state.isLoading) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(160.dp)
            ) {
                items(6) { ShimmerVideoCard(Modifier.width(180.dp)) }
            }
        }

        // Error state
        state.error?.let { error ->
            Row(
                Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.WifiOff, null,
                    tint = Color.White.copy(0.3f), modifier = Modifier.size(16.dp)
                )
                Text(
                    error,
                    color = Color.White.copy(0.3f),
                    fontSize = 12.sp
                )
                TextButton(onClick = { viewModel.loadRail() }) {
                    Text("Retry", color = Color(0xFF00E5FF), fontSize = 12.sp)
                }
            }
        }
    }
}
