// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.diagnostics.network

import android.util.Log
import okhttp3.Request
import okhttp3.Response

/**
 * NetworkLogRedactor — release-safe redaction for diagnostic network logging.
 *
 * OWASP and this app's security policy treat tokens, cookies, session
 * identifiers and raw account traffic as sensitive. These helpers let us emit
 * only non-sensitive diagnostic signals (booleans, counts, enums, status
 * codes) while guaranteeing that known secret-shaped values and header names
 * never reach logcat.
 *
 * All functions here are pure (except [logNetworkEvent], which emits a single
 * already-redacted line) so they are trivially unit-testable without Android.
 */
object NetworkLogRedactor {

    /** Canonical log tag for every redacted network diagnostic line. */
    const val TAG = "NetDiag"

    // ── Secret-value redaction ──────────────────────────────────────────────
    // Replace the *value* of any known-sensitive key=value or "key": "value"
    // pair with a fixed marker. Never the reverse: we always drop the secret,
    // we never transform it into something that still contains the secret.
    // Dedicated scheme-prefix pattern: "Bearer <token>" has no :/= delimiter,
    // so it is redacted independently of the key/value pattern below.
    private val BEARER_SCHEME_REGEX = Regex("(?i)\\bBearer\\s+\\S+")

    private val SECRET_KEY_VALUE_REGEX = Regex(
        "(?i)\\b(Authorization|Cookie|Set-Cookie|refresh_token|access_token|" +
            "id_token|SAPISID|__Secure-1PAPISID|__Secure-3PAPISID|HSID|SSID|SID|" +
            "APISID|SAPISIDHASH|__Secure-1PAPISIDHASH|__Secure-3PAPISIDHASH|" +
            "SIDCC|__Secure-3PSID|LOGIN_INFO|VISITOR_INFO1_LIVE)\\b" +
            "(\\s*\"?\\s*[:=]\\s*)" +
            "(\"[^\"]*\"|[^\\s,;\"}]+)"
    )

    /**
     * Strips the value of any known-sensitive key/value pair from [input],
     * leaving the key name so the diagnostic remains legible.
     *
     * Example: `"refresh_token": "1//0gXYZ..."` -> `"refresh_token": "<REDACTED>"`.
     */
    fun redact(input: String?): String {
        if (input.isNullOrEmpty()) return ""
        var out = BEARER_SCHEME_REGEX.replace(input) { "Bearer <REDACTED>" }
        out = SECRET_KEY_VALUE_REGEX.replace(out) { m ->
            "${m.groupValues[1]}${m.groupValues[2]}<REDACTED>"
        }
        return out
    }

    /**
     * Returns only the *names* of headers present on [request], never their
     * values. Used for low-level debugging where even presence is useful.
     */
    fun headerNames(request: Request): String =
        request.headers.names().sorted().joinToString(",")

    // ── Endpoint classification ─────────────────────────────────────────────
    /**
     * Classifies a request path into a coarse [EndpointCategory]. Only the
     * category (never the full URL, browseId, or playlistId) is ever logged.
     */
    fun endpointCategory(url: String): EndpointCategory {
        val lower = url.lowercase()
        return when {
            lower.contains("/youtubei/v1/browse") -> EndpointCategory.BROWSE
            lower.contains("/youtubei/v1/next") -> EndpointCategory.NEXT
            lower.contains("/youtubei/v1/search") -> EndpointCategory.SEARCH
            lower.contains("/youtubei/v1/") -> EndpointCategory.OTHER
            lower.contains("oauth2.googleapis.com") -> EndpointCategory.OAUTH
            else -> EndpointCategory.OTHER
        }
    }

    /**
     * Reports only whether the host is the expected youtubei host. The actual
     * host string is not logged so we never fingerprint exact infra.
     */
    fun hostMatchesExpected(url: String): Boolean =
        url.startsWith("https://www.youtube.com") ||
            url.startsWith("https://music.youtube.com")

    // ── Presence-only signals ───────────────────────────────────────────────
    /** True if an Authorization header exists (value never read/logged). */
    fun hasAuthHeader(request: Request): Boolean = request.header("Authorization") != null

    /** True if any Cookie header exists (value never read/logged). */
    fun hasCookieHeader(request: Request): Boolean = request.header("Cookie") != null

    /**
     * Maps an authorization header to a coarse [AuthMode] without reading the
     * token. A Bearer token maps to [AuthMode.OAUTH]; anything else that is
     * present maps to [AuthMode.UNKNOWN]; absent maps to [AuthMode.NONE].
     */
    fun authMode(request: Request): AuthMode {
        val auth = request.header("Authorization") ?: return AuthMode.NONE
        return if (auth.startsWith("Bearer ", ignoreCase = true)) AuthMode.OAUTH else AuthMode.UNKNOWN
    }

    /** True if the X-YouTube-Client-Version header is present (value not logged). */
    fun hasClientVersion(request: Request): Boolean =
        request.header("X-YouTube-Client-Version") != null

    /** True if the request body carries a non-zero payload (size only). */
    fun hasBody(request: Request): Boolean =
        (request.body?.contentLength() ?: 0L) > 0L

    /**
     * Emits a single fully-redacted network diagnostic line. Only the fields on
     * [NetworkEvent] (all non-sensitive) are printed; the raw header values,
     * bodies, tokens and identifiers are never included.
     *
     * This is the ONLY network logging entry point that should ever be called
     * from production code paths. Verbose logging is gated by
     * [NetworkDiagnostics.isEnabled], which callers must set explicitly and
     * which is always false in release builds.
     */
    fun logNetworkEvent(event: NetworkEvent) {
        if (!NetworkDiagnostics.isEnabled) return
        Log.i(TAG, event.toRedactedLine())
    }

    /**
     * Convenience for building a [NetworkEvent] from an in-flight request plus
     * its response, so callers never have to touch sensitive header values.
     */
    fun buildEvent(
        request: Request,
        response: Response?,
        elapsedMs: Long,
        retryCount: Int,
        fallbackReason: FallbackReason,
        parserResultCount: Int,
        playlistIdType: PlaylistIdType,
        clientName: KnownClientName,
        responseBodyBytes: Long
    ): NetworkEvent = NetworkEvent(
        correlationId = NetworkDiagnostics.nextCorrelationId(),
        endpointCategory = endpointCategory(request.url.toString()),
        hostExpected = hostMatchesExpected(request.url.toString()),
        method = request.method,
        statusCode = response?.code ?: -1,
        elapsedMs = elapsedMs,
        requestBodyBytes = request.body?.contentLength() ?: -1L,
        responseBodyBytes = responseBodyBytes,
        authMode = authMode(request),
        authHeaderPresent = hasAuthHeader(request),
        cookieHeaderPresent = hasCookieHeader(request),
        clientName = clientName,
        clientVersionPresent = hasClientVersion(request),
        playlistIdType = playlistIdType,
        parserResultCount = parserResultCount,
        fallbackReason = fallbackReason,
        retryCount = retryCount
    )
}
