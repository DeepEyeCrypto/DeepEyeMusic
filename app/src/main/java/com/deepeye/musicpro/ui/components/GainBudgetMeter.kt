// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.musicpro.dsp.model.GainBudget
import com.deepeye.musicpro.dsp.model.RiskLevel

@Composable
fun GainBudgetMeter(
    budget: GainBudget,
    modifier: Modifier = Modifier
) {
    val meterColor by animateColorAsState(
        targetValue = when (budget.risk) {
            RiskLevel.SAFE -> Color(0xFF4CAF50)    // Green
            RiskLevel.MODERATE -> Color(0xFFFFC107) // Amber
            RiskLevel.DANGER -> Color(0xFFF44336)   // Red
        }
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(meterColor.copy(alpha = 0.1f))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "GAIN BUDGET",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = meterColor
        )
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Mini dB segments
            repeat(10) { i ->
                val isActive = (budget.totalDb / 20f) * 10 > i
                Box(
                    modifier = Modifier
                        .size(width = 8.dp, height = 4.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(if (isActive) meterColor else meterColor.copy(alpha = 0.2f))
                )
            }
            
            Spacer(Modifier.width(4.dp))
            
            Text(
                text = "${String.format("%.1f", budget.totalDb)} dB",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = meterColor
            )
        }
    }
}
