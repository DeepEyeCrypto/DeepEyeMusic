// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Premium typography scale for DeepEye Music Pro.
 *
 * Rules:
 * - Maximum 3 font weights: Regular (400), SemiBold (600), Bold (700)
 * - Uppercase labels use 2sp letter-spacing
 * - Track titles and artist names use marquee for overflow (not ellipsis)
 */
object TypeScale {

    /** Display — large hero text (e.g., artist name on editorial page). */
    val Display = TextStyle(
        fontSize = 34.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp,
    )

    /** Headline — section headers, Now Playing track title. */
    val Headline = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp,
    )

    /** Title — secondary titles, artist name in Now Playing. */
    val Title = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.15.sp,
    )

    /** Body — general content text. */
    val Body = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.25.sp,
    )

    /** Label — uppercase labels, badges, chips. */
    val Label = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 2.sp, // Premium editorial spacing for uppercase
    )

    /** Caption — timestamps, metadata, secondary info. */
    val Caption = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.4.sp,
    )
}
