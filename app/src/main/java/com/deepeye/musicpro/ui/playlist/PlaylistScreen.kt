// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.playlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepeye.musicpro.ui.theme.AppAlertDialog
import com.deepeye.musicpro.ui.theme.CardGeometry

@Composable
fun PlaylistScreen(
    onNavigateToPlaylist: (Long) -> Unit = {},
    viewModel: PlaylistViewModel = hiltViewModel()
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Playlists", style = MaterialTheme.typography.headlineLarge)
            IconButton(onClick = { showCreateDialog = true }) { Icon(Icons.Filled.Add, "Create Playlist") }
        }

        if (playlists.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No playlists yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyVerticalGrid(
                // Adaptive, not Fixed(2). A hardcoded 2 columns forced exactly two
                // huge cards on a landscape phone; Adaptive fits as many tiles as
                // the width allows and keeps a partial final row tidy.
                columns = GridCells.Adaptive(CardGeometry.Compact.minWidth),
                contentPadding = PaddingValues(
                    start = CardGeometry.ScreenGutter,
                    top = CardGeometry.ScreenGutter,
                    end = CardGeometry.ScreenGutter,
                    bottom = 90.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(CardGeometry.CardSpacing),
                verticalArrangement = Arrangement.spacedBy(CardGeometry.CardSpacing),
            ) {
                items(playlists, key = { it.id }) { playlist ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CardGeometry.Compact.shape)
                            .clickable { onNavigateToPlaylist(playlist.id) },
                        shape = CardGeometry.Compact.shape,
                    ) {
                        Column(
                            Modifier.padding(CardGeometry.Compact.contentPadding * 2)
                        ) {
                            Text(
                                playlist.name,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "${playlist.songCount} songs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AppAlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Playlist") },
            text = {
                OutlinedTextField(value = newPlaylistName, onValueChange = {
                    newPlaylistName = it
                }, placeholder = { Text("Playlist name") }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.createPlaylist(newPlaylistName)
                    newPlaylistName = ""
                    showCreateDialog = false
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") } },
        )
    }
}
