// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.dsp.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.musicpro.dsp.model.DSPPreset
import com.deepeye.musicpro.ui.components.glassCard
import com.deepeye.musicpro.ui.components.hoverable

@Composable
fun DSPPresetSelector(
    currentPreset: DSPPreset,
    userRank: Int,
    onPresetChanged: (DSPPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(DSPPreset.entries) { preset ->
            val isLocked = userRank > preset.requiredRank
            Card(
                modifier = Modifier
                    .padding(8.dp)
                    .width(140.dp)
                    .height(110.dp)
                    .glassCard(
                        elevation = if (preset == currentPreset) 12.dp else 4.dp,
                        borderColor = if (preset == currentPreset) MaterialTheme.colorScheme.primary else Color(0xFF333333).copy(alpha = 0.5f)
                    )
                    .then(
                        if (isLocked) Modifier else Modifier
                            .clickable { onPresetChanged(preset) }
                            .hoverable(scale = 1.05f)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isLocked) {
                        Text(text = "🔒", fontSize = 16.sp)
                    }
                    Text(
                        text = preset.presetName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLocked) Color.DarkGray else if (preset == currentPreset) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isLocked) "Requires Top ${preset.requiredRank}" else preset.description,
                        fontSize = 11.sp,
                        color = if (isLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}
