// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.settings

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import kotlinx.coroutines.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepeye.musicpro.data.prefs.AppSettings
import com.deepeye.musicpro.data.prefs.ThemeMode
import com.deepeye.musicpro.data.source.remote.update.UpdateState
import com.deepeye.musicpro.ui.theme.AppAlertDialog
import com.deepeye.musicpro.ui.theme.boundedContent

private val neonCyan = Color(0xFF00E5FF)
private val darkSurface = Color(0xFF131722).copy(alpha = 0.85f)
private val glassBorder = Color(0x22FFFFFF)

// ── Setting Categories ────────────────────────────────────────────────────────
enum class SettingCategory(val title: String, val subtitle: String, val icon: ImageVector) {
    APPEARANCE("Appearance", "Theme, Dynamic Glow & Contrast", Icons.Default.Palette),
    AUDIO_ENGINE("Audio Engine (DSP)", "Lossless DSP, AEOS & Visualizer", Icons.Default.GraphicEq),
    NOTIFICATIONS("Notifications", "New Uploads & Channel Alerts", Icons.Default.Notifications),
    LIBRARY("Storage & Library", "Rescan, Cache & Cloud Sync", Icons.Default.Folder),
    UPDATES("Updates & Version", "OTA Engine & Release Status", Icons.Default.SystemUpdate),
    ACCOUNT("Account", "Session & Sign Out", Icons.Default.AccountCircle)
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    windowSizeClass: androidx.compose.material3.windowsizeclass.WindowSizeClass? = null,
    onNavigateBack: () -> Unit = {},
    onNavigateToAEOS: () -> Unit = {},
    onNavigateToVisualizerStudio: () -> Unit = {},
    onYouTubeLoginClick: () -> Unit = {},
    onGoogleSignInClick: () -> Unit = {},
    onLaunchTvMode: () -> Unit = {},
    onSignOut: () -> Unit = {},
    isSignedIn: Boolean = false,
    signedInEmail: String? = null,
    viewModel: SettingsViewModel = hiltViewModel(),
    changelogViewModel: com.deepeye.musicpro.updates.ChangelogViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = uiState.settings
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Sign-out is irreversible and destroys the user's session, so it is gated
    // behind an explicit confirmation rather than firing on a single tap.
    var showSignOutConfirm by remember { mutableStateOf(false) }
    var showYouTubeDisconnectConfirm by remember { mutableStateOf(false) }

    if (showYouTubeDisconnectConfirm) {
        AppAlertDialog(
            onDismissRequest = { showYouTubeDisconnectConfirm = false },
            containerColor = Color(0xFF131722),
            icon = {
                Icon(
                    Icons.Default.LinkOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            title = {
                Text("Disconnect YouTube Account", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Disconnecting will remove your authenticated YouTube session and account subscriptions from this device.",
                    color = Color.White.copy(alpha = 0.8f),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showYouTubeDisconnectConfirm = false
                        viewModel.logoutYouTube()
                    },
                ) {
                    Text(
                        "Disconnect",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showYouTubeDisconnectConfirm = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.8f))
                }
            },
        )
    }

