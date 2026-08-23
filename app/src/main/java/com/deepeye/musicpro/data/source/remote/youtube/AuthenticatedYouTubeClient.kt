package com.deepeye.musicpro.data.source.remote.youtube

import android.util.Log
import com.deepeye.musicpro.data.prefs.SettingsDataStore
import com.deepeye.musicpro.domain.auth.YouTubeDeviceAuthManager
import com.deepeye.musicpro.domain.model.home.HomeVideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthenticatedYouTubeClient @Inject constructor(
    private val client: OkHttpClient,
    private val settingsDataStore: SettingsDataStore,
    private val authManager: YouTubeDeviceAuthManager
) {
    private val INNERTUBE_API_URL = "https://youtubei.googleapis.com/youtubei/v1/browse?key="
    private val CONTEXT_JSON = """
        "context": {
          "client": {
            "clientName": "TVHTML5",
            "clientVersion": "7.20230412.08.00",
            "androidSdkVersion": 30,
            "hl": "en",
            "gl": "IN"
          }
        }
    """.trimIndent()

    private suspend fun getValidAccessToken(): String? {
        val settings = settingsDataStore.settings.first()
        return settings.youtubeAccessToken
    }

    private suspend fun handle401AndRetry(request: Request): List<HomeVideoItem> {
        val settings = settingsDataStore.settings.first()
        val refreshToken = settings.youtubeRefreshToken
        if (refreshToken != null) {
            val newToken = authManager.refreshToken(refreshToken)
            if (newToken != null) {
                settingsDataStore.setYouTubeTokens(newToken.accessToken, newToken.refreshToken)
                val retryReq = request.newBuilder().header("Authorization", "Bearer ${newToken.accessToken}").build()
                val retryRes = client.newCall(retryReq).execute()
                if (retryRes.isSuccessful) {
                    return parseInnerTubeVideos(retryRes.body?.string() ?: "")
                }
            }
        }
        return emptyList()
    }

    suspend fun browse(browseId: String, params: String? = null): List<HomeVideoItem> = withContext(Dispatchers.IO) {
        val token = getValidAccessToken() ?: return@withContext emptyList()
        val paramsPart = if (params != null) ", \"params\": \"$params\"" else ""
        val bodyStr = "{$CONTEXT_JSON, \"browseId\": \"$browseId\"$paramsPart}"

        try {
            val request = Request.Builder()
                .url(INNERTUBE_API_URL)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .post(bodyStr.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (response.code == 401) {
                return@withContext handle401AndRetry(request)
            }
            if (!response.isSuccessful) {
                Log.e("AuthYTClient", "browse($browseId) HTTP Error: ${response.code} - ${response.body?.string()}")
                return@withContext emptyList()
            }

            val items = parseInnerTubeVideos(response.body?.string() ?: "")
            Log.d("AuthYTClient", "browse($browseId) returned ${items.size} videos")
            items.distinctBy { it.id }
        } catch (e: Exception) {
            Log.e("AuthYTClient", "browse($browseId) failed", e)
            emptyList()
        }
    }

    suspend fun getHomeFeed(): List<HomeVideoItem> = browse("FEwhat_to_watch")

    suspend fun getHistory(): List<HomeVideoItem> = browse("FEhistory")

    suspend fun getSubscriptionsFeed(): List<HomeVideoItem> = browse("FEsubscriptions")

    suspend fun getLikedVideos(): List<HomeVideoItem> {
        val result = browse("VL${"LL"}")
        return if (result.isNotEmpty()) result else browse("FEliked_playlists")
    }

    suspend fun getWatchLater(): List<HomeVideoItem> = browse("VLWL")

    suspend fun getTrending(): List<HomeVideoItem> = browse("FEtrending")

    suspend fun getMusicFeed(): List<HomeVideoItem> {
        val result = browse("FEmusic_home")
        return if (result.isNotEmpty()) result else search("trending songs official music video")
    }

    suspend fun getMoviesFeed(): List<HomeVideoItem> {
        val result = browse("FEmovies_home")
        return if (result.isNotEmpty()) result else search("full movie official")
    }

    suspend fun getGamingFeed(): List<HomeVideoItem> {
        val result = browse("FEgaming")
        return if (result.isNotEmpty()) result else search("gaming gameplay let's play")
    }

    suspend fun getNewsFeed(): List<HomeVideoItem> {
        val result = browse("FEnews")
        return if (result.isNotEmpty()) result else search("news live report today")
    }

    suspend fun search(query: String): List<HomeVideoItem> = withContext(Dispatchers.IO) {
        val token = getValidAccessToken() ?: return@withContext emptyList()
        val requestJson = JSONObject().apply {
            put("context", JSONObject().apply {
                put("client", JSONObject().apply {
                    put("clientName", "TVHTML5")
                    put("clientVersion", "7.20230412.08.00")
                })
            })
            put("query", query)
        }

        try {
            val request = Request.Builder()
                .url("https://youtubei.googleapis.com/youtubei/v1/search")
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (response.code == 401) {
                return@withContext handle401AndRetry(request)
            }
            if (!response.isSuccessful) return@withContext emptyList()

            val jsonStr = response.body?.string() ?: ""
            val items = parseInnerTubeVideos(jsonStr)
            Log.d("AuthYTClient", "search '$query' returned ${items.size} tiles")
            items.distinctBy { it.id }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseInnerTubeVideos(jsonStr: String): List<HomeVideoItem> {
        val list = mutableListOf<HomeVideoItem>()
        if (jsonStr.isEmpty()) return list
        try {
            return parseVideoRenderersRecursive(JSONObject(jsonStr))
        } catch (e: Exception) {
            Log.e("AuthYTClient", "Parsing error", e)
        }
        return list
    }

    private fun parseVideoRenderersRecursive(json: JSONObject): List<HomeVideoItem> {
        val list = mutableListOf<HomeVideoItem>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            when (key) {
                "videoRenderer", "gridVideoRenderer", "compactVideoRenderer", "playlistVideoRenderer" -> {
                    val videoObj = json.optJSONObject(key)
                    if (videoObj != null) {
                        val item = parseVideoRenderer(videoObj)
                        if (item != null) list.add(item)
                    }
                }
                "reelItemRenderer" -> {
                    val reelObj = json.optJSONObject(key)
                    if (reelObj != null) {
                        val item = parseReelItemRenderer(reelObj)
                        if (item != null) list.add(item)
                    }
                }
                "tileRenderer" -> {
                    val tileObj = json.optJSONObject(key)
                    if (tileObj != null) {
                        val item = parseTileRenderer(tileObj)
                        if (item != null) list.add(item)
                    }
                }
                "lockupViewModel" -> {
                    val lockupObj = json.optJSONObject(key)
                    if (lockupObj != null) {
                        val item = parseLockupViewModel(lockupObj)
                        if (item != null) list.add(item)
                    }
                }
                else -> {
                    val obj = json.optJSONObject(key)
                    if (obj != null) {
                        list.addAll(parseVideoRenderersRecursive(obj))
                    } else {
                        val arr = json.optJSONArray(key)
                        if (arr != null) {
                            for (i in 0 until arr.length()) {
                                val childObj = arr.optJSONObject(i)
                                if (childObj != null) {
                                    list.addAll(parseVideoRenderersRecursive(childObj))
                                }
                            }
                        }
                    }
                }
            }
        }
        return list
    }

    private fun parseVideoRenderer(videoObj: JSONObject): HomeVideoItem? {
        val id = videoObj.optString("videoId")
        if (id.isEmpty()) return null

        val title = videoObj.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
            ?: videoObj.optJSONObject("title")?.optString("simpleText")
            ?: ""

        val channelName = videoObj.optJSONObject("shortBylineText")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
            ?: videoObj.optJSONObject("longBylineText")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
            ?: videoObj.optJSONObject("ownerText")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
            ?: ""

        val channelId = videoObj.optJSONObject("shortBylineText")?.optJSONArray("runs")?.optJSONObject(0)
            ?.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")?.optString("browseId") ?: ""

        val durationSeconds = parseDuration(
            videoObj.optJSONObject("lengthText")?.optString("simpleText")
                ?: videoObj.optJSONObject("lengthText")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                ?: ""
        )

        val thumbUrl = getLastThumbnail(videoObj.optJSONObject("thumbnail")?.optJSONArray("thumbnails"))

        val viewText = videoObj.optJSONObject("shortViewCountText")?.optString("simpleText")
            ?: videoObj.optJSONObject("shortViewCountText")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
            ?: videoObj.optJSONObject("viewCountText")?.optString("simpleText")
            ?: ""
        val views = parseViews(viewText)

        val uploadDate = videoObj.optJSONObject("publishedTimeText")?.optString("simpleText")
            ?: videoObj.optJSONObject("publishedTimeText")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
            ?: ""

        return HomeVideoItem(
            id = id,
            title = title,
            channelName = channelName,
            channelId = channelId,
            thumbnailUrl = thumbUrl,
            duration = durationSeconds,
            viewCount = views,
            uploadDate = uploadDate,
            isShort = durationSeconds in 1..60
        )
    }

    private fun parseReelItemRenderer(reelObj: JSONObject): HomeVideoItem? {
        val id = reelObj.optString("videoId")
        if (id.isEmpty()) return null

        val title = reelObj.optJSONObject("headline")?.optString("simpleText")
            ?: reelObj.optJSONObject("headline")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
            ?: ""

        val thumbUrl = getLastThumbnail(reelObj.optJSONObject("thumbnail")?.optJSONArray("thumbnails"))

        val viewText = reelObj.optJSONObject("viewCountText")?.optString("simpleText") ?: ""
        val views = parseViews(viewText)

        return HomeVideoItem(
            id = id,
            title = title,
            channelName = "",
            thumbnailUrl = thumbUrl,
            duration = 30L,
            viewCount = views,
            isShort = true
        )
    }

    private fun parseTileRenderer(tileObj: JSONObject): HomeVideoItem? {
        val id = tileObj.optJSONObject("onSelectCommand")?.optJSONObject("watchEndpoint")?.optString("videoId") ?: ""
        if (id.isEmpty()) return null

        val metaRenderer = tileObj.optJSONObject("metadata")?.optJSONObject("tileMetadataRenderer")
        val title = metaRenderer?.optJSONObject("title")?.optString("simpleText")
            ?: metaRenderer?.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
            ?: ""

        var channelName = ""
        var viewText = ""
        val lines = metaRenderer?.optJSONArray("lines")
        if (lines != null && lines.length() > 0) {
            val lineItems0 = lines.optJSONObject(0)?.optJSONObject("lineRenderer")?.optJSONArray("items")
            if (lineItems0 != null && lineItems0.length() > 0) {
                val runs = lineItems0.optJSONObject(0)?.optJSONObject("lineItemRenderer")?.optJSONObject("text")?.optJSONArray("runs")
                if (runs != null && runs.length() > 0) {
                    channelName = runs.optJSONObject(0)?.optString("text") ?: ""
                }
            }
            if (lines.length() > 1) {
                val lineItems1 = lines.optJSONObject(1)?.optJSONObject("lineRenderer")?.optJSONArray("items")
                if (lineItems1 != null && lineItems1.length() > 0) {
                    val runs = lineItems1.optJSONObject(0)?.optJSONObject("lineItemRenderer")?.optJSONObject("text")?.optJSONArray("runs")
                    if (runs != null && runs.length() > 0) {
                        viewText = runs.optJSONObject(0)?.optString("text") ?: ""
                    }
                }
            }
        }

        val headerRenderer = tileObj.optJSONObject("header")?.optJSONObject("tileHeaderRenderer")
        val thumbUrl = getLastThumbnail(headerRenderer?.optJSONObject("thumbnail")?.optJSONArray("thumbnails"))

        var durationSeconds = 0L
        val overlays = headerRenderer?.optJSONArray("thumbnailOverlays")
        if (overlays != null) {
            for (i in 0 until overlays.length()) {
                val timeRenderer = overlays.optJSONObject(i)?.optJSONObject("thumbnailOverlayTimeStatusRenderer")
                if (timeRenderer != null) {
                    val timeText = timeRenderer.optJSONObject("text")?.optString("simpleText")
                        ?: timeRenderer.optJSONObject("text")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                        ?: ""
                    durationSeconds = parseDuration(timeText)
                    break
                }
            }
        }

        return HomeVideoItem(
            id = id,
            title = title,
            channelName = channelName,
            thumbnailUrl = thumbUrl,
            duration = durationSeconds,
            viewCount = parseViews(viewText),
            isShort = durationSeconds in 1..60
        )
    }

    private fun parseLockupViewModel(lockupObj: JSONObject): HomeVideoItem? {
        val id = lockupObj.optString("contentId")
        if (id.isEmpty()) return null

        val title = lockupObj.optJSONObject("metadata")?.optJSONObject("lockupMetadataViewModel")?.optJSONObject("title")?.optString("content") ?: ""

        var channelName = ""
        var viewText = ""
        val metadataRows = lockupObj.optJSONObject("metadata")?.optJSONObject("lockupMetadataViewModel")?.optJSONObject("metadata")?.optJSONObject("contentMetadataViewModel")?.optJSONArray("metadataRows")
        if (metadataRows != null && metadataRows.length() > 0) {
            val parts = metadataRows.optJSONObject(0)?.optJSONArray("metadataParts")
            if (parts != null && parts.length() > 0) {
                channelName = parts.optJSONObject(0)?.optJSONObject("text")?.optString("content") ?: ""
                if (parts.length() > 1) {
                    viewText = parts.optJSONObject(1)?.optJSONObject("text")?.optString("content") ?: ""
                }
            }
        }

        val thumbUrl = lockupObj.optJSONObject("contentImage")?.optJSONObject("thumbnailViewModel")?.optJSONObject("image")?.optJSONArray("sources")?.optJSONObject(0)?.optString("url") ?: ""

        var durationText = ""
        val overlays = lockupObj.optJSONObject("contentImage")?.optJSONObject("thumbnailViewModel")?.optJSONArray("overlays")
        if (overlays != null && overlays.length() > 0) {
            val badges = overlays.optJSONObject(0)?.optJSONObject("thumbnailBottomOverlayViewModel")?.optJSONArray("badges")
            if (badges != null && badges.length() > 0) {
                durationText = badges.optJSONObject(0)?.optJSONObject("thumbnailBadgeViewModel")?.optString("text") ?: ""
            }
        }

        val durationSeconds = parseDuration(durationText)
        return HomeVideoItem(
            id = id,
            title = title,
            channelName = channelName,
            thumbnailUrl = thumbUrl,
            duration = durationSeconds,
            viewCount = parseViews(viewText),
            isShort = durationSeconds in 1..60
        )
    }

    private fun parseViews(viewText: String?): Long {
        if (viewText.isNullOrBlank()) return 0L
        val cleaned = viewText.lowercase().replace("views", "").replace("view", "").replace(",", "").trim()
        return try {
            when {
                cleaned.endsWith("m") -> (cleaned.removeSuffix("m").trim().toDouble() * 1_000_000).toLong()
                cleaned.endsWith("b") -> (cleaned.removeSuffix("b").trim().toDouble() * 1_000_000_000).toLong()
                cleaned.endsWith("k") -> (cleaned.removeSuffix("k").trim().toDouble() * 1_000).toLong()
                else -> cleaned.filter { it.isDigit() }.toLongOrNull() ?: 0L
            }
        } catch (e: Exception) {
            0L
        }
    }

    private fun parseDuration(duration: String): Long {
        val parts = duration.split(":")
        return when (parts.size) {
            2 -> (parts[0].toLongOrNull() ?: 0L) * 60 + (parts[1].toLongOrNull() ?: 0L)
            3 -> (parts[0].toLongOrNull() ?: 0L) * 3600 + (parts[1].toLongOrNull() ?: 0L) * 60 + (parts[2].toLongOrNull() ?: 0L)
            else -> 0L
        }
    }

    private fun getLastThumbnail(thumbnails: org.json.JSONArray?): String {
        if (thumbnails == null || thumbnails.length() == 0) return ""
        return thumbnails.optJSONObject(thumbnails.length() - 1)?.optString("url") ?: ""
    }
}
