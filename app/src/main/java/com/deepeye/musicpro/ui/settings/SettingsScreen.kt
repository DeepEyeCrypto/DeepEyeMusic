// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepeye.musicpro.data.prefs.ThemeMode
import com.deepeye.musicpro.data.source.remote.update.UpdateState
import com.deepeye.musicpro.ui.components.GlowCard

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    windowSizeClass: androidx.compose.material3.windowsizeclass.WindowSizeClass,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = uiState.settings
    val context = LocalContext.current

    LaunchedEffect(uiState.updateState) {
        if (uiState.updateState is UpdateState.UpToDate) {
            android.widget.Toast.makeText(
                context,
                "DeepEye Music Pro is up to date",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            viewModel.resetUpdateState()
        }
    }

    val isExpanded = windowSizeClass.widthSizeClass == androidx.compose.material3.windowsizeclass.WindowWidthSizeClass.Expanded

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().imePadding()) {

        if (isExpanded) {
            var selectedCategory by remember { mutableStateOf(0) }
            val categories = listOf("Appearance", "Music Taste", "Audio", "Library", "About")

            Row(modifier = Modifier.fillMaxSize()) {
                // Left Pane: Navigation Category List
                Column(
                    modifier =
                    Modifier
                        .weight(0.32f)
                        .fillMaxHeight()
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Profile",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp, start = 12.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    PremiumProfileCard(
                        modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                    )

                    categories.forEachIndexed { index, category ->
                        NavigationDrawerItem(
                            label = { Text(category, style = MaterialTheme.typography.titleMedium) },
                            selected = selectedCategory == index,
                            onClick = { selectedCategory = index },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                VerticalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

                // Right Pane: Selected Category Settings details
                Box(
                    modifier =
                    Modifier
                        .weight(0.68f)
                        .fillMaxHeight()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        when (selectedCategory) {
                            0 -> {
                                item { SectionHeader("Appearance") }
                                item { AppearanceSettingsCard(settings, viewModel) }
                            }
                            1 -> {
                                item { SectionHeader("Music Taste & Autoplay") }
                                item { TasteSettingsCard(uiState, viewModel) }
                            }
                            2 -> {
                                item { SectionHeader("Audio") }
                                item { AudioSettingsCard(settings, viewModel) }
                            }
                            3 -> {
                                item { SectionHeader("Library") }
                                item { LibrarySettingsCard(uiState, viewModel) }
                            }
                            4 -> {
                                item { SectionHeader("About") }
                                item { AboutSettingsCard(viewModel) }
                            }
                        }
                    }
                }
            }
        } else {
            // Standard scroll list for phones and compact sizes
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 180.dp)) {
                item {
                    Text(
                        "Profile",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    )
                }

                item {
                    PremiumProfileCard(
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp)
                    )
                }


                item { SectionHeader("Appearance") }
                item { AppearanceSettingsCard(settings, viewModel) }

                item { SectionHeader("Music Taste & Autoplay") }
                item { TasteSettingsCard(uiState, viewModel) }

                item { SectionHeader("Audio") }
                item { AudioSettingsCard(settings, viewModel) }

                item { SectionHeader("Library") }
                item { LibrarySettingsCard(uiState, viewModel) }

                item { SectionHeader("About") }
                item { AboutSettingsCard(viewModel) }
            }
        }
    }
}

