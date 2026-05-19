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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepeye.musicpro.data.prefs.ThemeMode

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = uiState.settings

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(20.dp))
        }

        // ── Appearance ──
        item { SectionHeader("Appearance") }
        item {
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

        // ── Music Taste & Autoplay ──
        item { SectionHeader("Music Taste & Autoplay") }
        item {
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

        // ── Audio ──
        item { SectionHeader("Audio") }
        item {
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

        // ── Library ──
        item { SectionHeader("Library") }
        item {
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

        // ── About ──
        item { SectionHeader("About") }
        item {
            SettingsCard {
                Text("DeepEye Music Pro", style = MaterialTheme.typography.titleSmall)
                Text("Version 1.0.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text("Premium music player with V4A DSP engine", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text("deepeye.tech", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
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
