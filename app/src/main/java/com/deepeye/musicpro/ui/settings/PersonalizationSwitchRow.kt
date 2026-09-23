// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.musicpro.ui.theme.*

private val NeonCyan = Color(0xFF00E5FF)
private val DarkCardBg = Color(0xFF0F121C)
private val GlassBorder = Color(0x1FFFFFFF)

@Composable
fun PersonalizationSwitchRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (checked) Color(0xFF131826) else DarkCardBg,
        border = BorderStroke(
            1.dp,
            if (checked) NeonCyan.copy(alpha = 0.35f) else GlassBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Glowing Icon Capsule
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (checked) NeonCyan.copy(alpha = 0.15f) else Color(0x14FFFFFF))
                    .border(
                        1.dp,
                        if (checked) NeonCyan.copy(alpha = 0.45f) else Color(0x14FFFFFF),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled && checked) NeonCyan else Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (enabled) Color.White else Color.White.copy(alpha = 0.4f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.5.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = if (enabled) Color.White.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.3f),
                    fontSize = 12.5.sp,
                    lineHeight = 16.sp
                )
            }

            Spacer(Modifier.width(10.dp))

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF090B10),
                    checkedTrackColor = NeonCyan,
                    uncheckedThumbColor = Color(0xFF90A4AE),
                    uncheckedTrackColor = Color(0xFF1E2433),
                    uncheckedBorderColor = Color(0x33FFFFFF)
                ),
                modifier = Modifier.semantics {
                    contentDescription = "$title toggle, currently ${if (checked) "enabled" else "disabled"}"
                }
            )
        }
    }
}