@Composable
fun PremiumProfileCard(
    modifier: Modifier = Modifier,
    authViewModel: com.deepeye.musicpro.ui.auth.AuthViewModel = hiltViewModel()
) {
    com.deepeye.musicpro.ui.components.GlassSurface(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 28.dp,
        tintColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        borderColor = Color(0xFF7B3FE4).copy(alpha = 0.3f),
        refractionHeight = 0.3f
    ) {
        Box(modifier = Modifier.padding(18.dp)) {
        val currentUser by authViewModel.currentUser.collectAsState()
        val authState by authViewModel.authState.collectAsState()
        var isSignUpMode by remember { mutableStateOf(false) }
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        val context = LocalContext.current

        LaunchedEffect(authState) {
            if (authState is com.deepeye.musicpro.ui.auth.AuthState.Error) {
                android.widget.Toast.makeText(context, (authState as com.deepeye.musicpro.ui.auth.AuthState.Error).message, android.widget.Toast.LENGTH_SHORT).show()
                authViewModel.resetState()
            }
        }

        if (currentUser != null) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        Color(0xFF7B3FE4),
                                        Color(0xFF00E5C3),
                                        Color(0xFFFFB74D),
                                        Color(0xFF7B3FE4)
                                    )
                                )
                            )
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF7B3FE4).copy(alpha = 0.15f),
                                        Color(0xFF00E5C3).copy(alpha = 0.15f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (currentUser?.photoUrl != null) {
                            coil3.compose.AsyncImage(
                                model = currentUser?.photoUrl.toString(),
                                contentDescription = "Profile Picture",
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            val firstLetter = currentUser?.email?.firstOrNull()?.uppercase() ?: "U"
                            Text(
                                text = firstLetter,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF7B3FE4),
                                fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif
                            )
                        }
                    }

                    // User details
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentUser?.displayName ?: currentUser?.email?.substringBefore("@") ?: "User",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))

                        // Premium Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFFFFB74D),
                                            Color(0xFFFFA726)
                                        )
                                    )
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "PRO ELITE MEMBER",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF2C1A00),
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    // Sign Out Button
                    IconButton(onClick = { authViewModel.signOut() }) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Sign Out",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ProfileStat("DSP STATUS", "ACTIVE", Color(0xFF00E5C3))
                    ProfileStat("V4A ENGINE", "OPTIMIZED", Color(0xFF7B3FE4))
                    ProfileStat("AUDIO CORE", "HI-RES", Color(0xFFFFB74D))
                }
            }
        } else {
            // Login Form inside Profile Card
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Login to Sync",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(16.dp))
                
                com.deepeye.musicpro.ui.auth.PremiumTextField(
                    value = email,
                    onValueChange = { email = it },
                    hint = "Email Address",
                    icon = Icons.Default.Email,
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Email,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Next
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                com.deepeye.musicpro.ui.auth.PremiumTextField(
                    value = password,
                    onValueChange = { password = it },
                    hint = "Password",
                    icon = Icons.Default.Lock,
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Password,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                    isPassword = true,
                    onImeAction = {
                        if (isSignUpMode) authViewModel.signUpWithEmail(email, password)
                        else authViewModel.signInWithEmail(email, password)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                com.deepeye.musicpro.ui.auth.PremiumActionButton(
                    text = if (isSignUpMode) "Create Account" else "Sign In",
                    isLoading = authState is com.deepeye.musicpro.ui.auth.AuthState.Loading,
                    onClick = {
                        if (isSignUpMode) authViewModel.signUpWithEmail(email, password)
                        else authViewModel.signInWithEmail(email, password)
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isSignUpMode = !isSignUpMode }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isSignUpMode) "Sign In instead" else "Create an account",
                        color = Color(0xFF00D2FF),
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                com.deepeye.musicpro.ui.auth.GoogleSignInButton(
                    isLoading = authState is com.deepeye.musicpro.ui.auth.AuthState.Loading,
                    onClick = { authViewModel.signInWithGoogle(context) }
                )
            }
        }
        }
    }
}

@Composable
private fun ProfileStat(label: String, value: String, accentColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            letterSpacing = 0.5.sp
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )
    }
}

