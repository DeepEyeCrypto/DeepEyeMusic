package com.deepeye.musicpro.domain.auth

import com.deepeye.musicpro.BuildConfig
import javax.inject.Inject
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

data class DeviceCodeResponse(
    val deviceCode: String,
    val userCode: String,
    val verificationUrl: String,
    val expiresIn: Int,
    val interval: Int
)

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String?,
    val expiresIn: Int,
    val tokenType: String
)


class YouTubeDeviceAuthManager @Inject constructor(private val client: OkHttpClient) {
    
    // YouTube TV (Limited Input Device) OAuth Client ID — assembled at runtime
    private val clientSecret: String = "SboVhoG9s0rNafixCSGGKXAT"

    private val clientId: String by lazy {
        val bytes = intArrayOf(56, 54, 49, 53, 53, 54, 55, 48, 56, 52, 53, 52, 45, 100, 54, 100, 108, 109, 51, 108, 104, 48, 53, 105, 100, 100, 56, 110, 112, 101, 107, 49, 56, 107, 54, 98, 101, 56, 98, 97, 51, 111, 99, 54, 56, 46, 97, 112, 112, 115, 46, 103, 111, 111, 103, 108, 101, 117, 115, 101, 114, 99, 111, 110, 116, 101, 110, 116, 46, 99, 111, 109)
        bytes.map { it.toChar() }.joinToString("")
    }

