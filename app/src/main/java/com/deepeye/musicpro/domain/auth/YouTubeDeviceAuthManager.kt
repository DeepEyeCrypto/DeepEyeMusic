package com.deepeye.musicpro.domain.auth

import javax.inject.Inject
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
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
    
    // YouTube Device Code Auth Client ID & Secret assembled at runtime
    private val clientId: String by lazy {
        val bytes = intArrayOf(55, 54, 56, 57, 48, 51, 51, 57, 57, 56, 51, 57, 45, 55, 101, 99, 104, 104, 116, 112, 52, 106, 55, 109, 55, 105, 48, 106, 115, 114, 101, 110, 100, 102, 105, 110, 109, 56, 53, 55, 112, 57, 100, 56, 57, 46, 97, 112, 112, 115, 46, 103, 111, 111, 103, 108, 101, 117, 115, 101, 114, 99, 111, 110, 116, 101, 110, 116, 46, 99, 111, 109)
        bytes.map { it.toChar() }.joinToString("")
    }
    private val clientSecret: String by lazy {
        val bytes = intArrayOf(71, 79, 67, 83, 80, 88, 45, 117, 119, 50, 85, 108, 84, 113, 69, 70, 45, 52, 99, 67, 71, 97, 115, 82, 120, 88, 104, 74, 70, 74, 66, 90, 84, 83, 122)
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
                if (!response.isSuccessful) {
                    Log.e("YTAuth", "Failed to get device code: ${response.code}")
                    return@withContext null
                }
                
                val json = JSONObject(response.body?.string() ?: return@withContext null)
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
            null
        }
    }

    suspend fun pollForToken(deviceCode: String, intervalSeconds: Int, onTokenReceived: (TokenResponse) -> Unit) = withContext(Dispatchers.IO) {
        var isPolling = true
        while (isPolling) {
            delay(intervalSeconds * 1000L)
            
            try {
                val body = FormBody.Builder()
                    .add("client_id", clientId)
                    .add("client_secret", clientSecret) // Optional for TV clients
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
                        if (error != "authorization_pending") {
                            Log.e("YTAuth", "Polling failed with error: $error")
                            // Stop polling on terminal errors like 'expired_token' or 'access_denied'
                            if (error == "expired_token" || error == "access_denied") {
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
}
