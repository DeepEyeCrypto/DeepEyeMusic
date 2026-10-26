// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.source.remote.youtube

import android.util.Log
import com.deepeye.musicpro.core.utils.LrcParser
import com.deepeye.musicpro.domain.auth.InnerTubeAuthManager
import com.deepeye.musicpro.domain.model.Lyrics
import com.deepeye.musicpro.domain.model.LyricsLine
import com.deepeye.musicpro.domain.model.home.HomeVideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Autoplay recommendation track extracted from YouTube's native InnerTube algorithmic next endpoint.
 */
data class AutoplayTrack(
    val videoId: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val durationSeconds: Long
)

/**
 * Thrown when an authenticated InnerTube request fails OAuth verification.
 */
class InnerTubeAuthException(message: String) : Exception(message)

/**
 * ACCOUNT-MUSIC-OMEGA: typed failure reasons for account-only music shelves.
 * The UI may render one common unavailable screen, but the reason is retained
 * for retry eligibility and diagnostics. String form never exposes tokens,
 * raw payloads, account/content/playlist IDs, titles, or stream URLs.
 */
enum class MusicShelfFailureReason {
    /** Auth expired/invalid -> use the existing refresh/reauth path. */
    REAUTH_REQUIRED,

    /** Transport failure (network/timeout) -> retryable, may use labelled cache. */
    NETWORK,

    /** Response body missing or structurally invalid -> retryable, no cache. */
    INVALID_RESPONSE,

    /** No usable music items in a valid response -> honest empty, no filler. */
    EMPTY_ACCOUNT_CONTENT,

    /** Provider surface definitively unavailable. */
    PROVIDER_UNAVAILABLE,
}

/**
 * ACCOUNT-MUSIC-OMEGA: typed unavailable outcome for the account-only music
 * home surface. Carries the retained [reason] and the transport detail
 * category only (HTTP status code or a sanitized `io`/`timeout` marker —
 * never tokens, payloads, account/content IDs, titles, or URLs).
 */
class InnerTubeMusicUnavailableException(
    val reason: MusicShelfFailureReason,
    message: String,
    val httpStatus: Int? = null,
) : Exception(message)

/**
 * YT-MUSIC-OMEGA: a single titled shelf from the authenticated YouTube Music home
 * feed (`FEmusic_home`), e.g. "Listen again", "Quick picks", "Mixed for you". Preserves
 * the carousel title so the Music tab can render one rail per shelf.
 */
data class MusicShelf(
    val title: String,
    val items: List<HomeVideoItem>,
)

/**
 * SmartTube-Omega Native InnerTube Remote Client.
 *
 * Communicates directly with YouTube's internal `youtubei/v1` API endpoints (/browse, /next, /search, /player)
 * using client spoofing (ANDROID_MUSIC / TVHTML5) and OAuth Bearer tokens to retrieve
 * authentic 1:1 personalization and native AutoPlay streams.
 */
