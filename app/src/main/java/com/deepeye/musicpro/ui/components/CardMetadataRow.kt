package com.deepeye.musicpro.ui.components

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * CardMetadataRow renders the metadata row "channelName · views · uploadDate"
 * matching the For You card standard. Fields set to null are simply omitted
 * from the row rather than showing "0 views" or "—".
 *
 * Usage in cards: CardMetadataRow(
 *     channelName = item.channelName,
 *     viewCount = item.viewCount,
 *     uploadDate = item.uploadDate,
 * )
 *
 * This mirrors the metadata logic from VideoCard.kt:
 * - item.channelName ?: "Unknown channel"
 * - item.viewCount?.formatCountShort() formats view count (12M views, 840K views, or 1234 views)
 * - We filter out "0 views" entries rather than showing them
 * - item.uploadDate ?: "—"
 */
@Composable
fun CardMetadataRow(
    channelName: String?,
    viewCount: Long?,
    uploadDate: String?,
    modifier: Modifier = Modifier,
) {
    // Format view count using the same logic as VideoCard.formatCountShort
    val viewCountStr = viewCount?.let { vc ->
        when {
            vc >= 1_000_000L -> "%.1fM views".format(vc / 1000000.0)
            vc >= 1_000L -> "%.1fK views".format(vc / 1000.0)
            else -> "$vc views"
        }
    }?.takeIf { it.isNotBlank() && it != "0 views" && it != "0" }
    val uploadDateStr = uploadDate?.takeIf { it.isNotBlank() && it != "—" }

    val metaParts = buildList {
        add(channelName?.takeIf { it.isNotBlank() })
        add(viewCountStr)
        add(uploadDateStr)
    }
    .filter { it != null }

    if (metaParts.isNotEmpty()) {
        Text(
            text = metaParts.joinToString(" • "),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.55f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier,
        )
    }
}