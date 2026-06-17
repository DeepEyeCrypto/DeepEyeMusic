package com.deepeye.musicpro.ui.gamification

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.deepeye.musicpro.domain.ranking.UserRank
import com.deepeye.musicpro.ui.components.glassCard

// Premium Colors
private val GoldPrimary = Color(0xFFFFD700)
private val GoldDark = Color(0xFFB8860B)
private val NeonCyan = Color(0xFF00E5FF)
private val FlameOrange = Color(0xFFFF6B35)
private val DeepBlack = Color(0xFF0D0D0D)
private val CardSurface = Color(0xFF151515)
private val CardSurfaceLight = Color(0xFF1C1C1C)
private val SubtleWhite = Color(0xFFE0E0E0)

@Composable
fun Top3LeaderboardCard(
    top3Users: List<UserRank>,
    currentStreak: Int,
    totalPoints: Int,
    onMoreClick: () -> Unit,
    onPointsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassCard(
                elevation = 8.dp, 
                cornerRadius = 20.dp,
                gradientTop = Color(0xFF2A2A2A).copy(alpha = 0.7f),
                gradientBottom = Color(0xFF222222).copy(alpha = 0.7f)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
        ) {
            // ═══════════════════════════════════════
            // SECTION 1: STREAK HEADER
            // ═══════════════════════════════════════

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Flame glow
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    FlameOrange.copy(alpha = 0.25f),
                                    Color.Transparent
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "Streak",
                        tint = FlameOrange,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "$currentStreak-day Streak",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Keep it going!",
                        color = FlameOrange.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                // Active Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(FlameOrange.copy(alpha = 0.2f), GoldPrimary.copy(alpha = 0.15f))
                            )
                        )
                        .border(1.dp, FlameOrange.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "🔥 Active",
                        color = FlameOrange,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Streak Progress Bar — premium gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF222222))
            ) {
                val targetStreak = 7
                val progress = (currentStreak.toFloat() / targetStreak.toFloat()).coerceIn(0f, 1f)
                val animatedProgress by animateFloatAsState(
                    targetValue = progress,
                    animationSpec = tween(1000),
                    label = "streak_progress"
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(FlameOrange, GoldPrimary, Color(0xFFFFF176))
                            )
                        )
                )
            }

            val remaining = (7 - currentStreak).coerceAtLeast(0)
            Text(
                text = if (remaining > 0) "$remaining more days for bonus!" else "🎉 Weekly Bonus Unlocked!",
                fontSize = 11.sp,
                color = GoldPrimary.copy(alpha = 0.5f),
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 6.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // ═══════════════════════════════════════
            // SECTION 2: POINTS + RANKING CHIPS
            // ═══════════════════════════════════════

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Points Chip
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    NeonCyan.copy(alpha = 0.12f),
                                    NeonCyan.copy(alpha = 0.04f)
                                )
                            )
                        )
                        .border(1.dp, NeonCyan.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        .clickable { onPointsClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            tint = NeonCyan,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "$totalPoints",
                                color = NeonCyan,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                text = "Points",
                                color = NeonCyan.copy(alpha = 0.5f),
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // Ranking Chip
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    GoldPrimary.copy(alpha = 0.12f),
                                    GoldPrimary.copy(alpha = 0.04f)
                                )
                            )
                        )
                        .border(1.dp, GoldPrimary.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        .clickable { onMoreClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🏆", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Ranking",
                                color = GoldPrimary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "View rank",
                                color = GoldPrimary.copy(alpha = 0.5f),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // ═══════════════════════════════════════
            // SECTION 3: TOP 3 LEADERBOARD
            // ═══════════════════════════════════════

            if (top3Users.isNotEmpty()) {
                Spacer(modifier = Modifier.height(22.dp))

                // Divider with gold accent
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    GoldPrimary.copy(alpha = 0.25f),
                                    GoldPrimary.copy(alpha = 0.4f),
                                    GoldPrimary.copy(alpha = 0.25f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Leaderboard Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        GoldPrimary.copy(alpha = 0.2f),
                                        Color.Transparent
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.WorkspacePremium,
                            contentDescription = "Top Gamers",
                            tint = GoldPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Top Gamers",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Top 3 List
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    top3Users.forEachIndexed { index, user ->
                        PremiumUserRow(user = user, rank = index + 1)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // View Full Leaderboard
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    GoldPrimary.copy(alpha = 0.12f),
                                    NeonCyan.copy(alpha = 0.08f)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            Brush.horizontalGradient(
                                listOf(GoldPrimary.copy(alpha = 0.2f), NeonCyan.copy(alpha = 0.15f))
                            ),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { onMoreClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "View Full Leaderboard →",
                        color = GoldPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumUserRow(
    user: UserRank,
    rank: Int
) {
    val rankColor = getRankColor(rank)
    val isFirst = rank == 1

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isFirst) {
                    Brush.horizontalGradient(
                        listOf(
                            GoldPrimary.copy(alpha = 0.08f),
                            GoldDark.copy(alpha = 0.04f),
                            Color.Transparent
                        )
                    )
                } else {
                    Brush.horizontalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.03f),
                            Color.Transparent
                        )
                    )
                }
            )
            .then(
                if (isFirst) Modifier.border(
                    1.dp,
                    GoldPrimary.copy(alpha = 0.12f),
                    RoundedCornerShape(14.dp)
                ) else Modifier
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank Medal
        val rankEmoji = when (rank) {
            1 -> "🥇"
            2 -> "🥈"
            3 -> "🥉"
            else -> "#$rank"
        }
        Text(
            text = rankEmoji,
            fontSize = 24.sp,
            modifier = Modifier.width(36.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        // Avatar with rank-colored border
        AsyncImage(
            model = user.photoUrl?.takeIf { it.isNotEmpty() } ?: "https://api.dicebear.com/7.x/avataaars/png?seed=${user.displayName}",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .border(
                    width = if (isFirst) 2.dp else 1.5.dp,
                    brush = Brush.linearGradient(
                        listOf(rankColor, rankColor.copy(alpha = 0.5f))
                    ),
                    shape = CircleShape
                ),
            contentDescription = user.displayName
        )

        Spacer(modifier = Modifier.width(14.dp))

        // Name + Points + Streak
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.displayName,
                color = if (isFirst) GoldPrimary else SubtleWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${user.points} pts",
                    color = rankColor.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(10.dp))
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = FlameOrange.copy(alpha = 0.7f),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "${user.streak}d",
                    color = FlameOrange.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

private fun getRankColor(rank: Int): Color {
    return when (rank) {
        1 -> GoldPrimary
        2 -> Color(0xFFC0C0C0)  // Silver
        3 -> Color(0xFFCD7F32)  // Bronze
        else -> Color.Gray
    }
}
