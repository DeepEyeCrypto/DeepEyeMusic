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

// Legibility floor: nothing below [UiScale.MinReadableFontSize] (12sp). The
// previous scale bottomed out at labelSmall = 10.sp, and labelSmall is the
// second most-referenced style in the app (76 call sites), so that floor was
// doing the damage on nearly every screen. It is also why displaySmall,
// headlineSmall, titleSmall and bodySmall were never written out: an omitted
// parameter silently falls back to the Material3 default rather than failing,
// so the smallest and most widely used styles were the ones nobody tuned —
// bodySmall (77 call sites) was inheriting 12sp with no say in it. All 15
// styles are now explicit.
val AppTypography = Typography(
    
    // Display (For massive headers like Hero banners)
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 30.sp, // was 36.sp — landscape density pass
        fontWeight = FontWeight.ExtraBold,
        lineHeight = 36.sp,
        letterSpacing = (-0.8).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 26.sp, // was 30.sp
        fontWeight = FontWeight.Bold,
        lineHeight = 31.sp,
        letterSpacing = (-0.6).sp,
    ),
    
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 23.sp, // was 26.sp
        fontWeight = FontWeight.Bold,
        lineHeight = 28.sp,
        letterSpacing = (-0.4).sp,
    ),

    // Headlines (For standard screen titles)
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 23.sp, // was 26.sp
        fontWeight = FontWeight.Bold,
        lineHeight = 28.sp,
        letterSpacing = (-0.4).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 20.sp, // was 22.sp
        fontWeight = FontWeight.SemiBold,
        lineHeight = 25.sp,
        letterSpacing = (-0.2).sp,
    ),
    
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 18.sp, // was 20.sp
        fontWeight = FontWeight.SemiBold,
        lineHeight = 23.sp,
        letterSpacing = (-0.1).sp,
    ),

    // Titles (For section headers, prominent list items like Song Titles)
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 18.sp, // was 20.sp
        fontWeight = FontWeight.SemiBold,
        lineHeight = 23.sp,
        letterSpacing = (-0.1).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 17.sp, // was 15.sp — song titles, the app's most-read text
        fontWeight = FontWeight.SemiBold, // Bumped to SemiBold for premium feel
        lineHeight = 23.sp,
        letterSpacing = 0.sp,
    ),
    
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),

    // Body (For descriptions, settings descriptions)
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 16.sp, // was 14.sp
        fontWeight = FontWeight.Normal,
        lineHeight = 23.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 14.sp, // was 13.sp
        fontWeight = FontWeight.Normal,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 13.sp, // was the inherited Material3 default of 12sp
        fontWeight = FontWeight.Normal,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp,
    ),

    // Labels (For buttons, small badges, Artist names under songs)
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 14.sp, // was 12.sp
        fontWeight = FontWeight.Medium,
        lineHeight = 19.sp,
        letterSpacing = 0.2.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 13.sp, // was 11.sp
        fontWeight = FontWeight.Medium,
        lineHeight = 17.sp,
        letterSpacing = 0.2.sp,
    ),
    labelSmall = TextStyle( // Captions, tiny metadata
        fontFamily = FontFamily.Default,
        fontSize = 12.sp, // was 10.sp — the floor of the whole app
        fontWeight = FontWeight.Medium,
        lineHeight = 16.sp,
        letterSpacing = 0.3.sp,
    )
)
