// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.deepeye.musicpro.domain.model.home.HomeVideoItem
import com.deepeye.musicpro.ui.components.GlassContainer
import com.deepeye.musicpro.ui.LocalHazeState

@Composable
fun NetMirrorScreen(
    modifier: Modifier = Modifier,
    viewModel: NetMirrorViewModel = hiltViewModel(),
    onExpandPlayer: () -> Unit = {}
) {
    val hazeState = LocalHazeState.current
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF00E5C3))
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp) // Leave space for dock
    ) {
        // Hero Banner Section
        uiState.heroMovie?.let { hero ->
            item {
                HeroBanner(
                    movie = hero,
                    onPlay = { 
                        viewModel.playVideo(hero)
                        onExpandPlayer()
                    }
                )
            }
        }

        // Horizontal Category Rows
        if (uiState.trendingMovies.isNotEmpty()) {
            item {
                CategoryRow(
                    title = "Trending Now", 
                    items = uiState.trendingMovies,
                    onPlay = { movie -> 
                        viewModel.playVideo(movie)
                        onExpandPlayer()
                    }
                )
            }
        }
        
        if (uiState.sciFiMovies.isNotEmpty()) {
            item {
                CategoryRow(
                    title = "Sci-Fi & Fantasy", 
                    items = uiState.sciFiMovies,
                    onPlay = { movie -> 
                        viewModel.playVideo(movie)
                        onExpandPlayer()
                    }
                )
            }
        }

        if (uiState.newReleases.isNotEmpty()) {
            item {
                CategoryRow(
                    title = "New Releases", 
                    items = uiState.newReleases,
                    onPlay = { movie -> 
                        viewModel.playVideo(movie)
                        onExpandPlayer()
                    }
                )
            }
        }
    }
}

@Composable
private fun HeroBanner(
    movie: HomeVideoItem,
    onPlay: () -> Unit
) {
    val hazeState = LocalHazeState.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(500.dp)
            .background(Color.Transparent)
    ) {
        // Hero Background Image
        AsyncImage(
            model = movie.thumbnailUrl.replace("hqdefault", "maxresdefault"),
            contentDescription = movie.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Gradient overlay for bottom fading into black
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.5f),
                            Color.Black
                        ),
                        startY = 300f
                    )
                )
        )

        // Hero Content
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = movie.title,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp
                ),
                maxLines = 2,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = movie.channelName, color = Color.LightGray, fontSize = 14.sp)
                Text(text = "•", color = Color.LightGray, fontSize = 14.sp)
                Text(text = "4K HDR", color = Color(0xFF00E5C3), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(24.dp))
            
            // Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Play Button
                Button(
                    onClick = onPlay,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = "Play", tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Play", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                
                // Info Button (Glassmorphism)
                GlassContainer(
                    tintColor = Color.White.copy(alpha = 0.2f),
                    hazeState = hazeState,
                    cornerRadius = 8.dp,
                    modifier = Modifier
                        .height(48.dp)
                        .clickable { /* TODO */ }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(imageVector = Icons.Filled.Info, contentDescription = "More Info", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("More Info", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(
    title: String, 
    items: List<HomeVideoItem>,
    onPlay: (HomeVideoItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            ),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
        )
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items.size) { index ->
                VideoCard(
                    movie = items[index],
                    onClick = { onPlay(items[index]) }
                )
            }
        }
    }
}

@Composable
private fun VideoCard(
    movie: HomeVideoItem,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(130.dp)
            .height(190.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF2C2C34))
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = movie.thumbnailUrl,
            contentDescription = movie.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        // Gradient for text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)),
                        startY = 100f
                    )
                )
        )
        
        Text(
            text = movie.title,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp),
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}
