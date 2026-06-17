package com.deepeye.musicpro.ui.update

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.deepeye.musicpro.data.source.remote.update.UpdateState
import kotlin.math.roundToInt

private val BrandTeal = Color(0xFF00F2FE)
private val BrandPurple = Color(0xFF4FACFE)
private val SurfaceDark = Color(0xFF121218)
private val SurfaceCard = Color(0xFF1A1A24)
private val TextPrimary = Color(0xFFE8E8E8)
private val TextSecondary = Color(0xFFB0B0B0)

@Composable
fun UpdateDialog(
    state: UpdateState,
    onDownloadClick: (url: String, version: String) -> Unit,
    onInstallClick: (java.io.File) -> Unit,
    onDismissClick: () -> Unit
) {
    if (state is UpdateState.Idle || state is UpdateState.UpToDate || state is UpdateState.Checking) {
        return
    }

    Dialog(
        onDismissRequest = onDismissClick,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(SurfaceCard, SurfaceDark)
                    )
                )
                .border(
                    1.dp,
                    Brush.linearGradient(
                        listOf(BrandTeal.copy(alpha = 0.3f), BrandPurple.copy(alpha = 0.1f))
                    ),
                    RoundedCornerShape(28.dp)
                )
        ) {
            // Background Glow
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                BrandTeal.copy(alpha = 0.15f),
                                Color.Transparent
                            ),
                            radius = 400f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(BrandTeal.copy(alpha = 0.2f), BrandPurple.copy(alpha = 0.2f))
                            )
                        )
                        .border(
                            1.dp,
                            Brush.linearGradient(
                                listOf(BrandTeal, BrandPurple)
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 0.9f,
                        targetValue = 1.1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "scale"
                    )
                    
                    Icon(
                        imageVector = if (state is UpdateState.Downloaded) Icons.Rounded.SystemUpdate else Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = BrandTeal,
                        modifier = Modifier
                            .size(36.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Title
                Text(
                    text = when (state) {
                        is UpdateState.UpdateAvailable -> "New Update Available!"
                        is UpdateState.Downloading -> "Downloading Update..."
                        is UpdateState.Downloaded -> "Ready to Install"
                        else -> "Update"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                // Version string
                if (state is UpdateState.UpdateAvailable) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Version ${state.version}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = BrandPurple,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Content (Release Notes / Progress)
                when (state) {
                    is UpdateState.UpdateAvailable -> {
                        if (state.releaseNotes.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 120.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.2f))
                                    .padding(12.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = state.releaseNotes,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2f
                                )
                            }
                        } else {
                            Text(
                                text = "Experience the latest features, bug fixes, and performance improvements.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    is UpdateState.Downloading -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${state.progress}%",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = BrandTeal
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { state.progress / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = BrandTeal,
                                trackColor = Color.White.copy(alpha = 0.1f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Please wait while we fetch the latest version...",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF808080)
                            )
                        }
                    }
                    is UpdateState.Downloaded -> {
                        Text(
                            text = "The update has been downloaded successfully. Install it now to enjoy the latest features!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                    else -> {}
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (state !is UpdateState.Downloading) {
                        OutlinedButton(
                            onClick = onDismissClick,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = TextSecondary
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                        ) {
                            Text("Later", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    when (state) {
                        is UpdateState.UpdateAvailable -> {
                            Button(
                                onClick = { onDownloadClick(state.apkUrl, state.version) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BrandTeal,
                                    contentColor = Color.Black
                                )
                            ) {
                                Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Update", fontWeight = FontWeight.Bold)
                            }
                        }
                        is UpdateState.Downloading -> {
                            // Only cancel button or downloading text
                            Button(
                                onClick = { },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BrandTeal.copy(alpha = 0.5f),
                                    contentColor = Color.Black
                                ),
                                enabled = false
                            ) {
                                Text("Downloading...", fontWeight = FontWeight.Bold)
                            }
                        }
                        is UpdateState.Downloaded -> {
                            Button(
                                onClick = { onInstallClick(state.file) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BrandTeal,
                                    contentColor = Color.Black
                                )
                            ) {
                                Icon(Icons.Rounded.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Install", fontWeight = FontWeight.Bold)
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}
