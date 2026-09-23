package com.deepeye.musicpro.ui.player.visualizer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.musicpro.ui.modifiers.preventParentScrollOnDrag
import com.deepeye.musicpro.ui.theme.ElectricViolet
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.Alignment
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualizerSettingsSheet(
    reducedMotion: Boolean,
    onReducedMotionChanged: (Boolean) -> Unit,
    visualizerIntensity: Float,
    onIntensityChanged: (Float) -> Unit,
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
                    text = "Visualizer Settings",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close Settings", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Intensity Slider
            Text("Intensity", color = Color.White, fontSize = 14.sp)
            Slider(
                value = visualizerIntensity,
                onValueChange = onIntensityChanged,
                valueRange = 0.5f..1.5f,
                colors = SliderDefaults.colors(
                    thumbColor = ElectricViolet,
                    activeTrackColor = ElectricViolet,
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Reduced Motion Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onReducedMotionChanged(!reducedMotion) }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Reduced Motion", color = Color.White, fontSize = 15.sp)
                    Text("Limit fast rotations and strobing", color = Color.White.copy(alpha=0.6f), fontSize = 12.sp)
                }
                Switch(
                    checked = reducedMotion,
                    onCheckedChange = onReducedMotionChanged,
                    colors = SwitchDefaults.colors(checkedThumbColor = ElectricViolet, checkedTrackColor = ElectricViolet.copy(alpha=0.5f))
                )
            }
        }
    }
}
