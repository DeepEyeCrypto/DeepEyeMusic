// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.components

import android.content.Context
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.deepeye.musicpro.ui.theme.GlassTokens
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import com.deepeye.musicpro.ui.LocalHazeState

/**
 * Modifier applying custom AGSL refraction and color aberration on Android 13+ (SDK 33+).
 */
fun Modifier.liquidGlassEffect(
    tiltProvider: () -> Offset,
    touch: Offset = Offset.Zero,
    refractionHeight: Float = 0.5f,
    chromaticAberration: Float = 0.1f,
    specularAlpha: Float = GlassTokens.SpecularAlpha,
    noiseFactor: Float = GlassTokens.NoiseFactor
): Modifier = this.composed {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@composed this

    val shader = remember {
        try {
            RuntimeShader(LiquidGlassShader.ShaderCode)
        } catch (e: Throwable) {
            null
        }
    }

    if (shader == null) return@composed this

    graphicsLayer {
        try {
            val tilt = tiltProvider()
            shader.setFloatUniform("size", size.width, size.height)
            shader.setFloatUniform("tilt", tilt.x, tilt.y)
            shader.setFloatUniform("touch", touch.x, touch.y)
            shader.setFloatUniform("refractionHeight", refractionHeight)
            shader.setFloatUniform("chromaticAberration", chromaticAberration)
            shader.setFloatUniform("specularAlpha", specularAlpha)
            shader.setFloatUniform("noiseFactor", noiseFactor)

            renderEffect = RenderEffect
                .createRuntimeShaderEffect(shader, "content")
                .asComposeRenderEffect()
        } catch (e: Throwable) {
            // Fallback
        }
    }
}

/**
 * Read accessibility reduce transparency system-level settings on Android.
 */
private fun isReduceTransparencyEnabled(context: Context): Boolean {
    val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
    val isHighContrastEnabled = try {
        val method = accessibilityManager?.javaClass?.getMethod("isHighTextContrastEnabled")
        method?.invoke(accessibilityManager) as? Boolean ?: false
    } catch (e: Exception) {
        false
    }
    val samsungKey = try {
        Settings.System.getInt(context.contentResolver, "accessibility_reduce_transparency", 0) != 0
    } catch (e: Exception) {
        false
    }
    val nativeReduceTransparency = if (Build.VERSION.SDK_INT >= 34) {
        try {
            val method = accessibilityManager?.javaClass?.getMethod("isReduceTransparencyEnabled")
            method?.invoke(accessibilityManager) as? Boolean ?: false
        } catch (e: Exception) {
            false
        }
    } else {
        false
    }
    return isHighContrastEnabled || samsungKey || nativeReduceTransparency
}

/**
 * Container component handling version fallbacks for blur and glassmorphism.
 */
@Composable
fun GlassContainer(
    tintColor: Color,
    hazeState: HazeState? = LocalHazeState.current,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = GlassTokens.CornerCard,
    shape: Shape? = null,
    refractionHeight: Float = 0.5f,
    chromaticAberration: Float = 0.1f,
    specularAlpha: Float = GlassTokens.SpecularAlpha,
    noiseFactor: Float = GlassTokens.NoiseFactor,
    tiltProvider: () -> Offset = { Offset.Zero },
    touch: Offset = Offset.Zero,
    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    val resolvedShape = shape ?: RoundedCornerShape(cornerRadius)
    val reduceTransparency = remember(context) { isReduceTransparencyEnabled(context) }

    when {
        // Reduce Transparency -> Opaque Fallback
        reduceTransparency -> {
            Box(
                modifier = modifier
                    .background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant, resolvedShape)
                    .border(1.dp, Color.White.copy(0.12f), resolvedShape),
                content = content
            )
        }
        // Android 13+ — full AGSL Liquid Glass + Haze
        Build.VERSION.SDK_INT >= 33 -> {
            Box(modifier = modifier, propagateMinConstraints = true) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(resolvedShape)
                        .run {
                            if (hazeState != null) {
                                this.hazeEffect(
                                    state = hazeState,
                                    style = HazeStyle(
                                        tint = HazeTint(tintColor.copy(alpha = tintColor.alpha.coerceAtMost(0.4f))),
                                        blurRadius = GlassTokens.BlurMedium,
                                        noiseFactor = GlassTokens.NoiseFactor
                                    )
                                )
                            } else {
                                this.background(tintColor, resolvedShape)
                            }
                        }
                        .border(1.dp, Color.White.copy(GlassTokens.BorderAlpha), resolvedShape)
                )
                content()
            }
        }
        // Android 12 — RenderEffect blur + Haze
        Build.VERSION.SDK_INT >= 31 -> {
            Box(modifier = modifier, propagateMinConstraints = true) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(resolvedShape)
                        .graphicsLayer {
                            renderEffect = android.graphics.RenderEffect
                                .createBlurEffect(32f, 32f, android.graphics.Shader.TileMode.CLAMP)
                                .asComposeRenderEffect()
                        }
                        .run {
                            if (hazeState != null) {
                                this.hazeEffect(
                                    state = hazeState,
                                    style = HazeStyle(
                                        tint = HazeTint(tintColor.copy(alpha = tintColor.alpha.coerceAtMost(0.4f))),
                                        blurRadius = GlassTokens.BlurMedium,
                                        noiseFactor = GlassTokens.NoiseFactor
                                    )
                                )
                            } else {
                                this.background(tintColor, resolvedShape)
                            }
                        }
                        .border(1.dp, Color.White.copy(GlassTokens.BorderAlpha), resolvedShape)
                )
                content()
            }
        }
        // Android 10/11 — static overlay fallback
        else -> {
            Box(
                modifier = modifier
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                tintColor.copy(alpha = 0.55f),
                                tintColor.copy(alpha = 0.35f)
                            )
                        ),
                        resolvedShape
                    )
                    .border(1.dp, Color.White.copy(0.15f), resolvedShape),
                content = content
            )
        }
    }
}

