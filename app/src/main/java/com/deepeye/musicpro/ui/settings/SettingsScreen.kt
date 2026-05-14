package com.deepeye.musicpro.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepeye.musicpro.data.prefs.ThemeMode

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
