// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.v4a.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.musicpro.dsp.model.GainBudget
import com.deepeye.musicpro.dsp.model.RiskLevel

/**
 * A premium UI component that visualizes the current DSP gain budget.
 * Features color-coded animations and a safety meter to prevent clipping.
 */
@Composable
fun GainBudgetCard(
    gainBudget: GainBudget,
    modifier: Modifier = Modifier
) {
    val riskColor by animateColorAsState(
        targetValue = when (gainBudget.risk) {
            RiskLevel.SAFE     -> Color(0xFF00E676)
            RiskLevel.MODERATE -> Color(0xFFFFD600)
            RiskLevel.DANGER   -> Color(0xFFFF4B4B)
        },
        animationSpec = tween(400),
        label = "riskColor"
    )

    val progress = (gainBudget.totalDb / 16f).coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "progress"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A2E)
        ),
        border = BorderStroke(1.dp, riskColor.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = riskColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "GAIN BUDGET",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF9E9E9E),
                        letterSpacing = 1.sp
                    )
                }

                // Risk badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = riskColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = gainBudget.risk.name,
                        modifier = Modifier.padding(
                            horizontal = 10.dp,
                            vertical = 4.dp
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = riskColor,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // dB value
            Text(
                text = "${"%.1f".format(gainBudget.totalDb)} dB",
                style = MaterialTheme.typography.headlineMedium,
                color = riskColor,
                fontWeight = FontWeight.Bold
            )

            // Animated progress bar
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF2A2A3E))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF00E676),
                                        Color(0xFFFFD600),
                                        Color(0xFFFF4B4B)
                                    ),
                                    endX = Float.POSITIVE_INFINITY
                                )
                            )
                    )
                }

                // Zone markers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0 dB",  style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF00E676))
                    Text("8 dB",  style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFFD600))
                    Text("14 dB", style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFF4B4B))
                }
            }

            // Auto-correct warning
            if (gainBudget.risk == RiskLevel.DANGER) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFF4B4B).copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFF4B4B),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Clipping risk! Reduce bass or EQ boost.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFF4B4B)
                        )
                    }
                }
            }

            // Moderate advisory
            if (gainBudget.risk == RiskLevel.MODERATE) {
                Text(
                    text = "💡 Consider reducing one boost module.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFFD600).copy(alpha = 0.8f)
                )
            }
        }
    }
}
