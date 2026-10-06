// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.source.remote.youtube

import android.content.Context
import android.util.Log
import com.deepeye.musicpro.domain.auth.InnerTubeAuthManager
import com.deepeye.musicpro.domain.model.Song
import com.deepeye.musicpro.domain.model.personalization.PersonalizedFeedItem
import com.deepeye.musicpro.domain.model.personalization.PersonalizedItemType
import com.deepeye.musicpro.domain.model.personalization.PersonalizedSection
import com.deepeye.musicpro.domain.model.personalization.PersonalizedSectionType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SmartTube-Omega Engine (TVHTML5 InnerTube Algorithmic Reversal).
 *
 * Emulates the exact SmartTube TVHTML5 reverse-engineered protocol to interface
 * directly with YouTube's internal `youtubei/v1` APIs (/browse, /next).
 *
 * Guarantees 1:1 authentic YouTube TV recommendations, personalized mixes,
 * and gapless algorithmic autoplay without YouTube Data API v3 rate limits.
 */
@Singleton
class SmartTubeEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authManager: InnerTubeAuthManager,
    private val client: OkHttpClient
) {
    companion object {
        private const val TAG = "SmartTubeEngine"
        private const val INNERTUBE_BASE_URL = "https://www.youtube.com/youtubei/v1/"
        private const val TVHTML5_CLIENT_VERSION = "7.20210614.03.00"
        private const val TVHTML5_USER_AGENT =
            "Mozilla/5.0 (SMART-TV; Linux; Tizen 5.0) AppleWebKit/538.1 (KHTML, like Gecko) Version/5.0 NativeTVAds Safari/538.1,gzip(gfe)"

        val TVHTML5_CONTEXT = """
            "context": {
              "client": {
                "clientName": "TVHTML5",
                "clientVersion": "$TVHTML5_CLIENT_VERSION",
                "userAgent": "$TVHTML5_USER_AGENT",
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
     * Executes an authenticated or spoofed POST request to a given youtubei/v1 endpoint.
     */
    suspend fun postInnerTube(endpoint: String, extraJson: String = ""): JSONObject? = withContext(Dispatchers.IO) {
        val token = authManager.getAccessToken()
        val payload = if (extraJson.isBlank()) {
            "{$TVHTML5_CONTEXT}"
        } else {
            "{$TVHTML5_CONTEXT, $extraJson}"
        }

        try {
            val reqBuilder = Request.Builder()
                .url("$INNERTUBE_BASE_URL$endpoint")
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", TVHTML5_USER_AGENT)
                .addHeader("X-YouTube-Client-Name", "85") // 85 = TVHTML5
                .addHeader("X-YouTube-Client-Version", TVHTML5_CLIENT_VERSION)
                .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))

            if (!token.isNullOrBlank()) {
                reqBuilder.addHeader("Authorization", "Bearer $token")
            }

            client.newCall(reqBuilder.build()).execute().use { response ->
                if (response.code == 401 && !token.isNullOrBlank()) {
                    Log.w(TAG, "Received 401 on /$endpoint, refreshing OAuth token...")
                    val refreshedToken = authManager.refreshAccessToken()
                    if (!refreshedToken.isNullOrBlank()) {
                        val retryReq = reqBuilder.header("Authorization", "Bearer $refreshedToken").build()
                        client.newCall(retryReq).execute().use { retryRes ->
                            if (retryRes.isSuccessful) {
                                val bodyStr = retryRes.body?.string() ?: return@withContext null
                                return@withContext JSONObject(bodyStr)
                            }
                        }
                    }
                }

                if (!response.isSuccessful) {
                    Log.w(TAG, "SmartTube InnerTube POST /$endpoint returned HTTP ${response.code}")
                    return@withContext null
                }

                val bodyStr = response.body?.string() ?: return@withContext null
                return@withContext JSONObject(bodyStr)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error posting to SmartTube InnerTube endpoint $endpoint", e)
            null
        }
    }

    enum class LikeStatus {
        LIKED,
        DISLIKED,
        INDIFFERENT
    }

    /**
     * Executes a Like, Dislike or RemoveLike mutation on the user's YouTube account.
     */
    suspend fun setLikeStatus(videoId: String, status: LikeStatus): Boolean = withContext(Dispatchers.IO) {
        if (videoId.isBlank()) return@withContext false
        val endpoint = when (status) {
            LikeStatus.LIKED -> "like/like"
            LikeStatus.DISLIKED -> "like/dislike"
            LikeStatus.INDIFFERENT -> "like/removelike"
        }
        val extraJson = "\"target\": {\"videoId\": \"$videoId\"}"
        val res = postInnerTube(endpoint, extraJson)
        return@withContext res != null
    }

    /**
     * Fetches YouTube's exact native AutoPlay next track for [videoId] via the /next endpoint.
     */
    suspend fun getAlgorithmicNext(videoId: String, playlistId: String? = null): AutoplayTrack? = withContext(Dispatchers.IO) {
        if (videoId.isBlank()) return@withContext null

        val playlistPart = if (!playlistId.isNullOrBlank()) "\"playlistId\": \"$playlistId\", " else ""
        val extraJson = "$playlistPart\"videoId\": \"$videoId\""

        val json = postInnerTube("next", extraJson) ?: return@withContext null
        return@withContext extractAutoplayFromJson(json, currentVideoId = videoId)
    }

    /**
     * Extracts next track recursively from SmartTube /next InnerTube response JSON.
     */
    private fun extractAutoplayFromJson(json: JSONObject, currentVideoId: String): AutoplayTrack? {
        // Priority 1: autoplayEndpointRenderer (Smart Mix / Playlist next)
        val autoplayNav = json.optJSONObject("playerOverlays")
            ?.optJSONObject("playerOverlayRenderer")
            ?.optJSONObject("autoplay")
            ?.optJSONObject("playerOverlayAutoplayRenderer")

        if (autoplayNav != null) {
            val nextId = autoplayNav.optJSONObject("videoEndpoint")
                ?.optJSONObject("watchEndpoint")
                ?.optString("videoId")

            if (!nextId.isNullOrBlank() && nextId != currentVideoId) {
                val title = parseRunsText(autoplayNav.optJSONObject("videoTitle"))
                val artist = parseRunsText(autoplayNav.optJSONObject("byline"))
                val thumb = extractThumbnailUrl(autoplayNav.optJSONObject("thumbnailRenderer")) ?: ""
                return AutoplayTrack(
                    videoId = nextId,
                    title = title.ifBlank { "Next Recommendation" },
                    artist = artist.ifBlank { "YouTube" },
                    thumbnailUrl = thumb,
                    durationSeconds = 0L
                )
            }
        }

        // Priority 2: maybeHistoryEndpointRenderer or nextVideoRenderer inside watchNextTabbedResults
        val tabs = json.optJSONObject("contents")
            ?.optJSONObject("singleColumnMusicWatchNextResultsRenderer")
            ?.optJSONObject("tabbedRenderer")
            ?.optJSONObject("watchNextTabbedResultsRenderer")
            ?.optJSONArray("tabs")

        if (tabs != null && tabs.length() > 0) {
            val tab0 = tabs.optJSONObject(0)?.optJSONObject("tabRenderer")
            val contents = tab0?.optJSONObject("content")
                ?.optJSONObject("musicQueueRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("playlistPanelRenderer")
                ?.optJSONArray("contents")

            if (contents != null && contents.length() > 0) {
                for (i in 0 until contents.length()) {
                    val panelItem = contents.optJSONObject(i)?.optJSONObject("playlistPanelVideoRenderer") ?: continue
                    val nextId = panelItem.optString("videoId")
                    if (nextId.isNotBlank() && nextId != currentVideoId) {
                        val title = parseRunsText(panelItem.optJSONObject("title"))
                        val artist = parseRunsText(panelItem.optJSONObject("longBylineText") ?: panelItem.optJSONObject("shortBylineText"))
                        val thumb = extractThumbnailUrl(panelItem.optJSONObject("thumbnail")) ?: ""
                        val durationStr = parseRunsText(panelItem.optJSONObject("lengthText"))
                        return AutoplayTrack(
                            videoId = nextId,
                            title = title.ifBlank { "Next Track" },
                            artist = artist.ifBlank { "YouTube Music" },
                            thumbnailUrl = thumb,
                            durationSeconds = parseDurationSec(durationStr)
                        )
                    }
                }
            }
        }

        // Priority 3: Two-Column Watch Next Secondary Results
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
                    val title = parseRunsText(compactVideo.optJSONObject("title"))
                    val artist = parseRunsText(compactVideo.optJSONObject("shortBylineText"))
                    val thumb = extractThumbnailUrl(compactVideo.optJSONObject("thumbnail")) ?: ""
                    val durationStr = parseRunsText(compactVideo.optJSONObject("lengthText"))
                    return AutoplayTrack(
                        videoId = nextId,
                        title = title.ifBlank { "Next Recommendation" },
                        artist = artist.ifBlank { "YouTube" },
                        thumbnailUrl = thumb,
                        durationSeconds = parseDurationSec(durationStr)
                    )
                }
            }
        }

        return null
    }

    /**
     * Fetches 1:1 personalized Home Feed using SmartTube TVHTML5 /browse endpoint.
     */
    suspend fun getPersonalizedHome(browseId: String = "FEwhat_to_watch"): List<PersonalizedSection> = withContext(Dispatchers.IO) {
        val extraJson = "\"browseId\": \"$browseId\""
        val json = postInnerTube("browse", extraJson) ?: return@withContext emptyList()

        val sections = mutableListOf<PersonalizedSection>()

        val tabs = json.optJSONObject("contents")
            ?.optJSONObject("twoColumnBrowseResultsRenderer")
            ?.optJSONArray("tabs")

        if (tabs != null && tabs.length() > 0) {
            for (t in 0 until tabs.length()) {
                val tab = tabs.optJSONObject(t)?.optJSONObject("tabRenderer") ?: continue
                val sectionList = tab.optJSONObject("content")
                    ?.optJSONObject("sectionListRenderer")
                    ?.optJSONArray("contents") ?: continue

                for (s in 0 until sectionList.length()) {
                    val itemSection = sectionList.optJSONObject(s)?.optJSONObject("itemSectionRenderer") ?: continue
                    val shelf = itemSection.optJSONArray("contents")?.optJSONObject(0)?.optJSONObject("shelfRenderer")
                    if (shelf != null) {
                        val sectionTitle = parseRunsText(shelf.optJSONObject("title")).ifBlank { "Recommended For You" }
                        val items = mutableListOf<PersonalizedFeedItem>()

                        val gridItems = shelf.optJSONObject("content")
                            ?.optJSONObject("gridRenderer")
                            ?.optJSONArray("items")
                            ?: shelf.optJSONObject("content")
                                ?.optJSONObject("horizontalListRenderer")
                                ?.optJSONArray("items")

                        if (gridItems != null) {
                            for (i in 0 until gridItems.length()) {
                                val gridObj = gridItems.optJSONObject(i) ?: continue
                                val videoObj = gridObj.optJSONObject("gridVideoRenderer")
                                    ?: gridObj.optJSONObject("compactVideoRenderer")
                                    ?: gridObj.optJSONObject("videoRenderer")
                                    ?: continue

                                val vId = videoObj.optString("videoId")
                                if (vId.isNotBlank()) {
                                    val vTitle = parseRunsText(videoObj.optJSONObject("title"))
                                    val vArtist = parseRunsText(videoObj.optJSONObject("shortBylineText"))
                                    val vThumb = extractThumbnailUrl(videoObj.optJSONObject("thumbnail"))
                                    val vDuration = parseRunsText(videoObj.optJSONObject("thumbnailOverlays"))
                                    items.add(
                                        PersonalizedFeedItem(
                                            id = vId,
                                            title = vTitle.ifBlank { "Track" },
                                            artist = vArtist.ifBlank { "YouTube" },
                                            artworkUrl = vThumb,
                                            itemType = PersonalizedItemType.VIDEO,
                                            durationMs = parseDurationSec(vDuration) * 1000L
                                        )
                                    )
                                }
                            }
                        }

                        if (items.isNotEmpty()) {
                            sections.add(
                                PersonalizedSection(
                                    id = "smarttube_sec_$s",
                                    title = sectionTitle,
                                    type = PersonalizedSectionType.BASED_ON_LISTENING,
                                    sourceLabel = "SmartTube TVHTML5",
                                    items = items
                                )
                            )
                        }
                    }
                }
            }
        }

        return@withContext sections
    }

    private fun parseRunsText(json: JSONObject?): String {
        if (json == null) return ""
        val simple = json.optString("simpleText", "")
        if (simple.isNotBlank()) return simple
        val runs = json.optJSONArray("runs") ?: return ""
        val sb = StringBuilder()
        for (i in 0 until runs.length()) {
            val r = runs.optJSONObject(i) ?: continue
            sb.append(r.optString("text", ""))
        }
        return sb.toString().trim()
    }

    private fun extractThumbnailUrl(json: JSONObject?): String? {
        if (json == null) return null
        val thumbs = json.optJSONArray("thumbnails")
            ?: json.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")
                ?.optJSONArray("thumbnails")
            ?: return null

        if (thumbs.length() == 0) return null
        val best = thumbs.optJSONObject(thumbs.length() - 1) ?: thumbs.optJSONObject(0)
        var url = best?.optString("url", "") ?: return null
        if (url.startsWith("//")) {
            url = "https:$url"
        }
        return url
    }

    private fun parseDurationSec(durationStr: String): Long {
        if (durationStr.isBlank()) return 0L
        val parts = durationStr.split(":")
        return try {
            when (parts.size) {
                2 -> parts[0].toLong() * 60 + parts[1].toLong()
                3 -> parts[0].toLong() * 3600 + parts[1].toLong() * 60 + parts[2].toLong()
                else -> 0L
            }
        } catch (_: Exception) {
            0L
        }
    }
}
