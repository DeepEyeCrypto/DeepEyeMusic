// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepeye.musicpro.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenContentScreen(
    onNavigateBack: () -> Unit,
    viewModel: HiddenContentViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissMessage()
        }
    }

    Scaffold(
        containerColor = RichBlack,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Hidden Content", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary)) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics { contentDescription = "Navigate back" }
                    ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextPrimary) }
                },
                actions = {
                    val total = uiState.hiddenSongs.size + uiState.hiddenArtists.size
                    if (total > 0) {
                        TextButton(onClick = { viewModel.requestRestoreAll() }, modifier = Modifier.semantics { contentDescription = "Restore all hidden items" }) {
                            Text("Restore All", color = NeonCyan, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RichBlack, titleContentColor = TextPrimary)
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(GraphiteGlassElevated).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TabButton("Songs (${uiState.hiddenSongs.size})", uiState.selectedTab == HiddenContentTab.SONGS, Modifier.weight(1f)) { viewModel.selectTab(HiddenContentTab.SONGS) }
                TabButton("Artists (${uiState.hiddenArtists.size})", uiState.selectedTab == HiddenContentTab.ARTISTS, Modifier.weight(1f)) { viewModel.selectTab(HiddenContentTab.ARTISTS) }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(if (uiState.selectedTab == HiddenContentTab.SONGS) "Search hidden songs..." else "Search hidden artists...", color = TextTertiary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextTertiary) },
                trailingIcon = if (uiState.searchQuery.isNotEmpty()) { { IconButton(onClick = { viewModel.setSearchQuery("") }) { Icon(Icons.Default.Close, contentDescription = "Clear search", tint = TextTertiary) } } } else null,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricViolet,
                    unfocusedBorderColor = GlassBorderLight,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = GraphiteGlass,
                    unfocusedContainerColor = GraphiteGlass
                ),
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            when (uiState.selectedTab) {
                HiddenContentTab.SONGS -> HiddenSongsList(uiState = uiState, viewModel = viewModel)
                HiddenContentTab.ARTISTS -> HiddenArtistsList(uiState = uiState, viewModel = viewModel)
            }
uiState.activeConfirmation?.let { conf ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissConfirmation() },
            title = { Text(conf.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary) },
            text = { Text(conf.message, style = MaterialTheme.typography.bodyMedium, color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = { conf.onConfirm() },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) { Text(conf.confirmLabel, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissConfirmation() }) { Text("Cancel", color = TextSecondary) }
            },
            containerColor = GraphiteGlassElevated,
            shape = RoundedCornerShape(20.dp)
        )
    }
        }
    }
}