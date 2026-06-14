// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.components.premium

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.deepeye.musicpro.domain.model.home.HomeMusicItem
import com.deepeye.musicpro.domain.model.home.HomeVideoItem

@Composable
fun SplitMediaHero(
    video: HomeVideoItem,
    music: HomeMusicItem,
    modifier: Modifier = Modifier,
    onVideoClick: () -> Unit = {},
    onMusicClick: () -> Unit = {},
) {
    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PremiumHeroCard(
            title = video.title,
            subtitle = video.channelName,
            imageUrl = video.thumbnailUrl,
            badge = "Featured Video",
            onClick = onVideoClick,
            modifier = Modifier.weight(1f),
        )

        PremiumHeroCard(
            title = music.title,
            subtitle = music.artist,
            imageUrl = music.thumbnailUrl,
            badge = "Featured Artist",
            onClick = onMusicClick,
            modifier = Modifier.weight(1f),
        )
    }
}
