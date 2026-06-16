// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.gamification

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.musicpro.domain.gamification.ListeningStreak
import com.deepeye.musicpro.domain.gamification.RewardPoints
import com.deepeye.musicpro.domain.gamification.UserAchievement
import com.deepeye.musicpro.ui.components.glassCard
import com.deepeye.musicpro.ui.components.hoverable

@Composable
fun GamificationBottomSheet(
    streak: ListeningStreak,
    rewardPoints: RewardPoints,
    unlockedBadges: List<UserAchievement>,
    lockedBadges: List<UserAchievement>,
    onClaimReward: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(elevation = 12.dp, cornerRadius = 24.dp)
            .padding(24.dp)
    ) {
        // Points Header
        Text(
            text = "${rewardPoints.totalPoints} pts",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary, // Gold
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        
        Text(
            text = "Reward Points",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Streak Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(elevation = 8.dp),
            colors = CardDefaults.cardColors(Color.Transparent)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        tint = MaterialTheme.colorScheme.secondary, // Orange
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Current Streak",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "${streak.currentStreak} days",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "Longest: ${streak.longestStreak} days",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Badges Grid
        Text(
            text = "Your Badges",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxWidth().height(300.dp) // Fixed height to avoid nested scroll issues
        ) {
            items(unlockedBadges) { badge ->
                BadgeCard(badge = badge, isLocked = false)
            }
            
            items(lockedBadges) { badge ->
                BadgeCard(badge = badge, isLocked = true)
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Claim Reward Button
        Button(
            onClick = onClaimReward,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .hoverable(scale = 1.02f),
            colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "Claim Daily Reward", 
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
fun BadgeCard(badge: UserAchievement, isLocked: Boolean) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .aspectRatio(1f)
            .glassCard(elevation = 6.dp, cornerRadius = 12.dp)
            .hoverable(scale = 1.05f),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isLocked) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
            } else {
                Icon(
                    painter = painterResource(id = badge.iconResId),
                    tint = MaterialTheme.colorScheme.primary, // Gold
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = badge.title,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                color = if (isLocked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground,
                maxLines = 2
            )
        }
    }
}
