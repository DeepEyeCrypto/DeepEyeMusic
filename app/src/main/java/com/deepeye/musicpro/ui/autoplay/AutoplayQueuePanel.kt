package com.deepeye.musicpro.ui.autoplay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.deepeye.musicpro.domain.autoplay.AutoplayMode
import com.deepeye.musicpro.domain.autoplay.AutoplayState
import com.deepeye.musicpro.domain.autoplay.QueueItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoplayQueuePanel(
    state: AutoplayState,
    onPlayNext: (String) -> Unit,
    onRemove: (String) -> Unit,
    onModeChange: (AutoplayMode) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF101018), Color(0xFF161622)),
                ),
            ),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Up Next", style = MaterialTheme.typography.titleMedium, color = Color.White)
                Text(
                    when {
                        state.discoveryMode -> "Discovering new music"
                        state.familiarMode -> "Playing familiar favorites"
                        else -> "Balanced autoplay"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.45f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.familiarMode,
                    onClick = { onModeChange(AutoplayMode.FAMILIAR) },
                    label = { Text("Familiar") },
                )
                FilterChip(
                    selected = state.discoveryMode,
                    onClick = { onModeChange(AutoplayMode.DISCOVERY) },
                    label = { Text("Discover") },
                )
            }
        }

        if (state.queue.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                Text("Queue is empty. It will populate automatically.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ) {
                items(state.queue, key = { it.videoId }) { item ->
                    QueueItemRow(
                        item = item,
                        onPlayNext = { onPlayNext(item.videoId) },
                        onRemove = { onRemove(item.videoId) },
                    )
                }
            }
        }
    }
}

@Composable
fun QueueItemRow(
    item: QueueItem,
    onPlayNext: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .clickable { onPlayNext() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = "https://img.youtube.com/vi/${item.videoId}/hqdefault.jpg",
            contentDescription = null,
            modifier =
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.08f)),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.White)
            Text(
                item.reason,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.45f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        TextButton(onClick = onRemove) { Text("Remove", color = MaterialTheme.colorScheme.primary) }
    }
}
