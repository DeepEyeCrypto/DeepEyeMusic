// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.util

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.deepeye.musicpro.ui.theme.CardGeometry
import kotlin.math.floor

/**
 * Card width that makes a carousel fill its viewport edge-to-edge with no
 * trailing gap.
 *
 * Picks the largest whole number of cards that fit at [minItemWidth], then
 * divides the *remaining* width between them. A fixed card width can never do
 * this: on a 891dp-wide landscape phone a 220dp card leaves roughly a third of
 * the row empty, which is the dead space this pass exists to remove.
 *
 * By construction the result is always >= [minItemWidth] — `count` is the
 * largest n with `n * (minItemWidth + spacing) <= usable + spacing`, which
 * rearranges to `(usable - spacing * (n - 1)) / n >= minItemWidth` — so no
 * upper clamp is needed and wide screens gain *more* columns rather than
 * larger cards.
 */
fun evenCarouselCardWidth(
    availableWidth: Dp,
    minItemWidth: Dp,
    spacing: Dp = CardGeometry.CardSpacing,
    edgeGutter: Dp = CardGeometry.ScreenGutter,
): Dp {
    val usable = availableWidth - edgeGutter * 2
    if (usable <= 0.dp) return minItemWidth
    val count = floor((usable + spacing) / (minItemWidth + spacing)).toInt().coerceAtLeast(1)
    return (usable - spacing * (count - 1)) / count
}

/**
 * Number of columns that fit [minItemWidth] cards across [availableWidth].
 *
 * Used where a grid is built by hand (chunked rows) rather than by
 * `LazyVerticalGrid`, so it has to answer the same question `GridCells.Adaptive`
 * answers internally.
 */
fun adaptiveColumnCount(
    availableWidth: Dp,
    minItemWidth: Dp,
    spacing: Dp = CardGeometry.CardSpacing,
    edgeGutter: Dp = CardGeometry.ScreenGutter,
    maxColumns: Int = 8,
): Int {
    val usable = availableWidth - edgeGutter * 2
    if (usable <= 0.dp) return 1
    return floor((usable + spacing) / (minItemWidth + spacing)).toInt().coerceIn(1, maxColumns)
}

/**
 * Provides responsive horizontal and vertical padding based on the current window width size class.
 */
@Composable
fun rememberResponsivePadding(windowSizeClass: WindowSizeClass): PaddingValues {
    return remember(windowSizeClass) {
        when (windowSizeClass.widthSizeClass) {
            WindowWidthSizeClass.Compact -> PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            WindowWidthSizeClass.Medium -> PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            else -> PaddingValues(horizontal = 32.dp, vertical = 16.dp)
        }
    }
}

/**
 * Unified Preview annotation supporting 5 key device profiles (portrait, landscape, tablet, foldable).
 */
@Preview(name = "Phone Portrait", device = Devices.PHONE)
@Preview(name = "Phone Landscape", device = "spec:width=891dp,height=411dp,dpi=420")
@Preview(name = "Tablet 7inch", device = Devices.TABLET)
@Preview(name = "Tablet 10inch", device = Devices.NEXUS_10)
@Preview(name = "Foldable", device = "spec:width=673dp,height=841dp,dpi=420")
annotation class DevicePreviews
