// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.player.controller

import com.deepeye.musicpro.player.format.DeepEyeFormat
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * Pure functional policy for merging SmartTube inventory with Media3 runtime track information.
 *
 * Preserves rich SmartTube inventories from being overwritten by sparse progressive/muxed
 * Media3 tracks. Uses deterministic rules to decide when to adopt incoming inventory vs.
 * retaining the existing authoritative SmartTube catalog.
 *
 * This is a production rule derived from the proven stream-recovery behavior:
 * - SmartTube resolver is authoritative for available format choices
 * - Media3 runtime tracks describe current active playback
 * - Sparse updates must never erase richer inventories
 */
data class FormatInventoryMergeResult(
    val videoFormats: ImmutableList<DeepEyeFormat>,
    val audioFormats: ImmutableList<DeepEyeFormat>,
    val preserveExistingVideoInventory: Boolean,
    val preserveExistingAudioInventory: Boolean,
    val decisionReason: String,
    val existingRealVideo: Int,
    val incomingRealVideo: Int,
    val existingRealAudio: Int,
    val incomingRealAudio: Int
)

class FormatInventoryMergePolicy {

    /**
     * Merges existing (SmartTube) inventory with incoming (Media3 runtime) inventory.
     *
     * @param existingVideo current authoritative video format list
     * @param existingAudio current authoritative audio format list
     * @param incomingVideo formats reported by Media3 runtime tracks
     * @param incomingAudio formats reported by Media3 runtime tracks
     * @return merged inventory with decision reasoning
     */
    fun merge(
        existingVideo: ImmutableList<DeepEyeFormat>,
        existingAudio: ImmutableList<DeepEyeFormat>,
        incomingVideo: ImmutableList<DeepEyeFormat>,
        incomingAudio: ImmutableList<DeepEyeFormat>
    ): FormatInventoryMergeResult {
        val existingVideoRealCount = countRealFormats(existingVideo)
        val existingAudioRealCount = countRealFormats(existingAudio)
        val incomingVideoRealCount = countRealFormats(incomingVideo)
        val incomingAudioRealCount = countRealFormats(incomingAudio)

        val preserveVideo = existingVideoRealCount > 1 && incomingVideoRealCount <= 1
        val preserveAudio = existingAudioRealCount > 1 && incomingAudioRealCount <= 1

        val mergedVideo = if (preserveVideo) {
            existingVideo
        } else if (incomingVideoRealCount > 1) {
            incomingVideo
        } else {
            existingVideo
        }

        val mergedAudio = if (preserveAudio) {
            existingAudio
        } else if (incomingAudioRealCount > 1) {
            incomingAudio
        } else {
            existingAudio
        }

        val reason = buildDecisionReason(
            preserveVideo = preserveVideo,
            preserveAudio = preserveAudio,
            existingVideoRealCount = existingVideoRealCount,
            existingAudioRealCount = existingAudioRealCount,
            incomingVideoRealCount = incomingVideoRealCount,
            incomingAudioRealCount = incomingAudioRealCount
        )

        return FormatInventoryMergeResult(
            videoFormats = mergedVideo,
            audioFormats = mergedAudio,
            preserveExistingVideoInventory = preserveVideo,
            preserveExistingAudioInventory = preserveAudio,
            decisionReason = reason,
            existingRealVideo = existingVideoRealCount,
            incomingRealVideo = incomingVideoRealCount,
            existingRealAudio = existingAudioRealCount,
            incomingRealAudio = incomingAudioRealCount
        )
    }

    /**
     * Counts non-Auto formats in a list.
     */
    private fun countRealFormats(formats: ImmutableList<DeepEyeFormat>): Int =
        formats.count { !it.isAuto }

    /**
     * Builds a human-readable decision reason for logging and debugging.
     */
    private fun buildDecisionReason(
        preserveVideo: Boolean,
        preserveAudio: Boolean,
        existingVideoRealCount: Int,
        existingAudioRealCount: Int,
        incomingVideoRealCount: Int,
        incomingAudioRealCount: Int
    ): String {
        val parts = mutableListOf<String>()

        if (preserveVideo) {
            parts.add("preserve_rich_smarttube_video(existing=$existingVideoRealCount vs incoming=$incomingVideoRealCount)")
        } else if (incomingVideoRealCount > 1) {
            parts.add("adopt_richer_incoming_video(existing=$existingVideoRealCount vs incoming=$incomingVideoRealCount)")
        } else {
            parts.add("keep_existing_video_fallback(existing=$existingVideoRealCount vs incoming=$incomingVideoRealCount)")
        }

        if (preserveAudio) {
            parts.add("preserve_rich_smarttube_audio(existing=$existingAudioRealCount vs incoming=$incomingAudioRealCount)")
        } else if (incomingAudioRealCount > 1) {
            parts.add("adopt_richer_incoming_audio(existing=$existingAudioRealCount vs incoming=$incomingAudioRealCount)")
        } else {
            parts.add("keep_existing_audio_fallback(existing=$existingAudioRealCount vs incoming=$incomingAudioRealCount)")
        }

        return parts.joinToString(" | ")
    }
}
