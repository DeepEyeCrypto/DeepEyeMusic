package com.deepeye.musicpro.domain.repository

import android.net.Uri
import com.deepeye.musicpro.core.utils.LrcParser
import com.deepeye.musicpro.domain.model.Lyrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LyricsRepository @Inject constructor(
    private val httpClient: OkHttpClient
) {
    suspend fun getLyrics(
        trackName: String,
        artistName: String,
        durationMs: Long
    ): Lyrics? = withContext(Dispatchers.IO) {
        // Clean up title (remove "Official Video", etc.)
        val cleanTrack = cleanTitle(trackName)
        val cleanArtist = artistName.replace(" - Topic", "").trim()

        try {
            // Strategy 1: Direct get with duration (duration in seconds)
            val durationSec = durationMs / 1000
            val getUrl = "https://lrclib.net/api/get?track_name=${Uri.encode(
                cleanTrack
            )}&artist_name=${Uri.encode(cleanArtist)}&duration=$durationSec"

            val request1 = Request.Builder()
                .url(getUrl)
                .addHeader("User-Agent", "DeepEyeMusicPro/1.0 (https://github.com)")
                .build()

            val response1 = httpClient.newCall(request1).execute()
            if (response1.isSuccessful) {
                val body = response1.body?.string()
                if (!body.isNullOrEmpty()) {
                    val json = JSONObject(body)
                    return@withContext parseLyricsJson(json)
                }
            }

            // Strategy 2: Search with just query
            val searchUrl = "https://lrclib.net/api/search?q=${Uri.encode("$cleanTrack $cleanArtist")}"
            val request2 = Request.Builder()
                .url(searchUrl)
                .addHeader("User-Agent", "DeepEyeMusicPro/1.0 (https://github.com)")
                .build()

            val response2 = httpClient.newCall(request2).execute()
            if (response2.isSuccessful) {
                val body = response2.body?.string()
                if (!body.isNullOrEmpty()) {
                    val array = JSONArray(body)
                    if (array.length() > 0) {
                        return@withContext parseLyricsJson(array.getJSONObject(0))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    private fun parseLyricsJson(json: JSONObject): Lyrics? {
        val syncedLyrics = json.optString("syncedLyrics", "")
        if (syncedLyrics.isNotEmpty() && syncedLyrics != "null") {
            return LrcParser.parseSyncedLyrics(syncedLyrics)
        }
        val plainLyrics = json.optString("plainLyrics", "")
        if (plainLyrics.isNotEmpty() && plainLyrics != "null") {
            return LrcParser.parsePlainLyrics(plainLyrics)
        }
        return null
    }

    private fun cleanTitle(title: String): String {
        return title
            .replace(Regex("\\(.*?video.*?\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\[.*?video.*?\\]", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\(.*?audio.*?\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\[.*?audio.*?\\]", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\(.*?lyric.*?\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\[.*?lyric.*?\\]", RegexOption.IGNORE_CASE), "")
            .trim()
    }
}
