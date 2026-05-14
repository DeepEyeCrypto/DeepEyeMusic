package com.deepeye.musicpro.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.deepeye.musicpro.ui.theme.DeepEyePrimary
import com.deepeye.musicpro.ui.theme.DeepEyeSecondary
import com.deepeye.musicpro.ui.theme.GlassBorder
import com.deepeye.musicpro.ui.theme.GlassWhite

/**
 * Glassmorphic card component with animated glow border.
 *
 * Features:
 * - Frosted glass background
 * - Configurable glow color with animated pulse
 * - Rounded corners with semi-transparent border
 */
@Composable
fun GlowCard(
    modifier: Modifier = Modifier,
    glowColor: Color = DeepEyePrimary,
    secondaryGlowColor: Color = DeepEyeSecondary,
    cornerRadius: Dp = 16.dp,
    glowIntensity: Float = 0.6f,
    animateGlow: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)

    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val animatedAlpha by infiniteTransition.animateFloat(
        initialValue = if (animateGlow) 0.3f else glowIntensity,
        targetValue = if (animateGlow) glowIntensity else glowIntensity,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(modifier = modifier) {
        // Glow layer (behind the card)
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(4.dp)
                .blur(20.dp)
                .clip(shape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            glowColor.copy(alpha = animatedAlpha),
                            secondaryGlowColor.copy(alpha = animatedAlpha * 0.6f)
                        )
                    )
                )
        )

        // Glass card
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .background(GlassWhite)
                .border(1.dp, GlassBorder, shape)
        )

        // Content
        Box(
            modifier = Modifier
                .clip(shape)
                .padding(16.dp),
            content = content
        )
    }
}

@Preview
@Composable
private fun GlowCardPreview() {
    GlowCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Preview content
    }
}
