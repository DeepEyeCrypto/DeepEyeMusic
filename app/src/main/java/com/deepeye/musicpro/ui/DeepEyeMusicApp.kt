package com.deepeye.musicpro.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.deepeye.musicpro.ui.navigation.NavGraph
import com.deepeye.musicpro.ui.navigation.Routes

/** CompositionLocal for PiP state — accessible anywhere in the Compose tree */
val LocalPipMode = compositionLocalOf { false }

/**
 * Root composable for the DeepEyeMusicPro app.
 * Hosts the Scaffold with bottom navigation and the NavGraph.
 */

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun DeepEyeMusicApp(
    isInPipMode: Boolean = false,
    fullscreenMode: FullscreenMode = FullscreenMode()
) {
    CompositionLocalProvider(
        LocalPipMode provides isInPipMode,
        LocalFullscreenMode provides fullscreenMode
    ) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val bottomNavItems = remember {
        listOf(
            BottomNavItem(Routes.Home.route, "Home", Icons.Filled.Home, Icons.Outlined.Home),
            BottomNavItem(Routes.YouTube.route, "YouTube", Icons.Filled.Subscriptions, Icons.Outlined.Subscriptions),
            BottomNavItem(Routes.Music.route, "Music", Icons.Filled.MusicNote, Icons.Outlined.MusicNote),
            BottomNavItem(Routes.Library.route, "Library", Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic),
            BottomNavItem(Routes.Search.route, "Search", Icons.Filled.Search, Icons.Outlined.Search),
            BottomNavItem(Routes.V4A.route, "DSP", Icons.Filled.GraphicEq, Icons.Outlined.GraphicEq)
        )
    }

    // Hide bottom nav on full-screen destinations AND in PiP mode
    val showBottomBar = !isInPipMode && currentDestination?.hierarchy?.any { dest ->
        bottomNavItems.any { it.route == dest.route }
    } == true

    Scaffold(
        bottomBar = {
            Column {
                // Mini Player
                AnimatedVisibility(
                    visible = showBottomBar,
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it }
                ) {
                    com.deepeye.musicpro.ui.player.MiniPlayer(
                        onClick = {
                            navController.navigate(Routes.NowPlaying.route)
                        }
                    )
                }

                // Bottom Navigation
                AnimatedVisibility(
                    visible = showBottomBar,
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it }
                ) {
                    NavigationBar {
                        bottomNavItems.forEach { item ->
                            val selected = currentDestination?.hierarchy?.any {
                                it.route == item.route
                            } == true

                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) },
                                selected = selected,
                                onClick = {
                                    if (!selected) {
                                        navController.navigate(item.route) {
                                            popUpTo(Routes.Home.route) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavGraph(
                navController = navController
            )
        }
    }
    } // end CompositionLocalProvider
}