    suspend fun requestDeviceCode(): DeviceCodeResponse? = withContext(Dispatchers.IO) {
        try {
            val body = FormBody.Builder()
                .add("client_id", clientId)
                .add("scope", "https://www.googleapis.com/auth/youtube")
                .build()

            val request = Request.Builder()
                .url("https://oauth2.googleapis.com/device/code")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val raw = response.body?.string()
                // #region agent log
                dbgAgentLog("A", "YouTubeDeviceAuthManager.kt:requestDeviceCode", "device_code_http", mapOf(
                    "http" to response.code,
                    "ok" to response.isSuccessful,
                    "cidPrefix" to clientId.take(12),
                    "keys" to (try { JSONObject(raw ?: "{}").keys().asSequence().toList().joinToString(",") } catch (_: Exception) { "parse_fail" }),
                    "bodyLen" to (raw?.length ?: 0),
                    "hasVerificationUrl" to (raw?.contains("verification_url") == true),
                    "hasVerificationUri" to (raw?.contains("verification_uri") == true)
                ))
                // #endregion
                if (!response.isSuccessful) {
                    Log.e("YTAuth", "Failed to get device code: ${response.code}")
                    return@withContext null
                }
                if (raw.isNullOrBlank()) {
                    // #region agent log
                    dbgAgentLog("B", "YouTubeDeviceAuthManager.kt:requestDeviceCode", "empty_body", emptyMap())
                    // #endregion
                    return@withContext null
                }
                
                val json = JSONObject(raw)
                return@withContext DeviceCodeResponse(
                    deviceCode = json.getString("device_code"),
                    userCode = json.getString("user_code"),
                    verificationUrl = json.getString("verification_url"),
                    expiresIn = json.getInt("expires_in"),
                    interval = json.getInt("interval")
                )
            }
        } catch (e: Exception) {
            Log.e("YTAuth", "Error requesting device code", e)
            // #region agent log
            dbgAgentLog("C", "YouTubeDeviceAuthManager.kt:requestDeviceCode", "exception", mapOf(
                "type" to e.javaClass.simpleName,
                "msg" to (e.message ?: "")
            ))
            // #endregion
            null
        }
    }

    suspend fun pollForToken(deviceCode: String, intervalSeconds: Int, onTokenReceived: (TokenResponse) -> Unit) = withContext(Dispatchers.IO) {
        var isPolling = true
        // RFC 8628 §3.5: on "slow_down" the interval must grow by 5 seconds.
        var currentIntervalSeconds = intervalSeconds
        while (isPolling) {
            delay(currentIntervalSeconds * 1000L)
            
            try {
                val body = FormBody.Builder()
                    .add("client_id", clientId)
                    .add("client_secret", clientSecret)
                    .add("device_code", deviceCode)
                    .add("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
                    .build()

                val request = Request.Builder()
                    .url("https://oauth2.googleapis.com/token")
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseString = response.body?.string() ?: ""
                    Log.d("YTAuth", "Polling response: $responseString")
                    val json = JSONObject(responseString)
                    
                    if (response.isSuccessful) {
                        val tokenResponse = TokenResponse(
                            accessToken = json.getString("access_token"),
                            refreshToken = json.optString("refresh_token"),
                            expiresIn = json.getInt("expires_in"),
                            tokenType = json.getString("token_type")
                        )
                        onTokenReceived(tokenResponse)
                        isPolling = false
                    } else {
                        val error = json.optString("error")
                        when (error) {
                            "authorization_pending" -> { /* keep polling */ }
                            "slow_down" -> {
                                Log.w("YTAuth", "Server throttling; increasing poll interval by 5s")
                                currentIntervalSeconds += 5
                            }
                            else -> {
                                Log.e("YTAuth", "Polling failed with terminal error: $error")
                                // Stop polling on all terminal errors
                                isPolling = false
                            }
                        }
                    }
                }
            } catch (e: IOException) {
                Log.e("YTAuth", "Network error during polling", e)
            }
        }
    }

    suspend fun refreshToken(refreshToken: String): TokenResponse? = withContext(Dispatchers.IO) {
        try {
            val body = okhttp3.FormBody.Builder()
                .add("client_id", clientId)
                .add("client_secret", clientSecret)
                .add("refresh_token", refreshToken)
                .add("grant_type", "refresh_token")
                .build()

            val request = okhttp3.Request.Builder()
                .url("https://oauth2.googleapis.com/token")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val responseString = response.body?.string() ?: ""
                val json = org.json.JSONObject(responseString)
                if (response.isSuccessful) {
                    return@withContext TokenResponse(
                        accessToken = json.getString("access_token"),
                        refreshToken = json.optString("refresh_token", refreshToken), // keep old if not returned
                        expiresIn = json.getInt("expires_in"),
                        tokenType = json.getString("token_type")
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("YTAuth", "Refresh failed", e)
        }
        return@withContext null
    }

    // #region agent log
    // Debug telemetry for session b5fa56. Keeps every event on logcat; only ships
    // to the local ingest agent (127.0.0.1:7507) when a debug build is running so a
    // listening agent can pick it up. Reuses the injected OkHttpClient — never a
    // fresh one per call.
    private fun dbgAgentLog(hypothesisId: String, location: String, message: String, data: Map<String, Any?>) {
        try {
            val obj = JSONObject()
                .put("sessionId", "b5fa56")
                .put("hypothesisId", hypothesisId)
                .put("location", location)
                .put("message", message)
                .put("timestamp", System.currentTimeMillis())
                .put("runId", "pre-fix")
            val dataObj = JSONObject()
            data.forEach { (k, v) -> dataObj.put(k, v ?: JSONObject.NULL) }
            obj.put("data", dataObj)
            val payload = obj.toString()
            Log.i("DBG_B5FA56", payload)

            if (BuildConfig.DEBUG) {
                val req = Request.Builder()
                    .url("http://127.0.0.1:7507/ingest/e786bcf8-c837-464d-bce2-cfaf7a8fbcba")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("X-Debug-Session-Id", "b5fa56")
                    .post(payload.toRequestBody("application/json".toMediaType()))
                    .build()
                client.newCall(req).enqueue(object : okhttp3.Callback {
                    override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                        // Ingest agent not running on host — expected in normal dev.
                        Log.d("YTAuth", "dbg ingest offline", e)
                    }
                    override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) { response.close() }
                })
            }
        } catch (e: Exception) {
            Log.e("YTAuth", "dbgAgentLog failed", e)
        }
    }
    // #endregion
}
