// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.domain.repository

import android.net.Uri
import android.util.Log
import android.util.LruCache
import com.deepeye.musicpro.core.utils.LrcParser
import com.deepeye.musicpro.domain.model.Lyrics
import com.deepeye.musicpro.domain.model.LyricsLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * High-performance Synchronized Lyrics Repository with multi-tiered LRCLIB caching and aggressive title cleaning.
 */
@Singleton
class LyricsRepository @Inject constructor(
    private val httpClient: OkHttpClient
) {
    // In-memory LRU Cache (max 100 tracks cached)
    private val memoryCache = LruCache<String, Lyrics>(100)

    private val lyricsHttpClient = httpClient.newBuilder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    suspend fun getLyrics(
        trackName: String,
        artistName: String,
        durationMs: Long = 0L
    ): Lyrics? = withContext(Dispatchers.IO) {
        val cleanTrack = cleanTitle(trackName)
        val cleanArtist = cleanArtist(artistName)
        val cacheKey = "${cleanTrack.lowercase()}:::${cleanArtist.lowercase()}"

        // 1. Check memory cache
        memoryCache.get(cacheKey)?.let {
            Log.d("LyricsRepo", "✅ Memory Cache Hit for '$cleanTrack' by '$cleanArtist'")
            return@withContext it
        }

        try {
            // 2. Strategy A: Direct Match with duration (if duration > 0)
            if (durationMs > 10_000L) {
                val durationSec = durationMs / 1000L
                val getUrl = "https://lrclib.net/api/get?track_name=${Uri.encode(cleanTrack)}&artist_name=${Uri.encode(cleanArtist)}&duration=$durationSec"
                fetchLyricsFromUrl(getUrl)?.let { lyrics ->
                    memoryCache.put(cacheKey, lyrics)
                    Log.i("LyricsRepo", "⚡ Fetched synced lyrics via exact match for $cleanTrack")
                    return@withContext lyrics
                }
            }

            // 3. Strategy B: Search without strict duration
            val searchUrl = "https://lrclib.net/api/search?track_name=${Uri.encode(cleanTrack)}&artist_name=${Uri.encode(cleanArtist)}"
            fetchLyricsSearch(searchUrl, durationMs)?.let { lyrics ->
                memoryCache.put(cacheKey, lyrics)
                Log.i("LyricsRepo", "⚡ Fetched lyrics via track+artist search for $cleanTrack")
                return@withContext lyrics
            }

            // 4. Strategy C: Broad query search
            val queryUrl = "https://lrclib.net/api/search?q=${Uri.encode("$cleanTrack $cleanArtist")}"
            fetchLyricsSearch(queryUrl, durationMs)?.let { lyrics ->
                memoryCache.put(cacheKey, lyrics)
                Log.i("LyricsRepo", "⚡ Fetched lyrics via broad search for $cleanTrack")
                return@withContext lyrics
            }
        } catch (e: Exception) {
            Log.w("LyricsRepo", "Lyrics fetch failed for '$cleanTrack': ${e.message}")
        }

        null
    }

    private fun fetchLyricsFromUrl(url: String): Lyrics? {
        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "DeepEyeMusicPro/3.1 (Android; Material3; ExoPlayer)")
            .build()

        lyricsHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            if (body.isBlank() || body == "null") return null
            return parseLyricsJson(JSONObject(body))
        }
    }

    private fun fetchLyricsSearch(url: String, durationMs: Long): Lyrics? {
        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "DeepEyeMusicPro/3.1 (Android; Material3; ExoPlayer)")
            .build()

        lyricsHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            if (body.isBlank() || body == "null") return null

            val array = JSONArray(body)
            if (array.length() == 0) return null

            // Find best candidate: prefer synced lyrics closest in duration
            var bestObject: JSONObject? = null
            var bestScore = Int.MAX_VALUE

            for (i in 0 until array.length().coerceAtMost(5)) {
                val item = array.getJSONObject(i)
                val isSynced = item.optString("syncedLyrics", "").isNotBlank() && item.optString("syncedLyrics", "") != "null"
                val itemDurationSec = item.optDouble("duration", 0.0)

                val durationDiff = if (durationMs > 0L) {
                    Math.abs((durationMs / 1000.0) - itemDurationSec).toInt()
                } else {
                    0
                }

                val score = durationDiff + (if (isSynced) 0 else 100)
                if (score < bestScore) {
                    bestScore = score
                    bestObject = item
                }
            }

            return bestObject?.let { parseLyricsJson(it) }
        }
    }

    private fun parseLyricsJson(json: JSONObject): Lyrics? {
        val syncedLyrics = json.optString("syncedLyrics", "")
        if (syncedLyrics.isNotBlank() && syncedLyrics != "null") {
            val parsed = LrcParser.parseSyncedLyrics(syncedLyrics)
            if (parsed.lines.isNotEmpty()) return parsed
        }

        val plainLyrics = json.optString("plainLyrics", "")
        if (plainLyrics.isNotBlank() && plainLyrics != "null") {
            val parsed = LrcParser.parsePlainLyrics(plainLyrics)
            if (parsed.lines.isNotEmpty()) return parsed
        }

        return null
    }

    private fun cleanTitle(title: String): String {
        return title
            .replace(Regex("(?i)\\(.*?official.*?\\)"), "")
            .replace(Regex("(?i)\\[.*?official.*?\\]"), "")
            .replace(Regex("(?i)\\(.*?video.*?\\)"), "")
            .replace(Regex("(?i)\\[.*?video.*?\\]"), "")
            .replace(Regex("(?i)\\(.*?audio.*?\\)"), "")
            .replace(Regex("(?i)\\[.*?audio.*?\\]"), "")
            .replace(Regex("(?i)\\(.*?lyric.*?\\)"), "")
            .replace(Regex("(?i)\\[.*?lyric.*?\\]"), "")
            .replace(Regex("(?i)\\(.*?remastered.*?\\)"), "")
            .replace(Regex("(?i)\\[.*?remastered.*?\\]"), "")
            .replace(Regex("(?i)\\(.*?4k.*?\\)"), "")
            .replace(Regex("(?i)\\[.*?4k.*?\\]"), "")
            .replace(Regex("(?i)\\(.*?hd.*?\\)"), "")
            .replace(Regex("(?i)\\[.*?hd.*?\\]"), "")
            .replace(Regex("(?i)feat\\..*"), "")
            .replace(Regex("(?i)ft\\..*"), "")
            .trim()
    }

    private fun cleanArtist(artist: String): String {
        return artist
            .replace(" - Topic", "", ignoreCase = true)
            .replace("VEVO", "", ignoreCase = true)
            .replace(Regex("(?i),.*"), "")
            .replace(Regex("(?i)&.*"), "")
            .trim()
    }
}
