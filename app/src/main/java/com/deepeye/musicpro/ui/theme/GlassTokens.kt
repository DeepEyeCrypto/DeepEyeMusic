package com.deepeye.musicpro.ui.theme

import androidx.compose.ui.unit.dp

object GlassTokens {
    // Blur
    val BlurLight = 12.dp
    val BlurMedium = 16.dp
    val BlurHeavy = 20.dp
    val BlurSheet = 24.dp

    // Tint alpha
    const val TintLight = 0.10f
    const val TintMedium = 0.15f
    const val TintDark = 0.22f

    // Border
    const val BorderAlpha = 0.20f
    const val BorderWidth = 1f // dp

    // Specular highlight
    const val SpecularAlpha = 0.08f

    // Corner radii
    val CornerCard = 24.dp
    val CornerPill = 48.dp
    val CornerSheet = 32.dp
    val CornerButton = 16.dp

    // Elevation shadow
    val ShadowBlur = 24.dp
    const val ShadowAlpha = 0.05f // Very soft

    // Noise
    const val NoiseFactor = 0f

    // Refraction (AGSL)
    val RefractionHeight = 8.dp
    const val ChromaMultiplier = 1.5f
    const val WhitePoint = 0.10f
}
