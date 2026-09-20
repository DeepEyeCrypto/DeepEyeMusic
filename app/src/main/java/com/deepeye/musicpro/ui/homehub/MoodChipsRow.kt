// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.homehub

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
 * Mood Chips Row — Futuristic Glassmorphic Activity & Mood Carousel
 */
@Composable
fun MoodChipsRow(
    moods: List<MoodMix>,
    onMoodClick: (MoodMix) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (moods.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF00E5FF))
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Moods & Activities",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
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

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF131722).copy(alpha = 0.85f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            accentColor.copy(alpha = 0.5f)
        ),
        modifier = modifier.heightIn(min = 48.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = mood.emoji,
                fontSize = 20.sp,
            )
            Text(
                text = mood.label,
                fontSize = 14.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp
            )
        }
    }
}
