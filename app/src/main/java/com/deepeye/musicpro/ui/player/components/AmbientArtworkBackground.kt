// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.player.components

import android.net.Uri
import android.os.Build
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/**
 * Ultra-premium Apple Music / Spotify-style Ambient Artwork Background.
 * Scales and hardware-blurs album art with a crossfade transition and safety scrim.
 */
@Composable
fun AmbientArtworkBackground(
    artworkUri: Uri?,
    primaryColor: Color = Color(0xFF00E5FF),
    secondaryColor: Color = Color(0xFFFF007F),
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF07090E))
    ) {
        // ── LAYER 1: AMBIENT HARDWARE-BLURRED ARTWORK WITH CROSSFADE ──
        Crossfade(
            targetState = artworkUri,
            animationSpec = tween(800),
            label = "AmbientArtworkCrossfade",
            modifier = Modifier.fillMaxSize()
        ) { uri ->
            if (uri != null) {
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(1.28f)
                        .then(
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                Modifier.blur(
                                    radius = 80.dp,
                                    edgeTreatment = BlurredEdgeTreatment.Unbounded
                                )
                            } else {
                                Modifier.blur(40.dp)
                            }
                        )
                        .alpha(0.50f)
                )
            }
        }

        // ── LAYER 2: DYNAMIC MONET COLOR TINT OVERLAY ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.35f),
                            secondaryColor.copy(alpha = 0.20f),
                            Color.Transparent
                        )
                    )
                )
        )

        // ── LAYER 3: SAFETY SCRIM (WCAG CONTRAST PROTECTOR) ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.30f),
                            Color.Black.copy(alpha = 0.45f),
                            Color.Black.copy(alpha = 0.70f)
                        )
                    )
                )
        )

        // ── LAYER 4 & 5: FOREGROUND CONTENT (VISUALIZERS, ARTWORK & CONTROLS) ──
        content()
    }
}
