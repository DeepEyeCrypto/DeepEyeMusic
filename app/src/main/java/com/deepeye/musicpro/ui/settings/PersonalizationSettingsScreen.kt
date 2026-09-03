// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
        containerColor = RichBlack,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Music Personalization",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics { contentDescription = "Navigate back" }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = RichBlack,
                    titleContentColor = TextPrimary
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
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
        AlertDialog(
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