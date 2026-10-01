// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepeye.musicpro.BuildConfig
import com.deepeye.musicpro.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalizationSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHiddenContent: () -> Unit,
    onNavigateToAccount: () -> Unit,
    onNavigateToDiagnostics: () -> Unit,
    viewModel: PersonalizationSettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissMessage()
        }
    }

    Scaffold(
        containerColor = Color(0xFF07090E),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF07090E))
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(Color(0xFF131826))
                            .border(1.dp, Color(0x22FFFFFF), androidx.compose.foundation.shape.CircleShape)
                            .semantics { contentDescription = "Navigate back" }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            "Music Personalization",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            "Feed customization & neural recommendation settings",
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 12.5.sp
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item(key = "section_personalization") {
                PersonalizationMasterSection(uiState = uiState, viewModel = viewModel)
            }

            item(key = "section_account") {
                ConnectedAccountSection(
                    uiState = uiState,
                    viewModel = viewModel,
                    onNavigateToAccount = onNavigateToAccount
                )
            }

            item(key = "section_discovery") {
                DiscoveryPreferencesSection(uiState = uiState, viewModel = viewModel)
            }

            item(key = "section_privacy") {
                PrivacyAndDataSection(
                    uiState = uiState,
                    viewModel = viewModel,
                    onNavigateToHiddenContent = onNavigateToHiddenContent
                )
            }

            item(key = "section_refresh") {
                RefreshSection(uiState = uiState, viewModel = viewModel)
            }

            if (BuildConfig.DEBUG) {
                item(key = "section_diagnostics") {
                    DeveloperDiagnosticsSection(onNavigateToDiagnostics = onNavigateToDiagnostics)
                }
            }

            item(key = "bottom_spacer") {
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    uiState.activeConfirmation?.let { conf ->
        AppAlertDialog(
            onDismissRequest = { viewModel.dismissConfirmation() },
            title = {
                Text(
                    text = conf.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = conf.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = { conf.onConfirm() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (conf.isDestructive) MaterialTheme.colorScheme.error else ElectricViolet,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(conf.confirmLabel, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissConfirmation() }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = GraphiteGlassElevated,
            shape = RoundedCornerShape(20.dp)
        )
    }
}