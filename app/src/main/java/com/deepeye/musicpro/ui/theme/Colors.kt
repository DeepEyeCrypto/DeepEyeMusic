// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * DeepEye Music Pro — Color Palette
 *
 * Premium dark-first color scheme with deep blues, electric accents,
 * and glassmorphic surface tones.
 */

// ── Brand Colors ──
val DeepEyePrimary       = Color(0xFF6C63FF)   // Electric violet
val DeepEyePrimaryDark   = Color(0xFF5A52D5)   // Deep violet
val DeepEyeSecondary     = Color(0xFF00D2FF)   // Cyan accent
val DeepEyeTertiary      = Color(0xFFFF6B9D)   // Coral pink
val DeepEyeAccent        = Color(0xFF7C4DFF)   // Bright purple

// ── Dark Theme Surfaces ──
val DarkBackground       = Color(0xFF0A0E1A)   // Near-black navy
val DarkSurface          = Color(0xFF121829)   // Card surface
val DarkSurfaceVariant   = Color(0xFF1A2035)   // Elevated surface
val DarkSurfaceHigh      = Color(0xFF222840)   // High-elevation surface
val DarkOnBackground     = Color(0xFFE8EAED)   // Primary text
val DarkOnSurface        = Color(0xFFCACED6)   // Secondary text
val DarkOnSurfaceVariant = Color(0xFF9098A8)   // Tertiary text
val DarkOutline          = Color(0xFF2A3150)   // Border / divider

// ── Light Theme Surfaces ──
val LightBackground       = Color(0xFFF5F6FA)
val LightSurface          = Color(0xFFFFFFFF)
val LightSurfaceVariant   = Color(0xFFF0F1F5)
val LightSurfaceHigh      = Color(0xFFE8E9EE)
val LightOnBackground     = Color(0xFF1A1C20)
val LightOnSurface        = Color(0xFF2D3038)
val LightOnSurfaceVariant = Color(0xFF6B7080)
val LightOutline          = Color(0xFFD0D3DC)

// ── Status / Functional Colors ──
val SuccessGreen   = Color(0xFF00E676)
val WarningAmber   = Color(0xFFFFAB00)
val ErrorRed       = Color(0xFFFF5252)
val InfoBlue       = Color(0xFF448AFF)

// ── DSP Gain Budget Risk Colors ──
val GainSafe       = Color(0xFF00E676)   // Green — safe headroom
val GainModerate   = Color(0xFFFFAB00)   // Amber — approaching limit
val GainDanger     = Color(0xFFFF5252)   // Red — clipping risk

// ── Glassmorphic Overlay ──
val GlassWhite     = Color(0x1AFFFFFF)   // 10% white for glass effect
val GlassBorder    = Color(0x33FFFFFF)   // 20% white for glass border
