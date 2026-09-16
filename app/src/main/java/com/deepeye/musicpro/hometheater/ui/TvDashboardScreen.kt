// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.hometheater.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.deepeye.musicpro.hometheater.model.MediaMetadata
import com.deepeye.musicpro.hometheater.model.MediaType

@Composable
fun TvDashboardScreen(
    movies: List<MediaMetadata>,
    onMediaSelected: (MediaMetadata) -> Unit,
    onBack: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("Movies") }
    var backgroundUrl by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Dynamic Fanart Background
        AnimatedContent(targetState = backgroundUrl, label = "fanart_bg") { url ->
            if (url != null) {
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color(0xFF0F172A), Color.Black))
                ))
            }
        }

        // Dark Vignette / Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)))
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))))
        )

        Row(modifier = Modifier.fillMaxSize().padding(start = 56.dp, top = 40.dp, bottom = 40.dp)) {
            // Left Navigation Menu
            Column(
                modifier = Modifier.width(200.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                listOf("Search", "Movies", "TV Shows", "Music", "Photos", "Add-ons", "Settings").forEach { category ->
                    TvMenuButton(
                        text = category,
                        isSelected = category == selectedCategory,
                        onFocus = { selectedCategory = category }
                    )
                }
            }

            Spacer(modifier = Modifier.width(32.dp))

            // Main Content Area
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = selectedCategory,
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                if (selectedCategory == "Movies") {
                    Text(
                        text = "Recently Added",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(movies.take(15)) { movie ->
                            TvPosterCard(
                                title = movie.title,
                                posterUrl = movie.posterUrl,
                                onFocus = { backgroundUrl = movie.fanartUrl ?: movie.posterUrl },
                                onClick = { onMediaSelected(movie) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TvMenuButton(text: String, isSelected: Boolean, onFocus: () -> Unit) {
    var hasFocus by remember { mutableStateOf(false) }
    
    val bgColor by animateColorAsState(
        if (hasFocus) Color.White.copy(alpha = 0.2f) else Color.Transparent, label = "bg"
    )
    val textColor by animateColorAsState(
        if (hasFocus) Color.White else if (isSelected) Color(0xFF38BDF8) else Color.Gray, label = "text"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .onFocusChanged { state ->
                hasFocus = state.isFocused
                if (state.isFocused) onFocus()
            }
            .focusable()
            .padding(16.dp)
    ) {
        Text(text = text, color = textColor, fontSize = 20.sp)
    }
}

@Composable
fun TvPosterCard(title: String, posterUrl: String?, onFocus: () -> Unit, onClick: () -> Unit) {
    var hasFocus by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (hasFocus) 1.1f else 1.0f, label = "scale")
    val outlineAlpha by animateFloatAsState(if (hasFocus) 1f else 0f, label = "outline")

    Column(
        modifier = Modifier
            .width(160.dp)
            .onFocusChanged {
                hasFocus = it.isFocused
                if (it.isFocused) onFocus()
            }
            .focusable()
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(160.dp * scale)
                .height(240.dp * scale)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.DarkGray)
        ) {
            if (posterUrl != null) {
                AsyncImage(
                    model = posterUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // Focus outline
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = outlineAlpha * 0.1f))
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = title,
            color = if (hasFocus) Color.White else Color.Gray,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
