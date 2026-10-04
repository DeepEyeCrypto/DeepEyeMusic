// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.source.remote.lyrics

import android.util.Log
import com.deepeye.musicpro.core.utils.LrcParser
import com.deepeye.musicpro.domain.model.Lyrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LrclibClient @Inject constructor(
    private val client: OkHttpClient
) {
    companion object {
        private const val TAG = "LrclibClient"
        private const val BASE_URL = "https://lrclib.net/api/get"
    }

    suspend fun getLyrics(
        trackName: String,
        artistName: String,
        durationSeconds: Long = 0L
    ): Lyrics? = withContext(Dispatchers.IO) {
        if (trackName.isBlank()) return@withContext null

        try {
            val urlBuilder = BASE_URL.toHttpUrlOrNull()?.newBuilder() ?: return@withContext null
            urlBuilder.addQueryParameter("track_name", cleanTrackTitle(trackName))
            if (artistName.isNotBlank()) {
                urlBuilder.addQueryParameter("artist_name", cleanArtistName(artistName))
            }
            if (durationSeconds > 0) {
                urlBuilder.addQueryParameter("duration", durationSeconds.toString())
            }

            val request = Request.Builder()
                .url(urlBuilder.build())
                .addHeader("User-Agent", "DeepEyeMusicPro/3.0.1 (https://github.com/DeepEyeCrypto/DeepEyeMusic)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.d(TAG, "LRCLIB returned HTTP ${response.code} for '$trackName' by '$artistName'")
                    return@withContext null
                }

                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)

                val syncedLyrics = json.optString("syncedLyrics", "")
                if (syncedLyrics.isNotBlank()) {
                    return@withContext LrcParser.parseSyncedLyrics(syncedLyrics)
                }

                val plainLyrics = json.optString("plainLyrics", "")
                if (plainLyrics.isNotBlank()) {
                    return@withContext LrcParser.parsePlainLyrics(plainLyrics)
                }

                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch lyrics from LRCLIB for '$trackName'", e)
            null
        }
    }

    private fun cleanTrackTitle(title: String): String {
        return title
            .replace(Regex("(?i)\\s*\\(official\\s*(music\\s*)?video\\)"), "")
            .replace(Regex("(?i)\\s*\\[official\\s*(music\\s*)?video\\]"), "")
            .replace(Regex("(?i)\\s*\\(audio\\)"), "")
            .replace(Regex("(?i)\\s*\\[audio\\]"), "")
            .replace(Regex("(?i)\\s*\\(lyrics?\\)"), "")
            .replace(Regex("(?i)\\s*\\[lyrics?\\]"), "")
            .replace(Regex("(?i)\\s*\\(official\\s*audio\\)"), "")
            .replace(Regex("(?i)\\s*\\(visualizer\\)"), "")
            .replace(Regex("(?i)\\s*\\(4k\\s*(remaster)?\\)"), "")
            .replace(Regex("(?i)\\s*\\(hd\\)"), "")
            .trim()
    }

    private fun cleanArtistName(artist: String): String {
        return artist
            .replace(Regex("(?i)\\s*-\\s*topic$"), "")
            .replace(Regex("(?i)\\s*vevo$"), "")
            .trim()
    }
}
