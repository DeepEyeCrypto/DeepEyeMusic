// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.features.visualizer.studio.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualizerTopBar(onBackClick: () -> Unit) {
    TopAppBar(
        title = { Text("Studio Visuals", color = Color.White, style = MaterialTheme.typography.titleMedium) },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                // Using standard IconButton for back action
                Text("←", color = Color.White)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Black.copy(alpha = 0.6f)
        )
    )
}
