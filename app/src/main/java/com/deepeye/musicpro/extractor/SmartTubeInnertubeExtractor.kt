package com.deepeye.musicpro.extractor

import android.util.Log
import com.deepeye.musicpro.player.smarttube.DeepEyePlaybackFormat
import com.deepeye.musicpro.player.smarttube.SmartTubeCodecCapabilityChecker
import com.deepeye.musicpro.player.smarttube.SmartTubeFormatAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/**
 * SmartTube-style direct Google Innertube client.
 *
 * Replaces the NewPipe (`org.schabi.newpipe.extractor`) backend. Instead of scraping
 * the web through the newpipe extractor library, this talks straight to YouTube's
 * internal `youtubei/v1/...` endpoints (player, search, browse, next) — the same
 * approach the official YouTube apps use — returning direct `googlevideo.com`
 * playback URLs from the ANDROID player response.
 */
class SmartTubeInnertubeExtractor(
    private val client: OkHttpClient,
    private val formatAdapter: SmartTubeFormatAdapter = SmartTubeFormatAdapter(SmartTubeCodecCapabilityChecker()),
    /** Optional OAuth access-token provider; authenticated player requests bypass YouTube's bot check. */
    private val accessTokenProvider: (suspend () -> String?)? = null,
) : IExtractorBridge {

    private companion object {
        const val TAG = "SmartTubeInnertube"
        const val INNERTUBE_BASE = "https://www.youtube.com/youtubei/v1/"
        const val UA =
            "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

        // Player requests must use the Android app client identity. Older identities
        // (ANDROID_VR 1.62.27, plain ANDROID 19.09.37) are now rejected by YouTube:
        //   ANDROID_VR   -> HTTP 200, playabilityStatus=LOGIN_REQUIRED ("not a bot")
        //   ANDROID 19.x -> HTTP 400 (attestation/PO-token enforcement)
        // The 20.x Android app identity still returns direct, signature-free
        // googlevideo.com URLs (verified against live /player on 2026-08-30).
        const val ANDROID_UA =
            "com.google.android.youtube/20.10.33 " +
                "(Linux; U; Android 12) gzip"

        const val IOS_UA =
            "com.google.ios.youtube/20.10.1 (iPhone16,2; U; CPU iOS 18_3 like Mac OS X)"

        // "3:45" or "1:01:02" -> seconds
        fun parseDurationSeconds(text: String?): Long {
            if (text.isNullOrBlank()) return 0L
            val parts = text.trim().split(":")
            return when {
                parts.size == 3 -> {
                    (parts[0].toLongOrNull() ?: 0L) * 3600 +
                        (parts[1].toLongOrNull() ?: 0L) * 60 +
                        (parts[2].toLongOrNull() ?: 0L)
                }
                parts.size == 2 -> {
                    (parts[0].toLongOrNull() ?: 0L) * 60 +
                        (parts[1].toLongOrNull() ?: 0L)
                }
                else -> parts.firstOrNull()?.toLongOrNull() ?: 0L
            }
        }

        // "1,234,567 views" / "1.2M views" -> count
        fun parseViewCount(text: String?): Long {
            if (text.isNullOrBlank()) return 0L
            val cleaned = text.lowercase().replace("views", "")
                .replace("view", "").replace(",", "").trim()
            return when {
                cleaned.endsWith("b") ->
                    (cleaned.removeSuffix("b").trim().toDoubleOrNull() ?: 0.0).times(1_000_000_000).toLong()
                cleaned.endsWith("m") ->
                    (cleaned.removeSuffix("m").trim().toDoubleOrNull() ?: 0.0).times(1_000_000).toLong()
                cleaned.endsWith("k") ->
                    (cleaned.removeSuffix("k").trim().toDoubleOrNull() ?: 0.0).times(1_000).toLong()
                else -> cleaned.filter { it.isDigit() }.toLongOrNull() ?: 0L
            }
        }

        // First run of a title/text object, e.g. `{"runs":[{"text":"..."}]}`.
        fun runsText(obj: JSONObject?): String {
            if (obj == null) return ""
            obj.optString("simpleText").trim().takeIf { it.isNotEmpty() }?.let { return it }
            val runs = obj.optJSONArray("runs") ?: return ""
            if (runs.length() == 0) return ""
            return runs.optJSONObject(0)?.optString("text", "") ?: ""
        }

        fun lastThumb(arr: JSONArray?): String {
            if (arr == null || arr.length() == 0) return ""
            val last = arr.optJSONObject(arr.length() - 1)?.optString("url", "") ?: ""
            return if (last.startsWith("//")) "https:$last" else last
        }

        /** Best-effort extraction of a playback URL, honouring direct url / signatureCipher. */
        fun streamUrl(f: JSONObject): String? {
            f.optString("url").trim().takeIf { it.isNotEmpty() }?.let { return it }
            val cipher = f.optString("signatureCipher").ifEmpty { f.optString("cipher") }
            if (cipher.isNotEmpty()) {
                val params = cipher.split("&").mapNotNull {
                    val idx = it.indexOf('=')
                    if (idx > 0) it.substring(0, idx) to it.substring(idx + 1) else null
                }.toMap()
                val rawUrl = params["url"] ?: return null
                val url = try { java.net.URLDecoder.decode(rawUrl, "UTF-8") } catch (_: Exception) { rawUrl }
                val sp = params["sp"] ?: "sig"
                val sig = params["s"] ?: params["sig"]
                val decodedSig = if (sig != null) try { java.net.URLDecoder.decode(sig, "UTF-8") } catch (_: Exception) { sig } else null
                if (decodedSig != null) {
                    return if (url.contains("?") && !url.contains("$sp=")) {
                        "$url&$sp=$decodedSig"
                    } else if (!url.contains("?")) {
                        "$url?$sp=$decodedSig"
                    } else {
                        url
                    }
                }
                return url
            }
            return null
        }

        /** True when the video ID looks valid (11 chars of YouTube alphabet). */
        fun isValidVideoId(id: String): Boolean = Regex("^[A-Za-z0-9_-]{11}$").matches(id)
    }

    override suspend fun initBridge(downloader: Any) {
        // No classloader bridging needed: this client has no third-party extractor dependency.
    }

    // ─────────────────────────── Innertube HTTP layer ───────────────────────────

    private fun androidClientContext(): JSONObject {
        // ANDROID app client 20.10.33: verified working for /player without PO tokens.
        // Returns streamingData with direct `url` entries (no signatureCipher).
        // Older identities (ANDROID_VR, ANDROID 19.x) are rejected by YouTube's bot gate.
        val clientCtx = JSONObject()
        clientCtx.put("clientName", "ANDROID")
        clientCtx.put("clientVersion", "20.10.33")
        clientCtx.put("androidSdkVersion", 32)
        clientCtx.put("osName", "Android")
        clientCtx.put("osVersion", "12")
        clientCtx.put("deviceModel", "Pixel 7")
        clientCtx.put("deviceMake", "Google")
        clientCtx.put("hl", "en")
        clientCtx.put("gl", "IN")
        clientCtx.put("utcOffsetMinutes", 330)
        val context = JSONObject()
        context.put("client", clientCtx)
        return context
    }

    private fun webClientContext(): JSONObject {
        val clientCtx = JSONObject()
        clientCtx.put("clientName", "WEB")
        clientCtx.put("clientVersion", "2.20240701.00.00")
        clientCtx.put("hl", "en")
        clientCtx.put("gl", "IN")
        val context = JSONObject()
        context.put("client", clientCtx)
        return context
    }

    private fun iosClientContext(): JSONObject {
        val clientCtx = JSONObject()
        clientCtx.put("clientName", "IOS")
        clientCtx.put("clientVersion", "20.10.1")
        clientCtx.put("deviceModel", "iPhone16,2")
        clientCtx.put("osName", "iOS")
        clientCtx.put("osVersion", "18.3.0.22D63")
        clientCtx.put("hl", "en")
        clientCtx.put("gl", "IN")
        clientCtx.put("utcOffsetMinutes", 330)
        val context = JSONObject()
        context.put("client", clientCtx)
        return context
    }

    private suspend fun postInnertube(
        endpoint: String,
        body: JSONObject,
        userAgent: String = UA,
        authToken: String? = null,
    ): String? = withContext(Dispatchers.IO) {
        val url = INNERTUBE_BASE + endpoint + "?prettyPrint=false"
        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", userAgent)
            .addHeader("Accept-Language", "hi-IN,en-IN;q=0.9,en-US;q=0.8")
            .addHeader("Content-Type", "application/json")
            .apply {
                if (userAgent == IOS_UA) {
                    addHeader("X-YouTube-Client-Name", "5")
                    addHeader("X-YouTube-Client-Version", "20.10.1")
                } else if (userAgent == ANDROID_UA) {
                    addHeader("X-YouTube-Client-Name", "3")
                    addHeader("X-YouTube-Client-Version", "20.10.33")
                }
                if (!authToken.isNullOrBlank()) {
                    addHeader("Authorization", "Bearer $authToken")
                }
            }
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        try {
            client.newCall(request).execute().use { res ->
                if (!res.isSuccessful) {
                    Log.e(TAG, "innertube $endpoint HTTP=${res.code} msg=${res.message}")
                    return@use null
                }
                res.body?.string()
            }
        } catch (e: Exception) {
            Log.e(TAG, "innertube $endpoint failed reason=${e.message}", e)
            null
        }
    }

    /** Recursively return the first JSONObject stored under the given key name. */
    private fun findObject(root: JSONObject, key: String): JSONObject? {
        if (root.has(key) && root.get(key) is JSONObject) return root.getJSONObject(key)
        val iter = root.keys()
        while (iter.hasNext()) {
            val k = iter.next()
            when (val v = root.opt(k)) {
                is JSONObject -> findObject(v, key)?.let { return it }
                is JSONArray -> for (i in 0 until v.length()) {
                    val item = v.optJSONObject(i) ?: continue
                    findObject(item, key)?.let { return it }
                }
                else -> {}
            }
        }
        return null
    }

    private fun collectObjects(root: JSONObject, key: String): List<JSONObject> {
        val out = mutableListOf<JSONObject>()
        fun walk(o: Any?) {
            when (o) {
                is JSONObject -> {
                    if (o.has(key) && o.get(key) is JSONObject) out.add(o.getJSONObject(key))
                    val it = o.keys()
                    while (it.hasNext()) walk(o.opt(it.next()))
                }
                is JSONArray -> for (i in 0 until o.length()) walk(o.opt(i))
                else -> {}
            }
        }
        walk(root)
        return out
    }

    /**
     * Locates the search-result item list.
     * First page: sectionListRenderer.contents. Pagination: appendContinuationItemsAction.
     * Returns (itemsArray, continuationToken).
     */
    private fun searchContents(root: JSONObject): Pair<JSONArray, String?> {
        findObject(root, "sectionListRenderer")?.let { section ->
            section.optJSONArray("contents")?.let { return it to continuationToken(it) }
        }
        val commands = root.optJSONArray("onResponseReceivedCommands")
        if (commands != null && commands.length() > 0) {
            val arr = commands.optJSONObject(0)
                ?.optJSONObject("appendContinuationItemsAction")
                ?.optJSONArray("continuationItems")
            if (arr != null) return arr to continuationToken(arr)
        }
        return JSONArray() to null
    }

    private fun continuationToken(items: JSONArray): String? {
        for (i in 0 until items.length()) {
            val obj = items.optJSONObject(i) ?: continue
            val tok = obj.optJSONObject("continuationItemRenderer")
                ?.optJSONObject("continuationEndpoint")
                ?.optJSONObject("continuationCommand")
                ?.optString("token")
            if (!tok.isNullOrEmpty()) return tok
        }
        return null
    }

    private fun parseSearchResponse(resp: String): ExtractorSearchResultPage {
        val root = try {
            JSONObject(resp)
        } catch (e: Exception) {
            Log.e(TAG, "search parse failed: ${e.message}", e)
            return ExtractorSearchResultPage(emptyList(), null)
        }
        val (contents, token) = searchContents(root)
        val items = mutableListOf<ExtractorVideoItem>()
        for (i in 0 until contents.length()) {
            val section = contents.optJSONObject(i) ?: continue
            val itemSection = section.optJSONObject("itemSectionRenderer")
                ?.optJSONArray("contents") ?: continue
            for (j in 0 until itemSection.length()) {
                val entry = itemSection.optJSONObject(j) ?: continue
                entry.optJSONObject("videoRenderer")?.let { items += parseVideoRenderer(it) }
                entry.optJSONObject("reelItemRenderer")?.let { items += parseReelRenderer(it) }
            }
        }
        return ExtractorSearchResultPage(items.filter { it.id.isNotEmpty() }, token)
    }

    private fun parseVideoRenderer(v: JSONObject): ExtractorVideoItem {
        val ownerName = runsText(v.optJSONObject("ownerText")).takeIf { it.isNotEmpty() }
        val bylineName = runsText(v.optJSONObject("shortBylineText"))
        val channelName = ownerName ?: bylineName
        val browseId = v.optJSONObject("ownerText")?.optJSONArray("runs")
            ?.optJSONObject(0)?.optJSONObject("navigationEndpoint")
            ?.optJSONObject("browseEndpoint")?.optString("browseId")
            ?: v.optJSONObject("navigationEndpoint")
                ?.optJSONObject("browseEndpoint")?.optString("browseId")
            ?: ""
        val duration = parseDurationSeconds(runsText(v.optJSONObject("lengthText")))
        // Channel avatar lives under ownerText runs[0].thumbnail (search / WEB client).
        val channelAvatar = lastThumb(
            v.optJSONObject("ownerText")?.optJSONArray("runs")
                ?.optJSONObject(0)?.optJSONObject("thumbnail")
                ?.optJSONArray("thumbnails")
        )
        return ExtractorVideoItem(
            id = v.optString("videoId", ""),
            title = runsText(v.optJSONObject("title")),
            artist = channelName,
            duration = duration,
            thumbnailUrl = lastThumb(v.optJSONObject("thumbnail")?.optJSONArray("thumbnails")),
            viewCount = parseViewCount(runsText(v.optJSONObject("viewCountText")).ifEmpty {
                runsText(v.optJSONObject("shortViewCountText"))
            }),
            isShort = duration in 1..60,
            channelAvatarUrl = channelAvatar,
            channelId = browseId,
        )
    }

    private fun parseReelRenderer(r: JSONObject): ExtractorVideoItem {
        val title = runsText(r.optJSONObject("headline"))
            .ifEmpty { runsText(r.optJSONObject("title")) }
        return ExtractorVideoItem(
            id = r.optString("videoId", ""),
            title = title,
            artist = runsText(r.optJSONObject("shortBylineText")),
            duration = 0L,
            thumbnailUrl = lastThumb(r.optJSONObject("thumbnail")?.optJSONArray("thumbnails")),
            viewCount = parseViewCount(runsText(r.optJSONObject("viewCount"))),
            isShort = true,
            channelAvatarUrl = "",
            channelId = "",
        )
    }

    private fun parseCompactVideoRenderer(cv: JSONObject): ExtractorVideoItem {
        val channelName = runsText(cv.optJSONObject("shortBylineText"))
            .ifEmpty { runsText(cv.optJSONObject("longBylineText")) }
        return ExtractorVideoItem(
            id = cv.optString("videoId", ""),
            title = runsText(cv.optJSONObject("title")),
            artist = channelName,
            duration = parseDurationSeconds(runsText(cv.optJSONObject("lengthText"))),
            thumbnailUrl = lastThumb(cv.optJSONObject("thumbnail")?.optJSONArray("thumbnails")),
            viewCount = parseViewCount(runsText(cv.optJSONObject("viewCountText"))),
            isShort = false,
            channelAvatarUrl = "",
            channelId = "",
        )
    }

    // ─────────────────────────── Public search API ───────────────────────────

    override suspend fun searchVideosFirstPage(query: String): ExtractorSearchResultPage = withContext(Dispatchers.IO) {
        val body = JSONObject()
        body.put("context", webClientContext())
        body.put("query", query)
        val resp = postInnertube("search", body) ?: return@withContext ExtractorSearchResultPage(emptyList(), null)
        parseSearchResponse(resp)
    }

    override suspend fun searchVideosNextPage(query: String, nextPageUrl: String): ExtractorSearchResultPage = withContext(Dispatchers.IO) {
        val body = JSONObject()
        body.put("context", webClientContext())
        body.put("continuation", nextPageUrl)
        val resp = postInnertube("search", body) ?: return@withContext ExtractorSearchResultPage(emptyList(), null)
        parseSearchResponse(resp)
    }

    override suspend fun getTrending(): List<ExtractorVideoItem> = withContext(Dispatchers.IO) {
        searchVideosFirstPage("trending music").videos
    }

    override suspend fun searchMusic(query: String): List<ExtractorMusicItem> = withContext(Dispatchers.IO) {
        val page = searchVideosFirstPage("$query official audio song")
        page.videos
            .filterNot { it.isShort }
            .filter { it.duration == 0L || it.duration >= 60L }
            .filter { com.deepeye.musicpro.data.source.remote.youtube.MusicFilter.isMusicTrack(it.title, it.artist, it.duration, it.isShort) }
            .take(24)
            .map {
                ExtractorMusicItem(
                    id = it.id,
                    title = it.title,
                    artist = it.artist,
                    album = "YouTube",
                    duration = if (it.duration > 0) it.duration else 0L,
                    thumbnailUrl = it.thumbnailUrl,
                )
            }
    }

    override suspend fun getShorts(): List<ExtractorVideoItem> = withContext(Dispatchers.IO) {
        searchVideosFirstPage("trending shorts").videos
            .map { it.copy(isShort = true) }
            .filter { it.id.isNotEmpty() }
    }

    override suspend fun extractStream(videoId: String, preferVideo: Boolean): ExtractorStreamResult? = withContext(Dispatchers.IO) {
        // IOS client is tried FIRST - returns official HLS master playlist and rich multi-res streams.
        // ANDROID and WEB clients act as robust fallbacks.
        val contexts = listOf(
            androidClientContext() to ANDROID_UA,
            iosClientContext() to IOS_UA,
            webClientContext() to UA
        )

        val authToken = try {
            accessTokenProvider?.invoke()?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            Log.w(TAG, "accessTokenProvider failed reason=${e.message}")
            null
        }

        for ((context, userAgent) in contexts) {
            val body = JSONObject()
            body.put("context", context)
            body.put("videoId", videoId)
            body.put("contentCheckOk", true)
            body.put("racyCheckOk", true)

            // Try anonymous first for minimal latency, then fall back to authToken if required
            var resp = postInnertube("player", body, userAgent = userAgent, authToken = null)
            if (resp == null && authToken != null) {
                resp = postInnertube("player", body, userAgent = userAgent, authToken = authToken)
            }
            if (resp == null) continue

            val root = try { JSONObject(resp) } catch (e: Exception) { continue }
            if (!root.has("streamingData")) {
                val status = root.optJSONObject("playabilityStatus")?.optString("status", "")
                if (status == "LOGIN_REQUIRED" && authToken != null) {
                    val authResp = postInnertube("player", body, userAgent = userAgent, authToken = authToken)
                    if (authResp != null) {
                        try {
                            val authRoot = JSONObject(authResp)
                            if (authRoot.has("streamingData")) {
                                val authStreaming = authRoot.getJSONObject("streamingData")
                                val authAdaptive = authStreaming.optJSONArray("adaptiveFormats")
                                val authFormats = authStreaming.optJSONArray("formats")
                                val authDash = authStreaming.optString("dashManifestUrl").takeIf { it.isNotEmpty() }
                                val authHls = authStreaming.optString("hlsManifestUrl").takeIf { it.isNotEmpty() }
                                val (authVideoFormats, authAudioFormats) = formatAdapter.parseStreamingData(authStreaming, { streamUrl(it) })
                                val authDashUri = authDash ?: com.deepeye.musicpro.player.smarttube.SmartTubeDashManifestGenerator.generateDashDataUri(authStreaming) { streamUrl(it) }
                                val authRes = if (authHls != null) {
                                    ExtractorStreamResult(
                                        url = authHls,
                                        container = "hls",
                                        quality = "HLS",
                                        extractorName = "SmartTubeInnertube",
                                        videoFormats = authVideoFormats,
                                        audioFormats = authAudioFormats,
                                        dashManifestUrl = authDashUri,
                                        hlsManifestUrl = authHls
                                    )
                                } else {
                                    bestProgressive(authFormats, authVideoFormats, authAudioFormats, authDashUri, authHls)
                                        ?: if (authDashUri != null && authVideoFormats.isNotEmpty() && authAudioFormats.isNotEmpty()) {
                                            ExtractorStreamResult(
                                                url = authDashUri,
                                                container = "adaptive",
                                                quality = "DASH",
                                                extractorName = "SmartTubeInnertube",
                                                videoFormats = authVideoFormats,
                                                audioFormats = authAudioFormats,
                                                dashManifestUrl = authDashUri,
                                                hlsManifestUrl = authHls
                                            )
                                        } else if (preferVideo) {
                                            bestAdaptiveVideo(authAdaptive, authVideoFormats, authAudioFormats, authDashUri, authHls)
                                        } else {
                                            bestAdaptiveAudio(authAdaptive, authVideoFormats, authAudioFormats, authDashUri, authHls)
                                        }
                                }
                                if (authRes != null) return@withContext authRes
                            }
                        } catch (_: Exception) {}
                    }
                }
                continue
            }

            val streaming = root.getJSONObject("streamingData")
            val adaptive = streaming.optJSONArray("adaptiveFormats")
            val formats = streaming.optJSONArray("formats")
            val dashManifest = streaming.optString("dashManifestUrl").takeIf { it.isNotEmpty() }
            val hlsManifest = streaming.optString("hlsManifestUrl").takeIf { it.isNotEmpty() }

            val (allVideoFormats, allAudioFormats) = formatAdapter.parseStreamingData(streaming, { streamUrl(it) })
            val dashDataUri = dashManifest ?: com.deepeye.musicpro.player.smarttube.SmartTubeDashManifestGenerator.generateDashDataUri(streaming) { streamUrl(it) }

            val result = if (hlsManifest != null) {
                ExtractorStreamResult(
                    url = hlsManifest,
                    container = "hls",
                    quality = "HLS",
                    extractorName = "SmartTubeInnertube",
                    videoFormats = allVideoFormats,
                    audioFormats = allAudioFormats,
                    dashManifestUrl = dashDataUri,
                    hlsManifestUrl = hlsManifest
                )
            } else {
                bestProgressive(formats, allVideoFormats, allAudioFormats, dashDataUri, hlsManifest)
                    ?: if (dashDataUri != null && allVideoFormats.isNotEmpty() && allAudioFormats.isNotEmpty()) {
                        ExtractorStreamResult(
                            url = dashDataUri,
                            container = "adaptive",
                            quality = "DASH",
                            extractorName = "SmartTubeInnertube",
                            videoFormats = allVideoFormats,
                            audioFormats = allAudioFormats,
                            dashManifestUrl = dashDataUri,
                            hlsManifestUrl = hlsManifest
                        )
                    } else if (preferVideo) {
                        bestAdaptiveVideo(adaptive, allVideoFormats, allAudioFormats, dashDataUri, hlsManifest)
                    } else {
                        bestAdaptiveAudio(adaptive, allVideoFormats, allAudioFormats, dashDataUri, hlsManifest)
                    }
            }

            if (result != null) {
                return@withContext result
            }
        }

        Log.w(TAG, "No stream format found for videoId=$videoId preferVideo=$preferVideo across all client identities")
        null
    }

    private fun bestProgressive(
        formats: JSONArray?,
        videoFormats: List<DeepEyePlaybackFormat> = emptyList(),
        audioFormats: List<DeepEyePlaybackFormat> = emptyList(),
        dashManifestUrl: String? = null,
        hlsManifestUrl: String? = null
    ): ExtractorStreamResult? {
        if (formats == null) return null
        var best: JSONObject? = null
        var bestScore = -1
        for (i in 0 until formats.length()) {
            val f = formats.optJSONObject(i) ?: continue
            val resolvedUrl = streamUrl(f) ?: continue
            val mime = f.optString("mimeType", "")
            val hasVideo = mime.contains("video")
            val hasAudio = mime.contains("audio") || mime.contains("mp4a") || f.has("audioQuality")
            if (!hasVideo && !hasAudio) continue
            val hasRateBypass = resolvedUrl.contains("ratebypass=yes") || f.optString("url").contains("ratebypass=yes")
            val bitrate = f.optInt("bitrate", 0)
            // Priority: (1) ratebypass=yes (immune to 403 throttling), (2) muxed audio+video, (3) bitrate
            val score = (if (hasRateBypass) 10_000_000 else 0) + (if (hasAudio) 1_000_000 else 0) + bitrate
            if (score > bestScore) {
                bestScore = score
                best = f
            }
        }
        return best?.let {
            val resolved = streamUrl(it)!!
            ExtractorStreamResult(
                url = resolved,
                itag = it.optInt("itag", -1),
                container = "",
                quality = it.optString("qualityLabel").ifEmpty { "${it.optInt("height", 0)}p" },
                extractorName = "SmartTubeInnertube",
                videoFormats = videoFormats,
                audioFormats = audioFormats,
                dashManifestUrl = dashManifestUrl,
                hlsManifestUrl = hlsManifestUrl
            )
        }
    }

    private fun bestAdaptiveVideo(
        adaptive: JSONArray?,
        videoFormats: List<DeepEyePlaybackFormat> = emptyList(),
        audioFormats: List<DeepEyePlaybackFormat> = emptyList(),
        dashManifestUrl: String? = null,
        hlsManifestUrl: String? = null
    ): ExtractorStreamResult? {
        if (adaptive == null) return null
        var best: JSONObject? = null
        var bestHeight = -1
        for (i in 0 until adaptive.length()) {
            val f = adaptive.optJSONObject(i) ?: continue
            if (streamUrl(f) == null) continue
            val mime = f.optString("mimeType", "")
            if (!mime.contains("video") || mime.contains("audio")) continue
            val h = f.optInt("height", 0)
            if (h > bestHeight) {
                bestHeight = h
                best = f
            }
        }
        return best?.let {
            val resolved = streamUrl(it)!!
            ExtractorStreamResult(
                url = resolved,
                itag = it.optInt("itag", -1),
                container = "adaptive",
                quality = "${it.optInt("height", 0)}p",
                extractorName = "SmartTubeInnertube",
                videoFormats = videoFormats,
                audioFormats = audioFormats,
                dashManifestUrl = dashManifestUrl,
                hlsManifestUrl = hlsManifestUrl
            )
        }
    }

    private fun bestAdaptiveAudio(
        adaptive: JSONArray?,
        videoFormats: List<DeepEyePlaybackFormat> = emptyList(),
        audioFormats: List<DeepEyePlaybackFormat> = emptyList(),
        dashManifestUrl: String? = null,
        hlsManifestUrl: String? = null
    ): ExtractorStreamResult? {
        if (adaptive == null) return null
        var best: JSONObject? = null
        var bestBitrate = 0
        for (i in 0 until adaptive.length()) {
            val f = adaptive.optJSONObject(i) ?: continue
            if (streamUrl(f) == null) continue
            val mime = f.optString("mimeType", "")
            if (!mime.contains("audio")) continue
            val br = f.optInt("bitrate", 0)
            if (br > bestBitrate) {
                bestBitrate = br
                best = f
            }
        }
        return best?.let {
            val resolved = streamUrl(it)!!
            ExtractorStreamResult(
                url = resolved,
                itag = it.optInt("itag", -1),
                container = "audio",
                quality = "${it.optInt("bitrate", 0) / 1000} kbps",
                extractorName = "SmartTubeInnertube",
                videoFormats = videoFormats,
                audioFormats = audioFormats,
                dashManifestUrl = dashManifestUrl,
                hlsManifestUrl = hlsManifestUrl
            )
        }
    }

    private suspend fun relatedVideos(videoId: String): List<ExtractorVideoItem> {
        val id = videoId.trim().substringBefore(" ").takeIf { isValidVideoId(it) } ?: return emptyList()
        val body = JSONObject()
        body.put("context", webClientContext())
        body.put("videoId", id)
        val resp = postInnertube("next", body) ?: return emptyList()
        val root = try { JSONObject(resp) } catch (e: Exception) {
            Log.e(TAG, "next parse failed: ${e.message}", e)
            return emptyList()
        }
        return collectObjects(root, "compactVideoRenderer")
            .map { parseCompactVideoRenderer(it) }
            .filter { it.id.isNotEmpty() }
            .distinctBy { it.id }
            .take(24)
    }

    override suspend fun getRelatedVideos(videoId: String): List<ExtractorVideoItem> = withContext(Dispatchers.IO) {
        relatedVideos(videoId)
    }

    override suspend fun getRelatedMusic(videoId: String): List<ExtractorMusicItem> = withContext(Dispatchers.IO) {
        // The datasource historically passes a "<title> <artist>" string here; resolve
        // it to a video id (direct match, otherwise first search hit) before calling next.
        val id = videoId.trim().substringBefore(" ").takeIf { isValidVideoId(it) }
            ?: searchVideosFirstPage(videoId.trim()).videos.firstOrNull()?.id
        if (id == null) return@withContext emptyList()
        relatedVideos(id).map {
            ExtractorMusicItem(
                id = it.id,
                title = it.title,
                artist = it.artist,
                album = "YouTube",
                duration = it.duration,
                thumbnailUrl = it.thumbnailUrl,
            )
        }
    }

    override suspend fun getSearchSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val url = "https://suggestqueries.google.com/complete/search?client=firefox&ds=yt&hl=en&q=" +
            URLEncoder.encode(query, "UTF-8")
        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", UA)
            .build()
        try {
            client.newCall(request).execute().use { res ->
                if (!res.isSuccessful) return@use emptyList<String>()
                val body = res.body?.string() ?: return@use emptyList()
                val arr = try { JSONArray(body) } catch (e: Exception) {
                    Log.w(TAG, "suggestions JSON malformed: ${e.message}")
                    return@use emptyList()
                }
                (arr.optJSONArray(1) ?: return@use emptyList()).let { suggestions ->
                    (0 until suggestions.length()).map { suggestions.optString(it) }.filter { it.isNotBlank() }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "suggestions failed reason=${e.message}", e)
            emptyList()
        }
    }
}