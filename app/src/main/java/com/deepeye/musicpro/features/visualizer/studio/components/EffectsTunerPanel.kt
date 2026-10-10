// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.features.visualizer.studio.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun EffectsTunerPanel(
    viewModel: com.deepeye.musicpro.features.visualizer.studio.VisualizerStudioViewModel,
    modifier: Modifier = Modifier
) {
    val rotation by viewModel.rotationSpeed.collectAsState()
    val baseGlow by viewModel.baseGlow.collectAsState()

    Column(modifier = modifier.padding(16.dp)) {
        Text("Effects Tuner", color = Color.White, style = MaterialTheme.typography.labelLarge)
        
        Spacer(modifier = Modifier.height(8.dp))
        Text("Rotation Speed", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
        Slider(
            value = rotation,
            onValueChange = { viewModel.setRotationSpeed(it) },
            valueRange = 0f..2f
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        Text("Base Glow", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
        Slider(
            value = baseGlow,
            onValueChange = { viewModel.setBaseGlow(it) },
            valueRange = 0f..1f
        )
    }
}
