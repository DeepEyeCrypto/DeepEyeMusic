// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.player.recovery

/**
 * Typed domain model representing recoverable and non-recoverable playback errors.
 */
sealed interface PlaybackRecoveryError {
    data class HttpStatus(
        val statusCode: Int,
        val safeReason: String? = null
    ) : PlaybackRecoveryError

    data class SourceLoad(
        val safeReason: String? = null
    ) : PlaybackRecoveryError

    data class Decoder(
        val safeReason: String? = null
    ) : PlaybackRecoveryError

    data class Unknown(
        val safeReason: String? = null
    ) : PlaybackRecoveryError
}

enum class RecoveryDecision {
    REFRESH_SOURCE,
    FALLBACK_FORMAT,
    RETRY_ONCE,
    DO_NOT_RETRY,
    SHOW_ERROR
}

enum class RecoveryTrigger {
    PLAYER_ERROR,
    SOURCE_LOAD_ERROR,
    SIMULATED_TEST,
    MANUAL_REFRESH
}
