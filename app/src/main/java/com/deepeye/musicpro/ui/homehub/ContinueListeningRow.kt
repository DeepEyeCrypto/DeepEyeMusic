// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.homehub

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
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
import androidx.compose.ui.unit.sp
import com.deepeye.musicpro.ui.components.DynamicLabel
import com.deepeye.musicpro.ui.components.SecondaryLabel
import com.deepeye.musicpro.ui.components.rememberSparseAwareRailLayout
import coil3.compose.AsyncImage
import coil3.request.crossfade
import com.deepeye.musicpro.domain.model.home.HomeMusicItem
import com.deepeye.musicpro.ui.theme.CardGeometry
import com.deepeye.musicpro.ui.theme.GlassBorder
import com.deepeye.musicpro.ui.components.bouncyClickable

/** Rail shows at most this many recent tracks. */
private const val ContinueListeningMaxItems = 4

/**
 * Landscape phones leave this rail very little vertical room. The
 * HomeGreetingHeader above consumes ~118dp, so a card that stacks artwork above
 * two text lines (~200dp) overflows and the scroll container hard-clips the
 * title mid-glyph with the artist pushed fully out of view.
 *
 * Laying the card out horizontally — artwork beside the metadata, the same
 * shape LocalResumeRail already uses for this model — bounds the height at
 * [CardGeometry.ContinueListening.height], so both text lines stay on screen.
 *
 * All dimensions live in [CardGeometry.ContinueListening]; ListRowSkeleton reads
 * the same fields so the loading row and this row are the same size.
 */

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

        val displayedItems = items.take(ContinueListeningMaxItems)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            displayedItems.chunked(2).forEach { chunk ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    chunk.forEach { music ->
                        ContinueListeningCard(
                            music = music,
                            onClick = { onItemClick(music) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(2 - chunk.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
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
    Row(
        modifier = modifier
            .heightIn(min = CardGeometry.ContinueListening.minHeight)
            .clip(CardGeometry.ContinueListening.shape)
            .background(Color.White.copy(alpha = 0.05f)) // Frost background
            .border(1.dp, Color.White.copy(alpha = 0.1f), CardGeometry.ContinueListening.shape)
            .bouncyClickable(onClick = onClick)
            .padding(CardGeometry.ContinueListening.contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Album art with Play button overlay. The artwork uses a FIXED size so
        // the card's height is bounded and the metadata beside it always fits.
        // Deriving it from the card width via aspectRatio() (or stacking it
        // above the text) overflowed the landscape viewport and clipped the
        // title and artist away entirely.
        Box(
            modifier = Modifier
                .size(CardGeometry.ContinueListening.artworkSize)
                .clip(RoundedCornerShape(CardGeometry.ContinueListening.cornerRadius / 1.5f))
                .background(Brush.linearGradient(listOf(Color(0xFF2A2A35), Color(0xFF1E1E28)))),
            contentAlignment = Alignment.Center,
        ) {
            if (music.thumbnailUrl.isNotEmpty()) {
                AsyncImage(
                    model = coil3.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                        .data(music.thumbnailUrl)
                        .crossfade(true)
                        .build(),
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
                    imageVector = Icons.Filled.PlayCircle,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Metadata sits beside the artwork so the card height stays bounded.
        // weight(1f) lets the text take the remaining width and ellipsize long
        // titles/artists instead of forcing the row wider than the card.
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            DynamicLabel(
                text = music.title,
                backgroundColor = Color.Black,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 15.sp),
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            SecondaryLabel(
                text = music.artist,
                backgroundColor = Color.Black,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
