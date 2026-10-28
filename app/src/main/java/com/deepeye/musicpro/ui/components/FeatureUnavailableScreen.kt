package com.deepeye.musicpro.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Typed unavailable state for features whose backing service was removed
 * (e.g. Firebase-backed Chat/Ranking). Renders an explicit, user-visible
 * "feature disabled" state instead of silently breaking or faking data.
 */
@androidx.compose.runtime.Composable
fun FeatureUnavailableScreen(
    title: String,
    detail: String,
    icon: ImageVector = Icons.Default.Block,
    onBack: (() -> Unit)? = null,
    tint: Color = Color(0xFF00E5FF),
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = detail,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
        if (onBack != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Back",
                style = MaterialTheme.typography.titleSmall,
                color = tint,
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 10.dp)
            )
        }
    }
}