package com.deepeye.musicpro.tv.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import coil3.compose.AsyncImage
import com.deepeye.musicpro.tv.ui.theme.TVTheme

@Composable
fun TvDetailsScreen(
    item: MediaItem,
    relatedItems: List<MediaItem> = emptyList(),
    onPlayClicked: () -> Unit,
    onRelatedItemSelected: (MediaItem) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    TvLazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            // Hero section with artwork and details
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Artwork
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.title,
                    modifier = Modifier
                        .width(300.dp)
                        .height(200.dp),
                    contentScale = ContentScale.Crop
                )
                
                Spacer(modifier = Modifier.width(32.dp))
                
                // Details
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    androidx.tv.material3.Text(
                        text = item.title,
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    androidx.tv.material3.Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    
                    // Description placeholder
                    androidx.tv.material3.Text(
                        text = "This is a sample description for the media content. It provides additional context about the video or audio.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // Play button
                    Button(
                        onClick = onPlayClicked,
                        modifier = Modifier
                            .height(56.dp)
                            .width(200.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        androidx.tv.material3.Text(
                            text = "Play",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
        
        // Related content
        if (relatedItems.isNotEmpty()) {
            item {
                androidx.tv.material3.Text(
                    text = "Related Content",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            
            item {
                TvLazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 12.dp)
                ) {
                    items(relatedItems) { related ->
                        RelatedItemCard(
                            item = related,
                            onClick = { onRelatedItemSelected(related) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RelatedItemCard(
    item: MediaItem,
    onClick: () -> Unit
) {
    androidx.tv.material3.Card(
        onClick = onClick,
        modifier = Modifier
            .width(180.dp)
            .height(100.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                    .padding(8.dp)
            ) {
                androidx.tv.material3.Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
