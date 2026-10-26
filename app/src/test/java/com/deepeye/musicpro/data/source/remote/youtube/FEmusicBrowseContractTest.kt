// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.source.remote.youtube

import com.deepeye.musicpro.domain.auth.InnerTubeAuthManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * FEMUSIC_HOME / FEMUSIC_LIKED HTTP 400 — request-contract classification.
 *
 * Live device evidence (2026-10-18, fresh-install force-stop restarts,
 * status-only signals):
 * - hasAuth=true; /browse 400 on FEmusic_liked AND FEmusic_home — consistent
 *   across restarts and both browseIds; same token family succeeds on the
 *   search path (HTTP 200). NOT expiry (no 401), NOT transport, NOT parser.
 * - PROBE-A (add `?key=` like the working search path): STILL HTTP 400
 *   on both browseIds → H1 disproven.
 * - PROBE-B (TVHTML5 identity 85 + SMART-TV UA + Bearer + key param on the
 *   music host): STILL HTTP 400 on both browseIds → H2 disproven.
 * - Working contrast: TVHTML5 + Bearer on the MAIN host
 *   (www.youtube.com/youtubei/v1/browse) succeeds in the same session.
 *
 * VERDICT: BLOCKED — server-side restriction or an additional
 * music-host request-contract requirement suspected (e.g. origin/cookie
 * binding the official YT Music client carries). No app-side candidate
 * remains; production reverted to the original ANDROID_MUSIC contract.
 * Synthetic fixtures only.
 */
class FEmusicBrowseContractTest {

    private val okHttpClient = mockk<OkHttpClient>()
    private val authManager = mockk<InnerTubeAuthManager>()

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun stubResponse(code: Int, bodyStr: String?) {
        val body = mockk<ResponseBody>()
        every { body.string() } returns (bodyStr ?: "")
        val response = mockk<Response>(relaxed = true) {
            every { isSuccessful } returns (code in 200..299)
            every { this@mockk.code } returns code
            every { this@mockk.body } returns body
        }
        val call = mockk<Call>()
        every { call.execute() } returns response
        every { okHttpClient.newCall(any()) } returns call
    }

    @Test
    fun `H0 - live HTTP 400 maps to typed INVALID_RESPONSE, never silent empty`() = runTest {
        coEvery { authManager.getAccessToken() } returns "synthetic-token"
        stubResponse(code = 400, bodyStr = "")
        val client = InnerTubeRemoteClient(okHttpClient, authManager)
        try {
            client.fetchMusicHomeShelves()
            fail("expected typed unavailable for live HTTP 400")
        } catch (e: InnerTubeMusicUnavailableException) {
            assertEquals(MusicShelfFailureReason.INVALID_RESPONSE, e.reason)
        }
    }

    @Test
    fun `H1 - music browse hits music host browse endpoint with Bearer but no key param`() = runTest {
        coEvery { authManager.getAccessToken() } returns "synthetic-token"
        stubResponse(code = 400, bodyStr = "")
        val client = InnerTubeRemoteClient(okHttpClient, authManager)
        try {
            client.fetchMusicHomeShelves()
        } catch (_: InnerTubeMusicUnavailableException) {
            // Expected — inspecting the outgoing request contract only.
        }
        val slots = mutableListOf<Request>()
        verify(atLeast = 1) { okHttpClient.newCall(capture(slots)) }
        val firstReq = slots.first()
        val url = firstReq.url.toString()
        assertTrue("expected music host, got: $url", url.startsWith("https://music.youtube.com/youtubei/v1/"))
        // H1 fixed: path (not raw suffix) — URL now carries `?key=...` too.
        assertTrue(
            "expected browse endpoint path, got: ${firstReq.url.encodedPath}",
            firstReq.url.encodedPath.endsWith("/browse"),
        )
        assertTrue(
            "Bearer must be attached on the signed-in music path",
            firstReq.header("Authorization")?.startsWith("Bearer ") == true,
        )
        // PROBE-A (2026-10-18, live status-only): adding `?key=` did NOT change
        // the HTTP 400 on either browseId — H1 disproven, contract reverted.
        assertTrue(
            "original contract: music browse carries no key query param: $url",
            firstReq.url.queryParameter("key") == null,
        )
    }

    @Test
    fun `H2 - music browse declares ANDROID_MUSIC client identity headers`() = runTest {
        coEvery { authManager.getAccessToken() } returns "synthetic-token"
        stubResponse(code = 400, bodyStr = "")
        val client = InnerTubeRemoteClient(okHttpClient, authManager)
        try {
            client.fetchMusicHomeShelves()
        } catch (_: InnerTubeMusicUnavailableException) {
        }
        val slots = mutableListOf<Request>()
        verify(atLeast = 1) { okHttpClient.newCall(capture(slots)) }
        val firstReq = slots.first()
        assertEquals("67", firstReq.header("X-YouTube-Client-Name"))
        assertEquals(
            InnerTubeRemoteClient.ANDROID_MUSIC_CLIENT_VERSION,
            firstReq.header("X-YouTube-Client-Version"),
        )
    }

    @Test
    fun `H3 - music browse falls back to TV FEtopics_music on TV host when FEmusic_home returns 400`() = runTest {
        coEvery { authManager.getAccessToken() } returns "synthetic-token"
        stubResponse(code = 400, bodyStr = "")
        val client = InnerTubeRemoteClient(okHttpClient, authManager)
        try {
            client.fetchMusicHomeShelves()
        } catch (_: InnerTubeMusicUnavailableException) {
        }
        val slots = mutableListOf<Request>()
        verify(atLeast = 2) { okHttpClient.newCall(capture(slots)) }
        assertEquals(2, slots.size)
        assertTrue(slots[0].url.toString().startsWith("https://music.youtube.com/youtubei/v1/browse"))
        assertTrue(slots[1].url.toString().startsWith("https://www.youtube.com/youtubei/v1/browse"))
    }
}
