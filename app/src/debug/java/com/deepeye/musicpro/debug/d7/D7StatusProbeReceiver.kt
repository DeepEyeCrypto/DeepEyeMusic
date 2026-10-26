// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.debug.d7

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import com.deepeye.musicpro.BuildConfig
import com.deepeye.musicpro.data.source.remote.youtube.InnerTubeRemoteClient
import com.deepeye.musicpro.domain.auth.InnerTubeAuthManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val TAG = "D7StatusProbe"

/**
 * Pure request builders for the D7 FEmusic HTTP 400 status-only probes.
 *
 * Contracts (mission decision matrix):
 *  - H1: music-host `/browse`, ANDROID_MUSIC identity (client name 67),
 *    Bearer token, API-key query param.
 *  - H2: music-host `/browse`, TVHTML5 identity (client name 85), Bearer token,
 *    no API-key query param (isolates client identity as the only variable).
 *  - Control: main-host `/browse`, TVHTML5 + Bearer — the session-validity
 *    contrast that previously returned HTTP 200 in the same session.
 *
 * Safety contract (asserted by D7ProbeContractTest): the Bearer token NEVER
 * appears in a URL/query — only in the Authorization header. Nothing here
 * logs; the receiver logs status/duration only.
 */
internal object D7ProbeRequests {

    const val BROWSE_HOME = "FEmusic_home"
    const val BROWSE_LIKED = "FEmusic_liked"

    private const val JSON = "application/json; charset=utf-8"

    /**
     * D7-H1KEY-FIX: probe selection given the resolved session + key state.
     * Pure (no Android/network) so the contract is unit-testable: a blank API
     * key only drops the two key-param H1 probes and never throws, so one bad
     * probe can never crash the host app process again.
     *
     * @return pair of (probes to fire, true when H1 was skipped for blank key).
     */
    fun selectProbes(token: String, apiKey: String): Pair<List<Pair<String, Request>>, Boolean> {
        val probes = mutableListOf<Pair<String, Request>>()
        var h1Skipped = false
        if (apiKey.isNotBlank()) {
            probes += "h1_home" to h1(BROWSE_HOME, token, apiKey)
            probes += "h1_liked" to h1(BROWSE_LIKED, token, apiKey)
        } else {
            h1Skipped = true
        }
        probes += "h2_home" to h2(BROWSE_HOME, token)
        probes += "h2_liked" to h2(BROWSE_LIKED, token)
        probes += "control_main" to control(token)
        return probes to h1Skipped
    }

    private fun browseBody(contextJson: String, browseId: String): String =
        "{$contextJson, \"browseId\": \"$browseId\"}"

    /** H1: ANDROID_MUSIC + Bearer + API-key query param on the music host. */
    fun h1(browseId: String, token: String, apiKey: String): Request {
        require(token.isNotBlank()) { "D7 probe requires an authorized session token" }
        require(apiKey.isNotBlank()) { "D7 H1 probe requires the InnerTube API key" }
        return Request.Builder()
            .url("${InnerTubeRemoteClient.YOUTUBE_MUSIC_INNERTUBE}browse?key=$apiKey")
            .addHeader("Content-Type", "application/json")
            .addHeader(
                "User-Agent",
                "com.google.android.apps.youtube.music/" +
                    "${InnerTubeRemoteClient.ANDROID_MUSIC_CLIENT_VERSION} (Linux; U; Android 14)",
            )
            .addHeader("X-YouTube-Client-Name", "67")
            .addHeader("X-YouTube-Client-Version", InnerTubeRemoteClient.ANDROID_MUSIC_CLIENT_VERSION)
            .addHeader("Authorization", "Bearer $token")
            .post(
                browseBody(InnerTubeRemoteClient.ANDROID_MUSIC_CONTEXT, browseId)
                    .toRequestBody(JSON.toMediaType()),
            )
            .build()
    }

    /** H2: TVHTML5 + Bearer on the music host (no API-key param). */
    fun h2(browseId: String, token: String): Request {
        require(token.isNotBlank()) { "D7 probe requires an authorized session token" }
        return Request.Builder()
            .url("${InnerTubeRemoteClient.YOUTUBE_MUSIC_INNERTUBE}browse")
            .addHeader("Content-Type", "application/json")
            .addHeader(
                "User-Agent",
                "Mozilla/5.0 (SMART-TV; Linux; Tizen 5.0) AppleWebKit/538.1 " +
                    "(KHTML, like Gecko) Version/5.0 NativeTVAds Safari/538.1,gzip(gfe)",
            )
            .addHeader("X-YouTube-Client-Name", "85")
            .addHeader("X-YouTube-Client-Version", InnerTubeRemoteClient.TVHTML5_CLIENT_VERSION)
            .addHeader("Authorization", "Bearer $token")
            .post(
                browseBody(InnerTubeRemoteClient.TVHTML5_CONTEXT, browseId)
                    .toRequestBody(JSON.toMediaType()),
            )
            .build()
    }

