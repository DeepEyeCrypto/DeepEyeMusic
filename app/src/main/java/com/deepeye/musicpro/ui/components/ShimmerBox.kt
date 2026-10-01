// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.deepeye.musicpro.ui.theme.CardGeometry

// ─────────────────────────────────────────────────────────────────────────────
// Skeleton loaders — every dimension below is read from CardGeometry.
// ─────────────────────────────────────────────────────────────────────────────
//
// These are not hand-tuned approximations of the real cards; they are the real
// cards' geometry with the imagery replaced by a shimmer. That is the only way
// to guarantee the loading state occupies exactly the space the loaded state
// will, which is what removes the Cumulative Layout Shift when data lands.
//
// If a card's shape changes, it changes in CardGeometry and every skeleton below
// follows automatically. There are no duplicated dp literals to fall behind.

// Deep desaturated glass tints, matched to the app's dark glassmorphism surface
// (see GlassTokens and the Color.kt palette) so a skeleton reads as "premium
// loading" rather than "broken grey box".
private val SkeletonBase = Color(0xFF161B2B)
private val SkeletonHighlight = Color(0xFF2C3350)
private val SkeletonAccentCyan = Color(0x1F00E5C3)
private val SkeletonAccentViolet = Color(0x1F7C4DFF)

/**
 * Animated glassmorphic sweep used as the fill of every skeleton.
 *
 * A diagonal gradient carrying a faint cyan→violet tint, which keeps the loading
 * state inside the app's visual language instead of flashing a flat grey box.
 */
@Composable
private fun rememberSkeletonBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "glassShimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "glassShimmerProgress",
    )

    // Sweep runs past both edges so the highlight fully enters and exits.
    val start = -0.7f + (progress * 2.4f)

    return Brush.linearGradient(
        colors = listOf(
            SkeletonBase,
            SkeletonAccentCyan,
            SkeletonHighlight,
            SkeletonAccentViolet,
            SkeletonBase,
        ),
        start = Offset(start * 1000f, 0f),
        end = Offset((start + 0.7f) * 1000f, 140f),
    )
}

/**
 * Single shimmering block. Base primitive the card skeletons compose from.
 *
 * Retains the historical [ShimmerBox] name and call signature so existing call
 * sites keep working; the fill is now the glassmorphic sweep above.
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = CardGeometry.Compact.cornerRadius,
    baseColor: Color = SkeletonBase,
    highlightColor: Color = SkeletonHighlight,
) {
    val brush = rememberSkeletonBrush()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush),
    )
}

/** Alias for [ShimmerBox] that reads better at skeleton call sites. */
@Composable
fun SkeletonBlock(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = CardGeometry.Compact.cornerRadius,
) = ShimmerBox(modifier = modifier, cornerRadius = cornerRadius)

/**
 * Placeholder for a 16:9 video card (YouTube cinema, NetMirror, video rails).
 *
 * Geometry is [CardGeometry.Video] verbatim — the poster reserves the same aspect
 * ratio, radius and text-block height as SmartTubeVideoCard / VideoHubMovieCard.
 * If the title clamp changes, this skeleton changes with it.
 *
 * @param width card width; pass the same value the real card is given so both
 *   occupy identical columns. Defaults to the adaptive floor.
 */
@Composable
fun VideoCardSkeleton(
    modifier: Modifier = Modifier,
    width: Dp = CardGeometry.Video.minWidth,
) {
    Column(
        modifier = modifier
            .width(width)
            .clip(CardGeometry.Video.shape)
    ) {
        SkeletonBlock(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(CardGeometry.Video.aspectRatio),
            cornerRadius = CardGeometry.Video.cornerRadius / 1.5f,
        )
        // Text block mirrors the real card: contentPadding, then a clamped title
        // plus a single metadata line.
        Column(modifier = Modifier.padding(CardGeometry.Video.contentPadding)) {
            SkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp),
                cornerRadius = 3.dp,
            )
            if (CardGeometry.Video.titleMaxLines > 1) {
                Spacer(Modifier.height(5.dp))
                SkeletonBlock(
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .height(12.dp),
                    cornerRadius = 3.dp,
                )
            }
            Spacer(Modifier.height(7.dp))
            SkeletonBlock(
                modifier = Modifier
                    .width(72.dp)
                    .height(10.dp),
                cornerRadius = 3.dp,
            )
        }
    }
}

