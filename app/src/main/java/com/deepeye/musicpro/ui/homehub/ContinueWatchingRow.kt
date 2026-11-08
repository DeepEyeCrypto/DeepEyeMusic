// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.homehub

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deepeye.musicpro.domain.model.home.HomeVideoItem
import com.deepeye.musicpro.ui.components.NeonMediaCard
import com.deepeye.musicpro.ui.components.toNeonCardData
import com.deepeye.musicpro.ui.theme.CardGeometry

/**
 * Continue Watching horizontal rail — shows partially watched videos with progress indicators.
 * Premium glassmorphic cards with gradient progress overlay.
 */
@Composable
fun ContinueWatchingRow(
    items: List<HomeVideoItem>,
    onItemClick: (HomeVideoItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "▶️ Continue Watching",
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )

        val displayedItems = items.take(4)
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            displayedItems.chunked(2).forEach { chunk ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    chunk.forEach { video ->
                        ContinueWatchingCard(
                            video = video,
                            onClick = { onItemClick(video) },
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
private fun ContinueWatchingCard(
    video: HomeVideoItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NeonMediaCard(
        data =
            video.toNeonCardData(
                progressFraction = video.progressPercent.takeIf { it > 0f }?.let { it / 100f },
                contentType = "video",
            ),
        onClick = onClick,
        modifier = modifier,
    )
}
