package com.deepeye.musicpro.tv.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.tv.material3.TvMaterialTheme
import androidx.tv.material3.darkColorScheme

val TvDarkColorScheme = darkColorScheme(
    primary = Color(0xFF0066CC),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF004A99),
    onPrimaryContainer = Color(0xFFE0E0E0),
    secondary = Color(0xFF0066CC),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF004A99),
    onSecondaryContainer = Color(0xFFE0E0E0),
    tertiary = Color(0xFF0066CC),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF004A99),
    onTertiaryContainer = Color(0xFFE0E0E0),
    background = Color(0xFF0F0F0F),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF1A1A1A),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFB0B0B0),
    error = Color(0xFFFF5252),
    onError = Color(0xFFFFFFFF),
    outline = Color(0xFF666666)
)

object TVTypography {
    val displayLarge = androidx.tv.material3.MaterialTheme.typography.displayLarge
    val displayMedium = androidx.tv.material3.MaterialTheme.typography.displayMedium
    val displaySmall = androidx.tv.material3.MaterialTheme.typography.displaySmall
    val headlineLarge = androidx.tv.material3.MaterialTheme.typography.headlineLarge
    val headlineMedium = androidx.tv.material3.MaterialTheme.typography.headlineMedium
    val headlineSmall = androidx.tv.material3.MaterialTheme.typography.headlineSmall
    val titleLarge = androidx.tv.material3.MaterialTheme.typography.titleLarge
    val titleMedium = androidx.tv.material3.MaterialTheme.typography.titleMedium
    val titleSmall = androidx.tv.material3.MaterialTheme.typography.titleSmall
    val bodyLarge = androidx.tv.material3.MaterialTheme.typography.bodyLarge
    val bodyMedium = androidx.tv.material3.MaterialTheme.typography.bodyMedium
    val bodySmall = androidx.tv.material3.MaterialTheme.typography.bodySmall
    val labelLarge = androidx.tv.material3.MaterialTheme.typography.labelLarge
    val labelMedium = androidx.tv.material3.MaterialTheme.typography.labelMedium
    val labelSmall = androidx.tv.material3.MaterialTheme.typography.labelSmall
}

@androidx.compose.runtime.Composable
fun TVTheme(
    content: @androidx.compose.runtime.Composable () -> Unit
) {
    TvMaterialTheme(
        colorScheme = TvDarkColorScheme,
        content = content
    )
}