// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import com.deepeye.musicpro.ui.theme.palette.PaletteConstrainer

/**
 * Applies a classic Apple-style vibrancy/glassmorphism effect to text.
 * When drawn over a blurred background, this forces the text to blend
 * with the underlying pixels, creating a luminous overlay effect.
 */
fun Modifier.vibrancyEffect(enabled: Boolean = true, blendMode: BlendMode = BlendMode.Overlay): Modifier {
    return if (enabled) {
        this.graphicsLayer {
            this.blendMode = blendMode
        }
    } else {
        this
    }
}

/**
 * Calculates the highest-contrast text color (either Dark or Light) for a given background,
 * and ensures it meets the required WCAG contrast ratio.
 */
@Composable
fun getDynamicTextColor(backgroundColor: Color, isAAA: Boolean = false): Color {
    return remember(backgroundColor, isAAA) {
        val isBackgroundLight = PaletteConstrainer.isLight(backgroundColor)
        val baseTextColor = if (isBackgroundLight) Color(0xFF1A1A1A) else Color(0xFFFAFAFA)
        
        if (isAAA) {
            PaletteConstrainer.constrainForTextAAA(baseTextColor, backgroundColor)
        } else {
            PaletteConstrainer.constrainForText(baseTextColor, backgroundColor)
        }
    }
}

/**
 * A highly adaptive text label that guarantees WCAG compliance against any background.
 * Automatically flips to Dark or Light text depending on [backgroundColor].
 */
@Composable
fun DynamicLabel(
    text: String,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified, // If specified, overrides dynamic color
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current,
    useAAAContrast: Boolean = false,
    useVibrancy: Boolean = false
) {
    val dynamicColor = if (color == Color.Unspecified) {
        getDynamicTextColor(backgroundColor, useAAAContrast)
    } else {
        color
    }

    Text(
        text = text,
        modifier = modifier.vibrancyEffect(enabled = useVibrancy),
        color = dynamicColor,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style
    )
}

/**
 * A secondary emphasis label. Automatically applies a subtle opacity/muting 
 * to the dynamic text color while maintaining minimum WCAG AA contrast.
 */
@Composable
fun SecondaryLabel(
    text: String,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    style: TextStyle = LocalTextStyle.current,
    useVibrancy: Boolean = false
) {
    val primaryColor = getDynamicTextColor(backgroundColor, false)
    val mutedColor = remember(primaryColor, backgroundColor) {
        val isLightText = PaletteConstrainer.isLight(primaryColor)
        val baseSecondary = if (isLightText) Color(0xFFB3B3B3) else Color(0xFF4D4D4D)
        PaletteConstrainer.constrainForText(baseSecondary, backgroundColor)
    }

    Text(
        text = text,
        modifier = modifier.vibrancyEffect(enabled = useVibrancy),
        color = mutedColor,
        fontSize = fontSize,
        fontWeight = fontWeight,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = overflow,
        style = style
    )
}

/**
 * A tertiary emphasis label, usually for timestamps or minor metadata.
 */
@Composable
fun TertiaryLabel(
    text: String,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    style: TextStyle = LocalTextStyle.current,
    useVibrancy: Boolean = false
) {
    val primaryColor = getDynamicTextColor(backgroundColor, false)
    val mutedColor = remember(primaryColor, backgroundColor) {
        val isLightText = PaletteConstrainer.isLight(primaryColor)
        val baseTertiary = if (isLightText) Color(0xFF808080) else Color(0xFF666666)
        PaletteConstrainer.constrainForText(baseTertiary, backgroundColor)
    }

    Text(
        text = text,
        modifier = modifier.vibrancyEffect(enabled = useVibrancy),
        color = mutedColor,
        fontSize = fontSize,
        fontWeight = fontWeight,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = overflow,
        style = style
    )
}
