// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.gamification

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.musicpro.ui.theme.GlassBorder

@Composable
fun StreakProgressBar(currentStreak: Int, targetStreak: Int = 7, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.LocalFireDepartment, // Flame icon
                tint = Color(0xFFFF6B35),
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$currentStreak-day Streak",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(Color(0xFF2A2A2A), RoundedCornerShape(4.dp))
        ) {
            val progress = (currentStreak.toFloat() / targetStreak.toFloat()).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(8.dp)
                    .background(Color(0xFFFF6B35), RoundedCornerShape(4.dp))
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        val remaining = (targetStreak - currentStreak).coerceAtLeast(0)
        Text(
            text = if (remaining > 0) "$remaining more days for bonus!" else "Bonus unlocked!",
            fontSize = 12.sp,
            color = Color(0xFFFF6B35),
            modifier = Modifier.align(Alignment.End)
        )
    }
}
