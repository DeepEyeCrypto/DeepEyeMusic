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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.deepeye.musicpro.ui.theme.CardGeometry

/**
 * Item count at or below which a rail is treated as "sparse" and centres its
 * contents instead of hugging the leading edge.
 *
 * Four is the point where a rail reads as a deliberate row rather than a
 * truncated one: below it, left-aligned cards leave a large empty tail that
 * looks like a failed load rather than a short feed.
 */
const val SparseRailThreshold = 4

/** Arrangement + content padding chosen together so they can never disagree. */
data class SparseAwareRailLayout(
    val arrangement: Arrangement.Horizontal,
    val contentPadding: PaddingValues,
)

/**
 * Layout for a horizontal rail that adapts to how much data it actually holds.
 *
 * ## The problem
 *
 * A rail laid out with `Arrangement.spacedBy` always starts at the leading edge.
 * When a feed returns two items on a wide landscape viewport, those two cards
 * sit in the top-left with most of the row empty — indistinguishable from a bug,
 * and the layout gives no hint that more content simply does not exist yet.
 *
 * ## The fix
 *
 * Below [SparseRailThreshold] items the rail centres itself, so a short feed
 * reads as intentionally composed rather than broken. At or above the threshold
 * it keeps the normal leading-aligned, edge-padded, scrollable rail.
 *
 * This is a pure function of the item count with no viewport measurement, so a
 * rail and the skeleton standing in for it cannot disagree about layout.
 */
@Composable
fun rememberSparseAwareRailLayout(
    itemCount: Int,
    spacing: Dp = CardGeometry.CardSpacing,
    edgePadding: Dp = CardGeometry.ScreenGutter,
): SparseAwareRailLayout =
    // Symmetric edge padding in both branches: an asymmetric leading padding
    // would offset the optical centre when the arrangement is Center.
    if (itemCount in 1 until SparseRailThreshold) {
        SparseAwareRailLayout(
            arrangement = Arrangement.Center,
            contentPadding = PaddingValues(horizontal = edgePadding)
        )
    } else {
        SparseAwareRailLayout(
            arrangement = Arrangement.spacedBy(spacing),
            contentPadding = PaddingValues(horizontal = edgePadding)
        )
    }

/**
 * Glassmorphic placeholder shown when a feed has resolved to zero items.
 *
 * ## Why this exists
 *
 * A feed that returns nothing used to render as a header followed by a large
 * empty region — the screen looked broken because there was no explanation and
 * no affordance. This card states plainly that there is nothing to show and
 * offers the one action that can change that.
 *
 * It uses the app's frosted surface rather than a flat Material container so an
 * empty state still looks on-brand, and it is deliberately *not* stretched to
 * full height: a centred card with breathing room reads better than a card
 * stretched down the screen.
 *
 * @param title headline, e.g. "No Content Available".
 * @param message one or two lines explaining why, or what to try.
 * @param offline true when the cause is connectivity; changes icon and copy tone.
 * @param onRetry optional action. Omit it for feeds with no meaningful retry.
 */
@Composable
fun EmptyFeedStateCard(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    offline: Boolean = false,
    onRetry: (() -> Unit)? = null,
) {
    val accent = if (offline) Color(0xFF9E9E9E) else Color(0xFF00E5C3)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(CardGeometry.ScreenGutter)
            .clip(CardGeometry.Compact.shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.06f),
                        Color.White.copy(alpha = 0.03f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.10f), CardGeometry.Compact.shape)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.15f))
                .border(1.dp, accent.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (offline) Icons.Rounded.WifiOff else Icons.Rounded.Refresh,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(22.dp),
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.65f),
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )

        if (onRetry != null) {
            TextButton(onClick = onRetry) {
                Text(
                    text = "Refresh",
                    color = accent,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

/**
 * Compact "nothing here yet" filler for a single rail inside an otherwise
 * populated feed.
 *
 * ## Geometry
 *
 * `fillMaxWidth()` × `height(CardGeometry.Video.minWidth)` — a 160dp tall band.
 * That height is deliberate: it matches the vertical footprint of a real video
 * row, so dropping a placeholder in for a missing rail does not change how far
 * down the page the content below it sits. A short placeholder would collapse
 * the layout, which reintroduces exactly the shift the geometry SSOT prevents.
 *
 * ## Dashed border
 *
 * Compose has no built-in dashed stroke, so the dashed cyan→violet outline is
 * drawn with [drawBehind] using a [PathEffect] dash. It reads as "placeholder"
 * at a glance without needing a label to carry that meaning.
 *
 * Use [EmptyFeedStateCard] instead when the *whole* feed is empty — a single
 * prominent empty state is better than a stack of per-section fillers.
 *
 * @param message muted, centred hint, e.g. "No recent activity".
 */
@Composable
fun EmptyFeedPlaceholder(
    message: String,
    modifier: Modifier = Modifier,
) {
    val radius = CardGeometry.Video.cornerRadius
    // Sweep the dash phase so the outline animates; a static dash can read as
    // a rendering artefact rather than a deliberate placeholder.
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(
        label = "placeholderDash"
    )
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * DASH_PERIOD_PX,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(
                durationMillis = 1600,
                easing = androidx.compose.animation.core.LinearEasing,
            ),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart,
        ),
        label = "placeholderDashPhase",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(CardGeometry.Video.minWidth)
            .clip(RoundedCornerShape(radius))
            .background(Color.White.copy(alpha = 0.05f))
            .drawBehind {
                val stroke = 1.dp.toPx()
                val inset = stroke / 2f
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF00E5C3).copy(alpha = 0.55f),
                            Color(0xFF7C4DFF).copy(alpha = 0.55f),
                        )
                    ),
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(radius.toPx()),
                    style = Stroke(
                        width = stroke,
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(DASH_ON_PX, DASH_OFF_PX),
                            phase = phase,
                        ),
                    ),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White.copy(alpha = 0.45f),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = CardGeometry.ScreenGutter),
        )
    }
}

/** Dash segment length, px. Must be > 0 or PathEffect throws. */
private const val DASH_ON_PX = 10f

/** Gap between dash segments, px. */
private const val DASH_OFF_PX = 8f

/** One full dash cycle, used to animate the phase over a seamless loop. */
private const val DASH_PERIOD_PX = DASH_ON_PX + DASH_OFF_PX