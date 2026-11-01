// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.deepeye.musicpro.domain.model.profile.ProfilePhase
import com.deepeye.musicpro.domain.model.profile.ProfileState
import com.deepeye.musicpro.ui.theme.boundedContent

private val neonCyan = Color(0xFF00E5FF)
private val darkSurface = Color(0xFF131722).copy(alpha = 0.85f)

// ── Tab placeholder definitions (P1: non-functional) ────────────────────────
data class ProfileTab(val label: String, val icon: ImageVector)

private val TABS = listOf(
    ProfileTab("Liked", Icons.Default.Favorite),
    ProfileTab("History", Icons.Default.History),
    ProfileTab("Watch Later", Icons.Default.Folder),
    ProfileTab("Playlists", Icons.Default.Folder),
    ProfileTab("Subscribed", Icons.Default.Subscriptions),
)

// ── Screen ───────────────────────────────────────────────────────────────────
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    windowSizeClass: WindowSizeClass,
    viewModel: ProfileViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onConnectAccount: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Persist counters on screen teardown so offline first-launch can show
    // last-known values.
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose { viewModel.persistCache() }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Profile", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .boundedContent(),
        ) {
            when (state.phase) {
                    ProfilePhase.LOADING ->
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = neonCyan)
                        }

                    ProfilePhase.SIGNED_OUT ->
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = Color.White.copy(alpha = 0.3f),
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = "Sign in to view your profile",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 16.sp,
                                )
                                Spacer(Modifier.height(24.dp))
                                androidx.compose.material3.Button(
                                    onClick = onConnectAccount,
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = neonCyan,
                                        contentColor = Color.Black,
                                    ),
                                ) {
                                    Text("Connect YouTube Account", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                    ProfilePhase.ERROR ->
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = state.errorMessage ?: "Something went wrong",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 14.sp,
                                )
                                Spacer(Modifier.height(16.dp))
                                androidx.compose.material3.OutlinedButton(
                                    onClick = { /* P1: no retry logic yet */ },
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Retry")
                                }
                            }
                        }

                    ProfilePhase.READY ->
                        ProfileContent(state = state)
                }
            }
        }
    }

// ── Ready-state content ─────────────────────────────────────────────────────
@Composable
private fun ProfileContent(state: ProfileState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        // Avatar + name block
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Avatar
                Box(
                    modifier =
                        Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!state.avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = state.avatarUrl,
                            contentDescription = "Profile avatar",
                            modifier = Modifier
                                .matchParentSize()
                                .clip(CircleShape),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Profile avatar",
                            modifier = Modifier.size(56.dp),
                            tint = neonCyan.copy(alpha = 0.6f),
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = state.displayName ?: "DeepEye User",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )

                state.email?.let { email ->
                    Text(
                        text = email,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                    )
                }

                Spacer(Modifier.height(4.dp))

                if (state.isFromCache) {
                    Text(
                        text = "Showing cached data — last synced ${
                            java.text.SimpleDateFormat(
                                "d MMM HH:mm",
                                java.util.Locale.getDefault()
                            ).format(java.util.Date(state.lastSyncedAt))
                        }",
                        color = Color.White.copy(alpha = 0.35f),
                        fontSize = 11.sp,
                    )
                }
            }
        }

        // Stats chips
        StatsRow(state = state)

        Spacer(Modifier.height(20.dp))

        // Tab placeholders (P1: non-functional)
        Text(
            text = "My Library",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
        )

        Spacer(Modifier.height(4.dp))

        TabRowPlaceholder(tabs = TABS)

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Content sections appear here in P2.",
            color = Color.White.copy(alpha = 0.35f),
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

@Composable
private fun StatsRow(state: ProfileState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
    ) {
        StatChip(
            count = state.subscribedCount,
            label = "Subscribed",
            modifier = Modifier.weight(1f),
        )
        StatChip(
            count = state.likedCount,
            label = "Liked",
            modifier = Modifier.weight(1f),
        )
        StatChip(
            count = state.playlistCount,
            label = "Playlists",
            modifier = Modifier.weight(1f),
        )
        StatChip(
            count = state.downloadCount,
            label = "Downloads",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatChip(count: Int, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.padding(horizontal = 4.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = darkSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "$count",
                color = neonCyan,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun TabRowPlaceholder(tabs: List<ProfileTab>) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 4.dp),
    ) {
        items(tabs) { tab ->
            TabPlaceholderChip(tab = tab)
        }
    }
}

@Composable
private fun TabPlaceholderChip(tab: ProfileTab) {
    Card(
        colors = CardDefaults.cardColors(containerColor = darkSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = tab.label,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 13.sp,
            )
        }
    }
}
