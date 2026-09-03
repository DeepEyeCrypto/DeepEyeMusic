// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.player.recovery

/**
 * Result data class describing the outcome of a stream recovery transaction.
 */
data class StreamRecoveryResult(
    val recovered: Boolean,
    val usedFallback: Boolean,
    val restoredPositionMs: Long?,
    val restoredPlaying: Boolean,
    val safeReason: String
)
