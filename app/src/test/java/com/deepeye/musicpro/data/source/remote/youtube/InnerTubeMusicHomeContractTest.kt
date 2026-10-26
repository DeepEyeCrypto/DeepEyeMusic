// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.source.remote.youtube

import com.deepeye.musicpro.domain.auth.InnerTubeAuthManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import java.io.IOException
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.CancellationException
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * ACCOUNT-MUSIC-OMEGA: FEmusic_home request provenance + typed-failure contract.
 *
 * A music-home request MUST NOT be issued without an account token.
 * An unauthenticated /browse returns a generic guest feed that must never be
 * rendered as account-context shelves. Expected failures map to typed
 * unavailable reasons; cancellation propagates. Synthetic fixtures only.
 */
class InnerTubeMusicHomeContractTest {

    private val okHttpClient = mockk<OkHttpClient>()
    private val authManager = mockk<InnerTubeAuthManager>()

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun okResponse(bodyStr: String?, code: Int = 200): Response {
        val body = mockk<ResponseBody>()
        every { body.string() } returns (bodyStr ?: "")
        return mockk<Response>(relaxed = true) {
            every { isSuccessful } returns (code in 200..299)
            every { this@mockk.code } returns code
            every { this@mockk.body } returns body
        }
    }

    private fun respondWith(response: Response) {
        val call = mockk<Call>()
        every { call.execute() } returns response
        every { okHttpClient.newCall(any()) } returns call
    }

    @Test
    fun `blank account token blocks music-home request with no network call`() = runTest {
        coEvery { authManager.getAccessToken() } returns null
        val client = InnerTubeRemoteClient(okHttpClient, authManager)
        var blocked = false
        try {
            client.fetchMusicHomeShelves()
        } catch (e: InnerTubeAuthException) {
            blocked = true
        }
        assertTrue(
            "unauthenticated music-home request must be blocked, not sent as guest",
            blocked,
        )
        verify(exactly = 0) { okHttpClient.newCall(any()) }
    }

    @Test
    fun `network failure maps to typed NETWORK unavailable, not empty filler`() = runTest {
        coEvery { authManager.getAccessToken() } returns "synthetic-token"
        val call = mockk<Call>()
        every { call.execute() } throws IOException("synthetic transport failure")
        every { okHttpClient.newCall(any()) } returns call
        val client = InnerTubeRemoteClient(okHttpClient, authManager)
        try {
            client.fetchMusicHomeShelves()
            fail("expected typed unavailable for network failure")
        } catch (e: InnerTubeMusicUnavailableException) {
            assertEquals(MusicShelfFailureReason.NETWORK, e.reason)
        }
        // Never a silent success-empty: caller maps the typed outcome explicitly.
        verify(exactly = 1) { okHttpClient.newCall(any()) }
    }

    @Test
    fun `missing required body maps to typed INVALID_RESPONSE, not empty`() = runTest {
        coEvery { authManager.getAccessToken() } returns "synthetic-token"
        respondWith(okResponse(bodyStr = ""))
        val client = InnerTubeRemoteClient(okHttpClient, authManager)
        try {
            client.fetchMusicHomeShelves()
            fail("expected typed unavailable for missing body")
        } catch (e: InnerTubeMusicUnavailableException) {
            assertEquals(MusicShelfFailureReason.INVALID_RESPONSE, e.reason)
        }
    }

    @Test
    fun `auth failure stays on auth path, never unavailable`() = runTest {
        coEvery { authManager.getAccessToken() } returns "synthetic-expired-token"
        coEvery { authManager.refreshAccessToken() } returns null
        respondWith(okResponse(bodyStr = null, code = 401))
        val client = InnerTubeRemoteClient(okHttpClient, authManager)
        try {
            client.fetchMusicHomeShelves()
            fail("expected auth failure to stay on the auth path")
        } catch (e: InnerTubeAuthException) {
            // Expected: reauth path, not an unavailable outcome.
        } catch (e: InnerTubeMusicUnavailableException) {
            fail("auth failure must not be flattened into unavailable: ${e.reason}")
        }
    }

    @Test
    fun `cancellation during fetchMusicHomeShelves propagates, never converted to unavailable`() = runTest {
        coEvery { authManager.getAccessToken() } returns "synthetic-token"
        val call = mockk<Call>()
        every { call.execute() } throws CancellationException("synthetic cancellation")
        every { okHttpClient.newCall(any()) } returns call
        val client = InnerTubeRemoteClient(okHttpClient, authManager)
        try {
            client.fetchMusicHomeShelves()
            fail("expected CancellationException to propagate")
        } catch (e: CancellationException) {
            // expected: cancellation propagates, never converted to unavailable
        }
        verify(exactly = 1) { okHttpClient.newCall(any()) }
    }
}