    /** Control: TVHTML5 + Bearer on the MAIN host (session-validity contrast). */
    fun control(token: String): Request {
        require(token.isNotBlank()) { "D7 probe requires an authorized session token" }
        return Request.Builder()
            .url("${InnerTubeRemoteClient.YOUTUBE_MAIN_INNERTUBE}browse")
            .addHeader("Content-Type", "application/json")
            .addHeader(
                "User-Agent",
                "Mozilla/5.0 (SMART-TV; Linux; Tizen 5.0) AppleWebKit/538.1 " +
                    "(KHTML, like Gecko) Version/5.0 NativeTVAds Safari/538.1,gzip(gfe)",
            )
            .addHeader("X-YouTube-Client-Name", "85")
            .addHeader("X-YouTube-Client-Version", InnerTubeRemoteClient.TVHTML5_CLIENT_VERSION)
            .addHeader("Authorization", "Bearer $token")
            .post(
                browseBody(InnerTubeRemoteClient.TVHTML5_CONTEXT, "FEwhat_to_watch")
                    .toRequestBody(JSON.toMediaType()),
            )
            .build()
    }
}

/**
 * D7 FEmusic HTTP 400 — authorized in-app H1/H2 status-only probes.
 * Debug builds only (registered from `src/debug/AndroidManifest.xml`).
 *
 * Trigger:
 * `adb shell am broadcast -a com.deepeye.musicpro.debug.action.D7_STATUS_PROBE`
 *
 * The probe runs inside the signed-in app process against its OWN session:
 * [InnerTubeAuthManager.getAccessToken] applies the real proactive-refresh
 * lifecycle, so a blank result means the app itself has no usable session and
 * every probe is skipped (never a tokenless music request).
 *
 * Emitted evidence is strictly status-only — probe name, endpoint family,
 * HTTP status or `io`/`timeout` class marker, durationMs and tokenPresent.
 * NEVER logged: Bearer token, API key, request/response bodies, account
 * identifiers, titles, media IDs, exception messages or stack traces (an
 * exception message can embed the full URL, which carries the key param).
 */
@AndroidEntryPoint
class D7StatusProbeReceiver : BroadcastReceiver() {

    @Inject
    lateinit var authManager: InnerTubeAuthManager

    @Inject
    lateinit var httpClient: OkHttpClient

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val ACTION_D7_STATUS_PROBE = "com.deepeye.musicpro.debug.action.D7_STATUS_PROBE"
        const val PROBE_TIMEOUT_MS = 15_000L
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != ACTION_D7_STATUS_PROBE) return
        Log.i(TAG, "event=d7_probe action=triggered")
        // Fire-and-forget on a private scope, matching RecoveryProbeReceiver:
        // the receiver returns immediately and the status lines land in logcat
        // when the bounded probes finish.
        scope.launch { runProbes() }
    }

    private suspend fun runProbes() {
        val token = authManager.getAccessToken()
        val tokenPresent = !token.isNullOrBlank()
        if (!tokenPresent) {
            // No authorized session in-app -> skip; never fire tokenless music
            // requests (guest fill is a provenance violation).
            Log.w(TAG, "event=d7_probe result=skipped reason=no_session tokenPresent=false")
            return
        }
        val bearer = token.orEmpty()
        // D7-H1KEY-FIX: blank BuildConfig key must skip H1, never throw —
        // the previous eager h1(...) build crashed the app process (RED:
        // FATAL IllegalArgumentException "D7 H1 probe requires the InnerTube
        // API key" on DefaultDispatcher-worker).
        val apiKey = BuildConfig.YOUTUBE_API_KEY
        val (probes, h1Skipped) = try {
            D7ProbeRequests.selectProbes(bearer, apiKey)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Residual guard: log class only (message may embed URL+key).
            Log.w(TAG, "event=d7_probe result=aborted reason=error_${e.javaClass.simpleName}")
            return
        }
        if (h1Skipped) {
            Log.w(TAG, "event=d7_probe result=skipped reason=no_api_key detail=h1_pair tokenPresent=true")
        }
        val client = httpClient.newBuilder()
            .callTimeout(PROBE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .build()
        Log.i(TAG, "event=d7_probe action=start probes=${probes.size} tokenPresent=true")
        for ((name, request) in probes) {
            val startedAt = SystemClock.elapsedRealtime()
            val family = if (request.url.host == "music.youtube.com") "music_browse" else "main_browse"
            val status: String = try {
                client.newCall(request).execute().use { response -> response.code.toString() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                // Class name only — message/stack may embed the URL (key param).
                if (e is SocketTimeoutException) "timeout" else "io_${e.javaClass.simpleName}"
            } catch (e: Exception) {
                // Residual programming errors: class only, never message/cause.
                "error_${e.javaClass.simpleName}"
            }
            val durationMs = SystemClock.elapsedRealtime() - startedAt
            Log.i(
                TAG,
                "event=d7_probe_result probe=$name family=$family " +
                    "path=${request.url.encodedPath} status=$status " +
                    "durationMs=$durationMs tokenPresent=true",
            )
        }
        Log.i(TAG, "event=d7_probe action=done result=complete")
    }
}


