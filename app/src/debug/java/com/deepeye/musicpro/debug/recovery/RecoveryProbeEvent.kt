// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.debug.recovery

/**
 * Lifecycle stages for recovery transaction probe.
 * Debug-only instrumentation to track recovery from start to completion.
 */
enum class RecoveryProbeStage {
    IDLE,
    BASELINE_CAPTURED,
    EXPIRY_TRIGGERED,
    ERROR_CLASSIFIED,
    POLICY_APPROVED,
    RECOVERY_LOCK_ACQUIRED,
    SNAPSHOT_CAPTURED,
    SOURCE_REFRESH_STARTED,
    SOURCE_REFRESH_SUCCEEDED,
    SOURCE_REFRESH_FALLBACK,
    RESTORE_STARTED,
    PLAYER_READY,
    PLAYBACK_RESTORED,
    DSP_RESTORED,
    HQ_INVENTORY_RETAINED,
    QUEUE_RETAINED,
    MEDIA_SESSION_VALID,
    NOTIFICATION_VALID,
    COMPLETED,
    FAILED,
    CANCELLED
}

/**
 * Immutable event record for a recovery stage transition.
 * All sensitive data (URLs, tokens, IDs) is redacted.
 */
data class RecoveryProbeEvent(
    val runId: String,
    val sequenceNumber: Int,
    val timestampMs: Long,
    val stage: RecoveryProbeStage,
    val mediaIdHash: String?,
    val titleSafe: String?,
    val queueIndex: Int?,
    val queueSize: Int?,
    val positionMs: Long?,
    val playbackState: String?,
    val wasPlaying: Boolean?,
    val selectedVideoFormatIdSafe: String?,
    val selectedAudioFormatIdSafe: String?,
    val videoFormatCount: Int?,
    val audioFormatCount: Int?,
    val dspEnabled: Boolean?,
    val dspPresetSafe: String?,
    val recoveryAttempt: Int?,
    val recoveryLockHeld: Boolean?,
    val fallbackUsed: Boolean?,
    val fallbackReason: String?,
    val safeMessage: String?
) {
    fun toJsonLine(): String {
        return buildString {
            append("{")
            append("\"runId\":\"$runId\",")
            append("\"seq\":$sequenceNumber,")
            append("\"tsMs\":$timestampMs,")
            append("\"stage\":\"$stage\",")
            append("\"mediaIdHash\":${mediaIdHash?.let { "\"$it\"" } ?: "null"},")
            append("\"title\":${titleSafe?.let { "\"${escapeJson(it)}\"" } ?: "null"},")
            append("\"queueIdx\":${queueIndex ?: "null"},")
            append("\"queueSize\":${queueSize ?: "null"},")
            append("\"posMs\":${positionMs ?: "null"},")
            append("\"playState\":${playbackState?.let { "\"$it\"" } ?: "null"},")
            append("\"wasPlaying\":${wasPlaying ?: "null"},")
            append("\"vidFmtId\":${selectedVideoFormatIdSafe?.let { "\"$it\"" } ?: "null"},")
            append("\"audFmtId\":${selectedAudioFormatIdSafe?.let { "\"$it\"" } ?: "null"},")
            append("\"vidFmtCnt\":${videoFormatCount ?: "null"},")
            append("\"audFmtCnt\":${audioFormatCount ?: "null"},")
            append("\"dspEnabled\":${dspEnabled ?: "null"},")
            append("\"dspPreset\":${dspPresetSafe?.let { "\"${escapeJson(it)}\"" } ?: "null"},")
            append("\"attempt\":${recoveryAttempt ?: "null"},")
            append("\"lockHeld\":${recoveryLockHeld ?: "null"},")
            append("\"fallback\":${fallbackUsed ?: "null"},")
            append("\"fallbackReason\":${fallbackReason?.let { "\"${escapeJson(it)}\"" } ?: "null"},")
            append("\"msg\":${safeMessage?.let { "\"${escapeJson(it)}\"" } ?: "null"}")
            append("}")
        }
    }

    private fun escapeJson(s: String): String {
        return s.replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
