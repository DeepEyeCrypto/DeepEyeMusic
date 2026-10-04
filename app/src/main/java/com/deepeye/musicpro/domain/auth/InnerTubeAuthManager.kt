// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.domain.auth

import android.util.Log
import com.deepeye.musicpro.data.prefs.SettingsDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * InnerTube OAuth 2.0 Device Authorization Manager (SmartTube Architecture).
 *
 * Implements Google's Limited Input Device Flow (RFC 8628) for pure Bearer token
 * authentication without Google Play Services or heavy webview dependencies.
 */
@Singleton
class InnerTubeAuthManager @Inject constructor(
    private val client: OkHttpClient,
    private val settingsDataStore: SettingsDataStore,
    private val deviceAuthManager: YouTubeDeviceAuthManager
) {
    private companion object {
        const val TAG = "InnerTubeAuthManager"
    }

    /**
     * Requests a new device code and user verification URL (https://www.youtube.com/activate).
     */
    suspend fun requestDeviceCode(): DeviceCodeResponse? {
        return deviceAuthManager.requestDeviceCode()
    }

    /**
     * Polls Google OAuth token endpoint until user authorizes or session expires.
     */
    suspend fun pollForToken(
        deviceCode: String,
        intervalSeconds: Int,
        onTokenReceived: suspend (TokenResponse) -> Unit
    ) {
        deviceAuthManager.pollForToken(deviceCode, intervalSeconds) { tokenResponse ->
            kotlinx.coroutines.runBlocking {
                onTokenReceived(tokenResponse)
            }
        }
    }

    /**
     * Returns the currently saved valid Bearer access token, if any.
     */
    suspend fun getAccessToken(): String? {
        return settingsDataStore.settings.first().youtubeAccessToken
    }

    /**
     * Refreshes the OAuth access token using the stored refresh token.
     */
    suspend fun refreshAccessToken(): String? = withContext(Dispatchers.IO) {
        val settings = settingsDataStore.settings.first()
        val refreshToken = settings.youtubeRefreshToken ?: return@withContext null
        val tokenResponse = deviceAuthManager.refreshToken(refreshToken)
        if (tokenResponse != null) {
            settingsDataStore.setYouTubeTokens(tokenResponse.accessToken, tokenResponse.refreshToken ?: refreshToken)
            return@withContext tokenResponse.accessToken
        }
        null
    }

    /**
     * Clears all InnerTube tokens (Logout).
     */
    suspend fun logout() {
        settingsDataStore.setYouTubeTokens("", null)
    }
}
