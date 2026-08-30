package com.deepeye.musicpro.tv.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import coil3.compose.AsyncImage
import com.deepeye.musicpro.tv.ui.theme.TVTheme

data class MediaItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val imageUrl: String,
    val progress: Float = 0f
)

data class Section(
    val title: String,
    val items: List<MediaItem>
)

@Composable
fun TvHomeScreen(
    sections: List<Section> = emptyList(),
    onItemSelected: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var focusedItem by remember { mutableStateOf<MediaItem?>(null) }
    var backgroundAlpha by remember { mutableStateOf(0.3f) }
    
    val animatedAlpha = animateFloatAsState(
        targetValue = backgroundAlpha,
        animationSpec = tween(durationMillis = 800),
        label = "backgroundAlpha"
    )
    
    Box(modifier = modifier.fillMaxSize()) {
        focusedItem?.imageUrl?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(animatedAlpha.value),
                contentScale = ContentScale.Crop
            )
        }
        
        TvLazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            sections.forEach { section ->
                item {
                    SectionHeader(title = section.title)
                }
                
                item {
                    MediaRow(
                        items = section.items,
                        onItemFocused = { item ->
                            focusedItem = item
                            backgroundAlpha = 0.3f
                        },
                        onItemClicked = onItemSelected
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    androidx.tv.material3.Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
private fun MediaRow(
    items: List<MediaItem>,
    onItemFocused: (MediaItem) -> Unit,
    onItemClicked: (MediaItem) -> Unit
) {
    TvLazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(end = 24.dp)
    ) {
        items(items) { item ->
            MediaCard(
                item = item,
                onFocused = onItemFocused,
                onClick = onItemClicked
            )
        }
    }
}

@Composable
private fun MediaCard(
    item: MediaItem,
    onFocused: (MediaItem) -> Unit,
    onClick: (MediaItem) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale = animateFloatAsState(
        targetValue = if (isFocused) 1.1f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "scale"
    )
    
    Card(
        onClick = { onClick(item) },
        modifier = Modifier
            .width(240.dp)
            .height(135.dp)
            .scale(scale.value),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(4.dp)) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            if (item.progress > 0f) {
                androidx.tv.material3.LinearProgressIndicator(
                    progress = item.progress,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    indicatorColor = MaterialTheme.colorScheme.primary
                )
            }
            
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                    .padding(8.dp)
            ) {
                androidx.tv.material3.Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
