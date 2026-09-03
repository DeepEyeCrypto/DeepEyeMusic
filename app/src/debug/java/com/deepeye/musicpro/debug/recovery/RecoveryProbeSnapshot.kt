// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.debug.recovery

/**
 * Immutable pre/post snapshot captured by the recovery test probe.
 * All sensitive data (URLs, tokens, IDs) is redacted at capture time.
 * Uses the same safe-hash / safe-name conventions as [RecoveryProbeEvent].
 */
data class RecoveryProbeSnapshot(
    val mediaIdHash: String,
    val titleSafe: String? = null,
    val queueIndex: Int? = null,
    val queueSize: Int? = null,
    val positionMs: Long? = null,
    val playbackState: String? = null,
    val wasPlaying: Boolean? = null,
    val selectedVideoFormatIdSafe: String? = null,
    val selectedAudioFormatIdSafe: String? = null,
    val videoFormatCount: Int? = null,
    val audioFormatCount: Int? = null,
    val dspEnabled: Boolean? = null,
    val dspPresetSafe: String? = null,
    val recoveryAttempt: Int? = null,
    val recoveryLockHeld: Boolean? = null,
    val fallbackUsed: Boolean? = null,
    val fallbackReason: String? = null,
    val totalDurationMs: Long? = null,
    val bufferedDurationMs: Long? = null,
    val bufferedPercentage: Int? = null,
    val isVideo: Boolean? = null,
    val audioSessionId: Int? = null,
)
