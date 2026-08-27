package com.deepeye.musicpro.domain.auth

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

private val jsonMedia = "application/json".toMediaType()

/**
 * JVM tests for the YouTube device-code flow state machine (RFC 8628).
 * Google's OAuth endpoints are faked with an OkHttp interceptor, so no real
 * network is touched. Timing assertions have generous margins because CI/host
 * load can stretch wall-clock delays.
 */
class YouTubeDeviceAuthManagerTest {

    /** Serves scripted (code, body) responses for googleapis.com calls only. */
    private class FakeGoogleOAuth(private val scripted: List<Pair<Int, String>>) : Interceptor {
        val served = AtomicInteger(0)

        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            // Debug-telemetry posts to a local agent must never consume scripted auth responses.
            if (!request.url.host.endsWith("googleapis.com")) {
                return basicResponse(request, 404, "{}")
            }
            val index = served.getAndIncrement()
            val (code, body) = scripted.getOrElse(index) { scripted.last() }
            return basicResponse(request, code, body)
        }

        private fun basicResponse(request: okhttp3.Request, code: Int, body: String): Response =
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("test")
                .body(ResponseBody.create(jsonMedia, body))
                .build()
    }

    private fun managerWith(scripted: List<Pair<Int, String>>): Pair<YouTubeDeviceAuthManager, FakeGoogleOAuth> {
        val fake = FakeGoogleOAuth(scripted)
        val client = OkHttpClient.Builder().addInterceptor(fake).build()
        return YouTubeDeviceAuthManager(client) to fake
    }

    private val tokenJson =
        """{"access_token":"at123","refresh_token":"rt456","expires_in":3600,"token_type":"Bearer"}"""

    private fun errorJson(error: String) = """{"error":"$error"}"""

    @Test(timeout = 30_000L)
    fun `pending keeps polling then success delivers token`() = runBlocking {
        val (manager, fake) = managerWith(
            listOf(
                400 to errorJson("authorization_pending"),
                200 to tokenJson,
            )
        )
        var received: TokenResponse? = null
        manager.pollForToken("dev", 1) { received = it }

        assertEquals(2, fake.served.get())
        assertEquals("at123", received?.accessToken)
        assertEquals("rt456", received?.refreshToken)
        assertEquals("Bearer", received?.tokenType)
        assertEquals(3600, received?.expiresIn)
        Unit
    }

    @Test(timeout = 30_000L)
    fun `slow_down backs off interval by five seconds per rfc8628`() = runBlocking {
        // Poll #1 waits 1s (pending); poll #2 waits 1s then slow_down bumps interval to 6s;
        // poll #3 therefore happens at t≈8s and succeeds.
        val (manager, fake) = managerWith(
            listOf(
                400 to errorJson("authorization_pending"),
                400 to errorJson("slow_down"),
                200 to tokenJson,
            )
        )
        var received: TokenResponse? = null
        val start = System.currentTimeMillis()
        manager.pollForToken("dev", 1) { received = it }
        val elapsedMs = System.currentTimeMillis() - start

        assertEquals(3, fake.served.get())
        assertEquals("at123", received?.accessToken)
        assertTrue(
            "Expected RFC 8628 backoff (~8s total with 1s base interval), got ${elapsedMs}ms",
            elapsedMs >= 7_300L
        )
        Unit
    }

    @Test(timeout = 30_000L)
    fun `terminal error stops polling without delivering token`() = runBlocking {
        val (manager, fake) = managerWith(listOf(403 to errorJson("access_denied")))
        var received: TokenResponse? = null
        val start = System.currentTimeMillis()
        manager.pollForToken("dev", 1) { received = it }
        val elapsedMs = System.currentTimeMillis() - start

        assertEquals(1, fake.served.get())
        assertNull(received)
        assertTrue("Polling should stop promptly on terminal errors, took ${elapsedMs}ms", elapsedMs < 5_000L)
        Unit
    }

    @Test(timeout = 30_000L)
    fun `requestDeviceCode parses google response`() = runBlocking {
        val (manager, fake) = managerWith(
            listOf(
                200 to """
                    {"device_code":"dc789","user_code":"ABCD-WXYZ",
                     "verification_url":"https://google.com/device",
                     "expires_in":1800,"interval":5}
                """.trimIndent(),
            )
        )

        val response = manager.requestDeviceCode()

        assertEquals(1, fake.served.get())
        assertEquals("dc789", response?.deviceCode)
        assertEquals("ABCD-WXYZ", response?.userCode)
        assertEquals("https://google.com/device", response?.verificationUrl)
        assertEquals(1800, response?.expiresIn)
        assertEquals(5, response?.interval)
        Unit
    }
}
