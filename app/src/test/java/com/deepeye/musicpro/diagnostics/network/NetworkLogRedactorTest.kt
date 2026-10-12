// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.diagnostics.network

import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [NetworkLogRedactor] and the [NetworkEvent] redaction contract.
 *
 * These tests feed *fake* secret-shaped values and assert the exact secret
 * never appears in the redacted output. No real tokens are used.
 */
class NetworkLogRedactorTest {

    // ── Fake secret fixtures (never real credentials) ──────────────────────
    private val fakeBearerToken = "Bearer test-secret-token"
    private val fakeSapisidCookie = "SAPISID=fake-cookie"
    private val fake3pCookie = "__Secure-3PAPISID=fake-cookie"
    private val fakeSsid = "SSID=fake-session"
    private val fakeAccessToken = "\"access_token\":\"ya29.fake-access-token-value\""
    private val fakeRefreshToken = "\"refresh_token\":\"1//0gfake-refresh-token\""

    @Test
    fun `redact strips Authorization bearer value`() {
        val out = NetworkLogRedactor.redact(fakeBearerToken)
        assertFalse("raw token must not survive", out.contains("test-secret-token"))
        assertTrue(out.contains("<REDACTED>"))
    }

    @Test
    fun `redact strips SAPISID cookie value`() {
        val out = NetworkLogRedactor.redact(fakeSapisidCookie)
        assertFalse(out.contains("fake-cookie"))
        assertTrue(out.contains("<REDACTED>"))
    }

    @Test
    fun `redact strips __Secure-3PAPISID cookie value`() {
        val out = NetworkLogRedactor.redact(fake3pCookie)
        assertFalse(out.contains("fake-cookie"))
    }

    @Test
    fun `redact strips SSID session value`() {
        val out = NetworkLogRedactor.redact(fakeSsid)
        assertFalse(out.contains("fake-session"))
    }

    @Test
    fun `redact strips OAuth access and refresh tokens from a JSON body`() {
        val combined = "{$fakeAccessToken,$fakeRefreshToken,\"expires_in\":3599}"
        val out = NetworkLogRedactor.redact(combined)
        assertFalse(out.contains("ya29.fake-access-token-value"))
        assertFalse(out.contains("1//0gfake-refresh-token"))
        // Non-secret structural field must remain so the line stays useful.
        assertTrue(out.contains("expires_in"))
    }

    @Test
    fun `redact is a no-op on empty or null input`() {
        assertEquals("", NetworkLogRedactor.redact(null))
        assertEquals("", NetworkLogRedactor.redact(""))
    }

    @Test
    fun `endpointCategory classifies youtubei paths`() {
        assertEquals(
            EndpointCategory.BROWSE,
            NetworkLogRedactor.endpointCategory("https://www.youtube.com/youtubei/v1/browse")
        )
        assertEquals(
            EndpointCategory.NEXT,
            NetworkLogRedactor.endpointCategory("https://music.youtube.com/youtubei/v1/next")
        )
        assertEquals(
            EndpointCategory.SEARCH,
            NetworkLogRedactor.endpointCategory("https://www.youtube.com/youtubei/v1/search")
        )
        assertEquals(
            EndpointCategory.OAUTH,
            NetworkLogRedactor.endpointCategory("https://oauth2.googleapis.com/token")
        )
        assertEquals(
            EndpointCategory.OTHER,
            NetworkLogRedactor.endpointCategory("https://example.com/health")
        )
    }

    @Test
    fun `hostMatchesExpected only accepts youtube hosts`() {
        assertTrue(NetworkLogRedactor.hostMatchesExpected("https://www.youtube.com/youtubei/v1/browse"))
        assertTrue(NetworkLogRedactor.hostMatchesExpected("https://music.youtube.com/youtubei/v1/next"))
        assertFalse(NetworkLogRedactor.hostMatchesExpected("https://evil.example.com/youtubei/v1/browse"))
    }

    @Test
    fun `authMode derives scheme without reading token`() {
        val bearer = Request.Builder()
            .url("https://www.youtube.com/youtubei/v1/browse")
            .addHeader("Authorization", fakeBearerToken)
            .build()
        assertEquals(AuthMode.OAUTH, NetworkLogRedactor.authMode(bearer))
        assertTrue(NetworkLogRedactor.hasAuthHeader(bearer))

        val none = Request.Builder()
            .url("https://www.youtube.com/youtubei/v1/browse")
            .build()
        assertEquals(AuthMode.NONE, NetworkLogRedactor.authMode(none))
        assertFalse(NetworkLogRedactor.hasAuthHeader(none))
    }

    @Test
    fun `headerNames never returns values`() {
        val req = Request.Builder()
            .url("https://www.youtube.com/youtubei/v1/browse")
            .addHeader("Authorization", fakeBearerToken)
            .addHeader("Cookie", fakeSapisidCookie)
            .build()
        val names = NetworkLogRedactor.headerNames(req)
        assertTrue(names.contains("Authorization"))
        assertTrue(names.contains("Cookie"))
        assertFalse(names.contains("test-secret-token"))
        assertFalse(names.contains("fake-cookie"))
    }

    @Test
    fun `networkEvent toRedactedLine contains no secret values`() {
        val req = Request.Builder()
            .url("https://www.youtube.com/youtubei/v1/browse")
            .addHeader("Authorization", fakeBearerToken)
            .addHeader("Cookie", fakeSapisidCookie)
            .addHeader("X-YouTube-Client-Version", "7.20210614.03.00")
            .post("{\"browseId\":\"FEmusic_home\"}".toRequestBody("application/json".toMediaType()))
            .build()

        val event = NetworkLogRedactor.buildEvent(
            request = req,
            response = null,
            elapsedMs = 123L,
            retryCount = 0,
            fallbackReason = FallbackReason.NONE,
            parserResultCount = 12,
            playlistIdType = PlaylistIdType.NONE,
            clientName = KnownClientName.TVHTML5,
            responseBodyBytes = 4096L
        )
        val line = event.toRedactedLine()

        // Structural, non-sensitive signals are present.
        assertTrue(line.contains("endpoint=BROWSE"))
        assertTrue(line.contains("authMode=OAUTH"))
        assertTrue(line.contains("authHeaderPresent=true"))
        assertTrue(line.contains("parsedItems=12"))
        assertTrue(line.contains("client=TVHTML5"))

        // No secret values, no browseId, no playlist ID, no raw body.
        assertFalse(line.contains("test-secret-token"))
        assertFalse(line.contains("fake-cookie"))
        assertFalse(line.contains("Bearer"))
        assertFalse(line.contains("FEmusic_home"))
        assertFalse(line.contains("browseId"))
    }

    @Test
    fun `networkDiagnostics gate is off by default`() {
        // Do not enable globally; just assert the default so release stays safe.
        assertFalse(NetworkDiagnostics.isEnabled)
    }
}