@Composable
private fun AppearanceSettingsCard(
    settings: com.deepeye.musicpro.data.prefs.AppSettings,
    viewModel: SettingsViewModel,
) {
    SettingsCard {
        Text("Theme", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(12.dp))

        // Premium Dark/Light mode toggle (Pinterest-inspired design)
        val isDark = settings.themeMode == ThemeMode.DARK
        val isSystem = settings.themeMode == ThemeMode.SYSTEM

        // Determine effective dark state
        val effectiveDark = when (settings.themeMode) {
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
            else -> isDark
        }

        // Animated colors
        val trackColor by animateColorAsState(
            targetValue = if (effectiveDark) Color(0xFF1A1A2E) else Color(0xFFE8E8F0),
            animationSpec = tween(400),
            label = "trackColor"
        )
        val thumbColor by animateColorAsState(
            targetValue = if (effectiveDark) Color(0xFF7C4DFF) else Color(0xFFFFB74D),
            animationSpec = tween(400),
            label = "thumbColor"
        )
        val thumbOffset by animateFloatAsState(
            targetValue = if (effectiveDark) 1f else 0f,
            animationSpec = tween(400),
            label = "thumbOffset"
        )

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(28.dp))
                    .background(trackColor)
                    .clickable {
                        // Cycle: DARK -> LIGHT -> SYSTEM -> DARK
                        val next = when (settings.themeMode) {
                            ThemeMode.DARK -> ThemeMode.LIGHT
                            ThemeMode.LIGHT -> ThemeMode.SYSTEM
                            ThemeMode.SYSTEM -> ThemeMode.DARK
                        }
                        viewModel.setThemeMode(next)
                    }
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Sun icon (left)
                Text(
                    text = "☀️",
                    fontSize = 20.sp,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )

                // Animated toggle switch
                Box(
                    modifier = Modifier
                        .width(52.dp)
                        .height(28.dp)
                        .clip(CircleShape)
                        .background(thumbColor),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Box(
                        modifier = Modifier
                            .offset(x = (2f + thumbOffset * 24f).dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (effectiveDark) "🌙" else "☀️",
                            fontSize = 14.sp,
                        )
                    }
                }

                // Moon icon (right)
                Text(
                    text = "🌙",
                    fontSize = 20.sp,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
            }
        }

        // System mode indicator
        if (isSystem) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Follows system theme",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
        Spacer(Modifier.height(12.dp))
        // Dynamic color
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Dynamic Color (Material You)", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = settings.dynamicColor, onCheckedChange = { viewModel.setDynamicColor(it) })
        }
        Spacer(Modifier.height(12.dp))
        // AMOLED Mode
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("AMOLED Black Mode", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = settings.amoledMode, onCheckedChange = { viewModel.setAmoledMode(it) })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TasteSettingsCard(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
) {
    val settings = uiState.settings
    SettingsCard {
        Text("Preferred Languages", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        val languagesList =
            listOf(
                "Hindi", "English", "Punjabi", "Bhojpuri", "Tamil", "Telugu",
                "Haryanvi", "Bengali", "Malayalam", "Kannada", "Marathi", "Gujarati",
            )
        val currentLangs = uiState.tasteProfile.preferredLanguages

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            languagesList.forEach { lang ->
                val isSelected = currentLangs.contains(lang)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        val updated = if (isSelected) currentLangs - lang else currentLangs + lang
                        viewModel.setPreferredLanguages(updated)
                    },
                    label = { Text(lang) },
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text("Favorite Artists", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        val currentArtists = uiState.tasteProfile.favoriteArtists

        if (currentArtists.isEmpty()) {
            Text(
                "No favorite artists selected.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                currentArtists.forEach { artist ->
                    InputChip(
                        selected = true,
                        onClick = {
                            viewModel.setFavoriteArtists(currentArtists - artist)
                        },
                        label = { Text(artist) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove",
                                modifier = Modifier.size(16.dp),
                            )
                        },
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        var newArtistName by remember { mutableStateOf("") }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = newArtistName,
                onValueChange = { newArtistName = it },
                placeholder = { Text("Add artist name...") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    if (newArtistName.isNotBlank()) {
                        viewModel.setFavoriteArtists(currentArtists + newArtistName.trim())
                        newArtistName = ""
                    }
                },
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Add")
            }
        }
    }
}

@Composable
private fun AudioSettingsCard(
    settings: com.deepeye.musicpro.data.prefs.AppSettings,
    viewModel: SettingsViewModel,
) {
    SettingsCard {
        Text("Crossfade Duration", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Slider(
                value = settings.crossfadeDuration.toFloat(),
                onValueChange = { viewModel.setCrossfadeDuration(it.toInt()) },
                valueRange = 0f..12f,
                steps = 11,
                modifier = Modifier.weight(1f),
            )
            Text(
                "${settings.crossfadeDuration}s",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.width(32.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Show Visualizer", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = settings.showVisualizer, onCheckedChange = { viewModel.setShowVisualizer(it) })
        }
    }
}

@Composable
private fun LibrarySettingsCard(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
) {
    SettingsCard {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Rescan Library", style = MaterialTheme.typography.bodyMedium)
            if (uiState.isRescanningLibrary) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                OutlinedButton(onClick = { viewModel.rescanLibrary() }) { Text("Scan") }
            }
        }
    }
}

@Composable
private fun AboutSettingsCard(viewModel: SettingsViewModel) {
    SettingsCard {
        Text("DeepEye Music Pro", style = MaterialTheme.typography.titleSmall)
        Text(
            "Version ${com.deepeye.musicpro.BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Premium music player with V4A DSP engine",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text("deepeye.tech", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Check for updates", style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(
                onClick = {
                    android.util.Log.d("SettingsScreen", "Check Now clicked")
                    viewModel.checkForUpdate()
                },
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Check Now")
            }
        }
    }
}

@Composable
fun UpdateBanner(
    state: UpdateState,
    onDownloadClick: (url: String, version: String) -> Unit,
    onInstallClick: (java.io.File) -> Unit,
    onDismissClick: () -> Unit,
) {
    AnimatedVisibility(
        visible = state !is UpdateState.Idle && state !is UpdateState.UpToDate,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        val tealGlow = Color(0xFF00F2FE)
        val purpleGlow = Color(0xFF4FACFE)

        GlowCard(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            glowColor = tealGlow,
            secondaryGlowColor = purpleGlow,
            cornerRadius = 16.dp,
        ) {
            Column(
                modifier =
                Modifier
                    .fillMaxWidth()
                    // Added solid background to avoid unreadable overlapping with HeroSection
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f), RoundedCornerShape(16.dp))
                    .border(1.dp, tealGlow.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .padding(16.dp),
            ) {
                when (state) {
                    is UpdateState.Checking -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = tealGlow,
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(
                                "Checking for updates...",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    is UpdateState.UpdateAvailable -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "New Update: v${state.version}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                            colors = listOf(tealGlow, purpleGlow)
                                        )
                                    ),
                                    fontWeight = FontWeight.ExtraBold,
                                )
                                if (state.releaseNotes.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        state.releaseNotes,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            IconButton(onClick = onDismissClick, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = { onDownloadClick(state.apkUrl, state.version) },
                                shape = RoundedCornerShape(12.dp),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = tealGlow,
                                    contentColor = Color.Black
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text("Download", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    is UpdateState.Downloading -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    "Downloading update...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    "${(state.progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { state.progress },
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    is UpdateState.Downloaded -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Update Downloaded",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "Ready to install v${state.version}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = { onInstallClick(state.file) },
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text("Install")
                            }
                        }
                    }
                    is UpdateState.Error -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Update Error",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.error,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    state.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            TextButton(onClick = onDismissClick) {
                                Text("Dismiss")
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    com.deepeye.musicpro.ui.components.GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        cornerRadius = 24.dp,
        tintColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        borderColor = com.deepeye.musicpro.ui.theme.GlassBorderLight,
        refractionHeight = 0.2f
    ) {
        Column(Modifier.padding(20.dp), content = content)
    }
}
