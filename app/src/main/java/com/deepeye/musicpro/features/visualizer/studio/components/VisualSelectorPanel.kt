// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.features.visualizer.studio.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.deepeye.musicpro.features.visualizer.studio.VisualizerStudioViewModel
import com.deepeye.musicpro.ui.player.visualizer.VisualizerSceneId

@Composable
fun VisualSelectorPanel(
    viewModel: com.deepeye.musicpro.features.visualizer.studio.VisualizerStudioViewModel,
    modifier: Modifier = Modifier
) {
    // Basic scene selector implementing a simple list for agora interaction
    Column(modifier = modifier.padding(16.dp)) {
        Text("Scenes", color = Color.White, style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        VisualizerSceneId.entries.forEach { scene ->
            Button(
                onClick = { viewModel.setActiveScene(scene) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
            ) {
                Text(scene.name)
            }
        }
    }
}