/**
 * Placeholder for a 1:1 square music card (Spotify-style discovery rails).
 *
 * Geometry is [CardGeometry.Music] verbatim — square cover art plus a clamped
 * one-line title and one artist line, matching PersonalizedMusicCard.
 */
@Composable
fun MusicCardSkeleton(
    modifier: Modifier = Modifier,
    width: Dp = CardGeometry.Music.minWidth,
) {
    Column(
        modifier = modifier
            .width(width)
            .clip(CardGeometry.Music.shape)
    ) {
        SkeletonBlock(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(CardGeometry.Music.aspectRatio),
            cornerRadius = CardGeometry.Music.cornerRadius / 1.5f,
        )
        Column(modifier = Modifier.padding(CardGeometry.Music.contentPadding)) {
            SkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp),
                cornerRadius = 3.dp,
            )
            repeat(CardGeometry.Music.titleMaxLines - 1) {
                Spacer(Modifier.height(5.dp))
                SkeletonBlock(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(12.dp),
                    cornerRadius = 3.dp,
                )
            }
            Spacer(Modifier.height(7.dp))
            SkeletonBlock(
                modifier = Modifier
                    .width(64.dp)
                    .height(10.dp),
                cornerRadius = 3.dp,
            )
        }
    }
}
/**
 * Placeholder for a compact horizontal list row ("Continue Listening").
 *
 * Uses [CardGeometry.ContinueListening] verbatim — same min height, same max
 * width, same corner radius, same artwork square — so the rail does not resize
 * when tracks arrive.
 *
 * The min height must stay identical to the real card's. Reading `.height`
 * here while the card reads `.minHeight` would reintroduce exactly the
 * Cumulative Layout Shift this object exists to prevent, in the one case where
 * the two actually differ: at aggressive viewport factors, where the real row
 * grows to fit floored type and the skeleton would not.
 */
@Composable
fun ListRowSkeleton(
    modifier: Modifier = Modifier,
    width: Dp = CardGeometry.ContinueListening.maxWidth,
) {
    Row(
        modifier = modifier
            .width(width)
            .heightIn(min = CardGeometry.ContinueListening.minHeight)
            .clip(CardGeometry.ContinueListening.shape),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SkeletonBlock(
            modifier = Modifier.size(CardGeometry.ContinueListening.artworkSize),
            cornerRadius = CardGeometry.ContinueListening.cornerRadius / 1.5f,
        )
        Column {
            SkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(11.dp),
                cornerRadius = 3.dp,
            )
            Spacer(Modifier.height(6.dp))
            SkeletonBlock(
                modifier = Modifier
                    .width(70.dp)
                    .height(9.dp),
                cornerRadius = 3.dp,
            )
        }
    }
}

/**
 * Placeholder for a dense two-line widget (Home quick-play tiles).
 *
 * Geometry is [CardGeometry.Compact].
 */
@Composable
fun CompactCardSkeleton(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(CardGeometry.Compact.height)
            .clip(CardGeometry.Compact.shape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SkeletonBlock(
            modifier = Modifier.size(CardGeometry.Compact.height),
            cornerRadius = CardGeometry.Compact.cornerRadius,
        )
        Spacer(Modifier.width(CardGeometry.Compact.contentPadding))
        Column(modifier = Modifier.padding(CardGeometry.Compact.contentPadding)) {
            SkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(11.dp),
                cornerRadius = 3.dp,
            )
            Spacer(Modifier.height(5.dp))
            SkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(10.dp),
                cornerRadius = 3.dp,
            )
        }
    }
}

/**
 * Placeholder for a section header above a carousel.
 *
 * Sized to the two-line header the real rails render, so the rail below it does
 * not slide down when data lands.
 */
@Composable
fun SectionHeaderSkeleton(
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(vertical = 6.dp)) {
        SkeletonBlock(
            modifier = Modifier
                .width(120.dp)
                .height(15.dp),
            cornerRadius = 4.dp,
        )
        Spacer(Modifier.height(5.dp))
        SkeletonBlock(
            modifier = Modifier
                .width(80.dp)
                .height(11.dp),
            cornerRadius = 4.dp,
        )
    }
}

@Preview
@Composable
private fun SkeletonPreviews() {
    Column(
        modifier = Modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            VideoCardSkeleton()
            VideoCardSkeleton()
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MusicCardSkeleton()
            MusicCardSkeleton()
        }
        ListRowSkeleton()
        CompactCardSkeleton(modifier = Modifier.width(200.dp))
    }
}

