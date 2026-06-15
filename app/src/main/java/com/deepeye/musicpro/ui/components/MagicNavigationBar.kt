// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import com.deepeye.musicpro.ui.motion.rememberPremiumHaptics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import com.deepeye.musicpro.ui.LocalHazeState

// ─── Shared Types ──────────────────────────────────────────────────────────

data class MagicNavItem(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: String,
    val isFab: Boolean = false
)

// ScrollTracker stub to keep compatibility with DeepEyeMusicApp if it references it
class ScrollTracker(val maxOffsetPx: Float) {
    var shrinkFactor by mutableStateOf(0f)
    var scrollOffset = 0f
    fun onScroll(delta: Float) {}
    fun reset() {}
}
val LocalScrollTracker = staticCompositionLocalOf { ScrollTracker(150f) }

/**
 * Premium Flagship Navigation Bar — glassmorphism floating dock, spring animations,
 * active pill background, and dynamic accent theming.
 */
@Composable
fun MagicNavigationBar(
    items: List<MagicNavItem>,
    selectedIndex: Int,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFF1E1E24).copy(alpha = 0.85f),
    indicatorColor: Color = Color(0xFF00E5C3),
    activeIconColor: Color = Color.White,
    inactiveIconColor: Color = Color.White.copy(alpha = 0.5f),
    surfaceColor: Color = Color.Black, // Not strictly needed for glass dock
) {
    val density = LocalDensity.current
    var totalWidthPx by remember { mutableStateOf(0) }
    val hazeState = LocalHazeState.current

    val tabCount = items.size
    val tabWidthPx = if (tabCount > 0) totalWidthPx.toFloat() / tabCount else 0f
    
    // Animate pill position mathematically based on total width division
    val targetPillX = selectedIndex * tabWidthPx
    val targetPillWidth = tabWidthPx

    val animatedPillX by animateFloatAsState(
        targetValue = targetPillX,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
        label = "pillX"
    )
    val animatedPillWidth by animateFloatAsState(
        targetValue = targetPillWidth,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
        label = "pillWidth"
    )

    GlassContainer(
        tintColor = backgroundColor,
        hazeState = hazeState,
        cornerRadius = 32.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
            .height(56.dp)
            .onGloballyPositioned { coords ->
                totalWidthPx = coords.size.width
            }
    ) {


        // Inner explicit border for extra Apple-like shine
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 1.dp,
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.5f),
                            Color.White.copy(alpha = 0.1f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
        )
        
        // Icons Row
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Start, // Weight-based items fill the row automatically
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = index == selectedIndex
                
                // Micro animation physics
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    targetValue = if (isPressed) 0.85f else 1f,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "iconScale"
                )

                val haptics = rememberPremiumHaptics()

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null, // Remove default ripple for premium feel
                            onClick = {
                                if (!isSelected) {
                                    haptics.heavyClick()
                                    onItemClick(index)
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val tint = if (isSelected) activeIconColor else inactiveIconColor
                    val targetAlpha = tint.alpha
                    val animatedAlpha by animateFloatAsState(
                        targetValue = targetAlpha,
                        animationSpec = tween(250),
                        label = "iconAlpha"
                    )

                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                        tint = tint.copy(alpha = animatedAlpha),
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                    )
                }
            }
        }
    }
}
