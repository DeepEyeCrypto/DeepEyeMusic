package com.deepeye.musicpro.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.deepeye.musicpro.domain.model.search.SearchFilter

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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        filters.forEach { filter ->
            FilterChip(
                selected = filter == selected,
                onClick = { onSelected(filter) },
                label = { Text(filter.name.lowercase().replaceFirstChar { it.uppercase() }) },
                leadingIcon =
                if (filter == selected) {
                    { Icon(Icons.Rounded.Check, contentDescription = null) }
                } else {
                    null
                },
            )
        }
    }
}