    if (showSignOutConfirm) {
        AppAlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            containerColor = Color(0xFF131722),
            icon = {
                Icon(
                    Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            title = {
                Text("Sign Out", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "You'll be returned to the login screen and signed out on this device. " +
                        "Your downloaded music stays on this device, but you'll need to sign in again to sync.",
                    color = Color.White.copy(alpha = 0.8f),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutConfirm = false
                        onSignOut()
                    },
                ) {
                    Text(
                        "Sign Out",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.8f))
                }
            },
        )
    }

    // Adaptive Master-Detail Navigator
    val navigator = rememberListDetailPaneScaffoldNavigator<SettingCategory>()

    // Current selected item with fallback
    val currentCategory = navigator.currentDestination?.contentKey ?: SettingCategory.APPEARANCE

    // Automatic Predictive & Back-Stack handling
    BackHandler(enabled = navigator.canNavigateBack()) {
        coroutineScope.launch {
            navigator.navigateBack()
        }
    }

    LaunchedEffect(uiState.updateState) {
        when (val state = uiState.updateState) {
            is UpdateState.UpToDate -> {
                android.widget.Toast.makeText(
                    context,
                    "DeepEye Music Pro is up to date (v${com.deepeye.musicpro.BuildConfig.VERSION_NAME})",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                viewModel.resetUpdateState()
            }
            is UpdateState.Error -> {
                android.widget.Toast.makeText(
                    context,
                    state.message,
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Global Header Bar (Automotive 64.dp minimum touch targets)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (navigator.canNavigateBack()) {
                            coroutineScope.launch {
                                navigator.navigateBack()
                            }
                        } else {
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(darkSurface)
                        .border(1.dp, glassBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = neonCyan,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(Modifier.width(16.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "DEEPEYE SETTINGS",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(neonCyan)
                        )
                    }
                    Text(
                        text = "System Architecture & Account Engine",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(Modifier.weight(1f))

                // YouTube Account Status Chip (Automotive scaled)
                if (settings.youtubeAccessToken != null) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0x2200E676),
                        border = BorderStroke(1.5.dp, Color(0xFF00E676)),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clickable { showYouTubeDisconnectConfirm = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF00E676), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("YouTube Active", color = Color(0xFF00E676), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // GHOST MODE: status chip is informational only — no sign-in affordance.
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0x2200E5FF),
                        border = BorderStroke(1.5.dp, Color(0x3300E5FF)),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Lock, null, tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Ghost Mode", color = Color(0xFF00E5FF), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            HorizontalDivider(color = glassBorder, thickness = 1.dp)

            // Official Google Material 3 Adaptive Master-Detail Scaffold
            NavigableListDetailPaneScaffold(
                navigator = navigator,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                listPane = {
                    AnimatedPane(modifier = Modifier.preferredWidth(350.dp)) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(end = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(SettingCategory.values()) { category ->
                                MasterCategoryItem(
                                    category = category,
                                    isSelected = category == currentCategory,
                                    onClick = {
                                        coroutineScope.launch {
                                            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, category)
                                        }
                                    }
                                )
                            }
                        }
                    }
                },
                detailPane = {
                    AnimatedPane(modifier = Modifier.fillMaxSize()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(24.dp)),
                            color = darkSurface,
                            border = BorderStroke(1.dp, glassBorder),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            SettingsDetailPane(
                                category = currentCategory,
                                uiState = uiState,
                                viewModel = viewModel,
                                context = context,
                                onNavigateToAEOS = onNavigateToAEOS,
                                onNavigateToVisualizerStudio = onNavigateToVisualizerStudio,
                                isSignedIn = isSignedIn,
                                signedInEmail = signedInEmail,
                                onRequestSignOut = { showSignOutConfirm = true },
                                onGoogleSignInClick = onGoogleSignInClick,
                                onYouTubeLoginClick = onYouTubeLoginClick,
                            )
                        }
                    }
                }
            )
        }
    }
}

