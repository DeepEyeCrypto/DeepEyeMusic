// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.account

import com.deepeye.musicpro.data.prefs.SettingsDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Derives the account connection state directly from SettingsDataStore.
 *
 * This implementation enforces a strict privacy boundary: raw tokens stored inside
 * SettingsDataStore are consumed here and never passed through to the domain model.
 * The [accountKey] is built as a stable SHA-256 hash of the refresh token.
 */
@Singleton
class SettingsAccountDataSourceImpl @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : AccountDataSource {

    override val connectedStream: Flow<ConnectedAccountSnapshot?> =
        settingsDataStore.settings.map { settings ->
            if (!settings.youtubeAccessToken.isNullOrBlank()) {
                val accountKey = deriveStableAccountKey(
                    settings.youtubeRefreshToken, 
                    settings.youtubeAccessToken
                )
                // Existing SettingsDataStore has no profile data
                ConnectedAccountSnapshot(
                    accountKey = accountKey,
                    displayName = null,
                    avatarUrl = null,
                    channelKey = null,
                )
            } else {
                null
            }
        }.distinctUntilChanged()

    /**
     * Creates an opaque, privacy-safe key for caching.
     * Uses the refresh token if available because it rarely changes across access token
     * rotations, guaranteeing a stable ID for cache invalidation.
     */
    private fun deriveStableAccountKey(refreshToken: String?, accessToken: String): String {
        val seed = if (!refreshToken.isNullOrBlank()) refreshToken else accessToken
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(seed.toByteArray())
            digest.fold("") { str, it -> str + "%02x".format(it) }
        } catch (_: Exception) {
            // Fallback that still hides the raw token if hashing fails
            seed.hashCode().toString()
        }
    }
}