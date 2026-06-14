// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.MusicOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Premium glass-styled empty state.
 * Displays when a list or screen has no content.
 */
@Composable
fun GlassEmptyState(
    icon: ImageVector = Icons.Outlined.MusicOff,
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth().padding(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = Color.White.copy(alpha = 0.4f),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                )
            }
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(8.dp))
                GlassButton(
                    onClick = onAction,
                    cornerRadius = 12.dp,
                    tintColor = Color.White.copy(alpha = 0.1f),
                    borderColor = Color.White.copy(alpha = 0.2f),
                ) {
                    Text(
                        text = actionLabel,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

/**
 * Premium glass-styled error state.
 * Displays when a network or loading error occurs.
 */
@Composable
fun GlassErrorState(
    icon: ImageVector = Icons.Outlined.CloudOff,
    message: String,
    retryLabel: String = "Retry",
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth().padding(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            GlassButton(
                onClick = onRetry,
                cornerRadius = 12.dp,
                tintColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.3f),
            ) {
                Text(
                    text = retryLabel,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

/**
 * Premium glass-styled skeleton loader with shimmer animation.
 * Matches the expected content layout dimensions.
 */
@Composable
fun GlassSkeletonLoader(
    modifier: Modifier = Modifier,
    height: Dp = 60.dp,
    cornerRadius: Dp = 12.dp,
) {
    val shimmerColors = listOf(
        Color.White.copy(alpha = 0.04f),
        Color.White.copy(alpha = 0.10f),
        Color.White.copy(alpha = 0.04f),
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerTranslate",
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 200f, 0f),
        end = Offset(translateAnim + 200f, 0f),
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush),
    )
}

/**
 * Skeleton list item — mimics a track row with artwork + text placeholders.
 */
@Composable
fun GlassSkeletonTrackRow(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Artwork placeholder
        GlassSkeletonLoader(
            modifier = Modifier.size(48.dp),
            height = 48.dp,
            cornerRadius = 8.dp,
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f),
        ) {
            // Title placeholder
            GlassSkeletonLoader(
                modifier = Modifier.fillMaxWidth(0.7f),
                height = 14.dp,
                cornerRadius = 4.dp,
            )
            // Artist placeholder
            GlassSkeletonLoader(
                modifier = Modifier.fillMaxWidth(0.4f),
                height = 12.dp,
                cornerRadius = 4.dp,
            )
        }
    }
}
