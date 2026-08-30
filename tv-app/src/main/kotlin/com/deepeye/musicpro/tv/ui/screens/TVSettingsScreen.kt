package com.deepeye.musicpro.tv.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Switch
import com.deepeye.musicpro.tv.ui.theme.TVTheme

sealed class SpeedOption(val value: Float, val label: String) {
    object Speed1x : SpeedOption(1f, "1x")
    object Speed1_25x : SpeedOption(1.25f, "1.25x")
    object Speed1_5x : SpeedOption(1.5f, "1.5x")
    object Speed2x : SpeedOption(2f, "2x")
}

sealed class QualityOption(val value: String, val label: String) {
    object Auto : QualityOption("auto", "Auto")
    object Q1080p : QualityOption("1080p", "1080p")
    object Q720p : QualityOption("720p", "720p")
    object Q480p : QualityOption("480p", "480p")
}

@Composable
fun TVSettingsScreen(
    sponsorBlockEnabled: Boolean = false,
    onSponsorBlockToggle: (Boolean) -> Unit = {},
    playbackSpeed: SpeedOption = SpeedOption.Speed1x,
    onPlaybackSpeedChange: (SpeedOption) -> Unit = {},
    autoSkipIntroEnabled: Boolean = false,
    onAutoSkipIntroToggle: (Boolean) -> Unit = {},
    videoQuality: QualityOption = QualityOption.Auto,
    onVideoQualityChange: (QualityOption) -> Unit = {},
    modifier: Modifier = Modifier
) {
    TvLazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            androidx.tv.material3.Text(
                text = "Settings",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        
        // SponsorBlock
        item {
            SettingsToggleRow(
                title = "SponsorBlock",
                description = "Skip sponsored segments automatically",
                checked = sponsorBlockEnabled,
                onCheckedChange = onSponsorBlockToggle
            )
        }
        
        // Auto-skip Intro
        item {
            SettingsToggleRow(
                title = "Auto-skip Intro",
                description = "Skip intro segments automatically",
                checked = autoSkipIntroEnabled,
                onCheckedChange = onAutoSkipIntroToggle
            )
        }
        
        // Playback Speed
        item {
            SettingsSectionTitle(title = "Playback Speed")
        }
        
        items(listOf(SpeedOption.Speed1x, SpeedOption.Speed1_25x, SpeedOption.Speed1_5x, SpeedOption.Speed2x)) { speed ->
            SettingsSelectableRow(
                title = speed.label,
                selected = speed == playbackSpeed,
                onClick = { onPlaybackSpeedChange(speed) }
            )
        }
        
        // Video Quality
        item {
            SettingsSectionTitle(title = "Video Quality")
        }
        
        items(listOf(QualityOption.Auto, QualityOption.Q1080p, QualityOption.Q720p, QualityOption.Q480p)) { quality ->
            SettingsSelectableRow(
                title = quality.label,
                selected = quality == videoQuality,
                onClick = { onVideoQualityChange(quality) }
            )
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    androidx.tv.material3.Card(
        onClick = { onCheckedChange(!checked) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                androidx.tv.material3.Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                androidx.tv.material3.Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
private fun SettingsSelectableRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    androidx.tv.material3.Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            androidx.tv.material3.Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            if (selected) {
                androidx.tv.material3.Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    androidx.tv.material3.Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = 8.dp)
    )
}
