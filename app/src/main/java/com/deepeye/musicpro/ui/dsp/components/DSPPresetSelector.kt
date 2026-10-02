// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.dsp.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
        contentPadding = PaddingValues(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(DSPPreset.entries) { preset ->
            val isSelected = preset == currentPreset
            val isLocked = preset.requiredRank > 0 && userRank > preset.requiredRank

            Card(
                modifier = Modifier
                    .width(125.dp)
                    .height(78.dp)
                    .glassCard(
                        elevation = if (isSelected) 8.dp else 1.dp,
                        borderColor = if (isSelected) Color(0xFF00E5FF) else Color(0x22FFFFFF)
                    )
                    .then(
                        if (isLocked) Modifier else Modifier
                            .clickable { onPresetChanged(preset) }
                            .hoverable(scale = 1.03f)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Color(0x2600E5FF) else Color.Transparent
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (isLocked) {
                        Text(text = "🔒", fontSize = 12.sp)
                    }
                    Text(
                        text = preset.presetName,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                        color = if (isLocked) Color.DarkGray else if (isSelected) Color(0xFF00E5FF) else Color.White,
                        maxLines = 1,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isLocked) "Requires Top ${preset.requiredRank}" else preset.description,
                        fontSize = 9.sp,
                        lineHeight = 11.sp,
                        color = if (isLocked) MaterialTheme.colorScheme.error else if (isSelected) Color.White.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.5f),
                        maxLines = 2,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}
