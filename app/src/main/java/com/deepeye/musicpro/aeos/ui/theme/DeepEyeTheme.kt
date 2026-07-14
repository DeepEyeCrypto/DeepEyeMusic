package com.deepeye.musicpro.aeos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DeepCyberBlue = Color(0xFF0F172A)
private val NeonTeal = Color(0xFF14B8A6)
private val DarkObsidian = Color(0xFF09090B)
private val TranslucentGlass = Color(0x33FFFFFF)

private val AeosDarkColorScheme = darkColorScheme(
    primary = NeonTeal,
    background = DarkObsidian,
    surface = DeepCyberBlue,
    surfaceVariant = TranslucentGlass
)

@Composable
fun DeepEyeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AeosDarkColorScheme,
        content = content
    )
}
