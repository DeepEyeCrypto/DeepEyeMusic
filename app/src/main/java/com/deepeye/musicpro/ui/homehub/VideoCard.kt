// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.homehub

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
                            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.Top,
            ) {
                // Channel Avatar (Coil AsyncImage, clipped to a circle) — left of the
                // title/metadata column. Falls back to the YouTube hqdefault thumbnail
                // derived from the video id when the parser did not surface an avatar.
                val avatarModel = item.channelAvatarUrl.ifBlank {
                    "https://i.ytimg.com/vi/${item.id}/default.jpg"
                }
                AsyncImage(
                    model = coil3.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                        .data(avatarModel)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.channelName,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x22FFFFFF)),
                    contentScale = ContentScale.Crop,
                )

                Spacer(Modifier.width(10.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
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

                    // Metadata row — "[Channel] • [Views] • [Publish Time]" in a single
                    // bodySmall line with a muted color, matching YouTube parity.
                    val metaParts = buildList {
                        if (item.channelName.isNotBlank()) add(item.channelName)
                        if (item.viewCount > 0) add(item.viewCount.formatCountShort())
                        if (item.uploadDate.isNotBlank()) add(item.uploadDate)
                    }
                    if (metaParts.isNotEmpty()) {
                        Text(
                            text = metaParts.joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.55f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
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

/**
 * Compact view count for the inline metadata row (e.g. "12M views", "840K views").
 * Drops the trailing ".0" so whole numbers read cleanly, unlike [formatCount].
 */
fun Long.formatCountShort(): String {
    fun trimZero(value: Double): String {
        val s = "%.1f".format(value)
        return if (s.endsWith(".0")) s.dropLast(2) else s
    }
    return when {
        this >= 1_000_000 -> "${trimZero(this / 1_000_000.0)}M views"
        this >= 1_000 -> "${trimZero(this / 1_000.0)}K views"
        else -> "$this views"
    }
}
