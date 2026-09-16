// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.player.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.musicpro.core.utils.TimeFormatter
import com.deepeye.musicpro.domain.model.PlayerState
import com.deepeye.musicpro.player.format.DeepEyeFormat
import com.deepeye.musicpro.player.format.QualityPreset
import com.deepeye.musicpro.ui.theme.NeonCyan
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.shape.RoundedCornerShape

private val HDR_PURPLE = Color(0xFF7B2CBF)
private val BRAND_RED = Color(0xFFD42B2B)

@Composable
private fun OverlayPreviewBackground(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(
        Brush.verticalGradient(listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F3460)))
    ))
}

@Composable
private fun OverlayPreviewControls(
    playerState: PlayerState,
    actions: VideoPlayerOverlayActions
) {
    OverlayPreviewBackground()
    val title = playerState.currentSong?.title ?: playerState.currentItem?.title ?: ""
    val selectedFormat = playerState.selectedVideoFormat
    val qualityText = selectedFormat?.formattedVideoResolution
        ?: if (playerState.isVideo) QualityPreset.AUTO.displayName.split("(").first().trim()
        else ""
    val is4K = selectedFormat?.height ?: 0 >= 2160
    val isHDR = selectedFormat?.isHdr ?: false

    Column(modifier = Modifier.fillMaxSize()) {
        // Top row
        Row(Modifier.fillMaxWidth().padding(top = 16.dp, start = 12.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(24.dp))
            Text(text = title, style = MaterialTheme.typography.labelLarge, color = Color.White,
                fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f))
            if (is4K) {
                Box(modifier = Modifier.background(BRAND_RED, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text(text = "4K", style = MaterialTheme.typography.labelSmall, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (isHDR) {
                Box(modifier = Modifier.background(HDR_PURPLE, RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                    Text(text = "HDR", style = MaterialTheme.typography.labelSmall, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (qualityText.isNotEmpty() && !is4K) {
                Box(modifier = Modifier.background(Color(0xFF7B2CBF).copy(alpha = 0.7f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text(text = qualityText.uppercase(), style = MaterialTheme.typography.labelSmall, color = Color.White, fontSize = 10.sp)
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Time row
        Row(Modifier.padding(horizontal = 16.dp)) {
            Text(text = TimeFormatter.formatDuration(playerState.position),
                style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.85f), fontSize = 11.sp, modifier = Modifier.width(44.dp))
            Box(Modifier.weight(1f).height(8.dp)) { /* Canvas placeholder */ }
            Text(text = TimeFormatter.formatDuration(playerState.duration),
                style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.85f), fontSize = 11.sp,
                modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
        }

        Spacer(Modifier.height(4.dp))

        // Bottom action bar
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Icon(imageVector = Icons.Default.HighQuality, contentDescription = "HQ", tint = Color.White, modifier = Modifier.size(24.dp))
            Icon(imageVector = Icons.Default.Equalizer, contentDescription = "Audio", tint = Color.White, modifier = Modifier.size(24.dp))
            Icon(imageVector = if (playerState.isLiked) Icons.Default.ThumbUp else Icons.Default.ThumbUp, contentDescription = "Like", tint = Color.White, modifier = Modifier.size(24.dp))
            Icon(imageVector = if (playerState.isDisliked) Icons.Default.ThumbDown else Icons.Default.ThumbDown, contentDescription = "Dislike", tint = Color.White, modifier = Modifier.size(24.dp))
            Icon(imageVector = Icons.Default.ClosedCaption, contentDescription = "CC", tint = Color.White, modifier = Modifier.size(24.dp))
            Icon(imageVector = Icons.Default.PlaylistAdd, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(24.dp))
            Icon(imageVector = Icons.Default.Info, contentDescription = "Info", tint = Color.White, modifier = Modifier.size(24.dp))
            Icon(imageVector = Icons.Default.Analytics, contentDescription = "Stats", tint = NeonCyan, modifier = Modifier.size(24.dp))
        }
    }
}

private fun make4KState(): PlayerState = PlayerState(
    isPlaying = true, position = 120000L, duration = 300000L,
    isVideo = true, isLoading = false, isLiked = false, isDisliked = false,
    isCaptionEnabled = false, selectedVideoFormat = DeepEyeFormat(
        id = "v_4k_0", groupIndex = 0, trackIndex = 0, type = com.deepeye.musicpro.player.format.FormatType.VIDEO,
        mimeType = "video/mp4", codecName = "H264", rawCodecs = "avc1",
        width = 3840, height = 2160, frameRate = 60f, bitrate = 30_000_000,
        qualityLabel = "2160p", isHdr = true, isHardwareAccelerated = true,
        isSupported = true, isSelected = true
    )
)

private fun make1080pState(): PlayerState = PlayerState(
    isPlaying = true, position = 60000L, duration = 300000L,
    isVideo = true, isLoading = false, isLiked = true, isDisliked = false,
    isCaptionEnabled = false, selectedVideoFormat = DeepEyeFormat(
        id = "v_1080_0", groupIndex = 0, trackIndex = 0, type = com.deepeye.musicpro.player.format.FormatType.VIDEO,
        mimeType = "video/mp4", codecName = "H264", rawCodecs = "avc1",
        width = 1920, height = 1080, frameRate = 30f, bitrate = 8_000_000,
        qualityLabel = "1080p", isHdr = false, isHardwareAccelerated = true,
        isSupported = true, isSelected = true
    )
)

private fun makeLoadingState(): PlayerState = PlayerState(
    isPlaying = false, position = 0L, duration = 0L,
    isVideo = true, isLoading = true
)

private fun makePausedState(): PlayerState = PlayerState(
    isPlaying = false, position = 150000L, duration = 300000L,
    isVideo = true, isLoading = false
)

private fun make4KHDRState(): PlayerState = PlayerState(
    isPlaying = true, position = 45000L, duration = 600000L,
    isVideo = true, isLoading = false, isLiked = false, isDisliked = false,
    isCaptionEnabled = true, selectedVideoFormat = DeepEyeFormat(
        id = "v_4k_hdr_0", groupIndex = 0, trackIndex = 0, type = com.deepeye.musicpro.player.format.FormatType.VIDEO,
        mimeType = "video/mp4", codecName = "HEVC", rawCodecs = "hev1",
        width = 3840, height = 2160, frameRate = 60f, bitrate = 45_000_000,
        qualityLabel = "2160p60", isHdr = true, isHardwareAccelerated = true,
        isSupported = true, isSelected = true
    )
)

@androidx.compose.ui.tooling.preview.Preview(name = "DeepEye Overlay 4K HDR")
@Composable
fun PreviewOverlay4KHDR() {
    MaterialTheme { OverlayPreviewControls(make4KHDRState(), VideoPlayerOverlayActions.fromLambdas()) }
}

@androidx.compose.ui.tooling.preview.Preview(name = "DeepEye Overlay 4K")
@Composable
fun PreviewOverlay4K() {
    MaterialTheme { OverlayPreviewControls(make4KState(), VideoPlayerOverlayActions.fromLambdas()) }
}

@androidx.compose.ui.tooling.preview.Preview(name = "DeepEye Overlay 1080p")
@Composable
fun PreviewOverlay1080p() {
    MaterialTheme { OverlayPreviewControls(make1080pState(), VideoPlayerOverlayActions.fromLambdas()) }
}

@androidx.compose.ui.tooling.preview.Preview(name = "DeepEye Overlay Loading")
@Composable
fun PreviewOverlayLoading() {
    MaterialTheme { OverlayPreviewControls(makeLoadingState(), VideoPlayerOverlayActions.fromLambdas()) }
}

@androidx.compose.ui.tooling.preview.Preview(name = "DeepEye Overlay Paused")
@Composable
fun PreviewOverlayPaused() {
    MaterialTheme { OverlayPreviewControls(makePausedState(), VideoPlayerOverlayActions.fromLambdas()) }
}
