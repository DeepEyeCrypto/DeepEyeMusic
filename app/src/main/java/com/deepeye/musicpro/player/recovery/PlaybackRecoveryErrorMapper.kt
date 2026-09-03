// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.player.recovery

import androidx.media3.common.PlaybackException
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.mediacodec.MediaCodecRenderer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sanitized error mapper that extracts structured recovery errors from Media3 exceptions
 * without leaking request URLs, query parameters, tokens, or auth headers.
 */
@Singleton
class PlaybackRecoveryErrorMapper @Inject constructor() {

    fun map(error: PlaybackException): PlaybackRecoveryError {
        // 1. Check for HttpDataSource.InvalidResponseCodeException in cause chain
        var currentCause: Throwable? = error
        while (currentCause != null) {
            if (currentCause is HttpDataSource.InvalidResponseCodeException) {
                val statusCode = currentCause.responseCode
                val safeReason = "HTTP $statusCode response"
                return PlaybackRecoveryError.HttpStatus(statusCode, safeReason)
            }
            if (currentCause is MediaCodecRenderer.DecoderInitializationException) {
                val codecName = currentCause.codecInfo?.name ?: "unknown"
                val safeReason = "Decoder init failure: $codecName"
                return PlaybackRecoveryError.Decoder(safeReason)
            }
            currentCause = currentCause.cause
        }

        // 2. Inspect error message & error code for status patterns
        val msg = error.message ?: ""
        val statusFromMsg = extractStatusCodeFromMessage(msg)
        if (statusFromMsg != null) {
            return PlaybackRecoveryError.HttpStatus(statusFromMsg, "HTTP $statusFromMsg parsed from error")
        }

        if (error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS) {
            return PlaybackRecoveryError.HttpStatus(403, "Bad HTTP status code")
        }

        if (error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ||
            error.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED
        ) {
            return PlaybackRecoveryError.Decoder("ExoPlayer decoding error: ${error.errorCodeName}")
        }

        if (error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
            error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ||
            error.errorCode == PlaybackException.ERROR_CODE_IO_UNSPECIFIED ||
            error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED
        ) {
            return PlaybackRecoveryError.SourceLoad("Source loading error: ${error.errorCodeName}")
        }

        return PlaybackRecoveryError.Unknown("Generic playback error: ${error.errorCodeName}")
    }

    private fun extractStatusCodeFromMessage(msg: String): Int? {
        val lower = msg.lowercase()
        return when {
            lower.contains("403") || lower.contains("forbidden") -> 403
            lower.contains("401") || lower.contains("unauthorized") -> 401
            lower.contains("410") || lower.contains("gone") -> 410
            lower.contains("503") || lower.contains("unavailable") -> 503
            lower.contains("500") -> 500
            else -> null
        }
    }
}
