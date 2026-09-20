package com.deepeye.musicpro.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.musicpro.domain.model.search.SearchFilter

// ── Glassmorphism Palette ───────────────────────────────────────────────────
private val GlassContainer = Color(0xFF131722).copy(alpha = 0.85f)
private val NeonCyanAccent = Color(0xFF00E5FF)
private val GlassBorder = Color(0x22FFFFFF)
private val TextWhite = Color.White

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchFilterBar(
    selected: SearchFilter,
    onSelected: (SearchFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val filters =
        listOf(
            SearchFilter.ALL,
            SearchFilter.SONGS,
            SearchFilter.ARTISTS,
            SearchFilter.ALBUMS,
            SearchFilter.VIDEOS,
            SearchFilter.PLAYLISTS,
            SearchFilter.DOWNLOADED,
        )

    FlowRow(
        modifier = modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        filters.forEach { filter ->
            val isSelected = filter == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelected(filter) },
                label = {
                    Text(
                        text = filter.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                    )
                },
                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                leadingIcon =
                if (isSelected) {
                    { Icon(Icons.Rounded.Check, contentDescription = null, tint = NeonCyanAccent) }
                } else {
                    null
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = GlassContainer,
                    labelColor = TextWhite,
                    selectedContainerColor = GlassContainer,
                    selectedLabelColor = NeonCyanAccent,
                    selectedLeadingIconColor = NeonCyanAccent,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = GlassBorder,
                    selectedBorderColor = NeonCyanAccent,
                    borderWidth = 1.dp,
                    selectedBorderWidth = 1.dp,
                ),
            )
        }
    }
}