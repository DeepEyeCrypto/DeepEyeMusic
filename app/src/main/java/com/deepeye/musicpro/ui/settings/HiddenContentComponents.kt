// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deepeye.musicpro.data.cache.HiddenItemEntry
import com.deepeye.musicpro.ui.components.GlowCard
import com.deepeye.musicpro.ui.theme.*

@Composable
fun TabButton(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(38.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) ElectricViolet else Color.Transparent,
            contentColor = if (isSelected) Color.White else TextSecondary
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        elevation = if (isSelected) ButtonDefaults.buttonElevation(defaultElevation = 2.dp) else ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        )
    }
}

@Composable
fun EmptyHiddenState(message: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Visibility, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text(text = message, style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
        }
    }
}

@Composable
fun HiddenSongsList(
    uiState: HiddenContentUiState,
    viewModel: HiddenContentViewModel,
) {
    val filteredSongs = remember(uiState.hiddenSongs, uiState.searchQuery) {
        if (uiState.searchQuery.isBlank()) uiState.hiddenSongs
        else uiState.hiddenSongs.filter {
            it.title.contains(uiState.searchQuery, ignoreCase = true) ||
                (it.artist?.contains(uiState.searchQuery, ignoreCase = true) == true)
        }
    }

    if (filteredSongs.isEmpty()) {
        EmptyHiddenState(message = if (uiState.searchQuery.isBlank()) "No hidden songs." else "No matching songs found.")
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(filteredSongs, key = { it.id }) { song ->
                HiddenSongRow(entry = song, onRestore = { viewModel.restoreItem(song.id) })
            }
        }
    }
}

@Composable
fun HiddenArtistsList(
    uiState: HiddenContentUiState,
    viewModel: HiddenContentViewModel,
) {
    val filteredArtists = remember(uiState.hiddenArtists, uiState.searchQuery) {
        if (uiState.searchQuery.isBlank()) uiState.hiddenArtists
        else uiState.hiddenArtists.filter {
            it.contains(uiState.searchQuery, ignoreCase = true)
        }
    }

    if (filteredArtists.isEmpty()) {
        EmptyHiddenState(message = if (uiState.searchQuery.isBlank()) "No hidden artists or channels." else "No matching artists found.")
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(filteredArtists, key = { it }) { artist ->
                HiddenArtistRow(artistName = artist, onRestore = { viewModel.restoreArtist(artist) })
            }
        }
    }
}

@Composable
fun HiddenSongRow(
    entry: HiddenItemEntry,
    onRestore: () -> Unit,
) {
    GlowCard(
        modifier = Modifier.fillMaxWidth(),
        glowColor = ElectricViolet.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.MusicNote, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = entry.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary, maxLines = 1)
                entry.artist?.let { artist ->
                    Text(text = artist, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1)
                }
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onRestore,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GraphiteGlassElevated, contentColor = NeonCyan),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.semantics { contentDescription = "Restore song ${entry.title}" }
            ) {
                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Restore", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}

@Composable
fun HiddenArtistRow(
    artistName: String,
    onRestore: () -> Unit,
) {
    GlowCard(
        modifier = Modifier.fillMaxWidth(),
        glowColor = ElectricViolet.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Person, contentDescription = null, tint = GlowOrange, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = artistName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary, maxLines = 1)
                Text(text = "Artist / Channel", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onRestore,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GraphiteGlassElevated, contentColor = NeonCyan),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.semantics { contentDescription = "Restore artist $artistName" }
            ) {
                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Restore", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}
