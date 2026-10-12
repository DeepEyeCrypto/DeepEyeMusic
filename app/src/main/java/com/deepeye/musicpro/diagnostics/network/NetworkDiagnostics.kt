// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.diagnostics.network

/**
 * Coarse classification of a network endpoint. Only the category — never the
 * full URL, browseId, playlistId or query — is ever logged.
 */
enum class EndpointCategory {
    BROWSE,
    NEXT,
    SEARCH,
    OAUTH,
    OTHER
}

/**
 * Coarse authorization mode, derived from header *presence and scheme only*.
 * The token value is never read or logged.
 */
enum class AuthMode {
    NONE,
    OAUTH,
    SESSION,
    UNKNOWN
}

/**
 * Whether a request referenced a radio playlist, a user playlist, or none.
 * The playlist ID itself is never logged.
 */
enum class PlaylistIdType {
    RADIO,
    USER_PLAYLIST,
    NONE,
    OTHER
}

/**
 * Known, allow-listed InnerTube client identities. These are public client
 * names, not user secrets, and are safe to log as enums. Any unrecognized
 * client maps to [OTHER] so we never surface a forged or unexpected identity.
 */
enum class KnownClientName {
    TVHTML5,
    WEB,
    ANDROID_MUSIC,
    OTHER
}

/**
 * Reason a request fell back to a degraded (e.g. guest) path. Drives both
 * telemetry and the user-facing "account data unavailable" state machine.
 */
enum class FallbackReason {
    NONE,
    AUTH_MISSING,
    TOKEN_EXPIRED,
    UNAUTHORIZED,
    EMPTY_RESULT,
    PARSE_ERROR,
    NETWORK_ERROR,
    UNSUPPORTED_ENDPOINT
}

/**
 * A fully-redacted network diagnostic record. Every field is a non-sensitive
 * signal: counts, enums, status codes, booleans, or an internally generated
 * correlation ID. No header values, tokens, cookies, bodies, titles, channel
 * or account identifiers are ever stored on this type.
 */
data class NetworkEvent(
    val correlationId: String,
    val endpointCategory: EndpointCategory,
    val hostExpected: Boolean,
    val method: String,
    val statusCode: Int,
    val elapsedMs: Long,
    val requestBodyBytes: Long,
    val responseBodyBytes: Long,
    val authMode: AuthMode,
    val authHeaderPresent: Boolean,
    val cookieHeaderPresent: Boolean,
    val clientName: KnownClientName,
    val clientVersionPresent: Boolean,
    val playlistIdType: PlaylistIdType,
    val parserResultCount: Int,
    val fallbackReason: FallbackReason,
    val retryCount: Int
) {
    /** Formats a single stable, redacted key=value log line. */
    fun toRedactedLine(): String = buildString {
        append("correlationId=").append(correlationId)
        append(" endpoint=").append(endpointCategory)
        append(" hostExpected=").append(hostExpected)
        append(" method=").append(method)
        append(" status=").append(statusCode)
        append(" durationMs=").append(elapsedMs)
        append(" reqBodyBytes=").append(requestBodyBytes)
        append(" respBodyBytes=").append(responseBodyBytes)
        append(" authMode=").append(authMode)
        append(" authHeaderPresent=").append(authHeaderPresent)
        append(" cookieHeaderPresent=").append(cookieHeaderPresent)
        append(" client=").append(clientName)
        append(" clientVersionPresent=").append(clientVersionPresent)
        append(" playlistIdType=").append(playlistIdType)
        append(" parsedItems=").append(parserResultCount)
        append(" fallback=").append(fallbackReason)
        append(" retries=").append(retryCount)
    }
}

/**
 * Global switch for verbose network diagnostics.
 *
 * MUST remain `false` in release builds. Diagnostic builds flip it explicitly
 * at startup (see [NetworkDiagnostics.enableForDiagnostics]); it is never
 * derived from user data and defaults to off. When disabled, every
 * [NetworkLogRedactor.logNetworkEvent] call is a no-op.
 */
object NetworkDiagnostics {
    @Volatile
    var isEnabled: Boolean = false
        private set

    private val counter = java.util.concurrent.atomic.AtomicLong(0)

    /** Enables verbose network diagnostics. Call only from diagnostic builds. */
    fun enableForDiagnostics() {
        isEnabled = true
    }

    /**
     * Generates the next correlation ID. Deliberately a local counter + random
     * suffix; it carries no account information and is safe to log.
     */
    fun nextCorrelationId(): String {
        val n = counter.incrementAndGet()
        val r = java.util.UUID.randomUUID().toString().take(8)
        return "net-$n-$r"
    }
}
