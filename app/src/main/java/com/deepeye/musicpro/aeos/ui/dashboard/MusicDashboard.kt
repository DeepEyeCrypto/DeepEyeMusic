package com.deepeye.musicpro.aeos.ui.dashboard

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deepeye.musicpro.aeos.control_plane.MusicControlPlane
import com.deepeye.musicpro.aeos.mesh.MusicIntent
import com.deepeye.musicpro.aeos.memory.CognitiveMemoryMesh

@Composable
fun MusicDashboard(
    controlPlane: MusicControlPlane = remember { MusicControlPlane() }
) {
    val uiState by controlPlane.uiState.collectAsStateWithLifecycle()
    val memoryState by CognitiveMemoryMesh.getInstance().state.collectAsStateWithLifecycle()

    // Stable lambdas for intent dispatch prevent recomposition leaks
    val onPlay = remember(controlPlane) { { controlPlane.dispatchIntent(MusicIntent.Play) } }
    val onPause = remember(controlPlane) { { controlPlane.dispatchIntent(MusicIntent.Pause) } }
    val onNext = remember(controlPlane) { { controlPlane.dispatchIntent(MusicIntent.NextTrack) } }
    val onChaos = remember(controlPlane) { { controlPlane.dispatchIntent(MusicIntent.SimulateChaos) } }

    DashboardContent(
        trackNameProvider = { uiState.currentTrack },
        isPlayingProvider = { uiState.isPlaying },
        isFailureProvider = { uiState.isSystemFailure },
        errorMessageProvider = { uiState.errorMessage ?: "CRITICAL FAILURE" },
        memoryStatusProvider = { memoryState.status },
        onPlay = onPlay,
        onPause = onPause,
        onNext = onNext,
        onChaos = onChaos
    )
}

@Composable
private fun DashboardContent(
    trackNameProvider: () -> String,
    isPlayingProvider: () -> Boolean,
    isFailureProvider: () -> Boolean,
    errorMessageProvider: () -> String,
    memoryStatusProvider: () -> String,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onNext: () -> Unit,
    onChaos: () -> Unit
) {
    Box(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)) {
        
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "AEOS Dashboard", 
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), 
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Isolated Control/Data Plane", 
                style = MaterialTheme.typography.bodyMedium, 
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Track Info with deferred reads
            TrackInfo(
                trackNameProvider = trackNameProvider,
                isPlayingProvider = isPlayingProvider
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Glassmorphism player controls with Chaos Button
            PlayerControls(onPlay = onPlay, onPause = onPause, onNext = onNext, onChaos = onChaos)
            
            Spacer(modifier = Modifier.height(64.dp))
            
            // Memory Mesh Logs
            MemoryMeshLog(memoryStatusProvider = memoryStatusProvider)
        }
        
        // Phase 4 Chaos Engineering Overlay
        RecoveryOverlay(
            isFailureProvider = isFailureProvider,
            errorMessageProvider = errorMessageProvider,
            onRecover = onPlay // Use Play intent as recovery mechanism
        )
    }
}

@Composable
private fun TrackInfo(
    trackNameProvider: () -> String,
    isPlayingProvider: () -> Boolean
) {
    val isPlaying = isPlayingProvider()
    Text(
        text = trackNameProvider(), 
        style = MaterialTheme.typography.titleLarge,
        color = Color.White
    )
    Text(
        text = if (isPlaying) "PLAYING" else "PAUSED",
        style = MaterialTheme.typography.labelLarge,
        color = if (isPlaying) MaterialTheme.colorScheme.primary else Color.Gray
    )
}

@Composable
private fun PlayerControls(
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onNext: () -> Unit,
    onChaos: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.extraLarge)
                .then(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(24.dp) else Modifier)
                .padding(vertical = 24.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onPlay,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface)
            ) { 
                Text("PLAY") 
            }
            Button(
                onClick = onPause,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface)
            ) { 
                Text("PAUSE") 
            }
            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface)
            ) { 
                Text("NEXT") 
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onChaos,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Simulate Core Failure")
        }
    }
}

@Composable
private fun MemoryMeshLog(memoryStatusProvider: () -> String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Cognitive Memory Logs:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = memoryStatusProvider(), style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
        }
    }
}

@Composable
private fun RecoveryOverlay(
    isFailureProvider: () -> Boolean,
    errorMessageProvider: () -> String,
    onRecover: () -> Unit
) {
    if (isFailureProvider()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .then(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(32.dp) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.padding(32.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("SYSTEM RECOVERY MODE", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(errorMessageProvider(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onRecover, 
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onErrorContainer, 
                            contentColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text("REBOOT & RECOVER")
                    }
                }
            }
        }
    }
}
