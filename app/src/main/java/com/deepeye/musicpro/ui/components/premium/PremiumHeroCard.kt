// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.components.premium

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.getValue
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.deepeye.musicpro.ui.components.DynamicLabel
import com.deepeye.musicpro.ui.components.SecondaryLabel
import com.deepeye.musicpro.ui.components.TertiaryLabel

@Composable
fun PremiumHeroCard(
    title: String,
    subtitle: String,
    imageUrl: String,
    modifier: Modifier = Modifier,
    badge: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.7f, stiffness = 400f)
    )

    val clickModifier = if (onClick != null) Modifier.clickable(interactionSource = interactionSource, indication = null, onClick = onClick) else Modifier
    Box(
        modifier =
        modifier
            .fillMaxWidth()
            .height(260.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .border(
                1.dp,
                Color(0x33FFFFFF),
                RoundedCornerShape(24.dp)
            )
            .clip(RoundedCornerShape(24.dp))
            .then(clickModifier),
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        // Gradient overlay for readability
        Box(
            modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent, 
                            Color.Black.copy(alpha = 0.4f),
                            Color.Black.copy(alpha = 0.95f)
                        ),
                        startY = 100f,
                    ),
                ),
        )

        Column(
            modifier =
            Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp),
        ) {
            if (badge != null) {
                Text(
                    text = badge.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF00E5FF), // Neon Cyan for premium pop
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    modifier =
                    Modifier
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
                Spacer(Modifier.height(4.dp))
            }

            DynamicLabel(
                text = title,
                backgroundColor = Color.Black,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                
            )

            SecondaryLabel(
                text = subtitle,
                backgroundColor = Color.Black,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                
            )
        }
    }
}