/**
 * Standard Glass Card component with dynamic specular highlight.
 */
@Composable
fun GlassCard(
    tintColor: Color,
    hazeState: HazeState? = LocalHazeState.current,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = GlassTokens.CornerCard,
    refractionHeight: Float = 0.5f,
    content: @Composable BoxScope.() -> Unit
) {
    val tiltState = rememberGyroscopeTilt()
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .clip(shape)
            .border(1.dp, Color.White.copy(GlassTokens.BorderAlpha), shape),
        contentAlignment = Alignment.Center
    ) {
        GlassContainer(
            tintColor = tintColor,
            hazeState = hazeState,
            modifier = Modifier.fillMaxSize(),
            cornerRadius = cornerRadius,
            refractionHeight = refractionHeight,
            tiltProvider = { tiltState.value }
        ) {
            GlassSpecularSurface(tiltProvider = { tiltState.value })
            content()
        }
    }
}

/**
 * Premium Glass Pill component for tabs, chips, and quick action options.
 */
@Composable
fun GlassPill(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.height(36.dp),
    icon: ImageVector? = null,
    active: Boolean = false,
    accent: Color = MaterialTheme.colorScheme.primary,
    hazeState: HazeState? = LocalHazeState.current
) {
    val tiltState = rememberGyroscopeTilt()
    val shape = RoundedCornerShape(GlassTokens.CornerPill)
    val tint by animateColorAsState(
        if (active) {
            accent.copy(alpha = 0.35f)
        } else {
            Color.White.copy(alpha = 0.08f)
        },
        animationSpec = tween(250),
        label = "GlassPillTint"
    )

    val haptics = com.deepeye.musicpro.ui.motion.rememberPremiumHaptics()

    Box(
        modifier = modifier
            .clip(shape)
            .clickable(onClick = {
                haptics.click()
                onClick()
            })
            .border(
                1.dp,
                if (active) {
                    accent.copy(alpha = 0.6f)
                } else {
                    Color.White.copy(alpha = 0.18f)
                },
                shape
            ),
        contentAlignment = Alignment.Center
    ) {
        GlassContainer(
            tintColor = tint,
            hazeState = hazeState,
            modifier = Modifier.fillMaxSize(),
            cornerRadius = GlassTokens.CornerPill,
            refractionHeight = 0.2f,
            tiltProvider = { tiltState.value }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                icon?.let {
                    Icon(
                        it,
                        null,
                        tint = if (active) accent else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    label,
                    color = if (active) accent else Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

/**
 * Premium Glass Button component.
 */
@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = GlassTokens.CornerButton,
    tintColor: Color = Color.White.copy(alpha = 0.15f),
    borderColor: Color = Color.White.copy(alpha = 0.2f),
    hazeState: HazeState? = LocalHazeState.current,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
    content: @Composable RowScope.() -> Unit
) {
    val tiltState = rememberGyroscopeTilt()
    val shape = RoundedCornerShape(cornerRadius)

    val haptics = com.deepeye.musicpro.ui.motion.rememberPremiumHaptics()

    Box(
        modifier = modifier
            .clip(shape)
            .clickable(onClick = {
                haptics.click()
                onClick()
            })
            .border(1.dp, borderColor, shape),
        contentAlignment = Alignment.Center
    ) {
        GlassContainer(
            tintColor = tintColor,
            hazeState = hazeState,
            modifier = Modifier.fillMaxSize(),
            cornerRadius = cornerRadius,
            refractionHeight = 0.3f,
            tiltProvider = { tiltState.value }
        ) {
            Row(
                modifier = Modifier.padding(contentPadding),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}

/**
 * Glass progress bar component with glass track border.
 */
@Composable
fun GlassProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.1f))
            .border(0.5.dp, Color.White.copy(alpha = 0.12f), shape)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(8.dp)
                .clip(shape)
                .background(color)
        )
    }
}

/**
 * Specular highlight overlay utilizing gyroscope tilt calculations.
 */
@Composable
fun GlassSpecularSurface(
    tiltProvider: () -> Offset,
    modifier: Modifier = Modifier,
    alpha: Float = GlassTokens.SpecularAlpha
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                val tilt = tiltProvider()
                val startOffset = Offset(
                    x = size.width * (0.1f - tilt.x * 0.15f),
                    y = size.height * (0.1f - tilt.y * 0.15f)
                )
                val endOffset = Offset(
                    x = size.width * (0.6f - tilt.x * 0.15f),
                    y = size.height * (0.6f - tilt.y * 0.15f)
                )
                val brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = alpha * 1.5f),
                        Color.White.copy(alpha = alpha * 0.3f),
                        Color.Transparent
                    ),
                    start = startOffset,
                    end = endOffset
                )
                drawRect(brush = brush)
            }
    )
}

