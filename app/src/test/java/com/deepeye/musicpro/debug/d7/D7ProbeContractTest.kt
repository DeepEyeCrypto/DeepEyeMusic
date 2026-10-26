// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.debug.d7

import com.deepeye.musicpro.data.source.remote.youtube.InnerTubeRemoteClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * D7 FEmusic HTTP 400 — contract tests for the debug-only status-only probe
 * requests. These assert the EXACT decision-matrix contracts the on-device
 * probe sends, so recorded H1/H2 HTTP statuses are attributable to the
 * intended variable (API-key param vs client identity), plus the safety
 * contract that the Bearer token never appears in a URL.
 *
 * Synthetic fixtures only — no live network, no real credentials.
 */
class D7ProbeContractTest {

    private val syntheticToken = "synthetic-session-token-value"
    private val syntheticKey = "synthetic-api-key-value"

    /** Reads a request body without any live network (okio buffer drain). */
    private fun okhttp3.RequestBody.contentString(): String {
        val buffer = okio.Buffer()
        writeTo(buffer)
        return buffer.readUtf8()
    }

    @Test
    fun `H1 - music host browse with ANDROID_MUSIC identity Bearer and key param`() {
        val request = D7ProbeRequests.h1(D7ProbeRequests.BROWSE_HOME, syntheticToken, syntheticKey)
        val url = request.url
        assertEquals("music.youtube.com", url.host)
        assertTrue("expected /browse path, got ${url.encodedPath}", url.encodedPath.endsWith("/browse"))
        assertEquals(syntheticKey, url.queryParameter("key"))
        assertEquals("67", request.header("X-YouTube-Client-Name"))
        assertEquals(
            InnerTubeRemoteClient.ANDROID_MUSIC_CLIENT_VERSION,
            request.header("X-YouTube-Client-Version"),
        )
        assertEquals("Bearer $syntheticToken", request.header("Authorization"))
        val body = request.body!!.contentString()
        assertTrue("H1 body must declare ANDROID_MUSIC client", body.contains("\"clientName\": \"ANDROID_MUSIC\""))
        assertTrue("H1 body must carry the probed browseId", body.contains("\"browseId\": \"${D7ProbeRequests.BROWSE_HOME}\""))
    }

    @Test
    fun `H2 - music host browse with TVHTML5 identity Bearer and no key param`() {
        val request = D7ProbeRequests.h2(D7ProbeRequests.BROWSE_LIKED, syntheticToken)
        val url = request.url
        assertEquals("music.youtube.com", url.host)
        assertTrue("expected /browse path, got ${url.encodedPath}", url.encodedPath.endsWith("/browse"))
        assertNull("H2 isolates client identity — no key param allowed", url.queryParameter("key"))
        assertEquals("85", request.header("X-YouTube-Client-Name"))
        assertEquals(
            InnerTubeRemoteClient.TVHTML5_CLIENT_VERSION,
            request.header("X-YouTube-Client-Version"),
        )
        assertEquals("Bearer $syntheticToken", request.header("Authorization"))
        val body = request.body!!.contentString()
        assertTrue("H2 body must declare TVHTML5 client", body.contains("\"clientName\": \"TVHTML5\""))
        assertTrue("H2 body must carry the probed browseId", body.contains("\"browseId\": \"${D7ProbeRequests.BROWSE_LIKED}\""))
    }

    @Test
    fun `control - main host browse with TVHTML5 identity and Bearer`() {
        val request = D7ProbeRequests.control(syntheticToken)
        assertEquals("www.youtube.com", request.url.host)
        assertTrue(request.url.encodedPath.endsWith("/browse"))
        assertEquals("85", request.header("X-YouTube-Client-Name"))
        assertEquals(
            InnerTubeRemoteClient.TVHTML5_CLIENT_VERSION,
            request.header("X-YouTube-Client-Version"),
        )
        assertEquals("Bearer $syntheticToken", request.header("Authorization"))
    }

    @Test
    fun `safety - Bearer token never appears in any probe URL`() {
        val requests = listOf(
            D7ProbeRequests.h1(D7ProbeRequests.BROWSE_HOME, syntheticToken, syntheticKey),
            D7ProbeRequests.h2(D7ProbeRequests.BROWSE_HOME, syntheticToken),
            D7ProbeRequests.control(syntheticToken),
        )
        for (request in requests) {
            val url = request.url.toString()
            assertFalse("token leaked into URL of ${request.url.host}", url.contains(syntheticToken))
        }
    }

    @Test
    fun `builders reject blank session - no tokenless probe can be built`() {
        val builders = listOf<() -> okhttp3.Request>(
            { D7ProbeRequests.h1(D7ProbeRequests.BROWSE_HOME, " ", syntheticKey) },
            { D7ProbeRequests.h2(D7ProbeRequests.BROWSE_HOME, " ") },
            { D7ProbeRequests.control(" ") },
        )
        for (builder in builders) {
            try {
                builder()
                fail("probe builders must require an authorized session token")
            } catch (e: IllegalArgumentException) {
                // Expected: tokenless probe construction is impossible.
            }
        }
    }

    /**
     * D7-H1KEY-FIX (RED: on-device FATAL IllegalArgumentException "D7 H1 probe
     * requires the InnerTube API key" — eager h1() build on
     * DefaultDispatcher-worker killed PID 7772 when BuildConfig.YOUTUBE_API_KEY
     * is blank). Blank key must drop the H1 pair WITHOUT throwing, so H2 +
     * control still fire.
     */
    @Test
    fun `selectProbes with blank api key skips H1 pair without throwing`() {
        val (probes, h1Skipped) = D7ProbeRequests.selectProbes(syntheticToken, "  ")
        assertTrue("blank key must be reported as H1-skipped", h1Skipped)
        val names = probes.map { it.first }
        assertEquals(listOf("h2_home", "h2_liked", "control_main"), names)
    }

    @Test
    fun `selectProbes with key present keeps full H1 H2 control matrix`() {
        val (probes, h1Skipped) = D7ProbeRequests.selectProbes(syntheticToken, syntheticKey)
        assertFalse("key present must not skip H1", h1Skipped)
        assertEquals(
            listOf("h1_home", "h1_liked", "h2_home", "h2_liked", "control_main"),
            probes.map { it.first },
        )
    }
}
