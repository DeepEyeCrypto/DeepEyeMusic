// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepeye.musicpro.BuildConfig
import com.deepeye.musicpro.domain.model.personalization.PersonalizedFeedState
import com.deepeye.musicpro.domain.personalization.SectionDiagnostics
import com.deepeye.musicpro.domain.repository.PersonalizationRepository
import com.deepeye.musicpro.ui.components.GlowCard
import com.deepeye.musicpro.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PersonalizationDiagnosticsViewModel @Inject constructor(
    private val personalizationRepository: PersonalizationRepository,
) : ViewModel() {
    val feedState: StateFlow<PersonalizedFeedState> = personalizationRepository.observePersonalizedFeed()

    fun refreshFeed() {
        viewModelScope.launch {
            personalizationRepository.refreshFeed(forceRefresh = true)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalizationDiagnosticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: PersonalizationDiagnosticsViewModel = hiltViewModel(),
) {
    val feedState by viewModel.feedState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = RichBlack,
        topBar = {
            TopAppBar(
                title = { Text("Personalization Diagnostics", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.semantics { contentDescription = "Navigate back" }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshFeed() }, modifier = Modifier.semantics { contentDescription = "Refresh diagnostics feed" }) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = NeonCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RichBlack, titleContentColor = TextPrimary)
            )
        }
    ) { paddingValues ->
        if (!BuildConfig.DEBUG) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("Diagnostics are restricted to developer/debug builds.", style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
            }
            return@Scaffold
        }

        val diagnostics = feedState.diagnostics

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item(key = "privacy_notice") {
                GlowCard(modifier = Modifier.fillMaxWidth(), glowColor = NeonCyan.copy(alpha = 0.2f)) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Privacy & Security Guarantee", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                            Text("Diagnostics never display access tokens, cookies, email, account keys, or raw media URLs.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                }
            }

            item(key = "summary_card") {
                DiagnosticsSummaryCard(feedState = feedState, diagnostics = diagnostics)
            }

            if (diagnostics.isEmpty()) {
                item(key = "empty_diagnostics") {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                        Text("No diagnostics captured yet. Tap Refresh to build.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }
            } else {
                items(diagnostics, key = { it.sectionId }) { diag ->
                    DiagnosticsSectionCard(diag = diag)
                }
            }
        }
    }
}
