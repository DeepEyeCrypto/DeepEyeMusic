package com.deepeye.musicpro.data.source.remote.youtube

import android.util.Log
import com.deepeye.musicpro.data.prefs.SettingsDataStore
import com.deepeye.musicpro.domain.auth.YouTubeDeviceAuthManager
import com.deepeye.musicpro.domain.model.home.HomeVideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
    private val INNERTUBE_API_URL = "https://youtubei.googleapis.com/youtubei/v1/browse?key=${com.deepeye.musicpro.BuildConfig.YOUTUBE_API_KEY}"

    // WEB client — returns videoRenderer with full channel avatars & metadata
    private val WEB_CONTEXT_JSON = """
        "context": {
          "client": {
            "clientName": "WEB",
            "clientVersion": "2.20240101.00.00",
            "hl": "en",
            "gl": "IN"
          }
        }
    """.trimIndent()

    // TVHTML5 client — works with OAuth tokens for personalized/auth content
    private val TV_CONTEXT_JSON = """
        "context": {
          "client": {
            "clientName": "TVHTML5",
            "clientVersion": "7.20230412.08.00",
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
                client.newCall(retryReq).execute().use { retryRes ->
                    if (retryRes.isSuccessful) {
                        return parseInnerTubeVideos(retryRes.body?.string() ?: "")
                    }
                    Log.e("AuthYTClient", "Post-refresh retry failed with HTTP ${retryRes.code}")
                }
            }
        }
        return emptyList()
    }

    /**
     * Browse with specified client context.
     * @param useAuthClient true = TVHTML5 + OAuth token (for personalized data), false = WEB (for public data with full metadata)
     */
    private suspend fun browseInternal(browseId: String, params: String? = null, useAuthClient: Boolean = false): List<HomeVideoItem> = withContext(Dispatchers.IO) {
        val token = getValidAccessToken()
        val contextJson = if (useAuthClient && token != null) TV_CONTEXT_JSON else WEB_CONTEXT_JSON
        val paramsPart = if (params != null) ", \"params\": \"$params\"" else ""
        val bodyStr = "{$contextJson, \"browseId\": \"$browseId\"$paramsPart}"

        try {
            val reqBuilder = Request.Builder()
                .url(INNERTUBE_API_URL)
                .addHeader("Content-Type", "application/json")
                .post(bodyStr.toRequestBody("application/json".toMediaType()))

            if (token != null) {
                reqBuilder.addHeader("Authorization", "Bearer $token")
            }

            val request = reqBuilder.build()
            val response = client.newCall(request).execute()
            if (response.code == 401 && token != null) {
                Log.e("AuthYTClient", "401 Unauthorized for $browseId, retrying...")
                response.close()
                return@withContext handle401AndRetry(request)
            }
            if (!response.isSuccessful) {
                val errorBody = response.body?.string()
                Log.e("AuthYTClient", "browse($browseId) HTTP Error: ${response.code} - $errorBody")
                return@withContext emptyList()
            }

            val jsonStr = response.body?.string() ?: ""
            val items = parseInnerTubeVideos(jsonStr)
            Log.d("AuthYTClient", "browse($browseId, auth=$useAuthClient) returned ${items.size} videos")
            items.distinctBy { it.id }
        } catch (e: Exception) {
            Log.e("AuthYTClient", "browse($browseId) failed", e)
            emptyList()
        }
    }

    /** Public browse (WEB client, works without auth, returns full channel metadata) */
    suspend fun browse(browseId: String, params: String? = null): List<HomeVideoItem> =
        browseInternal(browseId, params, useAuthClient = false)

    /** Auth browse (TVHTML5 + OAuth, for personalized content like history/subs) */
    private suspend fun authBrowse(browseId: String, params: String? = null): List<HomeVideoItem> =
        browseInternal(browseId, params, useAuthClient = true)

    suspend fun getHomeFeed(): List<HomeVideoItem> {
        // Try auth first for personalized home, fallback to public WEB
        val token = getValidAccessToken()
        if (token != null) {
            val authResult = authBrowse("FEwhat_to_watch")
            if (authResult.isNotEmpty()) return authResult
        }
        return browse("FEwhat_to_watch")
    }

    suspend fun getHistory(): List<HomeVideoItem> = authBrowse("FEhistory")

    suspend fun getSubscriptionsFeed(): List<HomeVideoItem> = authBrowse("FEsubscriptions")

    suspend fun getLikedVideos(): List<HomeVideoItem> {
        val result = authBrowse("VLLL")
        return if (result.isNotEmpty()) result else authBrowse("FEliked_playlists")
    }

    suspend fun getWatchLater(): List<HomeVideoItem> = authBrowse("VLWL")

    suspend fun getTrending(): List<HomeVideoItem> =
        search("trending videos")

    suspend fun getMusicFeed(): List<HomeVideoItem> = withContext(Dispatchers.IO) {
        val token = getValidAccessToken()
        val accountMusic = mutableListOf<HomeVideoItem>()
        if (token != null) {
            try {
                coroutineScope {
                    val likedDeferred = async { getLikedVideos() }
                    val historyDeferred = async { getHistory() }
                    val subsDeferred = async { getSubscriptionsFeed() }
                    val homeDeferred = async { getHomeFeed() }

                    val liked = try { likedDeferred.await() } catch (e: Exception) { emptyList() }
                    val history = try { historyDeferred.await() } catch (e: Exception) { emptyList() }
                    val subs = try { subsDeferred.await() } catch (e: Exception) { emptyList() }
                    val home = try { homeDeferred.await() } catch (e: Exception) { emptyList() }

                    val seen = mutableSetOf<String>()
                    for (item in liked + history + subs + home) {
                        if (!item.isShort &&
                            (item.duration == 0L || item.duration >= 60L) &&
                            MusicFilter.isMusicTrack(item.title, item.channelName, item.duration, item.isShort) &&
                            seen.add(item.id)
                        ) {
                            accountMusic.add(item)
                        }
                    }
                }
                Log.d("AuthYTClient", "getMusicFeed() found ${accountMusic.size} personalized music items")
            } catch (e: Exception) {
                Log.e("AuthYTClient", "getMusicFeed account fetch failed", e)
            }
        }

        // Fetch top trending music songs
        val trendingSongs = try {
            search("trending songs official audio video")
        } catch (e: Exception) {
            emptyList()
        }

        val combined = mutableListOf<HomeVideoItem>()
        val seenIds = mutableSetOf<String>()
        // Prioritize account songs, then trending songs
        for (item in accountMusic + trendingSongs) {
            if (MusicFilter.isMusicTrack(item.title, item.channelName, item.duration) && seenIds.add(item.id)) {
                combined.add(item)
            }
        }

        if (combined.isNotEmpty()) {
            return@withContext combined
        }
        trendingSongs.ifEmpty { search("latest bollywood songs hindi hits") }
    }

    suspend fun getLikedMusic(): List<HomeVideoItem> = withContext(Dispatchers.IO) {
        val liked = getLikedVideos()
        val filtered = liked.filter { MusicFilter.isMusicTrack(it.title, it.channelName, it.duration) }
        if (filtered.isNotEmpty()) filtered else search("top hit songs official audio")
    }

    suspend fun getMusicHistory(): List<HomeVideoItem> = withContext(Dispatchers.IO) {
        val history = getHistory()
        val filtered = history.filter { MusicFilter.isMusicTrack(it.title, it.channelName, it.duration) }
        if (filtered.isNotEmpty()) filtered else search("latest hindi songs audio")
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
        val token = getValidAccessToken()
        val requestJson = JSONObject().apply {
            put("context", JSONObject().apply {
                put("client", JSONObject().apply {
                    if (token != null) {
                        // OAuth tokens are only accepted with a device client context —
                        // WEB + Bearer returns 400 INVALID_ARGUMENT (verified via probe).
                        put("clientName", "TVHTML5")
                        put("clientVersion", "7.20230412.08.00")
                    } else {
                        put("clientName", "WEB")
                        put("clientVersion", "2.20240101.00.00")
                    }
                    put("hl", "en")
                    put("gl", "IN")
                })
            })
            put("query", query)
        }

        try {
            val reqBuilder = Request.Builder()
                .url("https://youtubei.googleapis.com/youtubei/v1/search?key=${com.deepeye.musicpro.BuildConfig.YOUTUBE_API_KEY}")
                .addHeader("Content-Type", "application/json")
                .post(requestJson.toString().toRequestBody("application/json".toMediaType()))

            if (token != null) {
                reqBuilder.addHeader("Authorization", "Bearer $token")
            }

            val request = reqBuilder.build()
            val response = client.newCall(request).execute()
            if (response.code == 401 && token != null) {
                Log.e("AuthYTClient", "401 Unauthorized for search $query, retrying...")
                return@withContext handle401AndRetry(request)
            }
            if (!response.isSuccessful) {
                val errBody = try { response.body?.string()?.take(300) } catch (e: Exception) { null }
                Log.e("AuthYTClient", "search '$query' HTTP Error: ${response.code} - $errBody")
                return@withContext emptyList()
            }

            val jsonStr = response.body?.string() ?: ""
            val items = parseInnerTubeVideos(jsonStr)
            Log.d("AuthYTClient", "search '$query' returned ${items.size} items")
            items.distinctBy { it.id }
        } catch (e: Exception) {
            Log.e("AuthYTClient", "search failed for $query", e)
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
            ?: videoObj.optJSONObject("navigationEndpoint")?.optJSONObject("watchEndpoint")?.optString("videoId")
            ?: ""
        if (id.isEmpty()) return null

        val title = videoObj.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
            ?: videoObj.optJSONObject("title")?.optString("simpleText")
            ?: videoObj.optJSONObject("headline")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
            ?: videoObj.optJSONObject("headline")?.optString("simpleText")
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
            ?: videoObj.optJSONObject("viewCountText")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
            ?: ""
        val views = parseViews(viewText)

        val uploadDate = videoObj.optJSONObject("publishedTimeText")?.optString("simpleText")
            ?: videoObj.optJSONObject("publishedTimeText")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
            ?: ""

        val channelAvatar = getLastThumbnail(
            videoObj.optJSONObject("channelThumbnailSupportedRenderers")
                ?.optJSONObject("channelThumbnailWithLinkRenderer")
                ?.optJSONObject("thumbnail")
                ?.optJSONArray("thumbnails")
        ).ifEmpty {
            getLastThumbnail(videoObj.optJSONObject("channelThumbnail")?.optJSONArray("thumbnails"))
        }

        return HomeVideoItem(
            id = id,
            title = title,
            channelName = channelName,
            channelId = channelId,
            thumbnailUrl = thumbUrl,
            duration = durationSeconds,
            viewCount = views,
            uploadDate = uploadDate,
            isShort = durationSeconds in 1..60,
            channelAvatarUrl = channelAvatar
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
        var uploadDate = ""
        val lines = metaRenderer?.optJSONArray("lines")
        if (lines != null) {
            val allTexts = mutableListOf<String>()
            for (i in 0 until lines.length()) {
                val lineObj = lines.optJSONObject(i)?.optJSONObject("lineRenderer")
                val items = lineObj?.optJSONArray("items")
                if (items != null) {
                    for (j in 0 until items.length()) {
                        val itemObj = items.optJSONObject(j)?.optJSONObject("lineItemRenderer")
                        val text = itemObj?.optJSONObject("text")?.optString("simpleText")
                            ?: itemObj?.optJSONObject("text")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                            ?: ""
                        if (text.isNotBlank()) {
                            allTexts.add(text)
                        }
                    }
                }
            }
            for (t in allTexts) {
                val clean = t.trim()
                val isMeta = clean.contains("view", ignoreCase = true) ||
                    clean.contains("watching", ignoreCase = true) ||
                    clean.contains("ago", ignoreCase = true) ||
                    clean.contains("streamed", ignoreCase = true) ||
                    clean.contains("premier", ignoreCase = true) ||
                    clean == "•" || clean == "·" || clean == "|" || clean.isBlank()
                when {
                    clean.contains("view", ignoreCase = true) || clean.contains("watching", ignoreCase = true) -> {
                        if (viewText.isEmpty()) viewText = clean
                    }
                    clean.contains("ago", ignoreCase = true) || clean.contains("streamed", ignoreCase = true) || clean.contains("premier", ignoreCase = true) -> {
                        if (uploadDate.isEmpty()) uploadDate = clean
                    }
                    channelName.isEmpty() && !isMeta && !clean.matches(Regex("^[\\d.,]+[KkMmBb]?\\s*(views?|watching)?$")) -> {
                        channelName = clean
                    }
                    uploadDate.isEmpty() && channelName.isNotEmpty() && channelName != clean && !isMeta -> {
                        uploadDate = clean
                    }
                }
            }
            if (channelName.isEmpty()) {
                channelName = allTexts.firstOrNull {
                    val c = it.trim()
                    !c.contains("view", ignoreCase = true) && !c.contains("watching", ignoreCase = true) && c != "•" && c != "·" && c != "|" && c.isNotBlank()
                }?.trim() ?: ""
            }
        }

        val headerRenderer = tileObj.optJSONObject("header")?.optJSONObject("tileHeaderRenderer")
        val thumbUrl = getLastThumbnail(headerRenderer?.optJSONObject("thumbnail")?.optJSONArray("thumbnails"))

        val channelAvatar = getLastThumbnail(
            metaRenderer?.optJSONObject("avatar")?.optJSONObject("avatarViewModel")?.optJSONObject("image")?.optJSONArray("sources")
        ).ifEmpty {
            getLastThumbnail(metaRenderer?.optJSONObject("avatar")?.optJSONArray("thumbnails"))
        }.ifEmpty {
            getLastThumbnail(headerRenderer?.optJSONObject("avatar")?.optJSONObject("avatarViewModel")?.optJSONObject("image")?.optJSONArray("sources"))
        }

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
            uploadDate = uploadDate,
            isShort = durationSeconds in 1..60,
            channelAvatarUrl = channelAvatar
        )
    }

    private fun parseLockupViewModel(lockupObj: JSONObject): HomeVideoItem? {
        val id = lockupObj.optString("contentId")
        if (id.isEmpty()) return null

        val metaVm = lockupObj.optJSONObject("metadata")?.optJSONObject("lockupMetadataViewModel")
        val title = metaVm?.optJSONObject("title")?.optString("content") ?: ""

        var channelName = ""
        var viewText = ""
        var uploadDate = ""
        val metadataRows = metaVm?.optJSONObject("metadata")?.optJSONObject("contentMetadataViewModel")?.optJSONArray("metadataRows")
        val allTexts = mutableListOf<String>()
        if (metadataRows != null) {
            for (i in 0 until metadataRows.length()) {
                val parts = metadataRows.optJSONObject(i)?.optJSONArray("metadataParts")
                if (parts != null) {
                    for (j in 0 until parts.length()) {
                        val t = parts.optJSONObject(j)?.optJSONObject("text")?.optString("content")
                            ?: parts.optJSONObject(j)?.optJSONObject("text")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                            ?: ""
                        if (t.isNotBlank()) allTexts.add(t)
                    }
                }
            }
        }
        if (allTexts.isNotEmpty()) {
            for (t in allTexts) {
                val clean = t.trim()
                val isMeta = clean.contains("view", ignoreCase = true) ||
                    clean.contains("watching", ignoreCase = true) ||
                    clean.contains("ago", ignoreCase = true) ||
                    clean.contains("streamed", ignoreCase = true) ||
                    clean.contains("premier", ignoreCase = true) ||
                    clean == "•" || clean == "·" || clean == "|" || clean.isBlank()
                when {
                    clean.contains("view", ignoreCase = true) || clean.contains("watching", ignoreCase = true) -> {
                        if (viewText.isEmpty()) viewText = clean
                    }
                    clean.contains("ago", ignoreCase = true) || clean.contains("streamed", ignoreCase = true) || clean.contains("premier", ignoreCase = true) -> {
                        if (uploadDate.isEmpty()) uploadDate = clean
                    }
                    channelName.isEmpty() && !isMeta && !clean.matches(Regex("^[\\d.,]+[KkMmBb]?\\s*(views?|watching)?$")) -> {
                        channelName = clean
                    }
                    uploadDate.isEmpty() && channelName.isNotEmpty() && channelName != clean && !isMeta -> {
                        uploadDate = clean
                    }
                }
            }
            if (channelName.isEmpty()) {
                channelName = allTexts.firstOrNull {
                    val c = it.trim()
                    !c.contains("view", ignoreCase = true) && !c.contains("watching", ignoreCase = true) && c != "•" && c != "·" && c != "|" && c.isNotBlank()
                }?.trim() ?: ""
            }
        }

        val thumbUrl = lockupObj.optJSONObject("contentImage")?.optJSONObject("thumbnailViewModel")?.optJSONObject("image")?.optJSONArray("sources")?.optJSONObject(0)?.optString("url") ?: ""

        val channelAvatar = getLastThumbnail(
            metaVm?.optJSONObject("image")?.optJSONArray("sources")
        ).ifEmpty {
            getLastThumbnail(
                metaVm?.optJSONObject("decoratedAvatarViewModel")
                    ?.optJSONObject("avatar")
                    ?.optJSONObject("avatarViewModel")
                    ?.optJSONObject("image")
                    ?.optJSONArray("sources")
            )
        }.ifEmpty {
            getLastThumbnail(
                lockupObj.optJSONObject("contentImage")
                    ?.optJSONObject("decoratedAvatarViewModel")
                    ?.optJSONObject("avatar")
                    ?.optJSONObject("avatarViewModel")
                    ?.optJSONObject("image")
                    ?.optJSONArray("sources")
            )
        }

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
            uploadDate = uploadDate,
            isShort = durationSeconds in 1..60,
            channelAvatarUrl = channelAvatar
        )
    }

    private fun parseViews(viewText: String?): Long {
        if (viewText.isNullOrBlank()) return 0L
        val cleaned = viewText.lowercase().replace("views", "").replace("view", "").replace("watching", "").replace(",", "").trim()
        return try {
            when {
                cleaned.endsWith("m") -> (cleaned.removeSuffix("m").trim().toDouble() * 1_000_000).toLong()
                cleaned.endsWith("b") -> (cleaned.removeSuffix("b").trim().toDouble() * 1_000_000_000).toLong()
                cleaned.endsWith("k") -> (cleaned.removeSuffix("k").trim().toDouble() * 1_000).toLong()
                cleaned.endsWith("cr") -> (cleaned.removeSuffix("cr").trim().toDouble() * 10_000_000).toLong()
                cleaned.endsWith("lakh") -> (cleaned.removeSuffix("lakh").trim().toDouble() * 100_000).toLong()
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
        val last = thumbnails.optJSONObject(thumbnails.length() - 1)?.optString("url") ?: ""
        return if (last.startsWith("//")) "https:$last" else last
    }
}
