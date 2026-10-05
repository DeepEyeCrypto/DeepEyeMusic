package com.deepeye.musicpro.ui.player.visualizer

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.musicpro.ui.theme.ElectricViolet
import com.deepeye.musicpro.ui.theme.NeonCyan
import com.deepeye.musicpro.ui.util.minTouchTarget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualizerLibraryScreen(
    currentSceneId: VisualizerSceneId,
    onSceneSelected: (VisualizerSceneId) -> Unit,
    reducedMotion: Boolean = false,
    onReducedMotionChanged: ((Boolean) -> Unit)? = null,
    visualizerIntensity: Float = 1f,
    onIntensityChanged: ((Float) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    var selectedCategory by remember { mutableStateOf("All") }
    var showSettings by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // ── Header Row ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Visualizer Scenes",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onIntensityChanged != null || onReducedMotionChanged != null) {
                    IconButton(
                        onClick = { showSettings = !showSettings },
                        modifier = Modifier
                            .size(36.dp)
                            .minTouchTarget()
                    ) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = "Visualizer Settings",
                            tint = if (showSettings) NeonCyan else Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .minTouchTarget()
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ── Optional Collapsible Settings Bar ─────────────────────────────────
        if (showSettings && (onIntensityChanged != null || onReducedMotionChanged != null)) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0x33FFFFFF),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    if (onIntensityChanged != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Intensity", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("${(visualizerIntensity * 100).toInt()}%", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = visualizerIntensity,
                            onValueChange = onIntensityChanged,
                            valueRange = 0.5f..1.5f,
                            colors = SliderDefaults.colors(
                                thumbColor = NeonCyan,
                                activeTrackColor = NeonCyan,
                                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (onReducedMotionChanged != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onReducedMotionChanged(!reducedMotion) }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Reduced Motion", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Limit fast strobing", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                            }
                            Switch(
                                checked = reducedMotion,
                                onCheckedChange = onReducedMotionChanged,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = NeonCyan,
                                    checkedTrackColor = NeonCyan.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }
            }
        }

        // ── Category Filter Chips ─────────────────────────────────────────────
        val categories = VisualizerCategories
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                val isCatSelected = selectedCategory == cat
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isCatSelected) NeonCyan.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
                    border = BorderStroke(
                        1.dp,
                        if (isCatSelected) NeonCyan else Color.White.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier
                        .minTouchTarget()
                        .clickable { selectedCategory = cat }
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cat,
                            color = if (isCatSelected) NeonCyan else Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        val filteredScenes = AvailableVisualizerScenes.filter {
            selectedCategory == "All" || it.tags.contains(selectedCategory)
        }

        // ── Adaptive Responsive Scene Grid (Touch Optimized) ────────────────
        val gridMinSize = if (isLandscape) 150.dp else 140.dp
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = gridMinSize),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = if (isLandscape) 200.dp else 420.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredScenes, key = { it.id.name }) { scene ->
                val isSelected = scene.id == currentSceneId
                val isAgslShader = scene.title.contains("AGSL")
                val borderGlow by animateColorAsState(
                    targetValue = if (isSelected) NeonCyan else Color.White.copy(alpha = 0.12f),
                    animationSpec = tween(200),
                    label = "borderGlow"
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) Color(0xFF162030) else Color(0xFF11141D),
                    border = BorderStroke(if (isSelected) 1.8.dp else 1.dp, borderGlow),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(108.dp)
                        .minTouchTarget()
                        .clickable { onSceneSelected(scene.id) }
                ) {
                    Column(
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isAgslShader) Icons.Default.Speed else Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = if (isSelected) NeonCyan else Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                            if (isSelected) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = NeonCyan.copy(alpha = 0.25f),
                                    border = BorderStroke(0.8.dp, NeonCyan)
                                ) {
                                    Text(
                                        text = "ACTIVE",
                                        color = NeonCyan,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            } else if (isAgslShader) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = ElectricViolet.copy(alpha = 0.25f),
                                    border = BorderStroke(0.8.dp, ElectricViolet.copy(alpha = 0.6f))
                                ) {
                                    Text(
                                        text = "GPU SHADER",
                                        color = Color(0xFFCE93D8),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Column {
                            Text(
                                text = scene.title.replace(" (AGSL)", ""),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = scene.description,
                                color = Color.White.copy(alpha = 0.60f),
                                fontSize = 10.sp,
                                maxLines = 2,
                                lineHeight = 12.sp,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
