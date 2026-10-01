// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.deepeye.musicpro.ui.theme.GlassTokens
import com.deepeye.musicpro.ui.theme.NeonCyan
import com.deepeye.musicpro.ui.theme.TextSecondary

/** Accent used for the CTA border, matching the app's violet secondary. */
private val HeroVioletAccent = Color(0xFF7C4DFF)


/**
 * Full-viewport empty state for a feed that resolved to *absolutely nothing*.
 *
 * ## Why this exists
 *
 * HomeHub declares ten conditional rails. When a brand-new account has no
 * history, no playlists and no recommendations, every one of those rails
 * renders nothing — and the user is left staring at a header, two dividers and
 * a large dark void where the content should be. That void reads as a bug or a
 * failed load, not as "you have not got any music yet".
 *
 * [EmptyFeedStateCard] solves the smaller problem (one empty rail inside an
 * otherwise populated feed). This solves the larger one: the *entire* feed is
 * empty, so the correct answer is a single prominent state that owns the whole
 * viewport, not a stack of per-section fillers.
 *
 * ## Geometry
 *
 * [Modifier.fillMaxSize] with a centred [BoxWithConstraints] means this
 * composable is sized by its parent, never by its content.
 *
 * [contentPadding] insets the *content* inside that full-viewport surface. It
 * is not a substitute for the host's own dock inset: DeepEyeMusicApp already
 * pads the screen clear of the glass dock, so passing another 140dp here would
 * double-count it and starve the hero of height on short viewports.
 *
 * Because the available height is not guaranteed, the layout is responsive:
 * [BoxWithConstraints] measures what is actually left and shrinks the disc and
 * the vertical rhythm on short viewports. A full-height design that assumes
 * room will, on a 360dp-tall landscape phone, push its own copy and CTA off the
 * bottom of the screen and leave the user staring at a lone icon.
 *
 * @param title headline. Short enough to stay on one line.
 * @param subtitle one or two lines explaining *why* it is empty.
 * @param actionText optional CTA label. Omit to render no button at all.
 * @param onAction invoked when the CTA is tapped. Ignored when null.
 * @param icon the glyph to feature. Defaults to a music note.
 * @param contentPadding insets for the content inside the full-viewport
 *   surface, such as extra breathing room. Defaults to 32dp on every edge.
 */
@Composable
fun HeroEmptyState(
    title: String,
    subtitle: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.LibraryMusic,
    contentPadding: PaddingValues = PaddingValues(32.dp),
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        // The disc must be sized from the space that is actually left, not
        // assumed. This device is an 802x360dp landscape phone and the host has
        // already inset the dock, so only ~235dp of height reaches us — while a
        // fixed 160dp disc plus copy plus a CTA needs ~350dp. Hard-coding the
        // large disc overflows the box and shoves the title, subtitle and CTA
        // below the fold: precisely the "lone icon on an empty screen" failure
        // this component exists to prevent.
        val compact = maxHeight < 360.dp
        val discSize = if (compact) 88.dp else 160.dp
        val glyphSize = if (compact) 34.dp else 40.dp

        Column(
            // verticalScroll is a safety net, not decoration: at extreme heights
            // (multi-window, IME open) even the compact layout can outgrow the
            // box. With it the content stays reachable instead of being clipped
            // away, and it is a no-op whenever the content already fits.
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HeroGlowIcon(icon, discSize, glyphSize)

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = if (compact) 12.dp else 20.dp),
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(top = if (compact) 6.dp else 10.dp)
                    .padding(horizontal = 16.dp),
            )

            // Rendered only when there is something to do *and* something to do
            // it with. A button that cannot be pressed is worse than no button.
            if (actionText != null && onAction != null) {
                OutlinedButton(
                    onClick = onAction,
                    shape = RoundedCornerShape(GlassTokens.CornerButton),
                    border = BorderStroke(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            listOf(
                                NeonCyan.copy(alpha = 0.55f),
                                HeroVioletAccent.copy(alpha = 0.55f),
                            )
                        ),
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier
                        .padding(top = if (compact) 14.dp else 28.dp)
                        // Guards against a long translated label stretching the
                        // pill past a narrow viewport.
                        .heightIn(min = 48.dp),
                ) {
                    Text(
                        text = actionText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * The large, desaturated, softly glowing glyph at the top of a [HeroEmptyState].
 *
 * The glow is a radial gradient disc drawn behind a crisp one. It is
 * deliberately *not* [Modifier.blur]: blur is clipped to the drawing layer's
 * bounds, so a blurred box renders as a visible hard-edged square rather than
 * a soft halo. A radial gradient reaches zero alpha at its own edge, so it fades
 * out with no seam, costs no offscreen render pass, and behaves identically on
 * API levels where blur is unsupported.
 */
@Composable
private fun HeroGlowIcon(icon: ImageVector, discSize: Dp, glyphSize: Dp) {
    Box(contentAlignment = Alignment.Center) {
        // Soft halo — radial gradient, transparent at the rim.
        Box(
            modifier = Modifier
                .size(discSize)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            NeonCyan.copy(alpha = 0.30f),
                            NeonCyan.copy(alpha = 0.0f),
                        ),
                    )
                ),
        )

        // Crisp disc + desaturated glyph.
        Box(
            modifier = Modifier
                .size(discSize * 0.6f)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                // Decorative: the title and subtitle already carry the meaning,
                // so announcing the glyph would only add noise.
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(glyphSize),
            )
        }
    }
}
