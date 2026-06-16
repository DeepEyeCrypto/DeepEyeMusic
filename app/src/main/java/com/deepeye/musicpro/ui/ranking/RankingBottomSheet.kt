// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.ranking

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepeye.musicpro.domain.ranking.RankingEngine
import com.deepeye.musicpro.domain.ranking.RankingRepository
import com.deepeye.musicpro.domain.ranking.UserRank
import com.deepeye.musicpro.ui.components.glassCard
import com.deepeye.musicpro.ui.components.hoverable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingBottomSheet(
    rankingRepository: RankingRepository,
    rankingEngine: RankingEngine,
    onDismissRequest: () -> Unit
) {
    val topUsers by rankingRepository.getTopUsers(100).collectAsStateWithLifecycle(initialValue = emptyList())
    val currentUserId = rankingRepository.getCurrentUserId()
    
    // We observe the current user rank specifically
    val currentUserRank by remember(currentUserId) {
        if (currentUserId != null) rankingRepository.observeUserRank(currentUserId)
        else kotlinx.coroutines.flow.flowOf(null)
    }.collectAsStateWithLifecycle(initialValue = null)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = Color.Transparent,
        modifier = Modifier.fillMaxHeight(0.9f) // Tall sheet
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .glassCard(elevation = 12.dp, cornerRadius = 24.dp)
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = "Global Leaderboard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                textAlign = TextAlign.Center
            )

            if (currentUserRank != null) {
                Text(
                    text = "Your Standing",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                UserProfileCard(
                    userRank = currentUserRank!!,
                    rankingEngine = rankingEngine
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Top Listeners",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(topUsers, key = { it.userId }) { user ->
                    LeaderboardItem(
                        userRank = user,
                        isCurrentUser = user.userId == currentUserId,
                        rankingEngine = rankingEngine
                    )
                }
            }
        }
    }
}

@Composable
fun LeaderboardItem(
    userRank: UserRank,
    isCurrentUser: Boolean,
    rankingEngine: RankingEngine,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isCurrentUser) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .glassCard(elevation = 4.dp, cornerRadius = 12.dp)
            .hoverable(scale = 1.02f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#${userRank.rank}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(48.dp),
                color = when (userRank.rank) {
                    1 -> Color(0xFFFFD700) // Gold
                    2 -> Color(0xFFC0C0C0) // Silver
                    3 -> Color(0xFFCD7F32) // Bronze
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = userRank.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = "Tier: ${rankingEngine.getUserTier(userRank.rank)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "${userRank.score.toInt()} pts",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
