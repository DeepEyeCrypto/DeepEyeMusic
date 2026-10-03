// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deepeye.musicpro.ui.components.GlowCard
import com.deepeye.musicpro.ui.theme.*

val AVAILABLE_REGIONS = listOf("US" to "United States", "IN" to "India", "GB" to "UK", "CA" to "Canada", "AU" to "Australia", "JP" to "Japan", "KR" to "Korea", "DE" to "Germany", "FR" to "France", "BR" to "Brazil")
val POPULAR_LANGUAGES = listOf("English", "Hindi", "Punjabi", "Spanish", "Korean", "Japanese", "Tamil", "Telugu", "French", "German")
val GENRE_MOOD_OPTIONS = listOf("Chill", "Workout", "Focus", "Party", "Romance", "Retro", "Pop", "Hip-Hop", "Rock", "Electronic", "Instrumental", "Devotional")
val DIVERSITY_LIMITS = listOf(1, 2, 3, 4, 5)
val COOLDOWN_OPTIONS = listOf(1 to "1 Hour", 24 to "1 Day", 168 to "7 Days", 720 to "30 Days")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiscoveryPreferencesSection(uiState: PersonalizationSettingsUiState, viewModel: PersonalizationSettingsViewModel) {
    val prefs = uiState.preferences
    GlowCard(modifier = Modifier.fillMaxWidth(), glowColor = GlowOrange.copy(alpha = 0.25f)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Discovery Preferences", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
            Spacer(Modifier.height(4.dp))
            Text("Customize languages, moods, artist diversity, and skip cooldowns.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(Modifier.height(14.dp))

            Text("Trending Region", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold, color = TextPrimary))
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 2.dp)) {
                items(AVAILABLE_REGIONS) { (code, name) ->
                    val isSelected = prefs.trendingRegion.equals(code, ignoreCase = true)
                    FilterChip(selected = isSelected, onClick = { viewModel.setCountryOverride(code) }, label = { Text("$code - $name") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ElectricViolet, selectedLabelColor = Color.White, containerColor = GraphiteGlassElevated, labelColor = TextSecondary), shape = RoundedCornerShape(10.dp))
                }
            }

            Spacer(Modifier.height(14.dp))

            Text("Preferred Music Languages", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold, color = TextPrimary))
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                POPULAR_LANGUAGES.forEach { lang ->
                    val isSelected = lang in uiState.preferredLanguages
                    FilterChip(selected = isSelected, onClick = { viewModel.toggleLanguage(lang) }, label = { Text(lang) }, leadingIcon = if (isSelected) { { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) } } else null, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ElectricViolet, selectedLabelColor = Color.White, containerColor = GraphiteGlassElevated, labelColor = TextSecondary), shape = RoundedCornerShape(10.dp))
                }
            Spacer(Modifier.height(14.dp))

            Text("Genre & Mood Preferences", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold, color = TextPrimary))
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GENRE_MOOD_OPTIONS.forEach { genre ->
                    val isSelected = genre in uiState.preferredGenres
                    FilterChip(selected = isSelected, onClick = { viewModel.toggleGenre(genre) }, label = { Text(genre) }, leadingIcon = if (isSelected) { { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) } } else null, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GlowOrange.copy(alpha = 0.85f), selectedLabelColor = Color.White, containerColor = GraphiteGlassElevated, labelColor = TextSecondary), shape = RoundedCornerShape(10.dp))
                }
            }

            Spacer(Modifier.height(14.dp))

            Text("Artist Diversity Limit (Max repeats: ${prefs.maxRepeatedArtistPerSection})", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold, color = TextPrimary))
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DIVERSITY_LIMITS.forEach { limit ->
                    val isSelected = prefs.maxRepeatedArtistPerSection == limit
                    FilterChip(selected = isSelected, onClick = { viewModel.setArtistDiversityLimit(limit) }, label = { Text("$limit") }, modifier = Modifier.weight(1f), colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NeonCyan.copy(alpha = 0.85f), selectedLabelColor = Color.Black, containerColor = GraphiteGlassElevated, labelColor = TextSecondary), shape = RoundedCornerShape(10.dp))
                }
            }

            Spacer(Modifier.height(14.dp))

            Text("Recently Skipped Song Cooldown", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold, color = TextPrimary))
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                COOLDOWN_OPTIONS.forEach { (hours, label) ->
                    val isSelected = prefs.recentlySkippedCooldownHours == hours
                    FilterChip(selected = isSelected, onClick = { viewModel.setRecentlySkippedCooldown(hours) }, label = { Text(label) }, modifier = Modifier.weight(1f), colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ElectricViolet, selectedLabelColor = Color.White, containerColor = GraphiteGlassElevated, labelColor = TextSecondary), shape = RoundedCornerShape(10.dp))
                }
            }

            }
        }
    }
}
