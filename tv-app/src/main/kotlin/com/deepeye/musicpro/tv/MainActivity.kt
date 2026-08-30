package com.deepeye.musicpro.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.tv.material3.MaterialTheme
import com.deepeye.musicpro.tv.ui.components.TVNavigationDrawer
import com.deepeye.musicpro.tv.ui.components.TVScreen
import com.deepeye.musicpro.tv.ui.screens.*
import com.deepeye.musicpro.tv.ui.theme.TVTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            TVTheme {
                TVAppContent()
            }
        }
    }
}

@Composable
fun TVAppContent() {
    var currentScreen by remember { mutableStateOf<TVScreen>(TVScreen.Home) }
    var selectedItem by remember { mutableStateOf<MediaItem?>(null) }
    
    // Sample data for preview
    val sampleSections = remember {
        listOf(
            Section(
                title = "Continue Watching",
                items = listOf(
                    MediaItem("1", "Believer", "Imagine Dragons", "https://picsum.photos/seed/believer/300/200", 0.5f),
                    MediaItem("2", "Thunder", "Imagine Dragons", "https://picsum.photos/seed/thunder/300/200", 0.3f),
                    MediaItem("3", "Radioactive", "Imagine Dragons", "https://picsum.photos/seed/radioactive/300/200", 0.8f)
                )
            ),
            Section(
                title = "Recommended",
                items = listOf(
                    MediaItem("4", "Shape of You", "Ed Sheeran", "https://picsum.photos/seed/shape/300/200"),
                    MediaItem("5", "Blinding Lights", "The Weeknd", "https://picsum.photos/seed/blinding/300/200"),
                    MediaItem("6", "Someone Like You", "Adele", "https://picsum.photos/seed/someone/300/200")
                )
            ),
            Section(
                title = "Trending",
                items = listOf(
                    MediaItem("7", "Uptown Funk", "Bruno Mars", "https://picsum.photos/seed/uptown/300/200"),
                    MediaItem("8", "Happy", "Pharrell Williams", "https://picsum.photos/seed/happy/300/200"),
                    MediaItem("9", "Shake It Off", "Taylor Swift", "https://picsum.photos/seed/shake/300/200")
                )
            )
        )
    }
    
    val relatedItems = remember {
        listOf(
            MediaItem("10", "Demons", "Imagine Dragons", "https://picsum.photos/seed/demons/300/200"),
            MediaItem("11", "Whatever It Takes", "Imagine Dragons", "https://picsum.photos/seed/whatever/300/200")
        )
    }
    
    androidx.tv.material3.Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxSize()) {
            // Navigation Drawer
            TVNavigationDrawer(
                currentScreen = currentScreen,
                onNavigate = { screen ->
                    currentScreen = screen
                    selectedItem = null
                },
                isExpanded = true
            )
            
            // Main Content
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            ) {
                when (currentScreen) {
                    is TVScreen.Home -> {
                        TvHomeScreen(
                            sections = sampleSections,
                            onItemSelected = { item ->
                                selectedItem = item
                                currentScreen = TVScreen.Details
                            }
                        )
                    }
                    is TVScreen.Search -> {
                        // Placeholder for Search screen
                        PlaceholderScreen("Search")
                    }
                    is TVScreen.Library -> {
                        // Placeholder for Library screen
                        PlaceholderScreen("Library")
                    }
                    is TVScreen.Settings -> {
                        TVSettingsScreen()
                    }
                    is TVScreen.Details -> {
                        selectedItem?.let { item ->
                            TvDetailsScreen(
                                item = item,
                                relatedItems = relatedItems,
                                onPlayClicked = { /* TODO: Start playback */ },
                                onRelatedItemSelected = { relatedItem ->
                                    selectedItem = relatedItem
                                },
                                onBack = {
                                    currentScreen = TVScreen.Home
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(name: String) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.tv.material3.Text(
            text = "$name Screen",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
