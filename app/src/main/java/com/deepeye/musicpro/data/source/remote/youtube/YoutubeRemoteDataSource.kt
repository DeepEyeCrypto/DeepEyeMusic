// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.source.remote.youtube

import android.util.Log
import com.deepeye.musicpro.domain.model.home.HomeMusicItem
import com.deepeye.musicpro.domain.model.home.HomeVideoItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YoutubeRemoteDataSource
@Inject
constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val client: okhttp3.OkHttpClient,
    private val rankingManager: com.deepeye.musicpro.diagnostics.ExtractionRankingManager,
    private val headlessExtractor: HeadlessWebViewExtractor,
    private val settingsDataStore: com.deepeye.musicpro.data.prefs.SettingsDataStore,
    private val formatAdapter: com.deepeye.musicpro.player.smarttube.SmartTubeFormatAdapter = com.deepeye.musicpro.player.smarttube.SmartTubeFormatAdapter(com.deepeye.musicpro.player.smarttube.SmartTubeCodecCapabilityChecker()),
) {
    // SmartTube-style direct Innertube client (replaces the NewPipe extractor backend).
    // OAuth token attached to /player requests bypasses the "confirm you're not a bot" gate.
    private val extractorClient: okhttp3.OkHttpClient by lazy {
        client.newBuilder()
            .connectionPool(okhttp3.ConnectionPool(8, 3, java.util.concurrent.TimeUnit.MINUTES))
            .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private val extractor by lazy {
        com.deepeye.musicpro.extractor.SmartTubeInnertubeExtractor(
            client = extractorClient,
            formatAdapter = formatAdapter
        ) {
            settingsDataStore.settings.first().youtubeAccessToken?.takeIf { it.isNotBlank() }
        }
    }
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    private val fastClient: okhttp3.OkHttpClient by lazy {
        client.newBuilder()
            .connectTimeout(2, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(2, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(2, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    // 🔍 Search videos
    suspend fun searchVideos(query: String): List<HomeVideoItem> = searchVideosFirstPage(query).items

    suspend fun getVideoDetails(videoId: String): com.deepeye.musicpro.domain.model.VideoDetails? = withContext(ioDispatcher) {
        try {
            val pipedInstances = listOf(
                "https://api.piped.private.coffee",
                "https://pipedapi.kavin.rocks",
                "https://pipedapi.us.projectsegfau.lt",
                "https://pipedapi.colt.top"
            ).shuffled()
            
            var result: com.deepeye.musicpro.domain.model.VideoDetails? = null
            for (instance in pipedInstances) {
                try {
                    val request = okhttp3.Request.Builder()
                        .url("$instance/streams/$videoId")
                        .addHeader("User-Agent", "DeepEyeMusicPro/2.0")
                        .build()
                    val response = fastClient.newCall(request).execute()
                    val body = response.body?.string()
                    if (response.isSuccessful && body != null) {
                        val json = org.json.JSONObject(body)
                        val views = json.optLong("views", 0L)
                        val uploader = json.optString("uploader", "")
                        val uploaderAvatar = json.optString("uploaderAvatar", "")
                        val subscriberCount = json.optLong("uploaderSubscriberCount", 0L)
                        val uploadDate = json.optString("uploadDate", "")
                        
                        result = com.deepeye.musicpro.domain.model.VideoDetails(
                            viewCount = views,
                            subscriberCount = subscriberCount,
                            uploadDate = com.deepeye.musicpro.util.formatUploadDate(uploadDate),
                            channelName = uploader,
                            channelAvatarUrl = uploaderAvatar
                        )
                        // #region agent log
                        try {
                            android.util.Log.i("DBG_B5FA56", org.json.JSONObject()
                                .put("sessionId", "b5fa56").put("hypothesisId", "E")
                                .put("location", "YoutubeRemoteDataSource.kt:getVideoDetails")
                                .put("message", "piped_ok").put("timestamp", System.currentTimeMillis())
                                .put("runId", "pre-fix")
                                .put("data", org.json.JSONObject()
                                    .put("host", instance.substringAfter("://").substringBefore("/"))
                                    .put("views", views).put("subs", subscriberCount)
                                    .put("hasAvatar", uploaderAvatar.isNotBlank())
                                    .put("uploaderLen", uploader.length)
                                ).toString())
                        } catch (_: Exception) {}
                        // #endregion
                        break
                    } else {
                        // #region agent log
                        try {
                            android.util.Log.i("DBG_B5FA56", org.json.JSONObject()
                                .put("sessionId", "b5fa56").put("hypothesisId", "E")
                                .put("location", "YoutubeRemoteDataSource.kt:getVideoDetails")
                                .put("message", "piped_http_fail").put("timestamp", System.currentTimeMillis())
                                .put("runId", "pre-fix")
                                .put("data", org.json.JSONObject()
                                    .put("host", instance.substringAfter("://").substringBefore("/"))
                                    .put("http", response.code)
                                ).toString())
                        } catch (_: Exception) {}
                        // #endregion
                    }
                } catch (e: Exception) {
                    // #region agent log
                    try {
                        android.util.Log.i("DBG_B5FA56", org.json.JSONObject()
                            .put("sessionId", "b5fa56").put("hypothesisId", "E")
                            .put("location", "YoutubeRemoteDataSource.kt:getVideoDetails")
                            .put("message", "piped_exc").put("timestamp", System.currentTimeMillis())
                            .put("runId", "pre-fix")
                            .put("data", org.json.JSONObject()
                                .put("host", instance.substringAfter("://").substringBefore("/"))
                                .put("type", e.javaClass.simpleName)
                            ).toString())
                    } catch (_: Exception) {}
                    // #endregion
                    continue
                }
            }
            result
        } catch (e: Exception) {
            null
        }
    }

    suspend fun searchVideosFirstPage(query: String): SearchResultPage =
        withContext(ioDispatcher) {
            try {
                val page = extractor.searchVideosFirstPage(query)
                SearchResultPage(page.videos.map { it.toHomeVideoItem() }, page.nextPageUrl)
            } catch (e: Exception) {
                Log.e("YoutubeDS", "searchVideosFirstPage failed: ${e.message}")
                SearchResultPage(emptyList(), null)
            }
        }

    suspend fun searchVideosNextPage(
        query: String,
        nextPageUrl: String,
    ): SearchResultPage =
        withContext(ioDispatcher) {
            try {
                val page = extractor.searchVideosNextPage(query, nextPageUrl)
                SearchResultPage(page.videos.map { it.toHomeVideoItem() }, page.nextPageUrl)
            } catch (e: Exception) {
                Log.e("YoutubeDS", "searchVideosNextPage failed: ${e.message}")
                SearchResultPage(emptyList(), null)
            }
        }

    // 📈 Trending
    suspend fun getTrending(): List<HomeVideoItem> =
        withContext(ioDispatcher) {
            try {
                extractor.getTrending().map { it.toHomeVideoItem() }
            } catch (e: Exception) {
                Log.e("YoutubeDS", "getTrending failed: ${e.message}")
                emptyList()
            }
        }

    // 🎵 Music search
    suspend fun searchMusic(query: String): List<HomeMusicItem> =
        withContext(ioDispatcher) {
            try {
                extractor.searchMusic(query).map { it.toHomeMusicItem() }
            } catch (e: Exception) {
                Log.e("YoutubeDS", "searchMusic failed: ${e.message}", e)
                throw e
            }
        }

    // ▶️ Get stream URL (audio only — best bitrate)
    suspend fun getAudioStreamUrl(videoId: String): String? = getStreamUrl(videoId, preferVideo = false)?.url

    suspend fun getVideoStreamUrl(videoId: String): String? = getStreamUrl(videoId, preferVideo = true)?.url

    // Unified helper that extracts the media stream using dynamically ranked extraction layers
    suspend fun getStreamUrl(
        videoId: String,
        preferVideo: Boolean,
        onLayerFallback: (() -> Unit)? = null
    ): StreamResult? =
        withContext(ioDispatcher) {
            val cleanId = videoId.trim().take(11)
            Log.d("YoutubeDS", "Resolving stream for video ID: $cleanId (preferVideo=$preferVideo)")

            // ⚡ TIER 1: Instant Direct Google Innertube Extraction (< 300ms)
            // Talks directly to YouTube's player endpoint without headless WebViews or scrapers!
            try {
                val t0 = System.currentTimeMillis()
                val directResult = kotlinx.coroutines.withTimeoutOrNull(2500L) {
                    extractSmartTube(cleanId, preferVideo)
                }
                if (directResult != null) {
                    val tookMs = System.currentTimeMillis() - t0
                    rankingManager.recordSuccess(com.deepeye.musicpro.diagnostics.ExtractionRankingManager.Layer.SMARTTUBE)
                    Log.i("YoutubeDS", "⚡ TIER 1 Direct Extraction SUCCEEDED in ${tookMs}ms for $cleanId")
                    return@withContext directResult
                }
            } catch (e: Exception) {
                Log.w("YoutubeDS", "TIER 1 Direct Extraction error for $cleanId: ${e.message}")
            }

            // 🔄 TIER 2: Fallback Parallel Racing (Only triggered if Tier 1 times out or fails)
            rankingManager.recordFailure(com.deepeye.musicpro.diagnostics.ExtractionRankingManager.Layer.SMARTTUBE)
            onLayerFallback?.invoke()
            Log.w("YoutubeDS", "⚠️ Tier 1 Direct failed for $cleanId. Triggering Tier 2 Fallback Racing...")

            val fallbackLayers = listOf(
                com.deepeye.musicpro.diagnostics.ExtractionRankingManager.Layer.ALT_EXTRACTOR,
                com.deepeye.musicpro.diagnostics.ExtractionRankingManager.Layer.PIPED,
                com.deepeye.musicpro.diagnostics.ExtractionRankingManager.Layer.INVIDIOUS,
                com.deepeye.musicpro.diagnostics.ExtractionRankingManager.Layer.WEBVIEW_CAPTURE
            )

            var finalResult: StreamResult? = null
            try {
                finalResult = kotlinx.coroutines.withTimeoutOrNull(8000L) {
                    kotlinx.coroutines.supervisorScope {
                        val scope = this
                        val channel = kotlinx.coroutines.channels.Channel<StreamResult?>(fallbackLayers.size)
                        
                        val jobs = fallbackLayers.map { layer ->
                            scope.launch {
                                val result = try {
                                    when (layer) {
                                        com.deepeye.musicpro.diagnostics.ExtractionRankingManager.Layer.ALT_EXTRACTOR -> extractAlternative(cleanId, preferVideo)
                                        com.deepeye.musicpro.diagnostics.ExtractionRankingManager.Layer.PIPED -> extractPiped(cleanId, preferVideo)
                                        com.deepeye.musicpro.diagnostics.ExtractionRankingManager.Layer.INVIDIOUS -> extractInvidious(cleanId, preferVideo)
                                        com.deepeye.musicpro.diagnostics.ExtractionRankingManager.Layer.WEBVIEW_CAPTURE -> {
                                            val url = headlessExtractor.extractStreamUrl(cleanId, preferVideo)
                                            if (url != null) StreamResult(url, preferVideo) else null
                                        }
                                        else -> null
                                    }
                                } catch (e: Exception) {
                                    Log.w("YoutubeDS", "Fallback Layer $layer error: ${e.message}")
                                    null
                                }
                                
                                if (result != null) {
                                    rankingManager.recordSuccess(layer)
                                    Log.i("YoutubeDS", "✅ Fallback layer $layer SUCCEEDED for $cleanId")
                                    channel.trySend(result)
                                } else {
                                    rankingManager.recordFailure(layer)
                                    channel.trySend(null)
                                }
                            }
                        }

                        var resResult: StreamResult? = null
                        for (i in 1..fallbackLayers.size) {
                            val res = channel.receive()
                            if (res != null) {
                                resResult = res
                                jobs.forEach { it.cancel() }
                                break
                            }
                        }
                        channel.close()
                        resResult
                    }
                }
            } catch (e: Exception) {
                Log.w("YoutubeDS", "Tier 2 fallback racing failed: ${e.message}")
            }

            if (finalResult == null) {
                Log.e("YoutubeDS", "🚨 All extraction tiers failed for $cleanId")
            }
            finalResult
        }

    private suspend fun extractSmartTube(videoId: String, preferVideo: Boolean): StreamResult? {
        return try {
            extractor.extractStream(videoId, preferVideo)?.let {
                val res = StreamResult(
                    url = it.url,
                    isVideo = it.container != "audio",
                    isAdaptive = it.container == "adaptive",
                    videoFormats = it.videoFormats,
                    audioFormats = it.audioFormats,
                    dashManifestUrl = it.dashManifestUrl,
                    hlsManifestUrl = it.hlsManifestUrl
                )
                val safeHash = videoId.hashCode().toString()
                val vLabels = it.videoFormats.take(5).map { vf -> "${vf.height ?: 0}p-${vf.frameRate?.toInt() ?: 30}-${vf.videoCodec?.displayName ?: "UNK"}" }
                val aLabels = it.audioFormats.take(5).map { af -> "${af.audioCodec?.displayName ?: "UNK"}-${if ((af.channelCount ?: 2) > 2) "Surround" else "Stereo"}-${(af.bitrate ?: 0L) / 1000}kbps" }
                Log.d("DeepEyeHQ", "event=resolver_inventory mediaKeyHash=$safeHash videoCount=${it.videoFormats.size} audioCount=${it.audioFormats.size} selectedVideo=${it.videoFormats.firstOrNull { f -> f.isSelected }?.stableId} selectedAudio=${it.audioFormats.firstOrNull { f -> f.isSelected }?.stableId} videoLabels=$vLabels audioLabels=$aLabels")
                res
            }
        } catch (e: Exception) {
            Log.e("YoutubeDS", "SmartTubeInnertube extract failed", e)
            null
        }
    }

    private suspend fun extractAlternative(videoId: String, preferVideo: Boolean): StreamResult? {
        Log.d("YoutubeDS", "Alternative Extractor: extracting $videoId...")
        val extractor = com.ar.youtubeextractor.core.YouTubeExtractor()
        val url = "https://www.youtube.com/watch?v=$videoId"
        val result = extractor.extractVideoData(url)
        return when (result) {
            is com.ar.youtubeextractor.core.Result.Success -> {
                val data = result.data
                val hlsUrl = data.streamingData?.hlsManifestUrl
                if (!hlsUrl.isNullOrEmpty()) {
                    StreamResult(hlsUrl, isVideo = preferVideo)
                } else {
                    val audioFormat = data.streamingData?.adaptiveFormats
                        ?.filter { it.mimeType?.contains("audio") == true }
                        ?.maxByOrNull { it.bitrate ?: 0 }
                    if (audioFormat?.url != null) {
                        StreamResult(audioFormat.url!!, isVideo = false)
                    } else {
                        null
                    }
                }
            }
            is com.ar.youtubeextractor.core.Result.Error -> {
                Log.e("YoutubeDS", "Alternative Extractor error: ${result.error}")
                null
            }
        }
    }

    private suspend fun extractPiped(videoId: String, preferVideo: Boolean): StreamResult? = kotlinx.coroutines.coroutineScope {
        Log.d("YoutubeDS", "Piped API: extracting $videoId...")
        val pipedInstances = listOf(
            "https://api.piped.private.coffee",
            "https://pipedapi.kavin.rocks",
            "https://pipedapi.us.projectsegfau.lt",
            "https://pipedapi.lunar.icu",
            "https://api-piped.mha.fi",
            "https://pipedapi.colt.top"
        ).shuffled().take(3) // Race 3 random instances for lowest latency

        val channel = kotlinx.coroutines.channels.Channel<StreamResult?>(pipedInstances.size)
        val jobs = pipedInstances.map { instance ->
            launch {
                try {
                    val pipedUrl = "$instance/streams/$videoId"
                    val pipedRequest = okhttp3.Request.Builder()
                        .url(pipedUrl)
                        .addHeader("User-Agent", "DeepEyeMusicPro/2.0")
                        .build()
                    val pipedResponse = fastClient.newCall(pipedRequest).execute()
                    if (pipedResponse.isSuccessful) {
                        val body = pipedResponse.body?.string() ?: ""
                        pipedResponse.close()
                        if (body.isNotEmpty()) {
                            val json = org.json.JSONObject(body)
                            
                            val hls = json.optString("hls", "")
                            if (hls.isNotEmpty() && preferVideo) {
                                channel.trySend(StreamResult(hls, isVideo = true))
                                return@launch
                            }
                            
                            if (preferVideo) {
                                val videoStreams = json.optJSONArray("videoStreams")
                                if (videoStreams != null && videoStreams.length() > 0) {
                                    var bestUrl: String? = null
                                    var bestBitrate = 0
                                    for (i in 0 until videoStreams.length()) {
                                        val stream = videoStreams.getJSONObject(i)
                                        val sUrl = stream.optString("url", "")
                                        val bitrate = stream.optInt("bitrate", 0)
                                        // Some apis put width/height. We'll use bitrate or quality
                                        if (sUrl.isNotEmpty() && bitrate >= bestBitrate) {
                                            bestUrl = sUrl
                                            bestBitrate = bitrate
                                        }
                                    }
                                    if (bestUrl != null) {
                                        channel.trySend(StreamResult(bestUrl, isVideo = true))
                                        return@launch
                                    }
                                }
                            }

                            val audioStreams = json.optJSONArray("audioStreams")
                            if (audioStreams != null && audioStreams.length() > 0) {
                                var bestUrl: String? = null
                                var bestBitrate = 0
                                for (i in 0 until audioStreams.length()) {
                                    val stream = audioStreams.getJSONObject(i)
                                    val sUrl = stream.optString("url", "")
                                    val bitrate = stream.optInt("bitrate", 0)
                                    if (sUrl.isNotEmpty() && bitrate >= bestBitrate) {
                                        bestUrl = sUrl
                                        bestBitrate = bitrate
                                    }
                                }
                                if (bestUrl != null) {
                                    channel.trySend(StreamResult(bestUrl, isVideo = false))
                                    return@launch
                                }
                            }
                            
                            if (hls.isNotEmpty()) {
                                channel.trySend(StreamResult(hls, isVideo = preferVideo))
                                return@launch
                            }
                        }
                    } else {
                        pipedResponse.close()
                    }
                } catch (e: Exception) {
                    Log.w("YoutubeDS", "Piped instance $instance failed: ${e.message}")
                }
                channel.trySend(null)
            }
        }

        var finalRes: StreamResult? = null
        for (i in pipedInstances.indices) {
            val res = channel.receive()
            if (res != null) {
                finalRes = res
                jobs.forEach { it.cancel() }
                break
            }
        }
        channel.close()
        finalRes
    }

    private suspend fun extractInvidious(videoId: String, preferVideo: Boolean): StreamResult? = kotlinx.coroutines.coroutineScope {
        Log.d("YoutubeDS", "Invidious API: extracting $videoId...")
        val invidiousInstances = listOf(
            "https://invidious.protokolla.fi",
            "https://invidious.poast.org",
            "https://inv.zzls.xyz",
            "https://invidious.epicsite.app",
            "https://yewtu.be",
            "https://invidious.projectsegfau.lt"
        ).shuffled().take(3)

        val channel = kotlinx.coroutines.channels.Channel<StreamResult?>(invidiousInstances.size)
        val jobs = invidiousInstances.map { instance ->
            launch {
                try {
                    val invUrl = "$instance/api/v1/videos/$videoId"
                    val request = okhttp3.Request.Builder()
                        .url(invUrl)
                        .addHeader("User-Agent", "DeepEyeMusicPro/2.0")
                        .build()
                    val response = fastClient.newCall(request).execute()
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        response.close()
                        if (body.isNotEmpty()) {
                            val json = org.json.JSONObject(body)
                            val adaptiveFormats = json.optJSONArray("adaptiveFormats")
                            if (adaptiveFormats != null && adaptiveFormats.length() > 0) {
                                var bestUrl: String? = null
                                var bestBitrate = 0
                                for (i in 0 until adaptiveFormats.length()) {
                                    val format = adaptiveFormats.getJSONObject(i)
                                    val type = format.optString("type", "")
                                    val sUrl = format.optString("url", "")
                                    val bitrate = format.optInt("bitrate", 0)
                                    if (type.contains("audio") && sUrl.isNotEmpty() && bitrate > bestBitrate) {
                                        bestUrl = sUrl
                                        bestBitrate = bitrate
                                    }
                                }
                                if (bestUrl != null) {
                                    channel.trySend(StreamResult(bestUrl, isVideo = false))
                                    return@launch
                                }
                            }
                            val formatStreams = json.optJSONArray("formatStreams")
                            if (formatStreams != null && formatStreams.length() > 0) {
                                var bestUrl: String? = null
                                var bestBitrate = 0
                                for (i in 0 until formatStreams.length()) {
                                    val format = formatStreams.getJSONObject(i)
                                    val sUrl = format.optString("url", "")
                                    val bitrate = format.optInt("bitrate", 0)
                                    if (sUrl.isNotEmpty() && bitrate > bestBitrate) {
                                        bestUrl = sUrl
                                        bestBitrate = bitrate
                                    }
                                }
                                if (bestUrl != null) {
                                    channel.trySend(StreamResult(bestUrl, isVideo = preferVideo))
                                    return@launch
                                }
                            }
                        }
                    } else {
                        response.close()
                    }
                } catch (e: Exception) {
                    Log.w("YoutubeDS", "Invidious instance $instance failed: ${e.message}")
                }
                channel.trySend(null)
            }
        }

        var finalRes: StreamResult? = null
        for (i in invidiousInstances.indices) {
            val res = channel.receive()
            if (res != null) {
                finalRes = res
                jobs.forEach { it.cancel() }
                break
            }
        }
        channel.close()
        finalRes
    }

    suspend fun getRelatedMusic(
        title: String,
        artist: String,
        isVideo: Boolean = false,
    ): List<com.deepeye.musicpro.domain.model.MediaItem.Remote> =
        withContext(ioDispatcher) {
            try {
                extractor.getRelatedMusic("$title $artist").map { item ->
                    com.deepeye.musicpro.domain.model.MediaItem.Remote(
                        id = item.id,
                        title = item.title,
                        artist = item.artist,
                        artworkUri = android.net.Uri.parse(item.thumbnailUrl.upgradeResolution()),
                        duration = item.duration * 1000L,
                        isVideo = isVideo,
                    )
                }
            } catch (e: Exception) {
                Log.e("YoutubeDS", "getRelatedMusic failed: ${e.message}")
                emptyList()
            }
        }

    // 🔗 Get related videos (for video autoplay)
    suspend fun getRelatedVideos(videoId: String): List<HomeVideoItem> =
        withContext(ioDispatcher) {
            try {
                extractor.getRelatedVideos(videoId).map { it.toHomeVideoItem() }
            } catch (e: Exception) {
                Log.e("YoutubeDS", "getRelatedVideos failed: ${e.message}")
                emptyList()
            }
        }

    // 📱 Shorts — vertical format
    suspend fun getShorts(): List<HomeVideoItem> =
        withContext(ioDispatcher) {
            try {
                extractor.getShorts().map { it.toHomeVideoItem() }
            } catch (e: Exception) {
                emptyList()
            }
        }

    // 💡 Search Suggestions
    suspend fun getSearchSuggestions(query: String): List<String> =
        withContext(ioDispatcher) {
            try {
                extractor.getSearchSuggestions(query)
            } catch (e: Exception) {
                Log.e("YoutubeDS", "getSearchSuggestions failed: ${e.message}")
                emptyList()
            }
        }

    // Mappers
    private fun com.deepeye.musicpro.extractor.ExtractorVideoItem.toHomeVideoItem() = HomeVideoItem(
        id = id,
        title = title,
        channelName = artist,
        channelId = channelId,
        thumbnailUrl = thumbnailUrl.upgradeResolution(),
        duration = duration,
        viewCount = viewCount,
        isShort = isShort,
        channelAvatarUrl = channelAvatarUrl
    )

    private fun com.deepeye.musicpro.extractor.ExtractorMusicItem.toHomeMusicItem() = HomeMusicItem(
        id = id,
        title = title,
        artist = artist,
        thumbnailUrl = thumbnailUrl.upgradeResolution(),
        duration = duration
    )

    private fun String.upgradeResolution(): String {
        return this.replace(Regex("(?<!maxres)(hqdefault|sddefault|mqdefault|default)\\.jpg"), "maxresdefault.jpg")
            .replace(Regex("=w\\d+-h\\d+([a-zA-Z0-9\\-]*)"), "=w1080-h1080$1")
    }

    private fun String.extractVideoId(): String {
        val id =
            when {
                contains("v=") -> substringAfter("v=").substringBefore("&")
                contains("youtu.be/") -> substringAfterLast("/")
                contains("/shorts/") -> substringAfterLast("/")
                else -> this
            }.take(11)
        Log.d("YoutubeDS", "Extracted Video ID: $id from $this")
        return id
    }
}

// Data class representing the YouTube stream extraction result
data class StreamResult(
    val url: String,
    val isVideo: Boolean,
    val isAdaptive: Boolean = false,
    val headers: Map<String, String> = emptyMap(),
    val expiresAt: Long = 0L,
    val videoFormats: List<com.deepeye.musicpro.player.smarttube.DeepEyePlaybackFormat> = emptyList(),
    val audioFormats: List<com.deepeye.musicpro.player.smarttube.DeepEyePlaybackFormat> = emptyList(),
    val dashManifestUrl: String? = null,
    val hlsManifestUrl: String? = null,
)

data class SearchResultPage(
    val items: List<HomeVideoItem>,
    val nextPageUrl: String?,
)
