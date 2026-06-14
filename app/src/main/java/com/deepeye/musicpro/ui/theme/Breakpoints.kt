// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Adaptive Fluid Design System (AFDS) Breakpoints.
 * 
 * Compact: 0-599dp (Phones)
 * Medium: 600-839dp (Tablets, small foldables)
 * Expanded: 840-1199dp (Large foldables, small desktops)
 * Large: 1200dp+ (Desktops, TVs)
 */
enum class AFDSBreakpoint {
    Compact,
    Medium,
    Expanded,
    Large
}

/**
 * Returns the current [AFDSBreakpoint] based on the screen width.
 */
@Composable
fun currentAFDSBreakpoint(): AFDSBreakpoint {
    val screenWidth = LocalConfiguration.current.screenWidthDp
    return when {
        screenWidth < 600 -> AFDSBreakpoint.Compact
        screenWidth < 840 -> AFDSBreakpoint.Medium
        screenWidth < 1200 -> AFDSBreakpoint.Expanded
        else -> AFDSBreakpoint.Large
    }
}

/**
 * Helper to determine grid columns based on AFDS rules.
 * Phone: 1 Column
 * Tablet: 2 Column
 * Foldable: 2-3 Column
 * Desktop: 3-4 Column
 */
@Composable
fun getAFDSGridColumns(): Int {
    return when (currentAFDSBreakpoint()) {
        AFDSBreakpoint.Compact -> 1
        AFDSBreakpoint.Medium -> 2
        AFDSBreakpoint.Expanded -> 3
        AFDSBreakpoint.Large -> 4
    }
}

/**
 * Calculates adaptive maximum content width to prevent UI from stretching infinitely on ultra-wide screens.
 */
@Composable
fun getAFDSMaxContentWidth(): Dp {
    return when (currentAFDSBreakpoint()) {
        AFDSBreakpoint.Compact -> Dp.Infinity
        AFDSBreakpoint.Medium -> 720.dp
        AFDSBreakpoint.Expanded -> 1000.dp
        AFDSBreakpoint.Large -> 1200.dp
    }
}
