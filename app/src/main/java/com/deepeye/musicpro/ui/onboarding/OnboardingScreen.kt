// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalConfiguration


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    viewModel: TasteProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentStep by rememberSaveable { mutableStateOf(1) }
    
    var selectedLanguages by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var selectedArtists by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var selectedGenres by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var selectedMood by rememberSaveable { mutableStateOf("Balanced") }
    var autoplayEnabled by rememberSaveable { mutableStateOf(true) }
    var mixEnabled by rememberSaveable { mutableStateOf(true) }

    val amoledBlack = Color(0xFF030307)
    val neonPurple = Color(0xFF7B3FE4)
    val neonCyan = Color(0xFF00D2FF)
    val totalSteps = 4
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(amoledBlack)
    ) {
        // Glowing orbital blobs in the background for high fidelity
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.TopEnd)
                .offset(x = 100.dp, y = (-100).dp)
                .blur(100.dp)
                .background(neonPurple.copy(alpha = 0.15f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-100).dp, y = 100.dp)
                .blur(100.dp)
                .background(neonCyan.copy(alpha = 0.15f), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .align(Alignment.TopCenter)
                .padding(24.dp)
                .safeDrawingPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Progress / Logo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DEEPEYE MUSIC PRO",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = neonCyan,
                    letterSpacing = 2.sp
                )
                
                if (currentStep > 1) {
                    TextButton(
                        onClick = {
                            viewModel.setLanguages(selectedLanguages.toSet())
                            viewModel.setArtists(selectedArtists.toSet())
                            viewModel.setGenres(selectedGenres.toSet())
                            viewModel.setMood(selectedMood)
                            viewModel.setAutoplay(autoplayEnabled)
                            viewModel.setPersonalizedMix(mixEnabled)
                            viewModel.completeOnboarding()
                            onOnboardingComplete()
                        }
                    ) {
                        Text(
                            text = "SKIP TO APP",
                            color = neonCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
            
            // Linear Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                val progressWidthFraction = currentStep.toFloat() / totalSteps.toFloat()
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressWidthFraction)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(listOf(neonPurple, neonCyan)))
                )
            }

            Spacer(Modifier.height(32.dp))

            // Multi-step Content with animated transition
            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        slideInHorizontally { width -> if (targetState > initialState) width else -width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> if (targetState > initialState) -width else width } + fadeOut()
                    },
                    label = "step_transition"
                ) { step ->
                    when (step) {
                        1 -> WelcomeStep()
                        2 -> LanguageSelectionStep(
                            selectedLanguages = selectedLanguages.toSet(),
                            onLanguageToggle = { lang ->
                                selectedLanguages = if (selectedLanguages.contains(lang)) {
                                    selectedLanguages - lang
                                } else {
                                    selectedLanguages + lang
                                }
                            }
                        )
                        3 -> ArtistSelectionStep(
                            localArtists = uiState.localArtists.map { it.name },
                            curatedArtists = uiState.curatedArtists,
                            selectedArtists = selectedArtists.toSet(),
                            onArtistToggle = { artist ->
                                selectedArtists = if (selectedArtists.contains(artist)) {
                                    selectedArtists - artist
                                } else {
                                    selectedArtists + artist
                                }
                            }
                        )
                        4 -> MoodAndMixStep(
                            selectedGenres = selectedGenres.toSet(),
                            onGenreToggle = { genre ->
                                selectedGenres = if (selectedGenres.contains(genre)) {
                                    selectedGenres - genre
                                } else {
                                    selectedGenres + genre
                                }
                            },
                            selectedMood = selectedMood,
                            onMoodSelect = { selectedMood = it },
                            autoplayEnabled = autoplayEnabled,
                            onAutoplayToggle = { autoplayEnabled = it },
                            mixEnabled = mixEnabled,
                            onMixToggle = { mixEnabled = it }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Control Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (currentStep > 1) {
                    OutlinedButton(
                        onClick = { currentStep-- },
                        shape = RoundedCornerShape(14.dp),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                            brush = SolidColor(Color.White.copy(alpha = 0.2f))
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                    ) {
                        Text("BACK", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(16.dp))
                }

                val nextEnabled = when (currentStep) {
                    1 -> true
                    2 -> selectedLanguages.isNotEmpty()
                    3 -> selectedArtists.size >= 3 || (selectedArtists.isNotEmpty() && uiState.localArtists.isEmpty())
                    4 -> selectedGenres.isNotEmpty()
                    else -> false
                }

                Button(
                    onClick = {
                        when (currentStep) {
                            1 -> currentStep++
                            2 -> {
                                viewModel.setLanguages(selectedLanguages.toSet())
                                currentStep++
                            }
                            3 -> {
                                viewModel.setArtists(selectedArtists.toSet())
                                currentStep++
                            }
                            4 -> {
                                viewModel.setGenres(selectedGenres.toSet())
                                viewModel.setMood(selectedMood)
                                viewModel.setAutoplay(autoplayEnabled)
                                viewModel.setPersonalizedMix(mixEnabled)
                                viewModel.completeOnboarding()
                                onOnboardingComplete()
                            }
                        }
                    },
                    enabled = nextEnabled,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (nextEnabled) neonPurple else Color.White.copy(alpha = 0.05f),
                        contentColor = if (nextEnabled) Color.White else Color.White.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier
                        .weight(if (currentStep > 1) 1.5f else 1f)
                        .height(54.dp)
                        .border(
                            width = if (nextEnabled) 1.dp else 0.dp,
                            brush = Brush.linearGradient(listOf(neonPurple, neonCyan)),
                            shape = RoundedCornerShape(14.dp)
                        )
                ) {
                    Text(
                        text = if (currentStep < totalSteps) "NEXT" else "FINISH SETUP",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
fun WelcomeStep() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val neonPurple = Color(0xFF7B3FE4)
        val neonCyan = Color(0xFF00D2FF)
        
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(neonPurple, neonCyan))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "DP",
                color = Color.White,
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        }
        
        Spacer(Modifier.height(32.dp))
        
        Text(
            text = "Welcome to DeepEye Music Pro",
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        
        Spacer(Modifier.height(16.dp))
        
        Text(
            text = "The ultimate AI-powered music recommendation engine. We learn what you love locally on-device and create endless personalized streams.",
            fontSize = 16.sp,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LanguageSelectionStep(
    selectedLanguages: Set<String>,
    onLanguageToggle: (String) -> Unit
) {
    val languagesList = listOf(
        "Hindi", "English", "Punjabi", "Bhojpuri", "Tamil", "Telugu", 
        "Haryanvi", "Bengali", "Malayalam", "Kannada", "Marathi", "Gujarati"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Select Preferred Languages",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "We will tune your feeds and autoplay streams matching these selections.",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.5f)
        )
        
        Spacer(Modifier.height(32.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            languagesList.forEach { lang ->
                val isSelected = selectedLanguages.contains(lang)
                val neonPurple = Color(0xFF7B3FE4)
                val neonCyan = Color(0xFF00D2FF)
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isSelected) neonPurple.copy(alpha = 0.25f)
                            else Color.White.copy(alpha = 0.04f)
                        )
                        .border(
                            width = 1.dp,
                            brush = if (isSelected) Brush.linearGradient(listOf(neonPurple, neonCyan))
                                    else SolidColor(Color.White.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { onLanguageToggle(lang) }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = neonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            text = lang,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistSelectionStep(
    localArtists: List<String>,
    curatedArtists: List<String>,
    selectedArtists: Set<String>,
    onArtistToggle: (String) -> Unit
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    
    // Unique-merged list of local + popular curated artists to avoid duplicates
    val mergedArtists = remember(localArtists, curatedArtists) {
        (localArtists + curatedArtists).distinct()
    }

    val filteredArtists = remember(mergedArtists, searchQuery) {
        if (searchQuery.isBlank()) {
            mergedArtists
        } else {
            mergedArtists.filter { it.contains(searchQuery, ignoreCase = true) }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Select Favorite Artists",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Choose at least 3 singers to start personalized recommendations.",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.5f)
        )
        
        Spacer(Modifier.height(24.dp))

        // Premium search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search singers...", color = Color.White.copy(alpha = 0.4f)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White.copy(alpha = 0.5f)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.03f),
                focusedBorderColor = Color(0xFF00D2FF),
                unfocusedBorderColor = Color.White.copy(alpha = 0.12f)
            ),
            singleLine = true
        )

        if (filteredArtists.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No singers found matching \"$searchQuery\"",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            val configuration = LocalConfiguration.current
            val gridCells = when {
                configuration.screenWidthDp >= 800 -> 5
                configuration.screenWidthDp >= 600 -> 4
                configuration.screenWidthDp >= 400 -> 3
                else -> 2
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(gridCells),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredArtists) { artist ->
                    val isSelected = selectedArtists.contains(artist)
                    val neonPurple = Color(0xFF7B3FE4)
                    val neonCyan = Color(0xFF00D2FF)
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.2f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) neonPurple.copy(alpha = 0.15f)
                                else Color.White.copy(alpha = 0.03f)
                            )
                            .border(
                                width = 1.dp,
                                brush = if (isSelected) Brush.linearGradient(listOf(neonPurple, neonCyan))
                                        else SolidColor(Color.White.copy(alpha = 0.08f)),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onArtistToggle(artist) }
                            .padding(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // Initial Avatar circle
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) Brush.linearGradient(listOf(neonPurple, neonCyan))
                                        else Brush.linearGradient(listOf(Color(0xFF2C2F48), Color(0xFF151829)))
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = artist.firstOrNull()?.toString()?.uppercase() ?: "?",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }

                            Spacer(Modifier.height(12.dp))

                            Text(
                                text = artist,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                        
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .align(Alignment.TopEnd)
                                    .background(neonCyan, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF030307),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoodAndMixStep(
    selectedGenres: Set<String>,
    onGenreToggle: (String) -> Unit,
    selectedMood: String,
    onMoodSelect: (String) -> Unit,
    autoplayEnabled: Boolean,
    onAutoplayToggle: (Boolean) -> Unit,
    mixEnabled: Boolean,
    onMixToggle: (Boolean) -> Unit
) {
    val genresList = listOf(
        "Bollywood", "Pop", "Hip Hop", "Lofi", "Classical", "Devotional",
        "Rock", "Electronic", "R&B", "Jazz", "Folk", "Indie"
    )
    
    val moodsList = listOf(
        "Balanced", "Calm", "Focus", "Party", "Workout", "Late Night"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Mood & Smart Features",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Customize your AI recommendation engine preferences.",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.5f)
        )
        
        Spacer(Modifier.height(24.dp))
        
        // Genres Section
        Text(
            text = "FAVORITE GENRES",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.4f),
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(12.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            genresList.forEach { genre ->
                val isSelected = selectedGenres.contains(genre)
                val neonPurple = Color(0xFF7B3FE4)
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSelected) neonPurple.copy(alpha = 0.2f)
                            else Color.White.copy(alpha = 0.05f)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) neonPurple else Color.Transparent,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { onGenreToggle(genre) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = genre,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        // Mood Section
        Text(
            text = "DEFAULT MOOD",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.4f),
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(12.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            moodsList.forEach { mood ->
                val isSelected = selectedMood == mood
                val neonCyan = Color(0xFF00D2FF)
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSelected) neonCyan.copy(alpha = 0.2f)
                            else Color.White.copy(alpha = 0.05f)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) neonCyan else Color.Transparent,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { onMoodSelect(mood) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mood,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        
        // Toggles Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.03f))
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Smart Autoplay",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Keep playing similar music infinitely when your queue ends.",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = autoplayEnabled,
                        onCheckedChange = onAutoplayToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF7B3FE4)
                        )
                    )
                }
                
                Spacer(Modifier.height(16.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.05f)))
                Spacer(Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Personalized Mixes",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Generate dynamic mixes on HomeHub based on your history.",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = mixEnabled,
                        onCheckedChange = onMixToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF00D2FF)
                        )
                    )
                }
            }
        }
    }
}
