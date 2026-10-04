// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.domain.auth

import android.util.Log
import com.deepeye.musicpro.data.prefs.SettingsDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
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
    companion object {
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
     * Returns the currently saved Bearer access token, or refreshes automatically if empty but refresh token exists.
     */
    suspend fun getAccessToken(): String? = withContext(Dispatchers.IO) {
        val settings = settingsDataStore.settings.first()
        val token = settings.youtubeAccessToken
        if (!token.isNullOrBlank()) {
            return@withContext token
        }
        if (!settings.youtubeRefreshToken.isNullOrBlank()) {
            Log.i(TAG, "Access token missing, attempting proactive refresh via refresh token...")
            return@withContext refreshAccessToken()
        }
        null
    }

    /**
     * Checks if an authenticated InnerTube session exists.
     */
    suspend fun hasAuthenticatedSession(): Boolean = withContext(Dispatchers.IO) {
        val settings = settingsDataStore.settings.first()
        !settings.youtubeAccessToken.isNullOrBlank() || !settings.youtubeRefreshToken.isNullOrBlank()
    }

    /**
     * Refreshes the OAuth access token using the stored refresh token and saves to DataStore.
     */
    suspend fun refreshAccessToken(): String? = withContext(Dispatchers.IO) {
        val settings = settingsDataStore.settings.first()
        val refreshToken = settings.youtubeRefreshToken
        if (refreshToken.isNullOrBlank()) {
            Log.w(TAG, "Cannot refresh access token: refresh token is null/blank")
            return@withContext null
        }
        Log.i(TAG, "Executing Google OAuth token refresh...")
        val tokenResponse = deviceAuthManager.refreshToken(refreshToken)
        if (tokenResponse != null && tokenResponse.accessToken.isNotBlank()) {
            val newRefreshToken = if (!tokenResponse.refreshToken.isNullOrBlank()) tokenResponse.refreshToken else refreshToken
            settingsDataStore.setYouTubeTokens(tokenResponse.accessToken, newRefreshToken)
            Log.i(TAG, "Successfully refreshed InnerTube Bearer token")
            return@withContext tokenResponse.accessToken
        }
        Log.e(TAG, "Failed to refresh token from Google OAuth endpoint")
        null
    }

    /**
     * Clears all InnerTube tokens (Logout).
     */
    suspend fun logout() {
        Log.i(TAG, "Logging out of InnerTube session, clearing tokens")
        settingsDataStore.setYouTubeTokens("", null)
    }
}
