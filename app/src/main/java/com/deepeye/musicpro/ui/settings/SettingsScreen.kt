// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepeye.musicpro.data.prefs.ThemeMode
import androidx.compose.ui.graphics.Color
import com.deepeye.musicpro.ui.components.GlowCard
import com.deepeye.musicpro.data.source.remote.update.UpdateState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    windowSizeClass: androidx.compose.material3.windowsizeclass.WindowSizeClass,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = uiState.settings
    val context = LocalContext.current

    LaunchedEffect(uiState.updateState) {
        if (uiState.updateState is UpdateState.UpToDate) {
            android.widget.Toast.makeText(context, "DeepEye Music Pro is up to date", android.widget.Toast.LENGTH_SHORT).show()
            viewModel.resetUpdateState()
        }
    }

    val isExpanded = windowSizeClass.widthSizeClass == androidx.compose.material3.windowsizeclass.WindowWidthSizeClass.Expanded

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        UpdateBanner(
            state = uiState.updateState,
            onDownloadClick = { url, ver -> viewModel.downloadUpdate(url, ver) },
            onInstallClick = { file -> viewModel.installApk(file) },
            onDismissClick = { viewModel.resetUpdateState() }
        )

        if (isExpanded) {
            var selectedCategory by remember { mutableStateOf(0) }
            val categories = listOf("Appearance", "Music Taste", "Audio", "Library", "About")

            Row(modifier = Modifier.fillMaxSize()) {
                // Left Pane: Navigation Category List
                Column(
                    modifier = Modifier
                        .weight(0.32f)
                        .fillMaxHeight()
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp, start = 12.dp),
                        color = Color.White
                    )
                    categories.forEachIndexed { index, category ->
                        NavigationDrawerItem(
                            label = { Text(category, style = MaterialTheme.typography.titleMedium) },
                            selected = selectedCategory == index,
                            onClick = { selectedCategory = index },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                VerticalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

                // Right Pane: Selected Category Settings details
                Box(
                    modifier = Modifier
                        .weight(0.68f)
                        .fillMaxHeight()
                        .padding(horizontal = 24.dp, vertical = 24.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (selectedCategory) {
                            0 -> {
                                item { SectionHeader("Appearance") }
                                item { AppearanceSettingsCard(settings, viewModel) }
                            }
                            1 -> {
                                item { SectionHeader("Music Taste & Autoplay") }
                                item { TasteSettingsCard(uiState, viewModel) }
                            }
                            2 -> {
                                item { SectionHeader("Audio") }
                                item { AudioSettingsCard(settings, viewModel) }
                            }
                            3 -> {
                                item { SectionHeader("Library") }
                                item { LibrarySettingsCard(uiState, viewModel) }
                            }
                            4 -> {
                                item { SectionHeader("About") }
                                item { AboutSettingsCard(viewModel) }
                            }
                        }
                    }
                }
            }
        } else {
            // Standard scroll list for phones and compact sizes
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
                item {
                    Text("Settings", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
                }

                item { SectionHeader("Appearance") }
                item { AppearanceSettingsCard(settings, viewModel) }

                item { SectionHeader("Music Taste & Autoplay") }
                item { TasteSettingsCard(uiState, viewModel) }

                item { SectionHeader("Audio") }
                item { AudioSettingsCard(settings, viewModel) }

                item { SectionHeader("Library") }
                item { LibrarySettingsCard(uiState, viewModel) }

                item { SectionHeader("About") }
                item { AboutSettingsCard(viewModel) }
            }
        }
    }
}

