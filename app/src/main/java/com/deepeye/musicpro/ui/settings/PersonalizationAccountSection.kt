// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import com.deepeye.musicpro.account.AccountSession
import com.deepeye.musicpro.ui.components.GlowCard
import com.deepeye.musicpro.ui.theme.*

@Composable
fun ConnectedAccountSection(
    uiState: PersonalizationSettingsUiState,
    viewModel: PersonalizationSettingsViewModel,
    onNavigateToAccount: () -> Unit,
) {
    val prefs = uiState.preferences
    val session = uiState.accountState
    val isConnected = session is AccountSession.Connected

    GlowCard(
        modifier = Modifier.fillMaxWidth(),
        glowColor = NeonCyan.copy(alpha = 0.25f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Connected Account", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
            Spacer(Modifier.height(4.dp))
            Text("Account sections use your connected account only when available.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(Modifier.height(12.dp))

            Surface(modifier = Modifier.fillMaxWidth(), color = GraphiteGlassElevated, shape = RoundedCornerShape(12.dp)) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(if (isConnected) ElectricViolet else TextTertiary), contentAlignment = Alignment.Center) {
                        Icon(if (isConnected) Icons.Default.CheckCircle else Icons.Default.PersonOutline, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = when (session) {
                            is AccountSession.Connected -> session.displayName ?: "Connected Account"
                            is AccountSession.Loading -> "Checking account..."
                            is AccountSession.LoggedOut -> "Not Connected"
                            is AccountSession.Error -> "Connection Issue"
                        }, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, color = TextPrimary))
                        Text(text = when (session) {
                            is AccountSession.Connected -> "YouTube / SmartTube Account Synced"
                            is AccountSession.Loading -> "Loading account state"
                            is AccountSession.LoggedOut -> "Sign in to enable account-based playlists"
                            is AccountSession.Error -> session.userMessage
                        }, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Button(onClick = onNavigateToAccount, shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = if (isConnected) GraphiteGlass else ElectricViolet, contentColor = TextPrimary), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                        Text(if (isConnected) "Manage" else "Sign In", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            PersonalizationSwitchRow(title = "Use Account Sections", subtitle = "Load personalized sections from your authenticated account", icon = Icons.Default.AccountCircle, checked = prefs.enableAccountSections && isConnected, enabled = isConnected, onCheckedChange = { viewModel.setAccountSectionsEnabled(it) })

            AnimatedVisibility(visible = prefs.enableAccountSections && isConnected) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    PersonalizationSwitchRow(title = "Show Liked Music", subtitle = "Include liked songs from your connected account", icon = Icons.Default.ThumbUp, checked = prefs.enableLikedMusic, onCheckedChange = { viewModel.setLikedMusicEnabled(it) })
                    Spacer(Modifier.height(10.dp))
                    PersonalizationSwitchRow(title = "Show New From Subscriptions", subtitle = "Include releases from subscribed channels", icon = Icons.Default.Subscriptions, checked = prefs.enableSubscriptions, onCheckedChange = { viewModel.setSubscriptionsEnabled(it) })
                }
            }
        }
    }
}