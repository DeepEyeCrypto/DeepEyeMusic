// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.player.recovery

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Policy defining which playback errors are recoverable, retry limits, and escalation logic.
 */
@Singleton
class StreamRecoveryPolicy @Inject constructor() {

    fun decide(error: PlaybackRecoveryError, currentAttemptCount: Int): RecoveryDecision {
        if (currentAttemptCount >= MAX_RECOVERY_ATTEMPTS) {
            return RecoveryDecision.DO_NOT_RETRY
        }

        return when (error) {
            is PlaybackRecoveryError.HttpStatus -> {
                when (error.statusCode) {
                    401, 403, 410 -> RecoveryDecision.REFRESH_SOURCE
                    in 500..599 -> RecoveryDecision.RETRY_ONCE
                    else -> RecoveryDecision.SHOW_ERROR
                }
            }
            is PlaybackRecoveryError.SourceLoad -> {
                RecoveryDecision.REFRESH_SOURCE
            }
            is PlaybackRecoveryError.Decoder -> {
                RecoveryDecision.FALLBACK_FORMAT
            }
            is PlaybackRecoveryError.Unknown -> {
                if (currentAttemptCount < 1) RecoveryDecision.RETRY_ONCE else RecoveryDecision.SHOW_ERROR
            }
        }
    }

    fun shouldRecover(error: PlaybackRecoveryError, currentAttemptCount: Int): Boolean {
        val decision = decide(error, currentAttemptCount)
        return decision == RecoveryDecision.REFRESH_SOURCE ||
               decision == RecoveryDecision.FALLBACK_FORMAT ||
               decision == RecoveryDecision.RETRY_ONCE
    }

    companion object {
        const val MAX_RECOVERY_ATTEMPTS = 1
        const val STABLE_PLAYBACK_RESET_MS = 60_000L
    }
}
