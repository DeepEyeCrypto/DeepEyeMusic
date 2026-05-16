package com.deepeye.musicpro.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

/**
 * Auto-scrolling marquee text for long song titles that overflow their container.
 *
 * If the text fits within the container, it displays normally.
 * If it overflows, it smoothly scrolls horizontally in a loop.
 */
@Composable
fun MarqueeText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
    gradientEdgeWidth: Float = 24f,
    scrollSpeed: Int = 8000 // ms per full scroll cycle
) {
    val density = LocalDensity.current
    var textWidth by remember { mutableStateOf(0) }
    var containerWidth by remember { mutableStateOf(0) }
    val needsScroll = textWidth > containerWidth

    if (needsScroll) {
        val infiniteTransition = rememberInfiniteTransition(label = "marquee")
        val offset by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = -(textWidth.toFloat() + containerWidth * 0.3f),
            animationSpec = infiniteRepeatable(
                animation = tween(scrollSpeed, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "marqueeOffset"
        )

        Box(
            modifier = modifier
                .clipToBounds()
                .onSizeChanged { containerWidth = it.width }
                .drawWithContent {
                    drawContent()
                    // Fade edges
                    drawRect(
                        brush = Brush.horizontalGradient(
                            0f to Color.Transparent,
                            gradientEdgeWidth / size.width to color,
                            (size.width - gradientEdgeWidth) / size.width to color,
                            1f to Color.Transparent
                        )
                    )
                }
        ) {
            Row {
                Text(
                    text = "$text     $text",
                    style = style,
                    color = color,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier
                        .offset { IntOffset(offset.toInt(), 0) }
                        .onSizeChanged { textWidth = it.width / 2 }
                )
            }
        }
    } else {
        Text(
            text = text,
            style = style,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier
                .onSizeChanged { containerWidth = it.width }
        )
    }
}

@Preview
@Composable
private fun MarqueeTextPreview() {
    MarqueeText(
        text = "This Is A Very Long Song Title That Should Scroll Smoothly Across The Screen",
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    )
}