@Singleton
class InnerTubeRemoteClient @Inject constructor(
    private val client: OkHttpClient,
    private val authManager: InnerTubeAuthManager
) {
    companion object {
        const val TAG = "InnerTubeRemoteClient"
        const val AUTH_TAG = "InnerTubeAuth"
        const val YOUTUBE_MUSIC_INNERTUBE = "https://music.youtube.com/youtubei/v1/"
        const val YOUTUBE_MAIN_INNERTUBE = "https://www.youtube.com/youtubei/v1/"

        const val ANDROID_MUSIC_CLIENT_VERSION = "6.42.52"
        const val TVHTML5_CLIENT_VERSION = "7.20210614.03.00"

        val ANDROID_MUSIC_CONTEXT = """
            "context": {
              "client": {
                "clientName": "ANDROID_MUSIC",
                "clientVersion": "$ANDROID_MUSIC_CLIENT_VERSION",
                "hl": "en",
                "gl": "IN"
              },
              "user": {
                "enableSafetyMode": false,
                "lockedSafetyMode": false
              }
            }
        """.trimIndent()

        val TVHTML5_CONTEXT = """
            "context": {
              "client": {
                "clientName": "TVHTML5",
                "clientVersion": "$TVHTML5_CLIENT_VERSION",
                "hl": "en",
                "gl": "IN"
              },
              "user": {
                "enableSafetyMode": false,
                "lockedSafetyMode": false
              }
            }
        """.trimIndent()
    }

    /**
     * Fetches YouTube's exact native AutoPlay next track for [videoId] via the /youtubei/v1/next endpoint.
     */
    suspend fun fetchNextAutoplay(videoId: String, playlistId: String? = null): AutoplayTrack? = withContext(Dispatchers.IO) {
        if (videoId.isBlank()) return@withContext null

        val token = authManager.getAccessToken()
        val playlistPart = if (!playlistId.isNullOrBlank()) ", \"playlistId\": \"$playlistId\"" else ""
        val payload = "{$ANDROID_MUSIC_CONTEXT, \"videoId\": \"$videoId\"$playlistPart}"

        try {
            val reqBuilder = Request.Builder()
                .url("${YOUTUBE_MUSIC_INNERTUBE}next")
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", "com.google.android.apps.youtube.music/$ANDROID_MUSIC_CLIENT_VERSION (Linux; U; Android 14)")
                .addHeader("X-YouTube-Client-Name", "67")
                .addHeader("X-YouTube-Client-Version", ANDROID_MUSIC_CLIENT_VERSION)
                .post(payload.toRequestBody("application/json".toMediaType()))

            if (!token.isNullOrBlank()) {
                reqBuilder.addHeader("Authorization", "Bearer $token")
            }

            client.newCall(reqBuilder.build()).execute().use { response ->
                if (response.code == 401 && !token.isNullOrBlank()) {
                    Log.w(AUTH_TAG, "[InnerTube Auth-Rescue] 401 on /next, refreshing token...")
                    val refreshedToken = authManager.refreshAccessToken()
                    if (!refreshedToken.isNullOrBlank()) {
                        val retryReq = reqBuilder.header("Authorization", "Bearer $refreshedToken").build()
                        client.newCall(retryReq).execute().use { retryRes ->
                            if (retryRes.isSuccessful) {
                                val bodyStr = retryRes.body?.string() ?: return@withContext null
                                val json = JSONObject(bodyStr)
                                val track = parseAutoplayFromJson(json, currentVideoId = videoId)
                                if (track != null) return@withContext track
                            }
                        }
                    }
                }

                if (!response.isSuccessful) {
                    Log.w(TAG, "Autoplay /next returned HTTP ${response.code}, trying fallback client")
                    return@withContext fetchNextAutoplayFallback(videoId)
                }

                val bodyStr = response.body?.string() ?: return@withContext null
                val json = JSONObject(bodyStr)

                // Check direct autoplayEndpoint inside musicQueueRenderer or watchNextFeed
                val track = parseAutoplayFromJson(json, currentVideoId = videoId)
                if (track != null) {
                    // Autoplay tracks are algorithmic recommendations tied to the
                    // account's history — title/artist are never logged. Diagnostic
                    // builds may log the videoId only, gated off in release.
                    if (com.deepeye.musicpro.diagnostics.network.NetworkDiagnostics.isEnabled) {
                        Log.i(TAG, "Resolved AutoPlay track videoId=${track.videoId} (title/artist redacted)")
                    }
                    return@withContext track
                }

                return@withContext fetchNextAutoplayFallback(videoId)
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Req 1: cancellation propagates — never convert to fallback/empty.
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching InnerTube /next for video $videoId", e)
            return@withContext fetchNextAutoplayFallback(videoId)
        }
    }

    /**
     * Fallback to main YouTube TVHTML5 /next endpoint if Music InnerTube returns empty queue.
     */
    private suspend fun fetchNextAutoplayFallback(videoId: String): AutoplayTrack? = withContext(Dispatchers.IO) {
        val token = authManager.getAccessToken()
        val payload = "{$TVHTML5_CONTEXT, \"videoId\": \"$videoId\"}"

        try {
            val reqBuilder = Request.Builder()
                .url("${YOUTUBE_MAIN_INNERTUBE}next")
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", "Mozilla/5.0 (SMART-TV; Linux; Tizen 6.0) SamsungBrowser/4.0 TV Safari/537.36")
                .addHeader("X-YouTube-Client-Name", "85")
                .addHeader("X-YouTube-Client-Version", TVHTML5_CLIENT_VERSION)
                .post(payload.toRequestBody("application/json".toMediaType()))

            if (!token.isNullOrBlank()) {
                reqBuilder.addHeader("Authorization", "Bearer $token")
            }

            client.newCall(reqBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val bodyStr = response.body?.string() ?: return@withContext null
                val json = JSONObject(bodyStr)
                return@withContext parseAutoplayFromJson(json, currentVideoId = videoId)
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Req 1: cancellation propagates — never convert to null/fallback.
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error in fallback InnerTube /next", e)
            null
        }
    }

    /**
     * Fetches YouTube Music synchronized or plain lyrics for [videoId] via InnerTube /next -> /browse.
     */
    suspend fun fetchLyrics(videoId: String): Lyrics? = withContext(Dispatchers.IO) {
        if (videoId.isBlank()) return@withContext null

        val token = authManager.getAccessToken()
        val payload = "{$ANDROID_MUSIC_CONTEXT, \"videoId\": \"$videoId\"}"

        try {
            val reqBuilder = Request.Builder()
                .url("${YOUTUBE_MUSIC_INNERTUBE}next")
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", "com.google.android.apps.youtube.music/$ANDROID_MUSIC_CLIENT_VERSION (Linux; U; Android 14)")
                .addHeader("X-YouTube-Client-Name", "67")
                .addHeader("X-YouTube-Client-Version", ANDROID_MUSIC_CLIENT_VERSION)
                .post(payload.toRequestBody("application/json".toMediaType()))

            if (!token.isNullOrBlank()) {
                reqBuilder.addHeader("Authorization", "Bearer $token")
            }

            var lyricsBrowseId: String? = null
            client.newCall(reqBuilder.build()).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: return@withContext null
                    val json = JSONObject(bodyStr)
                    lyricsBrowseId = extractLyricsBrowseId(json)
                }
            }

            val targetBrowseId = lyricsBrowseId
            if (!targetBrowseId.isNullOrBlank()) {
                Log.d(TAG, "Found lyrics browseId=$targetBrowseId for videoId=$videoId")
                return@withContext fetchLyricsFromBrowse(targetBrowseId)
            }
            null
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Req 1: cancellation propagates — never convert to null/fallback.
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching InnerTube lyrics for $videoId", e)
            null
        }
    }

    private fun extractLyricsBrowseId(json: JSONObject): String? {
        val tabs = json.optJSONObject("contents")
            ?.optJSONObject("singleColumnMusicWatchNextResultsRenderer")
            ?.optJSONObject("tabbedRenderer")
            ?.optJSONObject("watchNextTabbedResultsRenderer")
            ?.optJSONArray("tabs") ?: return null

        for (i in 0 until tabs.length()) {
            val tab = tabs.optJSONObject(i)?.optJSONObject("tabRenderer") ?: continue
            val title = tab.optString("title", "")
            val endpoint = tab.optJSONObject("endpoint")
            val browseId = endpoint?.optJSONObject("browseEndpoint")?.optString("browseId")

            if (title.equals("Lyrics", ignoreCase = true) || (browseId != null && browseId.startsWith("MPLY"))) {
                return browseId
            }
        }
        return null
    }

    private suspend fun fetchLyricsFromBrowse(browseId: String): Lyrics? = withContext(Dispatchers.IO) {
        val token = authManager.getAccessToken()
        val payload = "{$ANDROID_MUSIC_CONTEXT, \"browseId\": \"$browseId\"}"

        try {
            val reqBuilder = Request.Builder()
                .url("${YOUTUBE_MUSIC_INNERTUBE}browse")
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", "com.google.android.apps.youtube.music/$ANDROID_MUSIC_CLIENT_VERSION (Linux; U; Android 14)")
                .addHeader("X-YouTube-Client-Name", "67")
                .addHeader("X-YouTube-Client-Version", ANDROID_MUSIC_CLIENT_VERSION)
                .post(payload.toRequestBody("application/json".toMediaType()))

            if (!token.isNullOrBlank()) {
                reqBuilder.addHeader("Authorization", "Bearer $token")
            }

            client.newCall(reqBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Lyrics /browse returned HTTP ${response.code} for $browseId")
                    return@withContext null
                }
                val bodyStr = response.body?.string() ?: return@withContext null
                val json = JSONObject(bodyStr)
                return@withContext parseLyricsBrowseResponse(json)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving lyrics browse payload for $browseId", e)
            null
        }
    }

    private fun parseLyricsBrowseResponse(json: JSONObject): Lyrics? {
        val sectionList = json.optJSONObject("contents")
            ?.optJSONObject("sectionListRenderer")
            ?.optJSONArray("contents")

        if (sectionList != null) {
            for (i in 0 until sectionList.length()) {
                val section = sectionList.optJSONObject(i) ?: continue

                // Strategy 1: musicTimedLyricsRenderer (Synchronized/Karaoke Timestamps)
                val musicTimedLyricsRenderer = section.optJSONObject("musicTimedLyricsRenderer")
                if (musicTimedLyricsRenderer != null) {
                    val timedLyricsData = musicTimedLyricsRenderer.optJSONArray("timedLyricsData")
                    if (timedLyricsData != null && timedLyricsData.length() > 0) {
                        val lines = mutableListOf<LyricsLine>()
                        for (j in 0 until timedLyricsData.length()) {
                            val lineObj = timedLyricsData.optJSONObject(j) ?: continue
                            val lyricLine = lineObj.optString("lyricLine", "").trim()
                            val cueRange = lineObj.optJSONObject("cueRange")
                            val startMs = cueRange?.optLong("startTimeMilliseconds") ?: 0L
                            if (lyricLine.isNotEmpty()) {
                                lines.add(LyricsLine(startMs, lyricLine))
                            }
                        }
                        if (lines.isNotEmpty()) {
                            Log.i(TAG, "Parsed ${lines.size} synchronized lyrics lines from InnerTube")
                            return Lyrics(lines = lines.sortedBy { it.timestampMs }, isSynced = true)
                        }
                    }
                }

                // Strategy 2: musicDescriptionShelfRenderer (Plain Text Lyrics)
                val shelfRenderer = section.optJSONObject("musicDescriptionShelfRenderer")
                if (shelfRenderer != null) {
                    val runs = shelfRenderer.optJSONObject("description")?.optJSONArray("runs")
                    if (runs != null && runs.length() > 0) {
                        val fullText = StringBuilder()
                        for (j in 0 until runs.length()) {
                            fullText.append(runs.optJSONObject(j)?.optString("text", "") ?: "")
                        }
                        val plain = fullText.toString().trim()
                        if (plain.isNotEmpty()) {
                            Log.i(TAG, "Parsed plain text lyrics from InnerTube shelfRenderer")
                            return LrcParser.parsePlainLyrics(plain)
                        }
                    }
                }
            }
        }
        return null
    }

    /**
     * Parses the next video ID and metadata out of an InnerTube /next response tree.
     */
    private fun parseAutoplayFromJson(json: JSONObject, currentVideoId: String): AutoplayTrack? {
        // Strategy A: Music Queue Renderer (Playlist Panel)
        val contents = json.optJSONObject("contents")
            ?.optJSONObject("singleColumnMusicWatchNextResultsRenderer")
            ?.optJSONObject("tabbedRenderer")
            ?.optJSONObject("watchNextTabbedResultsRenderer")
            ?.optJSONArray("tabs")
            ?.optJSONObject(0)
            ?.optJSONObject("tabRenderer")
            ?.optJSONObject("content")
            ?.optJSONObject("musicQueueRenderer")
            ?.optJSONObject("content")
            ?.optJSONObject("playlistPanelRenderer")
            ?.optJSONArray("contents")

        if (contents != null && contents.length() > 0) {
            for (i in 0 until contents.length()) {
                val panelItem = contents.optJSONObject(i)?.optJSONObject("playlistPanelVideoRenderer") ?: continue
                val nextId = panelItem.optString("videoId")
                if (nextId.isNotBlank() && nextId != currentVideoId) {
                    val title = runsText(panelItem.optJSONObject("title"))
                    val artist = runsText(panelItem.optJSONObject("longBylineText") ?: panelItem.optJSONObject("shortBylineText"))
                    val thumb = extractThumbnail(panelItem.optJSONObject("thumbnail"))
                    val durationStr = runsText(panelItem.optJSONObject("lengthText"))
                    return AutoplayTrack(
                        videoId = nextId,
                        title = title.ifBlank { "Next Track" },
                        artist = artist.ifBlank { "YouTube Music" },
                        thumbnailUrl = thumb,
                        durationSeconds = parseDuration(durationStr)
                    )
                }
            }
        }

        // Strategy B: Standard Watch Next Autoplay Endpoint
        val autoplayNav = json.optJSONObject("playerOverlays")
            ?.optJSONObject("playerOverlayRenderer")
            ?.optJSONObject("autoplay")
            ?.optJSONObject("playerOverlayAutoplayRenderer")

        if (autoplayNav != null) {
            val nextId = autoplayNav.optJSONObject("videoEndpoint")
                ?.optJSONObject("watchEndpoint")
                ?.optString("videoId")

            if (!nextId.isNullOrBlank() && nextId != currentVideoId) {
                val title = runsText(autoplayNav.optJSONObject("videoTitle"))
                val artist = runsText(autoplayNav.optJSONObject("byline"))
                val thumb = extractThumbnail(autoplayNav.optJSONObject("thumbnailRenderer"))
                return AutoplayTrack(
                    videoId = nextId,
                    title = title.ifBlank { "Next Recommendation" },
                    artist = artist.ifBlank { "YouTube" },
                    thumbnailUrl = thumb,
                    durationSeconds = 0L
                )
            }
        }

        // Strategy C: Secondary Results (Two Column Watch Next)
        val secondaryItems = json.optJSONObject("contents")
            ?.optJSONObject("twoColumnWatchNextResults")
            ?.optJSONObject("secondaryResults")
            ?.optJSONObject("secondaryResults")
            ?.optJSONArray("results")

        if (secondaryItems != null && secondaryItems.length() > 0) {
            for (i in 0 until secondaryItems.length()) {
                val compactVideo = secondaryItems.optJSONObject(i)?.optJSONObject("compactVideoRenderer") ?: continue
                val nextId = compactVideo.optString("videoId")
                if (nextId.isNotBlank() && nextId != currentVideoId) {
                    val title = runsText(compactVideo.optJSONObject("title"))
                    val artist = runsText(compactVideo.optJSONObject("shortBylineText"))
                    val thumb = extractThumbnail(compactVideo.optJSONObject("thumbnail"))
                    val durationStr = runsText(compactVideo.optJSONObject("lengthText"))
                    return AutoplayTrack(
                        videoId = nextId,
                        title = title.ifBlank { "Next Video" },
                        artist = artist.ifBlank { "YouTube" },
                        thumbnailUrl = thumb,
                        durationSeconds = parseDuration(durationStr)
                    )
                }
            }
        }

        return null
    }

    /**
     * Authenticated /browse query on YouTube Music endpoint for personalized home mixes, liked songs, and shelves.
     */
    suspend fun browseMusic(browseId: String = "FEmusic_home", params: String? = null): List<HomeVideoItem> = withContext(Dispatchers.IO) {
        val token = authManager.getAccessToken()
        try {
            val bodyStr = postMusicBrowse(browseId, params)
            if (bodyStr.isBlank()) return@withContext emptyList()
            val items = parseBrowseSections(JSONObject(bodyStr))
            auditBrowseTelemetry(browseId, token, bodyStr, items)
            return@withContext items
        } catch (e: InnerTubeAuthException) {
            throw e
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Req 1: cancellation propagates — never convert to emptyList.
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error in InnerTube /browse for browseId=$browseId", e)
            if (!token.isNullOrBlank()) {
                Log.w(AUTH_TAG, "Authenticated browse failure for $browseId: ${e.message}")
            }
            emptyList()
        }
    }

    /**
     * Shared ANDROID_MUSIC `/browse` POST with OAuth Bearer injection and a single
     * 401 refresh-and-retry. Returns the raw JSON body, or "" on a non-auth failure.
     * Throws [InnerTubeAuthException] when an authenticated request is rejected.
     *
     * Extracted so both [browseMusic] (flattened items) and [fetchMusicHomeShelves]
     * (shelf-structured) drive the exact same authenticated request path.
     */
    private suspend fun postMusicBrowse(browseId: String, params: String?): String {
        val token = authManager.getAccessToken()
        val paramsPart = if (params != null) ", \"params\": \"$params\"" else ""
        val payload = "{$ANDROID_MUSIC_CONTEXT, \"browseId\": \"$browseId\"$paramsPart}"

        val reqBuilder = Request.Builder()
            .url("${YOUTUBE_MUSIC_INNERTUBE}browse")
            .addHeader("Content-Type", "application/json")
            .addHeader("User-Agent", "com.google.android.apps.youtube.music/$ANDROID_MUSIC_CLIENT_VERSION (Linux; U; Android 14)")
            .addHeader("X-YouTube-Client-Name", "67")
            .addHeader("X-YouTube-Client-Version", ANDROID_MUSIC_CLIENT_VERSION)
            .post(payload.toRequestBody("application/json".toMediaType()))

        if (!token.isNullOrBlank()) {
            reqBuilder.addHeader("Authorization", "Bearer $token")
        }

        client.newCall(reqBuilder.build()).execute().use { response ->
            if (response.code == 401 && !token.isNullOrBlank()) {
                Log.w(AUTH_TAG, "[InnerTube Auth-Rescue] 401 on /browse ($browseId), refreshing token...")
                val refreshedToken = authManager.refreshAccessToken()
                if (!refreshedToken.isNullOrBlank()) {
                    val retryReq = reqBuilder.header("Authorization", "Bearer $refreshedToken").build()
                    client.newCall(retryReq).execute().use { retryRes ->
                        if (retryRes.isSuccessful) {
                            return retryRes.body?.string() ?: ""
                        }
                    }
                }
                throw InnerTubeAuthException("InnerTube authenticated browse failed (401 Unauthorized) for $browseId")
            }

            if (!response.isSuccessful) {
                Log.e(AUTH_TAG, "[InnerTube Auth-Rescue] /browse failed for $browseId with HTTP ${response.code}")
                if (!token.isNullOrBlank() && (response.code == 401 || response.code == 403)) {
                    throw InnerTubeAuthException("InnerTube browse HTTP ${response.code} for $browseId")
                }
                return ""
            }

            return response.body?.string() ?: ""
        }
    }

    /**
     * Shared TVHTML5 `/browse` POST with OAuth Bearer injection and a single
     * 401 refresh-and-retry. Returns the raw JSON body, or "" on a non-auth failure.
     */
    private suspend fun postMainBrowse(browseId: String, params: String? = null): String {
        val token = authManager.getAccessToken()
        val paramsPart = if (params != null) ", \"params\": \"$params\"" else ""
        val payload = "{$TVHTML5_CONTEXT, \"browseId\": \"$browseId\"$paramsPart}"

        val reqBuilder = Request.Builder()
            .url("${YOUTUBE_MAIN_INNERTUBE}browse")
            .addHeader("Content-Type", "application/json")
            .addHeader("User-Agent", "Mozilla/5.0 (SMART-TV; Linux; Tizen 5.0) AppleWebKit/538.1 (KHTML, like Gecko) Version/5.0 NativeTVAds Safari/538.1,gzip(gfe)")
            .addHeader("X-YouTube-Client-Name", "85")
            .addHeader("X-YouTube-Client-Version", TVHTML5_CLIENT_VERSION)
            .post(payload.toRequestBody("application/json".toMediaType()))

        if (!token.isNullOrBlank()) {
            reqBuilder.addHeader("Authorization", "Bearer $token")
        }

        client.newCall(reqBuilder.build()).execute().use { response ->
            if (response.code == 401 && !token.isNullOrBlank()) {
                Log.w(AUTH_TAG, "[InnerTube Auth-Rescue] 401 on TV /browse ($browseId), refreshing token...")
                val refreshedToken = authManager.refreshAccessToken()
                if (!refreshedToken.isNullOrBlank()) {
                    val retryReq = reqBuilder.header("Authorization", "Bearer $refreshedToken").build()
                    client.newCall(retryReq).execute().use { retryRes ->
                        if (retryRes.isSuccessful) {
                            return retryRes.body?.string() ?: ""
                        }
                    }
                }
                throw InnerTubeAuthException("InnerTube TV browse failed (401 Unauthorized) for $browseId")
            }

            if (!response.isSuccessful) {
                Log.e(AUTH_TAG, "[InnerTube Auth-Rescue] Main /browse failed for $browseId with HTTP ${response.code}")
                if (!token.isNullOrBlank() && (response.code == 401 || response.code == 403)) {
                    throw InnerTubeAuthException("InnerTube TV browse HTTP ${response.code} for $browseId")
                }
                return ""
            }

            return response.body?.string() ?: ""
        }
    }

    /**
     * Authenticated /browse query on Main YouTube (TVHTML5) for history, subscriptions, and what to watch.
     */
    suspend fun browseMain(browseId: String = "FEwhat_to_watch", params: String? = null): List<HomeVideoItem> = withContext(Dispatchers.IO) {
        val token = authManager.getAccessToken()
        try {
            val bodyStr = postMainBrowse(browseId, params)
            if (bodyStr.isBlank()) return@withContext emptyList()
            val items = parseBrowseSections(JSONObject(bodyStr))
            auditBrowseTelemetry(browseId, token, bodyStr, items)
            items
        } catch (e: InnerTubeAuthException) {
            throw e
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Req 1: cancellation propagates — never convert to emptyList.
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error in Main InnerTube /browse for browseId=$browseId", e)
            emptyList()
        }
    }

    /**
     * Dedicated browse alias for Liked Music (FEmusic_liked).
     */
    suspend fun browseLikedMusic(): List<HomeVideoItem> = browseMusic("FEmusic_liked")

    /**
     * Dedicated browse alias for History (FEhistory).
     */
    suspend fun browseHistory(): List<HomeVideoItem> = browseMain("FEhistory")

    /**
     * Dedicated browse alias for Subscriptions (FEsubscriptions).
     */
    suspend fun browseSubscriptions(): List<HomeVideoItem> = browseMain("FEsubscriptions")

    /**
     * Dedicated browse alias for Liked Videos (VLLL).
     */
    suspend fun browseLikedVideos(): List<HomeVideoItem> {
        val ytLiked = browseMain("VLLL")
        if (ytLiked.isNotEmpty()) return ytLiked
        return browseMusic("FEmusic_liked")
    }

    /**
     * Mantis Reflect telemetry logger: verifies authenticated user personalization signatures.
     */
    private fun auditBrowseTelemetry(browseId: String, token: String?, bodyStr: String, items: List<HomeVideoItem>) {
        // Mantis Reflect is a developer diagnostic, never a release metric. The
        // personalized /browse body carries the signed-in user's private history
        // (video titles, channels, IDs) which the secret-key redactor does NOT
        // strip — so the whole logger is gated behind the diagnostic switch and
        // emits nothing in release builds.
        if (!com.deepeye.musicpro.diagnostics.network.NetworkDiagnostics.isEnabled) return

        // Presence-only signals: never the token, and never the raw body (a
        // personalized body is account history even after secret redaction).
        // Body size only — no content.
        Log.i(AUTH_TAG, "[InnerTube Auth-Rescue] /browse AuthTokenPresent=${!token.isNullOrBlank()} ItemsParsed=${items.size} BodyBytes=${bodyStr.length}")

        if (browseId == "FEmusic_home") {
            val personalSignatures = listOf(
                "Mixed for you", "Listen again", "Quick picks", "Forgotten favorites",
                "Similar to", "Your recap", "From your library", "Trending", "Recommended"
            )
            val detected = personalSignatures.filter { bodyStr.contains(it, ignoreCase = true) }
            // Shelf *names* are static, non-account strings (e.g. "Quick picks")
            // and are safe. Item titles/channels are account history — never logged.
            Log.i(AUTH_TAG, "[InnerTube-Personalized] Detected Shelves: ${if (detected.isNotEmpty()) detected.joinToString(", ") else "None (Guest feed or custom shelves)"}")

            // Deliberately NOT logging track titles or channel names: they are
            // tied to the signed-in user's private history. Count only.
            Log.i(AUTH_TAG, "[InnerTube-Personalized] ItemsParsed: ${items.size} (titles redacted)")
        }
    }

    /**
     * YT-MUSIC-OMEGA: fetch the authenticated YouTube Music home feed (`FEmusic_home`)
     * as titled shelves using the ANDROID_MUSIC client context and the real OAuth Bearer
     * token. Each [MusicShelf] preserves its carousel title (e.g. "Listen again",
     * "Quick picks", "Mixed for you") so the Music tab can render one rail per shelf
     * instead of a single flattened list.
     */
    suspend fun fetchMusicHomeShelves(): List<MusicShelf> = withContext(Dispatchers.IO) {
        val token = authManager.getAccessToken()
        // ACCOUNT-MUSIC-OMEGA: block an unauthenticated music-home request. A
        // tokenless /browse returns a generic guest feed (HTTP 200) which must
        // never be rendered as account-context shelves. No network call here.
        if (token.isNullOrBlank()) {
            Log.w(AUTH_TAG, "[YT-MUSIC] FEmusic_home blocked reason=no_account_token")
            throw InnerTubeAuthException("YouTube Music home requires a connected account token")
        }
        try {
            var bodyStr = try {
                postMusicBrowse("FEmusic_home", null)
            } catch (e: InnerTubeAuthException) {
                // Req 1+4: auth failures travel the auth path, never unavailable.
                throw e
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Req 1: cancellation propagates — it is not an error state.
                throw e
            } catch (e: java.io.IOException) {
                // Req 2: transport failures -> typed unavailable, retryable.
                Log.e(AUTH_TAG, "[YT-MUSIC] FEmusic_home unavailable reason=network")
                throw InnerTubeMusicUnavailableException(
                    MusicShelfFailureReason.NETWORK,
                    "YouTube Music home request failed",
                )
            }

            var shelves = if (bodyStr.isNotBlank()) {
                try {
                    parseMusicCarouselShelves(JSONObject(bodyStr))
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }

            // TVHTML5 fallback for device-flow OAuth tokens:
            // Device authorization tokens (RFC 8628 via youtube.com/activate) are issued
            // for YouTube TV / Living Room clients. music.youtube.com rejects them with HTTP 400.
            // On www.youtube.com, the TV client receives authentic account music shelves via FEtopics_music.
            if (shelves.isEmpty()) {
                Log.i(AUTH_TAG, "[YT-MUSIC] FEmusic_home empty or rejected; attempting TV Living Room music browse (FEtopics_music)...")
                val tvBody = try {
                    postMainBrowse("FEtopics_music", null)
                } catch (e: InnerTubeAuthException) {
                    throw e
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: java.io.IOException) {
                    Log.e(AUTH_TAG, "[YT-MUSIC] FEtopics_music unavailable reason=network")
                    throw InnerTubeMusicUnavailableException(
                        MusicShelfFailureReason.NETWORK,
                        "YouTube Music TV browse request failed",
                    )
                } catch (e: Exception) {
                    ""
                }
                if (tvBody.isNotBlank()) {
                    shelves = try {
                        parseMusicCarouselShelves(JSONObject(tvBody))
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
            }

            if (shelves.isEmpty()) {
                // Req 3: missing required body -> INVALID_RESPONSE (not empty).
                Log.e(AUTH_TAG, "[YT-MUSIC] FEmusic_home unavailable reason=empty_browse_body")
                throw InnerTubeMusicUnavailableException(
                    MusicShelfFailureReason.INVALID_RESPONSE,
                    "YouTube Music home browse returned no data",
                )
            }

            Log.i(
                AUTH_TAG,
                "[YT-MUSIC] FEmusic_home shelves=${shelves.size} " +
                    "authTokenPresent=${!token.isNullOrBlank()}"
            )
            shelves
        } catch (e: InnerTubeAuthException) {
            throw e
        } catch (e: InnerTubeMusicUnavailableException) {
            throw e
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Req 1: never convert cancellation into an unavailable outcome.
            throw e
        } catch (e: Exception) {
            // Req 2: narrow residual guard — programming errors only reach here;
            // network/parse/body failures are already typed above. Mark
            // provider-unavailable so the UI stays honest without a filler.
            Log.e(TAG, "Error fetching FEmusic_home shelves reason=provider_unavailable", e)
            throw InnerTubeMusicUnavailableException(
                MusicShelfFailureReason.PROVIDER_UNAVAILABLE,
                "YouTube Music home failed unexpectedly",
            )
        }
    }

    /**
     * Walks a YouTube Music `/browse` payload and groups items into titled shelves.
     * Handles both grid carousels (`musicCarouselShelfRenderer`), list shelves
     * (`musicShelfRenderer`), and TV shelves (`shelfRenderer`), reusing [parseAnyItemRenderer]
     * for each child so videoId, title, artist and thumbnail extraction stays identical.
     */
    private fun parseMusicCarouselShelves(json: JSONObject): List<MusicShelf> {
        val shelves = mutableListOf<MusicShelf>()

        fun collectShelf(title: String, contents: org.json.JSONArray?) {
            if (title.isBlank() || contents == null) return
            val items = mutableListOf<HomeVideoItem>()
            val seen = mutableSetOf<String>()
            for (i in 0 until contents.length()) {
                val child = contents.optJSONObject(i) ?: continue
                parseAnyItemRenderer(child)?.let { parsed ->
                    if (seen.add(parsed.id)) items.add(parsed)
                }
            }
            if (items.isNotEmpty()) shelves.add(MusicShelf(title = title, items = items))
        }

        fun walk(node: Any?) {
            when (node) {
                is JSONObject -> {
                    node.optJSONObject("musicCarouselShelfRenderer")?.let { carousel ->
                        val header = carousel.optJSONObject("header")
                        val title = header?.optJSONObject("musicCarouselShelfBasicHeaderRenderer")
                            ?.optJSONObject("title")?.let { runsText(it) }
                            ?: header?.optJSONObject("musicResponsiveHeaderRenderer")
                                ?.optJSONObject("title")?.let { runsText(it) }
                            ?: ""
                        // YT Music carousels nest items under contents.items (newer) or
                        // expose contents directly as an array (older shapes).
                        val contentsArray = carousel.optJSONObject("contents")?.optJSONArray("items")
                            ?: carousel.optJSONArray("contents")
                        collectShelf(title, contentsArray)
                    }
                    node.optJSONObject("musicShelfRenderer")?.let { shelf ->
                        val title = shelf.optJSONObject("title")?.let { runsText(it) } ?: ""
                        collectShelf(title, shelf.optJSONArray("contents"))
                    }
                    node.optJSONObject("shelfRenderer")?.let { shelf ->
                        val header = shelf.optJSONObject("headerRenderer")?.optJSONObject("shelfHeaderRenderer")
                        val title = header?.optJSONObject("title")?.let { runsText(it) }
                            ?: header?.optJSONObject("avatarLockup")?.optJSONObject("avatarLockupRenderer")?.optJSONObject("title")?.let { runsText(it) }
                            ?: ""
                        val contentsArray = shelf.optJSONObject("content")?.optJSONArray("items")
                            ?: shelf.optJSONObject("content")?.optJSONObject("horizontalListRenderer")?.optJSONArray("items")
                            ?: shelf.optJSONObject("content")?.optJSONObject("gridRenderer")?.optJSONArray("items")
                        collectShelf(title, contentsArray)
                    }
                    val keys = node.keys()
                    while (keys.hasNext()) walk(node.opt(keys.next()))
                }
                is JSONArray -> for (i in 0 until node.length()) walk(node.opt(i))
            }
        }

        walk(json.optJSONObject("contents") ?: json)
        return shelves
    }

    private fun parseBrowseSections(json: JSONObject): List<HomeVideoItem> {
        val results = mutableListOf<HomeVideoItem>()
        val seen = mutableSetOf<String>()

        fun scanJson(node: Any?) {
            when (node) {
                is JSONObject -> {
                    val parsed = parseAnyItemRenderer(node)
                    if (parsed != null && seen.add(parsed.id)) {
                        results.add(parsed)
                    }
                    val keys = node.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        scanJson(node.opt(key))
                    }
                }
                is JSONArray -> {
                    for (i in 0 until node.length()) {
                        scanJson(node.opt(i))
                    }
                }
            }
        }

        scanJson(json.optJSONObject("contents") ?: json)
        return results
    }

    private fun parseAnyItemRenderer(obj: JSONObject): HomeVideoItem? {
        val target = obj.optJSONObject("musicResponsiveListItemRenderer")
            ?: obj.optJSONObject("musicTwoRowItemRenderer")
            ?: obj.optJSONObject("gridVideoRenderer")
            ?: obj.optJSONObject("videoRenderer")
            ?: obj.optJSONObject("compactVideoRenderer")
            ?: obj.optJSONObject("playlistPanelVideoRenderer")
            ?: obj.optJSONObject("tileRenderer")
            ?: obj.optJSONObject("tvItemRenderer")
            ?: if (obj.has("videoId") || obj.has("contentId") || obj.optJSONObject("navigationEndpoint")?.optJSONObject("watchEndpoint") != null || obj.optJSONObject("onSelectCommand")?.optJSONObject("watchEndpoint") != null) obj else null
            ?: return null

        val videoId = extractVideoId(target)
        if (videoId.isBlank() || videoId.length < 11) return null
        val cleanVideoId = if (videoId.length > 11) videoId.substring(0, 11) else videoId

        val title = runsText(target.optJSONObject("title"))
            .ifBlank { runsText(target.optJSONObject("headline")) }
            .ifBlank { target.optJSONObject("metadata")?.optJSONObject("tileMetadataRenderer")?.optJSONObject("title")?.let { runsText(it) } ?: "" }
            .ifBlank { "YouTube Stream" }

        val artist = runsText(target.optJSONObject("subtitle"))
            .ifBlank { runsText(target.optJSONArray("flexColumns")?.optJSONObject(1)?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")?.optJSONObject("text")) }
            .ifBlank { runsText(target.optJSONObject("shortBylineText")) }
            .ifBlank { runsText(target.optJSONObject("longBylineText")) }
            .ifBlank { target.optJSONObject("metadata")?.optJSONObject("tileMetadataRenderer")?.optJSONArray("lines")?.optJSONObject(0)?.optJSONObject("lineRenderer")?.optJSONArray("items")?.optJSONObject(0)?.optJSONObject("lineItemRenderer")?.optJSONObject("text")?.let { runsText(it) } ?: "" }
            .ifBlank { "YouTube" }

        val thumb = extractThumbnail(
            target.optJSONObject("thumbnail")
                ?: target.optJSONObject("thumbnailRenderer")
                ?: target.optJSONObject("musicThumbnailRenderer")
                ?: target.optJSONObject("header")?.optJSONObject("tileHeaderRenderer")?.optJSONObject("thumbnail")
        ).ifBlank { "https://i.ytimg.com/vi/$cleanVideoId/hqdefault.jpg" }

        val durationStr = runsText(target.optJSONObject("lengthText"))
        val duration = if (durationStr.isNotBlank()) parseDuration(durationStr) else 0L

        return HomeVideoItem(
            id = cleanVideoId,
            title = title,
            channelName = artist,
            thumbnailUrl = thumb,
            duration = duration,
            viewCount = 0L
        )
    }

    private fun extractVideoId(obj: JSONObject): String {
        val nav = obj.optJSONObject("navigationEndpoint")
            ?: obj.optJSONObject("overlay")?.optJSONObject("musicItemThumbnailOverlayRenderer")?.optJSONObject("content")?.optJSONObject("musicPlayButtonRenderer")?.optJSONObject("playNavigationEndpoint")
            ?: obj.optJSONObject("onSelectCommand")
            ?: obj.optJSONObject("content")?.optJSONObject("tileRenderer")?.optJSONObject("onSelectCommand")
        return nav?.optJSONObject("watchEndpoint")?.optString("videoId")
            ?: nav?.optJSONObject("watchPlaylistEndpoint")?.optString("videoId")
            ?: obj.optString("contentId").takeIf { it.isNotBlank() }
            ?: obj.optString("videoId")
    }

    private fun extractThumbnail(obj: JSONObject?): String {
        if (obj == null) return ""
        val thumbs = obj.optJSONObject("musicThumbnailRenderer")
            ?.optJSONObject("thumbnail")
            ?.optJSONArray("thumbnails")
            ?: obj.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
            ?: obj.optJSONObject("thumbnails")?.optJSONArray("thumbnails")
            ?: obj.optJSONArray("thumbnails")
            ?: return ""

        if (thumbs.length() == 0) return ""
        return thumbs.optJSONObject(thumbs.length() - 1)?.optString("url") ?: ""
    }

    private fun runsText(obj: JSONObject?): String {
        if (obj == null) return ""
        obj.optString("simpleText").takeIf { it.isNotBlank() }?.let { return it }
        val runs = obj.optJSONArray("runs") ?: return ""
        val sb = StringBuilder()
        for (i in 0 until runs.length()) {
            sb.append(runs.optJSONObject(i)?.optString("text") ?: "")
        }
        return sb.toString()
    }

    private fun parseDuration(text: String): Long {
        if (text.isBlank()) return 0L
        val parts = text.split(":")
        return when (parts.size) {
            2 -> (parts[0].toLongOrNull() ?: 0L) * 60 + (parts[1].toLongOrNull() ?: 0L)
            3 -> (parts[0].toLongOrNull() ?: 0L) * 3600 + (parts[1].toLongOrNull() ?: 0L) * 60 + (parts[2].toLongOrNull() ?: 0L)
            else -> parts[0].toLongOrNull() ?: 0L
        }
    }
}
