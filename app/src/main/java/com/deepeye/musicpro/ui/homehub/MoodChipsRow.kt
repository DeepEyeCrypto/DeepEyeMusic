// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.homehub

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.deepeye.musicpro.domain.model.home.MoodMix

/**
 * Mood Chips Row — horizontal scrollable mood/activity chips.
 * Each chip has a gradient accent color matching the mood and triggers a mood-based feed.
 */
@Composable
fun MoodChipsRow(
    moods: List<MoodMix>,
    onMoodClick: (MoodMix) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (moods.isEmpty()) {
        android.util.Log.d("MoodChips", "Moods list is empty!")
        return
    }

    android.util.Log.d("MoodChips", "Rendering MoodChipsRow with ${moods.size} moods")

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = "Moods & Activities",
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.titleMedium.copy(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF00D2FF), Color(0xFF7C4DFF))
                )
            ),
            fontWeight = FontWeight.ExtraBold,
        )

        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
        ) {
            moods.forEach { mood ->
                MoodChip(mood = mood, onClick = { onMoodClick(mood) })
            }
        }
    }
}

@Composable
private fun MoodChip(
    mood: MoodMix,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accentColor = Color(mood.accentColor.toInt())

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(accentColor.copy(alpha = 0.2f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = mood.emoji,
                fontSize = 18.sp,
            )
            Text(
                text = mood.label,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
        }
    }
}
