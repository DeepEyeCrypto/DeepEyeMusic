// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.homehub

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.deepeye.musicpro.ui.components.DynamicLabel
import com.deepeye.musicpro.ui.components.SecondaryLabel
import coil3.compose.AsyncImage
import com.deepeye.musicpro.domain.model.home.HomeMusicItem
import com.deepeye.musicpro.ui.theme.GlassBorder

/**
 * Continue Listening horizontal rail — shows recently played music tracks.
 * Glassmorphic cards with subtle album art and metadata.
 */
@Composable
fun ContinueListeningRow(
    items: List<HomeMusicItem>,
    onItemClick: (HomeMusicItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DynamicLabel(
            text = "🎵 Continue Listening",
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.titleMedium,
            backgroundColor = Color.Black,
            fontWeight = FontWeight.ExtraBold,
            
        )

        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
        ) {
            items.take(4).forEach { music ->
                ContinueListeningCard(music = music, onClick = { onItemClick(music) }, modifier = Modifier.weight(1f, fill = false))
            }
        }
    }
}

@Composable
private fun ContinueListeningCard(
    music: HomeMusicItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(160.dp)
            .height(210.dp) // Taller elegant aspect ratio
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.05f)) // Frost background
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Album art with Play button overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF2A2A35), Color(0xFF1E1E28)))),
                contentAlignment = Alignment.Center,
            ) {
                if (music.thumbnailUrl.isNotEmpty()) {
                    AsyncImage(
                        model = music.thumbnailUrl,
                        contentDescription = music.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(32.dp),
                    )
                }

                // Play Button Overlay
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .border(1.dp, Color.White.copy(alpha = 0.5f), androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            val frostBg = Color.White.copy(alpha = 0.05f)
            DynamicLabel(
                text = music.title,
                backgroundColor = Color.Black,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            SecondaryLabel(
                text = music.artist,
                backgroundColor = Color.Black,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
