// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.util

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Devices

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
