package com.deepeye.musicpro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/**
 * CardAvatar renders a circular channel avatar with YouTube hqdefault fallback.
 * Falls back to the standard YouTube default thumbnail derived from the video ID
 * when the avatarUrl is blank — matching the For You card standard.
 * 
 * This composable mirrors the avatar logic from VideoCard.kt
 * (item.channelAvatarUrl.ifBlank { "https://i.ytimg.com/vi/${item.id}/default.jpg" }).
 */
@Composable
fun CardAvatar(
    avatarUrl: String?,
    videoId: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
) {
    val resolvedUrl = avatarUrl?.takeIf { it.isNotBlank() }
        ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0x22FFFFFF)),
    ) {
        AsyncImage(
            model = resolvedUrl,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    }
}