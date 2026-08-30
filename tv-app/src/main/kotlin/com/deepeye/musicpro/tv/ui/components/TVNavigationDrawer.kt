package com.deepeye.musicpro.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme

sealed class TVScreen(val route: String, val label: String, val icon: ImageVector) {
    object Home : TVScreen("home", "Home", Icons.Default.Home)
    object Search : TVScreen("search", "Search", Icons.Default.Search)
    object Library : TVScreen("library", "Library", Icons.Default.VideoLibrary)
    object Settings : TVScreen("settings", "Settings", Icons.Default.Settings)
}

@Composable
fun TVNavigationDrawer(
    currentScreen: TVScreen,
    onNavigate: (TVScreen) -> Unit,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = true
) {
    val screens = listOf(TVScreen.Home, TVScreen.Search, TVScreen.Library, TVScreen.Settings)
    
    Column(
        modifier = modifier
            .width(if (isExpanded) 80.dp else 60.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        
        screens.forEach { screen ->
            NavigationDrawerItem(
                screen = screen,
                isSelected = screen == currentScreen,
                onClick = { onNavigate(screen) },
                isExpanded = isExpanded
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun NavigationDrawerItem(
    screen: TVScreen,
    isSelected: Boolean,
    onClick: () -> Unit,
    isExpanded: Boolean
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Card(
        onClick = onClick,
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .scale(if (isFocused) 1.1f else 1.0f),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .width(if (isExpanded) 64.dp else 44.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = screen.icon,
                contentDescription = screen.label,
                modifier = Modifier.size(32.dp)
            )
            if (isExpanded) {
                androidx.tv.material3.Text(
                    text = screen.label,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Preview
@Composable
private fun TVNavigationDrawerPreview() {
    TVTheme {
        TVNavigationDrawer(
            currentScreen = TVScreen.Home,
            onNavigate = {},
            isExpanded = true
        )
    }
}