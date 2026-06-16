// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Ultra Premium Dark Luxury Palette (v2026) ─────────────────────────────────

// Backgrounds
val RichBlack = Color(0xFF060608) // Deepest black, OLED friendly but not absolute 000
val GraphiteGlass = Color(0xFF14141B) // Elevated glass surface
val GraphiteGlassElevated = Color(0xFF1D1D27) // Higher elevation glass

// Brand Accents
val ElectricViolet = Color(0xFF7B3FE4) // Primary brand color
val DeepPurple = Color(0xFF4A148C) // Secondary deeper tone
val NeonCyan = Color(0xFF00E5C3) // High-contrast accent

// Text Colors
val TextPrimary = Color(0xFFF9FAFF) // Crisp, slightly cool white
val TextSecondary = Color(0xFFA5A9B8) // Muted blue-grey for metadata
val TextTertiary = Color(0xFF6C7086) // Very muted for borders/disabled

// Special FX Colors
val GlowTeal = Color(0xFF33E1D1)
val GlowPink = Color(0xFFFF5DA2)
val GlowOrange = Color(0xFFFF6B35)

// Glass Tokens
val GlassBorderLight = Color.White.copy(alpha = 0.12f)
val GlassBorderDark = Color.Black.copy(alpha = 0.30f)
val GlassTintDark = Color.Black.copy(alpha = 0.22f)
val SpecularHighlight = Color.White.copy(alpha = 0.08f)

// Legacy Fallbacks (kept for compilation safety during migration)
val DeepBg = RichBlack
val DeepSurface = GraphiteGlass
val DeepSurface2 = GraphiteGlassElevated
val TealGlow = GlowTeal
val TealDim = Color(0xFF1CAFA3)
val AccentHot = GlowOrange
val AccentPink = GlowPink
val TextMuted = TextTertiary
val GlassBorder = GlassBorderLight
val GlassTint = GlassTintDark

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

val DeepEyePrimary = ElectricViolet
val DeepEyeSecondary = NeonCyan

val AmoledBlack = Color(0xFF000000)
val AmoledSurface = Color(0xFF0A0A0E)
val AmoledSurface2 = Color(0xFF111118)

// ─── Material 3 Expressive Premium Palette ──────────────────────────────────
val ExpressivePrimary = Color(0xFFFFD700) // Gold
val ExpressivePrimaryVariant = Color(0xFFFFC400) // Darker Gold
val ExpressiveSecondary = Color(0xFFFF6B35) // Orange
val ExpressiveTertiary = Color(0xFF2196F3) // Blue
val ExpressiveBackground = Color(0xFF0A0A0A) // Deep black
val ExpressiveSurface = Color(0xFF1E1E1E) // Dark gray
val ExpressiveSurfaceVariant = Color(0xFF2A2A2A) // Lighter dark gray
val ExpressiveOnPrimary = Color(0xFF000000) // Black
val ExpressiveOnBackground = Color(0xFFFFFFFF) // White
