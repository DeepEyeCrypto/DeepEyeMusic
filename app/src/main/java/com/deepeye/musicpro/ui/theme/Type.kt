// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ─── Ultra Premium Typography System (v2026) ─────────────────────────────────
// Inspired by Apple Music & Spotify Premium: Tighter tracking on display,
// robust weights for hierarchy, slightly looser tracking for metadata.

val AppTypography = Typography(
    
    // Display (For massive headers like Hero banners)
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 34.sp,
        fontWeight = FontWeight.ExtraBold,
        lineHeight = 40.sp,
        letterSpacing = (-1.0).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 34.sp,
        letterSpacing = (-0.8).sp,
    ),
    
    // Headlines (For standard screen titles)
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 30.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 26.sp,
        letterSpacing = (-0.3).sp,
    ),
    
    // Titles (For section headers, prominent list items like Song Titles)
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 24.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold, // Bumped to SemiBold for premium feel
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    
    // Body (For descriptions, settings descriptions)
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp,
    ),
    
    // Labels (For buttons, small badges, Artist names under songs)
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 14.sp,
        letterSpacing = 0.2.sp,
    ),
    labelSmall = TextStyle( // Captions, tiny metadata
        fontFamily = FontFamily.Default,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 12.sp,
        letterSpacing = 0.3.sp,
    )
)
