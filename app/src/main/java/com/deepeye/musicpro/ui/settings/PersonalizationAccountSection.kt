// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.musicpro.account.AccountSession

private val NeonCyan = Color(0xFF00E5FF)
private val DarkSurfaceCard = Color(0xFF0C0F17)
private val GlassBorder = Color(0x22FFFFFF)

@Composable
fun ConnectedAccountSection(
    uiState: PersonalizationSettingsUiState,
    viewModel: PersonalizationSettingsViewModel,
    onNavigateToAccount: () -> Unit,
) {
    val prefs = uiState.preferences
    val session = uiState.accountState
    val isConnected = session is AccountSession.Connected

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = DarkSurfaceCard,
        border = BorderStroke(1.2.dp, GlassBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (isConnected) Color(0xFF00E676) else NeonCyan)
                )
                Text(
                    "Connected Cloud Account",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    letterSpacing = 0.5.sp
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Account-based playlists, history sync & authenticated recommendations.",
                color = Color.White.copy(alpha = 0.60f),
                fontSize = 13.sp,
                lineHeight = 17.sp
            )

            Spacer(Modifier.height(16.dp))

            // Account Status Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF131826),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (isConnected) Color(0x3300E676) else Color(0x22FFFFFF))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isConnected) Color(0x2600E676) else Color(0x1AFFFFFF))
                            .border(1.dp, if (isConnected) Color(0xFF00E676) else Color(0x33FFFFFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isConnected) Icons.Default.CheckCircle else Icons.Default.PersonOutline,
                            contentDescription = null,
                            tint = if (isConnected) Color(0xFF00E676) else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (session) {
                                is AccountSession.Connected -> session.displayName ?: "Connected Account"
                                is AccountSession.Loading -> "Checking status..."
                                is AccountSession.LoggedOut -> "Not Connected"
                                is AccountSession.Error -> "Connection Issue"
                            },
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = when (session) {
                                is AccountSession.Connected -> "YouTube / Cloud Account Synced"
                                is AccountSession.Loading -> "Loading account state"
                                is AccountSession.LoggedOut -> "Sign in to enable personalized cloud playlists"
                                is AccountSession.Error -> session.userMessage
                            },
                            color = Color.White.copy(alpha = 0.60f),
                            fontSize = 12.5.sp
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Button(
                        onClick = onNavigateToAccount,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isConnected) Color(0x26FFFFFF) else NeonCyan,
                            contentColor = if (isConnected) Color.White else Color(0xFF090B10)
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            if (isConnected) "Manage" else "Sign In",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.5.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            PersonalizationSwitchRow(
                title = "Use Account Sections",
                subtitle = "Load personalized feeds from your authenticated cloud profile",
                icon = Icons.Default.AccountCircle,
                checked = prefs.enableAccountSections && isConnected,
                enabled = isConnected,
                onCheckedChange = { viewModel.setAccountSectionsEnabled(it) }
            )

            AnimatedVisibility(
                visible = prefs.enableAccountSections && isConnected,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PersonalizationSwitchRow(
                        title = "Show Liked Music",
                        subtitle = "Include liked songs and tracks from your cloud account",
                        icon = Icons.Default.ThumbUp,
                        checked = prefs.enableLikedMusic,
                        onCheckedChange = { viewModel.setLikedMusicEnabled(it) }
                    )

                    PersonalizationSwitchRow(
                        title = "Show New From Subscriptions",
                        subtitle = "Include latest music releases from subscribed artists & channels",
                        icon = Icons.Default.Subscriptions,
                        checked = prefs.enableSubscriptions,
                        onCheckedChange = { viewModel.setSubscriptionsEnabled(it) }
                    )
                }
            }
        }
    }
}