@Composable
private fun AppearanceSettingsCard(
    settings: com.deepeye.musicpro.data.prefs.AppSettings,
    viewModel: SettingsViewModel
) {
    SettingsCard {
        // Theme picker
        Text("Theme", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = settings.themeMode == mode,
                    onClick = { viewModel.setThemeMode(mode) },
                    label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        // Dynamic color
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Dynamic Color (Material You)", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = settings.dynamicColor, onCheckedChange = { viewModel.setDynamicColor(it) })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TasteSettingsCard(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel
) {
    val settings = uiState.settings
    SettingsCard {
        Text("Preferred Languages", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        val languagesList = listOf(
            "Hindi", "English", "Punjabi", "Bhojpuri", "Tamil", "Telugu", 
            "Haryanvi", "Bengali", "Malayalam", "Kannada", "Marathi", "Gujarati"
        )
        val currentLangs = uiState.tasteProfile.preferredLanguages
        
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            languagesList.forEach { lang ->
                val isSelected = currentLangs.contains(lang)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        val updated = if (isSelected) currentLangs - lang else currentLangs + lang
                        viewModel.setPreferredLanguages(updated)
                    },
                    label = { Text(lang) }
                )
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        Text("Favorite Artists", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        val currentArtists = uiState.tasteProfile.favoriteArtists
        
        if (currentArtists.isEmpty()) {
            Text("No favorite artists selected.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                currentArtists.forEach { artist ->
                    InputChip(
                        selected = true,
                        onClick = {
                            viewModel.setFavoriteArtists(currentArtists - artist)
                        },
                        label = { Text(artist) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
        }
        
        Spacer(Modifier.height(12.dp))
        
        var newArtistName by remember { mutableStateOf("") }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newArtistName,
                onValueChange = { newArtistName = it },
                placeholder = { Text("Add artist name...") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    if (newArtistName.isNotBlank()) {
                        viewModel.setFavoriteArtists(currentArtists + newArtistName.trim())
                        newArtistName = ""
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Add")
            }
        }
    }
}

@Composable
private fun AudioSettingsCard(
    settings: com.deepeye.musicpro.data.prefs.AppSettings,
    viewModel: SettingsViewModel
) {
    SettingsCard {
        Text("Crossfade Duration", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Slider(
                value = settings.crossfadeDuration.toFloat(),
                onValueChange = { viewModel.setCrossfadeDuration(it.toInt()) },
                valueRange = 0f..12f,
                steps = 11,
                modifier = Modifier.weight(1f)
            )
            Text("${settings.crossfadeDuration}s", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(32.dp))
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Show Visualizer", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = settings.showVisualizer, onCheckedChange = { viewModel.setShowVisualizer(it) })
        }
    }
}

@Composable
private fun LibrarySettingsCard(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel
) {
    SettingsCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Rescan Library", style = MaterialTheme.typography.bodyMedium)
            if (uiState.isRescanningLibrary) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                OutlinedButton(onClick = { viewModel.rescanLibrary() }) { Text("Scan") }
            }
        }
    }
}

@Composable
private fun AboutSettingsCard(
    viewModel: SettingsViewModel
) {
    SettingsCard {
        Text("DeepEye Music Pro", style = MaterialTheme.typography.titleSmall)
        Text("Version ${com.deepeye.musicpro.BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text("Premium music player with V4A DSP engine", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text("deepeye.tech", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Check for updates", style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(
                onClick = {
                    android.util.Log.d("SettingsScreen", "Check Now clicked")
                    viewModel.checkForUpdate()
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Check Now")
            }
        }
    }
}

@Composable
fun UpdateBanner(
    state: UpdateState,
    onDownloadClick: (url: String, version: String) -> Unit,
    onInstallClick: (java.io.File) -> Unit,
    onDismissClick: () -> Unit
) {
    AnimatedVisibility(
        visible = state !is UpdateState.Idle && state !is UpdateState.UpToDate,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        val tealGlow = Color(0xFF00F2FE)
        val purpleGlow = Color(0xFF4FACFE)

        GlowCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            glowColor = tealGlow,
            secondaryGlowColor = purpleGlow,
            cornerRadius = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                when (state) {
                    is UpdateState.Checking -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(
                                "Checking for updates...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    is UpdateState.UpdateAvailable -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Update Available: v${state.version}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (state.releaseNotes.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        state.releaseNotes,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 3,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = { onDownloadClick(state.apkUrl, state.version) },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Download")
                            }
                        }
                    }
                    is UpdateState.Downloading -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Downloading update...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    "${(state.progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { state.progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    is UpdateState.Downloaded -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Update Downloaded",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "Ready to install v${state.version}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = { onInstallClick(state.file) },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Install")
                            }
                        }
                    }
                    is UpdateState.Error -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Update Error",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    state.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            TextButton(onClick = onDismissClick) {
                                Text("Dismiss")
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}