// ── Left Pane Category Item (Automotive-Grade Touch Target >= 64.dp) ─────────
@Composable
private fun MasterCategoryItem(
    category: SettingCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) Color(0x3300E5FF) else darkSurface,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) neonCyan else glassBorder
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) Color(0x3300E5FF) else Color(0x14FFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = category.title,
                    tint = if (isSelected) neonCyan else Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.title,
                    color = if (isSelected) neonCyan else Color.White,
                    fontSize = 16.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                )
                Text(
                    text = category.subtitle,
                    color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

// ── Right Pane Detail Component (Automotive Hyper-Ergonomic) ──────────────────
@Composable
private fun SettingsDetailPane(
    category: SettingCategory,
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    context: Context,
    onNavigateToAEOS: () -> Unit,
    onNavigateToVisualizerStudio: () -> Unit = {},
    isSignedIn: Boolean,
    signedInEmail: String?,
    onRequestSignOut: () -> Unit,
    onGoogleSignInClick: () -> Unit = {},
    onYouTubeLoginClick: () -> Unit = {},
) {
    val settings = uiState.settings

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Detail Header
        // The header and divider carry the same `boundedContent` treatment as
        // the list below, so the category title stays flush with the rows it
        // titles. Bounding only the list would leave a heading hard against the
        // left edge with its own content floating in the middle.
        Row(
            modifier = Modifier.boundedContent(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x3300E5FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = neonCyan,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Text(
                text = category.title,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        HorizontalDivider(
            modifier = Modifier.boundedContent(),
            color = glassBorder,
            thickness = 1.dp
        )

        LazyColumn(
            // `boundedContent` caps this at a 600dp reading column and centres
            // it. Without the cap every row below stretched to the full ~800dp
            // landscape viewport, putting a switch at each extreme and a lake of
            // empty glass in between — the "khali" symptom.
            //
            // `weight(1f)` — not `fillMaxSize()`. As a direct Column child,
            // fillMaxSize() resolves against the Column's *incoming* max height,
            // so this list was laid out taller than the space below the header
            // and its trailing rows were pushed out of the pane where they could
            // neither be seen nor scrolled to. weight() gives it only the
            // remaining height, so it scrolls within the pane.
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterHorizontally)
                .boundedContent(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            when (category) {
                SettingCategory.APPEARANCE -> {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("Theme Preset", color = Color.White.copy(0.7f), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ThemeModeChip(
                                    label = "System",
                                    isSelected = settings.themeMode == ThemeMode.SYSTEM && !settings.amoledMode,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        viewModel.setThemeMode(ThemeMode.SYSTEM)
                                        viewModel.setAmoledMode(false)
                                    }
                                )
                                ThemeModeChip(
                                    label = "Dark",
                                    isSelected = settings.themeMode == ThemeMode.DARK && !settings.amoledMode,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        viewModel.setThemeMode(ThemeMode.DARK)
                                        viewModel.setAmoledMode(false)
                                    }
                                )
                                ThemeModeChip(
                                    label = "AMOLED",
                                    isSelected = settings.amoledMode,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        viewModel.setThemeMode(ThemeMode.DARK)
                                        viewModel.setAmoledMode(true)
                                    }
                                )
                            }

                            SettingSwitchRow(
                                label = "Dynamic Ambient Album Glow",
                                description = "Extracts colors from artwork for animated background mesh",
                                isChecked = settings.dynamicColor,
                                onCheckedChange = { viewModel.setDynamicColor(it) }
                            )

                            SettingSwitchRow(
                                label = "Pure AMOLED Black",
                                description = "Deepest true-black background for battery saving",
                                isChecked = settings.amoledMode,
                                onCheckedChange = { viewModel.setAmoledMode(it) }
                            )
                        }
                    }
                }
                SettingCategory.AUDIO_ENGINE -> {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            SettingSwitchRow(
                                label = "Audio Visualizer Overlay",
                                description = "Displays realtime FFT spectrum in Now Playing screen",
                                isChecked = settings.showVisualizer,
                                onCheckedChange = { viewModel.setShowVisualizer(it) }
                            )

                            SettingSwitchRow(
                                label = "Autoplay on Cellular",
                                description = "Allow infinite-radio pre-fetch on metered mobile data",
                                isChecked = settings.autoplayOnCellular,
                                onCheckedChange = { viewModel.setAutoplayOnCellular(it) }
                            )

                            Surface(
                                onClick = onNavigateToVisualizerStudio,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 64.dp)
                                    .clip(RoundedCornerShape(18.dp)),
                                color = Color(0x28FF007F),
                                border = BorderStroke(1.dp, neonCyan),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Open Visualizer Studio",
                                        color = neonCyan,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Open Visualizer Studio",
                                        tint = neonCyan
                                    )
                                }
                            }

                            DSPDebugOverlay(viewModel = androidx.hilt.navigation.compose.hiltViewModel())

                            Surface(
                                onClick = onNavigateToAEOS,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 64.dp)
                                    .clip(RoundedCornerShape(18.dp)),
                                color = Color(0x2800E5FF),
                                border = BorderStroke(1.dp, neonCyan),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Open Audio Engine OS (AEOS)",
                                        color = neonCyan,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = neonCyan, modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
                SettingCategory.NOTIFICATIONS -> {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            SettingSwitchRow(
                                label = "YouTube Upload Alerts",
                                description = "Receive rich background notifications when subscribed channels upload new tracks",
                                isChecked = uiState.notificationsEnabled,
                                onCheckedChange = { enabled ->
                                    viewModel.setNotificationsEnabled(enabled)
                                }
                            )

                            // Manual Check Button
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 68.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(darkSurface)
                                    .border(1.dp, glassBorder, RoundedCornerShape(18.dp))
                                    .padding(horizontal = 18.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                    Text("Check Subscriptions Now", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text("Scans subscribed channels for new uploads immediately", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                                }
                                Button(
                                    onClick = {
                                        viewModel.triggerSubscriptionCheckNow(context)
                                        android.widget.Toast.makeText(context, "Scanning subscriptions for new uploads...", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = neonCyan),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Text("Check Now", color = Color(0xFF090B10), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                SettingCategory.LIBRARY -> {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Rescan Button
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 68.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(darkSurface)
                                    .border(1.dp, glassBorder, RoundedCornerShape(18.dp))
                                    .padding(horizontal = 18.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                    Text("Rescan Local Tracks", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text("Scan device storage for newly added audio files", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                                }
                                IconButton(
                                    onClick = { viewModel.rescanLibrary() },
                                    enabled = !uiState.isRescanningLibrary,
                                    modifier = Modifier.size(52.dp)
                                ) {
                                    if (uiState.isRescanningLibrary) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = neonCyan, strokeWidth = 2.5.dp)
                                    } else {
                                        Icon(Icons.Default.Refresh, "Rescan", tint = neonCyan, modifier = Modifier.size(28.dp))
                                    }
                                }
                            }

                            // Clear Cache Button
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 68.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(darkSurface)
                                    .border(1.dp, glassBorder, RoundedCornerShape(18.dp))
                                    .padding(horizontal = 18.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                    Text("Clear Artwork Cache", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text("Frees internal image cache memory", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                                }
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            context.cacheDir.deleteRecursively()
                                            context.cacheDir.mkdirs()
                                            android.widget.Toast.makeText(context, "Cache Cleared", android.widget.Toast.LENGTH_SHORT).show()
                                        } catch (_: Exception) {}
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, glassBorder),
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Text("Clear", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                SettingCategory.UPDATES -> {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 72.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(darkSurface)
                                .border(1.dp, glassBorder, RoundedCornerShape(18.dp))
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text("DeepEye Music Pro", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                Text("Version v${com.deepeye.musicpro.BuildConfig.VERSION_NAME} • Build ${com.deepeye.musicpro.BuildConfig.VERSION_CODE}", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                            }
                            
                            val isChecking = uiState.updateState is com.deepeye.musicpro.data.source.remote.update.UpdateState.Checking
                            val isUpToDate = uiState.updateState is com.deepeye.musicpro.data.source.remote.update.UpdateState.UpToDate
                            
                            Button(
                                onClick = { viewModel.checkForUpdate() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isUpToDate) Color(0xFF00E676) else neonCyan,
                                    disabledContainerColor = neonCyan.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(14.dp),
                                enabled = !isChecking,
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                if (isChecking) {
                                    CircularProgressIndicator(
                                        color = Color(0xFF090B10),
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Checking...", color = Color(0xFF090B10), fontWeight = FontWeight.Black, fontSize = 14.sp)
                                } else if (isUpToDate) {
                                    Text("Latest Version ✓", color = Color(0xFF090B10), fontWeight = FontWeight.Black, fontSize = 14.sp)
                                } else {
                                    Text("Check Updates", color = Color(0xFF090B10), fontWeight = FontWeight.Black, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
                SettingCategory.ACCOUNT -> {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            // 1. Google Cloud / DeepEye Account Section
                            Text(
                                "Google Account",
                                color = neonCyan,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = darkSurface,
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, glassBorder)
                            ) {
                                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(if (isSignedIn) Color(0x3300E5FF) else Color(0x14FFFFFF)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isSignedIn) Icons.Default.AccountCircle else Icons.Default.PersonOutline,
                                                contentDescription = null,
                                                tint = if (isSignedIn) neonCyan else Color.White.copy(alpha = 0.6f),
                                                modifier = Modifier.size(30.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                if (isSignedIn) (signedInEmail ?: "Google Account Active") else "Guest Mode (Not Signed In)",
                                                color = Color.White,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                if (isSignedIn) "Cloud sync, playlists & backup active" else "Sign in to backup & sync your library across devices",
                                                color = Color.White.copy(alpha = 0.6f),
                                                fontSize = 12.sp
                                            )
                                        }
                                    }

                                    if (isSignedIn) {
                                        Button(
                                            onClick = onRequestSignOut,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                                contentColor = MaterialTheme.colorScheme.error
                                            ),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.fillMaxWidth().height(48.dp)
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.Logout, null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("Sign Out Google Account", fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        // GHOST MODE: authentication UI removed.
                                        // DeepEyeMusicPro runs as a fully anonymous InnerTube client.
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            color = Color(0x14FFFFFF),
                                            shape = RoundedCornerShape(14.dp),
                                            border = BorderStroke(1.dp, Color(0x22FFFFFF))
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(48.dp)
                                                    .padding(horizontal = 16.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(Icons.Default.Lock, null, tint = Color(0xFFFFD700), modifier = Modifier.size(18.dp))
                                                Spacer(Modifier.width(8.dp))
                                                Text("Ghost Mode · No account required", color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            // 2. YouTube / InnerTube Account Section
                            Text(
                                "YouTube Account",
                                color = Color(0xFFFF4444),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = darkSurface,
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, if (settings.youtubeAccessToken != null) Color(0x3300E676) else glassBorder)
                            ) {
                                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(if (settings.youtubeAccessToken != null) Color(0x2600E676) else Color(0x22FF4444)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (settings.youtubeAccessToken != null) Icons.Default.CheckCircle else Icons.Default.PlayCircle,
                                                contentDescription = null,
                                                tint = if (settings.youtubeAccessToken != null) Color(0xFF00E676) else Color(0xFFFF4444),
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                if (settings.youtubeAccessToken != null) (settings.youtubeUserName ?: "YouTube Linked") else "YouTube Not Connected",
                                                color = Color.White,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                if (settings.youtubeAccessToken != null) (settings.youtubeUserEmail ?: "Authenticated InnerTube Session Active") else "Connect via TV Device Code or OAuth to access subscriptions & history",
                                                color = Color.White.copy(alpha = 0.6f),
                                                fontSize = 12.sp
                                            )
                                        }
                                    }

                                    if (settings.youtubeAccessToken != null) {
                                        Button(
                                            onClick = { viewModel.logoutYouTube() },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0x22FF5252),
                                                contentColor = Color(0xFFFF5252)
                                            ),
                                            border = BorderStroke(1.dp, Color(0x55FF5252)),
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.fillMaxWidth().height(48.dp)
                                        ) {
                                            Icon(Icons.Default.LinkOff, null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("Disconnect YouTube Account", fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        // GHOST MODE: no YouTube sign-in. Recommendations come
                                        // purely from seed-based, anonymous InnerTube payloads.
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            color = Color(0x14FFFFFF),
                                            shape = RoundedCornerShape(14.dp),
                                            border = BorderStroke(1.dp, Color(0x22FFFFFF))
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(48.dp)
                                                    .padding(horizontal = 16.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(Icons.Default.Lock, null, tint = Color(0xFFFFD700), modifier = Modifier.size(18.dp))
                                                Spacer(Modifier.width(8.dp))
                                                Text("Ghost Mode · Anonymous session", color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Reusable Component Helpers (Automotive Enforced) ─────────────────────────

@Composable
fun ThemeModeChip(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) neonCyan else darkSurface,
        border = BorderStroke(1.5.dp, if (isSelected) neonCyan else glassBorder),
        shape = RoundedCornerShape(14.dp)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = if (isSelected) Color(0xFF090B10) else Color.White,
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun SettingSwitchRow(
    label: String,
    description: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(darkSurface)
            .border(1.dp, glassBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(text = label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(text = description, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.scale(1.35f),
            colors = SwitchDefaults.colors(
                checkedThumbColor = neonCyan,
                checkedTrackColor = Color(0x4400E5FF),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0x1AFFFFFF)
            )
        )
    }
}
