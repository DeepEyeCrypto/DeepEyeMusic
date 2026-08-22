package com.deepeye.musicpro.ui.homehub

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

data class FluidMenuItem(
    val icon: ImageVector,
    val tint: Color = Color.White,
    val onClick: () -> Unit
)

@Composable
fun FluidMenu(
    items: List<FluidMenuItem>,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    // Animation state
    val transition = updateTransition(targetState = isExpanded, label = "MenuExpansion")
    
    val rotation by transition.animateFloat(
        transitionSpec = { tween(300, easing = FastOutSlowInEasing) },
        label = "MenuRotation"
    ) { expanded ->
        if (expanded) 180f else 0f
    }
    
    Box(
        modifier = modifier.width(64.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        // Render items in reverse order so z-index stacks correctly
        items.forEachIndexed { index, item ->
            val translateY by transition.animateFloat(
                transitionSpec = { 
                    spring(
                        dampingRatio = 0.7f,
                        stiffness = Spring.StiffnessLow
                    )
                },
                label = "TranslateY_$index"
            ) { expanded ->
                if (expanded) (index + 1) * 72f else 0f
            }
            
            val alpha by transition.animateFloat(
                transitionSpec = { tween(durationMillis = 300) },
                label = "Alpha_$index"
            ) { expanded ->
                if (expanded) 1f else 0f
            }
            
            val scale by transition.animateFloat(
                transitionSpec = { spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow) },
                label = "Scale_$index"
            ) { expanded ->
                if (expanded) 1f else 0.5f
            }

            if (alpha > 0f) {
                Surface(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(56.dp)
                        .graphicsLayer {
                            translationY = translateY * density
                            this.alpha = alpha
                            scaleX = scale
                            scaleY = scale
                        }
                        .zIndex(100f - index),
                    shape = CircleShape,
                    color = Color(0xFF1E1E1E).copy(alpha = 0.9f),
                    shadowElevation = 8.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = androidx.compose.material3.ripple(bounded = false)
                            ) { 
                                isExpanded = false
                                item.onClick() 
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = item.tint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
        
        // Trigger Button (Always on top)
        Surface(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(56.dp)
                .zIndex(200f),
            shape = CircleShape,
            color = if (isExpanded) Color(0xFFE91E63) else Color(0xFF1E1E1E).copy(alpha = 0.9f),
            shadowElevation = 8.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = androidx.compose.material3.ripple(bounded = false)
                    ) { isExpanded = !isExpanded },
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.graphicsLayer { rotationZ = rotation }) {
                    if (isExpanded) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Menu",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open Menu",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
