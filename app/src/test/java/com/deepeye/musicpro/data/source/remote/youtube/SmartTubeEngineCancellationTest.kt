package com.deepeye.musicpro.data.source.remote.youtube

import android.content.Context
import com.deepeye.musicpro.domain.auth.InnerTubeAuthManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/**
 * D1 — SmartTubeEngine CancellationException propagation.
 * Verifies that CancellationException is rethrown from all public endpoints
 * and never converted into provider error/empty/fallback.
 */
class SmartTubeEngineCancellationTest {

    private lateinit var engine: SmartTubeEngine
    private val context: Context = mockk(relaxed = true)
    private val authManager: InnerTubeAuthManager = mockk(relaxed = true)
    private val okHttpClient: OkHttpClient = mockk(relaxed = true)

    @Before
    fun setup() {
        engine = SmartTubeEngine(context, authManager, okHttpClient)
    }

    private fun stubCancellingCall() {
        val call = mockk<Call>()
        every { call.execute() } throws CancellationException("synthetic cancellation")
        every { okHttpClient.newCall(any()) } returns call
    }

    @Test
    fun `postInnerTube rethrows CancellationException`() = runTest {
        coEvery { authManager.getAccessToken() } returns "synthetic-token"
        stubCancellingCall()
        try {
            engine.postInnerTube("browse")
            fail("expected CancellationException to propagate")
        } catch (e: CancellationException) {
            // expected
        }
    }

    @Test
    fun `fetchUpNext rethrows CancellationException`() = runTest {
        coEvery { authManager.getAccessToken() } returns "synthetic-token"
        stubCancellingCall()
        try {
            engine.fetchUpNext("videoId123")
            fail("expected CancellationException to propagate")
        } catch (e: CancellationException) {
            // expected
        }
    }

    @Test
    fun `searchTracks rethrows CancellationException`() = runTest {
        coEvery { authManager.getAccessToken() } returns "synthetic-token"
        stubCancellingCall()
        try {
            engine.searchTracks("query")
            fail("expected CancellationException to propagate")
        } catch (e: CancellationException) {
            // expected
        }
    }

    @Test
    fun `fetchLyrics rethrows CancellationException`() = runTest {
        coEvery { authManager.getAccessToken() } returns "synthetic-token"
        stubCancellingCall()
        try {
            engine.fetchLyrics("browseId123")
            fail("expected CancellationException to propagate")
        } catch (e: CancellationException) {
            // expected
        }
    }

    @Test
    fun `postInnerTube 401 retry path does not swallow CancellationException`() = runTest {
        coEvery { authManager.getAccessToken() } returns "synthetic-token"
        coEvery { authManager.refreshAccessToken() } throws CancellationException("refresh cancelled")

        val call = mockk<Call>()
        val response = mockk<Response>()
        every { response.code } returns 401
        every { call.execute() } returns response
        every { okHttpClient.newCall(any()) } returns call

        try {
            engine.postInnerTube("browse")
            fail("expected CancellationException to propagate")
        } catch (e: CancellationException) {
            // expected: refresh cancellation propagates
        }
    }

    @Test
    fun `postInnerTube broad Exception does not catch CancellationException`() = runTest {
        // If a nested call throws CancellationException, the broad catch (Exception)
        // must not intercept it. The explicit CancellationException catch above it
        // must have already rethrown. This test confirms the order is correct.
        coEvery { authManager.getAccessToken() } returns "synthetic-token"
        stubCancellingCall()

        var caughtCancellation = false
        try {
            engine.postInnerTube("browse")
        } catch (e: CancellationException) {
            caughtCancellation = true
        }
        assertTrue("CancellationException must propagate, not be swallowed", caughtCancellation)
    }
}