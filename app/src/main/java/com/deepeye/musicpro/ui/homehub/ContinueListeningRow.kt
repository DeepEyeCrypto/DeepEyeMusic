// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.homehub

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.musicpro.domain.model.home.HomeMusicItem
import com.deepeye.musicpro.ui.components.DynamicLabel
import com.deepeye.musicpro.ui.components.NeonMediaCard
import com.deepeye.musicpro.ui.components.toNeonCardData
import com.deepeye.musicpro.ui.theme.CardGeometry

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
            modifier =
                Modifier
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
                            modifier =
                                Modifier
                                    .width(CardGeometry.Video.minWidth)
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
    NeonMediaCard(
        data = music.toNeonCardData(contentType = "music"),
        onClick = onClick,
        modifier = modifier,
    )
}
