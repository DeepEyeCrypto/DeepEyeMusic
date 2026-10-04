// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.source.remote.youtube

import android.util.Log
import com.deepeye.musicpro.domain.auth.InnerTubeAuthManager
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
    private companion object {
        const val TAG = "InnerTubeRemoteClient"
        const val YOUTUBE_MUSIC_INNERTUBE = "https://music.youtube.com/youtubei/v1/"
        const val YOUTUBE_MAIN_INNERTUBE = "https://www.youtube.com/youtubei/v1/"

        const val ANDROID_MUSIC_CLIENT_VERSION = "6.42.52"
        const val TVHTML5_CLIENT_VERSION = "7.20230412.08.00"

        val ANDROID_MUSIC_CONTEXT = """
            "context": {
              "client": {
                "clientName": "ANDROID_MUSIC",
                "clientVersion": "$ANDROID_MUSIC_CLIENT_VERSION",
                "hl": "en",
                "gl": "IN"
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
                if (!response.isSuccessful) {
                    Log.w(TAG, "Autoplay /next returned HTTP ${response.code}, trying fallback client")
                    return@withContext fetchNextAutoplayFallback(videoId)
                }

                val bodyStr = response.body?.string() ?: return@withContext null
                val json = JSONObject(bodyStr)

                // 1. Check direct autoplayEndpoint inside musicQueueRenderer or watchNextFeed
                val track = parseAutoplayFromJson(json, currentVideoId = videoId)
                if (track != null) {
                    Log.i(TAG, "Successfully resolved InnerTube AutoPlay track: ${track.videoId} (${track.title} - ${track.artist})")
                    return@withContext track
                }

                return@withContext fetchNextAutoplayFallback(videoId)
            }
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
        } catch (e: Exception) {
            Log.e(TAG, "Error in fallback InnerTube /next", e)
            null
        }
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
     * Authenticated /browse query for personalized home mixes, liked songs, and subscriptions.
     */
    suspend fun browse(browseId: String = "FEmusic_home", params: String? = null): List<HomeVideoItem> = withContext(Dispatchers.IO) {
        val token = authManager.getAccessToken()
        val paramsPart = if (params != null) ", \"params\": \"$params\"" else ""
        val payload = "{$ANDROID_MUSIC_CONTEXT, \"browseId\": \"$browseId\"$paramsPart}"

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
                    Log.e(TAG, "InnerTube /browse failed with HTTP ${response.code}")
                    return@withContext emptyList()
                }

                val bodyStr = response.body?.string() ?: return@withContext emptyList()
                return@withContext parseBrowseSections(JSONObject(bodyStr))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in InnerTube /browse for browseId=$browseId", e)
            emptyList()
        }
    }

    private fun parseBrowseSections(json: JSONObject): List<HomeVideoItem> {
        val results = mutableListOf<HomeVideoItem>()
        val sectionList = json.optJSONObject("contents")
            ?.optJSONObject("singleColumnBrowseResultsRenderer")
            ?.optJSONArray("tabs")
            ?.optJSONObject(0)
            ?.optJSONObject("tabRenderer")
            ?.optJSONObject("content")
            ?.optJSONObject("sectionListRenderer")
            ?.optJSONArray("contents") ?: return results

        for (i in 0 until sectionList.length()) {
            val shelf = sectionList.optJSONObject(i) ?: continue
            val musicCarousel = shelf.optJSONObject("musicCarouselShelfRenderer")
            val musicShelf = shelf.optJSONObject("musicShelfRenderer")

            val items = musicCarousel?.optJSONArray("contents") ?: musicShelf?.optJSONArray("contents")
            if (items != null) {
                for (j in 0 until items.length()) {
                    val itemObj = items.optJSONObject(j) ?: continue
                    val responsiveItem = itemObj.optJSONObject("musicResponsiveListItemRenderer")
                        ?: itemObj.optJSONObject("musicTwoRowItemRenderer")
                        ?: continue

                    val videoId = extractVideoId(responsiveItem)
                    if (videoId.isNotBlank()) {
                        val title = runsText(responsiveItem.optJSONObject("title"))
                        val artist = runsText(responsiveItem.optJSONObject("subtitle") ?: responsiveItem.optJSONArray("flexColumns")?.optJSONObject(1)?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")?.optJSONObject("text"))
                        val thumb = extractThumbnail(responsiveItem.optJSONObject("thumbnail") ?: responsiveItem.optJSONObject("thumbnailRenderer"))

                        results.add(
                            HomeVideoItem(
                                id = videoId,
                                title = title.ifBlank { "YouTube Music Track" },
                                channelName = artist.ifBlank { "YouTube Music" },
                                thumbnailUrl = thumb.ifBlank { "https://i.ytimg.com/vi/$videoId/hqdefault.jpg" },
                                duration = 210000L,
                                viewCount = 0L
                            )
                        )
                    }
                }
            }
        }
        return results
    }

    private fun extractVideoId(obj: JSONObject): String {
        val nav = obj.optJSONObject("navigationEndpoint")
            ?: obj.optJSONObject("overlay")?.optJSONObject("musicItemThumbnailOverlayRenderer")?.optJSONObject("content")?.optJSONObject("musicPlayButtonRenderer")?.optJSONObject("playNavigationEndpoint")
        return nav?.optJSONObject("watchEndpoint")?.optString("videoId")
            ?: obj.optString("videoId")
    }

    private fun extractThumbnail(obj: JSONObject?): String {
        if (obj == null) return ""
        val thumbs = obj.optJSONObject("musicThumbnailRenderer")
            ?.optJSONObject("thumbnail")
            ?.optJSONArray("thumbnails")
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
