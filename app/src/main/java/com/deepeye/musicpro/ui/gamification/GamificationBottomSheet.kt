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
            .padding(24.dp)
    ) {
        // Points Header
        Text(
            text = "${rewardPoints.totalPoints} pts",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFFD700), // Gold
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        
        Text(
            text = "Reward Points",
            fontSize = 16.sp,
            color = Color.Gray,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Streak Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(Color(0xFF1E1E1E)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        tint = Color(0xFFFF6B35),
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Current Streak",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "${streak.currentStreak} days",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF6B35)
                )
                Text(
                    text = "Longest: ${streak.longestStreak} days",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Badges Grid
        Text(
            text = "Your Badges",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
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
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(Color(0xFFFFD700)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Claim Daily Reward", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }
    }
}

@Composable
fun BadgeCard(badge: UserAchievement, isLocked: Boolean) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .aspectRatio(1f),
        colors = CardDefaults.cardColors(
            containerColor = if (isLocked) Color(0xFF2A2A2A) else Color(0xFF1E1E1E)
        ),
        shape = RoundedCornerShape(12.dp)
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
                    tint = Color.Gray,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
            } else {
                Icon(
                    painter = painterResource(id = badge.iconResId),
                    tint = Color(0xFFFFD700),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = badge.title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                color = if (isLocked) Color.Gray else Color.White,
                maxLines = 2
            )
        }
    }
}