/**
 * Slide-up Premium Glass Bottom Sheet.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun GlassBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = LocalHazeState.current,
    tintColor: Color = Color.White.copy(alpha = GlassTokens.TintDark),
    content: @Composable ColumnScope.() -> Unit
) {
    val tiltState = rememberGyroscopeTilt()
    val shape = RoundedCornerShape(topStart = GlassTokens.CornerSheet, topEnd = GlassTokens.CornerSheet)

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        shape = shape,
        containerColor = Color.Transparent,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(36.dp, 4.dp)
                    .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
        ) {
            GlassContainer(
                tintColor = tintColor,
                hazeState = hazeState,
                modifier = Modifier.fillMaxSize(),
                cornerRadius = GlassTokens.CornerSheet,
                refractionHeight = 0.6f,
                tiltProvider = { tiltState.value }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    content = content
                )
            }
        }
    }
}

/**
 * Custom GlassSurface component for general surfaces.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = GlassTokens.CornerCard,
    shape: Shape? = null,
    tintColor: Color = Color.White.copy(alpha = GlassTokens.TintMedium),
    borderColor: Color = Color.White.copy(alpha = GlassTokens.BorderAlpha),
    hazeState: HazeState? = LocalHazeState.current,
    refractionHeight: Float = 0.2f,
    content: @Composable BoxScope.() -> Unit,
) {
    val tiltState = rememberGyroscopeTilt()
    val resolvedShape = shape ?: RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .clip(resolvedShape)
            .border(1.dp, borderColor, resolvedShape),
        contentAlignment = Alignment.Center
    ) {
        GlassContainer(
            tintColor = tintColor,
            hazeState = hazeState,
            modifier = Modifier.fillMaxSize(),
            cornerRadius = cornerRadius,
            shape = resolvedShape,
            refractionHeight = refractionHeight,
            tiltProvider = { tiltState.value },
            content = content
        )
    }
}

/**
 * Premium Glass Top App Bar that blurs content scrolling underneath it.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun GlassTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    hazeState: dev.chrisbanes.haze.HazeState? = LocalHazeState.current,
    tintColor: Color = Color(0xFF1D1E26).copy(alpha = 0.4f)
) {
    androidx.compose.material3.TopAppBar(
        title = title,
        navigationIcon = navigationIcon,
        actions = actions,
        colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent
        ),
        modifier = modifier.then(
            if (hazeState != null) {
                Modifier.hazeEffect(
                    state = hazeState,
                    style = dev.chrisbanes.haze.HazeStyle(
                        tint = dev.chrisbanes.haze.HazeTint(tintColor),
                        blurRadius = 32.dp,
                        noiseFactor = 0.05f
                    )
                )
            } else {
                Modifier.background(tintColor)
            }
        )
    )
}
