package com.deepeye.musicpro.ui.theme

import androidx.compose.ui.unit.dp

object GlassTokens {
    // Blur - Keep under 25dp to avoid Android RenderEffect limits failing
    val BlurLight = 12.dp
    val BlurMedium = 16.dp
    val BlurHeavy = 20.dp
    val BlurSheet = 24.dp

    // Tint alpha
    const val TintLight = 0.15f
    const val TintMedium = 0.25f
    const val TintDark = 0.40f

    // Border
    const val BorderAlpha = 0.25f
    const val BorderWidth = 1f // dp

    // Specular highlight
    const val SpecularAlpha = 0.12f

    // Corner radii
    val CornerCard = 24.dp
    val CornerPill = 48.dp
    val CornerSheet = 32.dp
    val CornerButton = 16.dp

    // Elevation shadow
    val ShadowBlur = 32.dp
    const val ShadowAlpha = 0.08f // Soft but visible

    // Noise - keeps the premium frosted glass texture
    const val NoiseFactor = 0.05f 

    // Refraction (AGSL)
    val RefractionHeight = 8.dp
    const val ChromaMultiplier = 1.5f
    const val WhitePoint = 0.15f
}
