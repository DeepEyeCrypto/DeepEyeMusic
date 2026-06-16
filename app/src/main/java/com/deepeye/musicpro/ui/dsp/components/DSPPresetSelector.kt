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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.musicpro.dsp.model.DSPPreset

@Composable
fun DSPPresetSelector(
    currentPreset: DSPPreset,
    onPresetChanged: (DSPPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(DSPPreset.entries) { preset ->
            Card(
                modifier = Modifier
                    .padding(8.dp)
                    .width(140.dp)
                    .height(100.dp)
                    .clickable { onPresetChanged(preset) },
                colors = CardDefaults.cardColors(
                    containerColor = if (preset == currentPreset) Color(0xFF00E5FF).copy(0.2f) else Color(0xFF2A2A2A),
                    contentColor = if (preset == currentPreset) Color(0xFF00E5FF) else Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                border = if (preset == currentPreset) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF)) else null
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = preset.presetName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = preset.description,
                        fontSize = 11.sp,
                        color = Color.Gray,
                        maxLines = 2
                    )
                }
            }
        }
    }
}
