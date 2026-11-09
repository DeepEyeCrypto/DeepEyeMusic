// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.deepeye.musicpro.ui.theme.CardGeometry
import com.deepeye.musicpro.ui.theme.VideoCard

private val NeonCyan = Color(0xFF00E5FF)
private val NeonPurple = Color(0xFF7C4DFF)
private val CardBackground = Color(0xFF131722).copy(alpha = 0.85f)

/**
 * The single neon media card composable shared by every card surface in the app
 * (YouTube tab, Home, Video Hub, Music, Library, Search).
 *
 * Implements the locked unified design rules:
 *
 * - viewCount null/0 -> hide views (never "0 views")
 * - uploadDate blank/"—" -> hide date
 * - durationMs null/0 -> hide duration badge (never "0:00")
 * - qualityLabel: ONLY real quality data; never derived from id hash
 * - avatar fallback: video-ID-derived ytimg thumbnail
 * - channelName blank/"."/"·" -> "DeepEye"
 *
 * Geometry comes from [CardGeometry] (SSOT) selected by [data.cardType].
 */
@Composable
fun NeonMediaCard(
    data: NeonCardData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isHovered by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }

    // Geometry follows CardGeometry (SSOT) per card type. ALL content sections
        // (Home, YouTube, Video Hub, Music, Library) render the SAME 16:9 video
        // card — 210px × 219px, thumbnail 16:9, title 2 lines. The Music square
        // album-art design is abandoned; square source artwork is cropped into
        // the 16:9 box by ContentScale.Crop. COMPACT keeps the same 16:9 geometry
        // so library/history rows match the section grid.
        val geometry: VideoCard =
            when (data.cardType) {
                NeonCardType.VIDEO, NeonCardType.MUSIC, NeonCardType.COMPACT ->
                    CardGeometry.Video
            }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else if (isHovered) 1.02f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "scale",
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (isPressed || isHovered) 0.8f else 0.3f,
        animationSpec = tween(300),
        label = "glow",
    )
    val borderBrush = Brush.linearGradient(
        colors = listOf(NeonPurple.copy(alpha = glowAlpha), NeonCyan.copy(alpha = glowAlpha)),
    )

    val cleanChannel = data.channelName?.trim()?.let {
        if (it.isBlank() || it == "." || it == "·") "DeepEye" else it
    } ?: "DeepEye"

    val viewText = data.viewCount?.let { if (it > 0) formatViewsShort(it) else null }

    val dateText = data.uploadDate?.let {
        when {
            it.isBlank() || it == "—" -> null
            else -> it
        }
    }

    val durationText = data.durationMs?.let { if (it > 0) formatDurationMs(it) else null }

    val metaParts = buildList {
        viewText?.let { add(it) }
        dateText?.let { add(it) }
    }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(geometry.shape)
                .background(CardBackground)
                .border(
                    width = if (isHovered || isPressed) 1.5.dp else 1.dp,
                    brush = borderBrush,
                    shape = CardGeometry.Video.shape,
                )
                .clickable {
                    isPressed = true
                    onClick()
                    isPressed = false
                },
    ) {
        // Thumbnail area
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(geometry.aspectRatio)
                    .clip(
                        RoundedCornerShape(
                            topStart = geometry.cornerRadius,
                            topEnd = geometry.cornerRadius,
                            bottomStart = geometry.cornerRadius / 2,
                            bottomEnd = geometry.cornerRadius / 2,
                        )
                    ),
        ) {
            val thumbUrl = data.thumbnailUrl?.ifBlank {
                "https://i.ytimg.com/vi/${data.id}/default.jpg"
            }
            AsyncImage(
                model = thumbUrl,
                contentDescription = data.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )

            // Vignette
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors =
                                    listOf(
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                                        Color.Transparent,
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                    )
                            )
                        )
            )

            // Quality badge — ONLY real quality data; LIVE takes precedence
            val qualityText = if (data.isLive) "LIVE" else data.qualityLabel
            if (!qualityText.isNullOrEmpty()) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .background(
                                Color(0xFFFF3D00).copy(alpha = 0.15f),
                                RoundedCornerShape(4.dp)
                            )
                            .border(
                                0.5.dp,
                                Color(0xFFFF3D00).copy(alpha = 0.6f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        qualityText.uppercase(),
                        color = Color(0xFFFF3D00),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }

            // Duration badge — hidden when unknown/0
            if (durationText != null) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(11.dp),
                        )
                        Text(
                            text = durationText,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }

            // Watch-progress bar
            val progress = data.progressFraction
            if (progress != null && progress > 0f) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .align(Alignment.BottomStart)
                            .background(Color.White.copy(alpha = 0.25f))
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth(progress.coerceIn(0f, 1f))
                                .fillMaxSize()
                                .background(Color(0xFFFF0033))
                    )
                }
            }
        }

        // Metadata column
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(geometry.contentPadding),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = data.title,
                color = MaterialTheme.colorScheme.onSurface,
                style =
                    MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 16.sp,
                        letterSpacing = 0.1.sp
                    ),
                maxLines = geometry.titleMaxLines,
                overflow = TextOverflow.Ellipsis,
            )

            // Channel · views · date row with gradient avatar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val avatarModel =
                    data.channelAvatarUrl?.ifBlank {
                        "https://i.ytimg.com/vi/${data.id}/default.jpg"
                    }
                Box(
                    modifier =
                        Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                brush =
                                    Brush.linearGradient(
                                        colors =
                                            listOf(
                                                NeonPurple.copy(alpha = 0.25f),
                                                NeonCyan.copy(alpha = 0.25f),
                                            ),
                                    ),
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.sweepGradient(listOf(NeonPurple, NeonCyan, NeonPurple)),
                                shape = CircleShape,
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = avatarModel,
                        contentDescription = cleanChannel,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                }

                Spacer(Modifier.width(6.dp))

                Text(
                    text = cleanChannel,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    style =
                        MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp,
                        ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )

                if (metaParts.isNotEmpty()) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = metaParts.joinToString(" · "),
                        color = NeonCyan.copy(alpha = 0.9f),
                        style =
                            MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                            ),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

private fun formatDurationMs(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) String.format("%d:%02d:%02d", hours, minutes, seconds)
    else String.format("%d:%02d", minutes, seconds)
}

private fun formatViewsShort(views: Long): String {
    return when {
        views >= 1_000_000 -> String.format("%.1fM views", views / 1_000_000f)
        views >= 1_000 -> String.format("%.0fK views", views / 1_000f)
        else -> "$views views"
    }
}

/**
 * Geometry selection mirror of [NeonMediaCard]'s internal `when`. Exposed so the
 * TDD contract test can assert on the 16:9-for-all-sections rule without
 * rendering a Composable.
 */
internal fun NeonMediaCardGeometry(cardType: NeonCardType): VideoCard =
    when (cardType) {
        NeonCardType.VIDEO, NeonCardType.MUSIC, NeonCardType.COMPACT -> CardGeometry.Video
    }
