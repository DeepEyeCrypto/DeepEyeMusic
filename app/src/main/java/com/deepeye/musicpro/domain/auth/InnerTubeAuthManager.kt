// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.domain.auth

import android.util.Log
import com.deepeye.musicpro.data.prefs.SettingsDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import java.io.IOException
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
    private val deviceAuthManager: YouTubeDeviceAuthManager,
    private val authCacheManager: AuthCacheManager
) {
    companion object {
        const val TAG = "InnerTubeAuthManager"

        /**
         * Upper bound for one authorized refresh network round-trip. Refresh is
         * triggered at most once per [getAccessToken]/[refreshAccessToken] call
         * (single-flight), so this bounds the added latency. No infinite retry.
         */
        const val REFRESH_TIMEOUT_MS = 25_000L

        /**
         * A 400 + `invalid_grant` error body from the Google token endpoint means
         * the refresh token is revoked/expired. Only this definitive signal clears
         * credentials; every other failure keeps the old token (recoverable).
         */
        private const val INVALID_GRANT_MARKER = "invalid_grant"
    }

    /**
     * Serializes concurrent refreshes so N parallel browse/music calls share a
     * single token-endpoint round-trip (single-flight). The winner persists the
     * new token; waiters re-read the store inside the lock and reuse it.
     */
    private val refreshMutex = Mutex()

    /** Injectable clock (epoch seconds) for deterministic expiry tests. */
    @Volatile
    var clockEpochSec: () -> Long = { System.currentTimeMillis() / 1000 }

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
                authCacheManager.purgeAllCaches()
                onTokenReceived(tokenResponse)
            }
        }
    }

    /**
     * Returns a usable Bearer access token, proactively refreshing when the
     * persisted expiry says the token is expired or within the skew safety
     * margin. Unknown (legacy null) expiry keeps the previous behavior: the
     * token is used as-is and the 401 rescue path in the request layer applies.
     */
    suspend fun getAccessToken(): String? = withContext(Dispatchers.IO) {
        val settings = settingsDataStore.settings.first()
        val token = settings.youtubeAccessToken
        if (token.isNullOrBlank()) {
            if (!settings.youtubeRefreshToken.isNullOrBlank()) {
                Log.i(TAG, "event=token_acquire reason=missing_token refresh=attempted")
                return@withContext refreshAccessToken()
            }
            return@withContext null
        }
        val needsRefresh =
            TokenExpiry.isExpiredOrNear(
                settings.youtubeTokenExpiresAtEpochSec,
                clockEpochSec(),
            )
        if (!needsRefresh) {
            return@withContext token
        }
        if (settings.youtubeRefreshToken.isNullOrBlank()) {
            Log.w(TAG, "event=token_acquire reason=expired_no_refresh_token refresh=unsupported")
            return@withContext token
        }
        Log.i(TAG, "event=token_acquire reason=expired_or_near refresh=attempted")
        refreshAccessToken() ?: token
    }

    /**
     * Checks if an authenticated InnerTube session exists.
     */
    suspend fun hasAuthenticatedSession(): Boolean = withContext(Dispatchers.IO) {
        val settings = settingsDataStore.settings.first()
        !settings.youtubeAccessToken.isNullOrBlank() || !settings.youtubeRefreshToken.isNullOrBlank()
    }

    /**
     * Refreshes the OAuth access token through the authorized device-flow token
     * endpoint and persists the new pair plus expiry. Single-flight: concurrent
     * callers share one network round-trip; waiters reuse the winner's token.
     *
     * Only a definitive `invalid_grant` response clears credentials (→ re-auth).
     * Transient network/5xx/timeout failures keep the old token (recoverable).
     * Cancellation propagates; the refresh is bounded by [REFRESH_TIMEOUT_MS].
     */
    suspend fun refreshAccessTokenOutcome(): TokenRefreshOutcome = withContext(Dispatchers.IO) {
        refreshMutex.withLock {
            // Re-read inside the lock: a concurrent winner may already have
            // refreshed while this caller was waiting on the mutex.
            val settings = settingsDataStore.settings.first()
            val refreshToken = settings.youtubeRefreshToken
            if (refreshToken.isNullOrBlank()) {
                Log.w(TAG, "event=token_refresh outcome=no_refresh_token")
                return@withContext TokenRefreshOutcome.NoRefreshToken
            }
            if (!TokenExpiry.isExpiredOrNear(settings.youtubeTokenExpiresAtEpochSec, clockEpochSec()) &&
                !settings.youtubeAccessToken.isNullOrBlank()
            ) {
                return@withContext TokenRefreshOutcome.Success(settings.youtubeAccessToken)
            }
            Log.i(TAG, "event=token_refresh outcome=attempted")
            val tokenResponse =
                try {
                    withTimeoutOrNull(REFRESH_TIMEOUT_MS) {
                        deviceAuthManager.refreshToken(refreshToken)
                    }
                } catch (e: IOException) {
                    Log.w(TAG, "event=token_refresh outcome=transient reason=io")
                    return@withContext TokenRefreshOutcome.TransientFailure
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    Log.w(TAG, "event=token_refresh outcome=transient reason=error")
                    return@withContext TokenRefreshOutcome.TransientFailure
                }
            if (tokenResponse == null) {
                Log.w(TAG, "event=token_refresh outcome=transient reason=null_response")
                return@withContext TokenRefreshOutcome.TransientFailure
            }
            if (tokenResponse.accessToken.isBlank()) {
                val revoked = tokenResponse.refreshToken.isNullOrBlank()
                if (revoked) {
                    Log.e(TAG, "event=token_refresh outcome=invalid_grant action=credentials_cleared")
                    settingsDataStore.setYouTubeTokens("", null)
                    authCacheManager.purgeAllCaches()
                    return@withContext TokenRefreshOutcome.InvalidGrant
                }
                Log.w(TAG, "event=token_refresh outcome=transient reason=blank_access_token")
                return@withContext TokenRefreshOutcome.TransientFailure
            }
            val newRefreshToken =
                if (!tokenResponse.refreshToken.isNullOrBlank()) tokenResponse.refreshToken else refreshToken
            val expiresAt =
                TokenExpiry.computeExpiresAt(
                    clockEpochSec(),
                    tokenResponse.expiresIn.takeIf { it > 0 }?.toLong(),
                )
            settingsDataStore.setYouTubeTokens(tokenResponse.accessToken, newRefreshToken, expiresAt)
            authCacheManager.purgeAllCaches()
            Log.i(TAG, "event=token_refresh outcome=success")
            TokenRefreshOutcome.Success(tokenResponse.accessToken)
        }
    }

    /**
     * Refreshes the OAuth access token using the stored refresh token and saves to DataStore.
     * Backward-compatible wrapper: returns the new token or null. Prefer
     * [refreshAccessTokenOutcome] when the caller must distinguish revoked
     * credentials (re-auth) from transient failures (keep old state).
     */
    suspend fun refreshAccessToken(): String? =
        when (val outcome = refreshAccessTokenOutcome()) {
            is TokenRefreshOutcome.Success -> outcome.accessToken
            TokenRefreshOutcome.NoRefreshToken,
            TokenRefreshOutcome.TransientFailure,
            TokenRefreshOutcome.InvalidGrant -> null
        }

    /**
     * Clears all InnerTube tokens (Logout).
     */
    suspend fun logout() {
        Log.i(TAG, "Logging out of InnerTube session, clearing tokens")
        settingsDataStore.setYouTubeTokens("", null)
        authCacheManager.purgeAllCaches()
    }
}
