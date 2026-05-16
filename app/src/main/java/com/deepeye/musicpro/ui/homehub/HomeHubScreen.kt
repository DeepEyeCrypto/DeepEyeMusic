package com.deepeye.musicpro.ui.homehub

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.deepeye.musicpro.domain.model.home.HomeVideoItem
import com.deepeye.musicpro.domain.model.home.HomeMusicItem
import com.deepeye.musicpro.ui.components.ShimmerBox

@Composable
fun HomeHubScreen(
    onNavigateToVideo: (String) -> Unit,
    onNavigateToMusic: (String) -> Unit,
    onNavigateToLibrary: () -> Unit,
    onOpenV4A: () -> Unit,
    viewModel: HomeHubViewModel = hiltViewModel()
) {
    val feedState     by viewModel.feedState.collectAsStateWithLifecycle()
    val gainBudget    by viewModel.gainBudget.collectAsStateWithLifecycle()
    val currentPreset by viewModel.currentPresetName.collectAsStateWithLifecycle()
    val audioRoute    by viewModel.audioRoute.collectAsStateWithLifecycle()
    val fftData       by viewModel.fftData.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A14)),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // 1. Greeting
        item {
            HomeGreetingHeader()
        }

        // 2. DSP Quick Panel (always visible)
        item {
            DspQuickPanel(
                currentPreset = currentPreset,
                gainBudget    = gainBudget,
                audioRoute    = audioRoute,
                fftData       = fftData,
                onPresetClick = onOpenV4A,
                onV4AOpen     = onOpenV4A,
                modifier      = Modifier.padding(horizontal = 16.dp)
            )
        }

        // 3. Trending Videos Rail
        if (feedState.isLoading) {
            item {
                HomeRailShimmer(title = "🔥 Trending")
            }
        } else if (feedState.trending.isNotEmpty()) {
            item {
                HomeVideoRail(
                    title  = "🔥 Trending",
                    items  = feedState.trending,
                    onClick = { 
                        android.util.Log.e("HomeHubScreen", "Trending clicked: ${it.id}")
                        viewModel.playVideo(it)
                        onNavigateToVideo(it.id) 
                    }
                )
            }
        }

        // 4. Shorts Rail
        if (feedState.shorts.isNotEmpty()) {
            item {
                ShortsRail(
                    items   = feedState.shorts,
                    onClick = { 
                        android.util.Log.e("HomeHubScreen", "Short clicked: ${it.id}")
                        viewModel.playVideo(it)
                        onNavigateToVideo(it.id) 
                    }
                )
            }
        }

        // 5. Quick Picks (Music)
        if (feedState.quickPicks.isNotEmpty()) {
            item {
                HomeMusicRail(
                    title  = "🎵 Quick Picks",
                    items  = feedState.quickPicks,
                    onClick = { 
                        android.util.Log.e("HomeHubScreen", "Music clicked: ${it.id}")
                        viewModel.playMusic(it)
                        onNavigateToMusic(it.id) 
                    }
                )
            }
        }

        // 6. Offline fallback
        if (feedState.isOffline && !feedState.isLoading) {
            item {
                OfflineFallbackCard(
                    onRetry = viewModel::loadFeed
                )
            }
        }
    }
}

@Composable
private fun HomeGreetingHeader() {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {
        Text(
            text = "Good Morning",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Ready for some premium sound?",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
        )
    }
}

@Composable
private fun HomeVideoRail(
    title: String,
    items: List<HomeVideoItem>,
    onClick: (HomeVideoItem) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            title,
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFFE0E0E0),
            fontWeight = FontWeight.Bold
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(items, key = { it.id }) { video ->
                VideoCard(item = video, onClick = onClick)
            }
        }
    }
}

@Composable
private fun ShortsRail(
    items: List<HomeVideoItem>,
    onClick: (HomeVideoItem) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "📱 Shorts",
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFFE0E0E0),
            fontWeight = FontWeight.Bold
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(items, key = { it.id }) { short ->
                VideoCard(
                    item = short, 
                    onClick = onClick,
                    modifier = Modifier.width(160.dp) // Narrower for shorts
                )
            }
        }
    }
}

@Composable
private fun HomeMusicRail(
    title: String,
    items: List<HomeMusicItem>,
    onClick: (HomeMusicItem) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            title,
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFFE0E0E0),
            fontWeight = FontWeight.Bold
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(items, key = { it.id }) { music ->
                // Use a simple music card or VideoCard for now
                Card(
                    modifier = Modifier.width(160.dp).clickable { onClick(music) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))
                ) {
                    Column(Modifier.padding(8.dp)) {
                        Box(Modifier.fillMaxWidth().aspectRatio(1f).background(Color.DarkGray))
                        Spacer(Modifier.height(8.dp))
                        Text(music.title, style = MaterialTheme.typography.bodySmall, color = Color.White, maxLines = 1)
                        Text(music.artist, style = MaterialTheme.typography.labelSmall, color = Color.Gray, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeRailShimmer(title: String) {
    Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, modifier = Modifier.padding(horizontal = 20.dp), color = Color.White)
        Row(Modifier.padding(horizontal = 20.dp)) {
            repeat(3) {
                ShimmerBox(Modifier.size(140.dp))
                Spacer(Modifier.width(12.dp))
            }
        }
    }
}

@Composable
private fun OfflineFallbackCard(onRetry: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A2E)
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Rounded.WifiOff,
                contentDescription = null,
                tint = Color(0xFF9E9E9E),
                modifier = Modifier.size(48.dp)
            )
            Text(
                "Offline Mode",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFE0E0E0)
            )
            Text(
                "YouTube content unavailable.\nLocal library still works!",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9E9E9E),
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7B3FE4)
                )
            ) {
                Text("Retry")
            }
        }
    }
}
