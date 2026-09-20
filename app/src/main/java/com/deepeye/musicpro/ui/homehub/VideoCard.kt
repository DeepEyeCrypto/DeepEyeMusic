// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.homehub

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.crossfade
import com.deepeye.musicpro.domain.model.home.HomeVideoItem
import kotlin.math.absoluteValue
import com.deepeye.musicpro.ui.components.bouncyClickable

@Composable
fun VideoCard(
    item: HomeVideoItem,
    onClick: (HomeVideoItem) -> Unit,
    modifier: Modifier = Modifier.width(300.dp),
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .bouncyClickable(
                downScale = 0.95f,
                onClick = { onClick(item) }
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF131722).copy(alpha = 0.85f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = Color(0x22FFFFFF)
        )
    ) {
        Column {
            // 16:9 Thumbnail Poster
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            ) {
                AsyncImage(
                    model = coil3.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                        .data(item.thumbnailUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )

                // Bottom vignette gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.75f)
                                )
                            )
                        )
                )

                // Quality Badge Pill (Top-Left)
                val quality = remember(item.id) {
                    val hash = item.id.hashCode().absoluteValue
                    when {
                        item.isLive -> "LIVE"
                        hash % 3 == 0 -> "4K"
                        hash % 3 == 1 -> "HDR"
                        else -> "1080P"
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .background(
                            if (quality == "LIVE") Color(0xFFFF0033) else Color.Black.copy(alpha = 0.85f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = quality,
                        color = if (quality == "4K") Color(0xFFFF3D00) else if (quality == "HDR") Color(0xFF00E676) else Color(0xFF00E5FF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Duration Badge Pill (Bottom-Right)
                if (!item.isLive && item.duration > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                            .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = item.duration.formatDuration(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }

            // Metadata with premium typography
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = item.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    lineHeight = 20.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.channelName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (item.viewCount > 0) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = item.viewCount.formatCount(),
                            color = Color(0xFF00E5FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// Extension functions
fun Long.formatDuration(): String {
    val h = this / 3600
    val m = (this % 3600) / 60
    val s = this % 60
    return if (h > 0) {
        "%d:%02d:%02d".format(h, m, s)
    } else {
        "%d:%02d".format(m, s)
    }
}

fun Long.formatCount(): String =
    when {
        this >= 1_000_000 -> "${"%.1f".format(this / 1_000_000.0)}M views"
        this >= 1_000 -> "${"%.1f".format(this / 1_000.0)}K views"
        else -> "$this views"
    }
