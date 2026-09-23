package com.deepeye.musicpro.ui.player.visualizer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.musicpro.ui.modifiers.preventParentScrollOnDrag
import com.deepeye.musicpro.ui.theme.ElectricViolet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualizerLibraryScreen(
    currentSceneId: VisualizerSceneId,
    onSceneSelected: (VisualizerSceneId) -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 240.dp)
            .preventParentScrollOnDrag(),
        color = Color(0xDD090B10), // Translucent dark
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Visualizer Library",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close Library", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scene library rail
            val categories = listOf("All", "Geometric", "Spectrum", "Ambient")
            var selectedCategory by remember { mutableStateOf("All") }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 12.dp)
            ) {
                categories.forEach { cat ->
                    val isCatSelected = selectedCategory == cat
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isCatSelected) ElectricViolet else Color.White.copy(alpha = 0.1f))
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = cat,
                            color = if (isCatSelected) Color.White else Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            val filteredScenes = AvailableVisualizerScenes.filter {
                selectedCategory == "All" || it.tags.contains(selectedCategory)
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filteredScenes) { scene ->
                    val isSelected = scene.id == currentSceneId
                    Box(
                        modifier = Modifier
                            .width(160.dp)
                            .height(100.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF151820))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) ElectricViolet else Color(0x33FFFFFF),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onSceneSelected(scene.id) }
                            .padding(12.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = scene.title,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = scene.description,
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                maxLines = 2,
                                lineHeight = 14.sp
                            )
                            if (isSelected) {
                                Text(
                                    text = "ACTIVE",
                                    color = ElectricViolet,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
