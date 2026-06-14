// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.source.remote.youtube

import android.util.Log
import com.deepeye.musicpro.domain.model.home.HomeMusicItem
import com.deepeye.musicpro.domain.model.home.HomeVideoItem
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YoutubeRemoteDataSource
@Inject
constructor(
    private val downloader: NewPipeDownloader,
    private val client: okhttp3.OkHttpClient,
    private val rankingManager: com.deepeye.musicpro.diagnostics.ExtractionRankingManager,
    private val headlessExtractor: HeadlessWebViewExtractor,
) {
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    private val fastClient: okhttp3.OkHttpClient by lazy {
        client.newBuilder()
            .connectTimeout(2, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(2, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(2, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    private val yt: StreamingService by lazy {
        NewPipe.init(downloader, Localization.DEFAULT, ContentCountry.DEFAULT)
        ServiceList.YouTube
    }

    // 🔍 Search videos
    suspend fun searchVideos(query: String): List<HomeVideoItem> = searchVideosFirstPage(query).items

    suspend fun searchVideosFirstPage(query: String): SearchResultPage =
        withContext(ioDispatcher) {
            try {
                val extractor = yt.getSearchExtractor(query)
                extractor.fetchPage()
                val page = extractor.initialPage
                val infoItems: List<InfoItem> = page.items
                val videos =
                    infoItems.filterIsInstance<StreamInfoItem>()
                        .map { item: StreamInfoItem -> item.toHomeVideoItem() }
                SearchResultPage(videos, page.nextPage)
            } catch (e: Exception) {
                Log.e("YoutubeDS", "searchVideosFirstPage failed: ${e.message}")
                SearchResultPage(emptyList(), null)
            }
        }

    suspend fun searchVideosNextPage(
        query: String,
        nextPageToken: Page,
    ): SearchResultPage =
        withContext(ioDispatcher) {
            try {
                val extractor = yt.getSearchExtractor(query)
                extractor.fetchPage() // fetchPage initialization
                val page = extractor.getPage(nextPageToken)
                val infoItems: List<InfoItem> = page.items
                val videos =
                    infoItems.filterIsInstance<StreamInfoItem>()
                        .map { item: StreamInfoItem -> item.toHomeVideoItem() }
                SearchResultPage(videos, page.nextPage)
            } catch (e: Exception) {
                Log.e("YoutubeDS", "searchVideosNextPage failed: ${e.message}")
                SearchResultPage(emptyList(), null)
            }
        }

    // 📈 Trending (Using search as a more reliable fallback for KMP fork)
    suspend fun getTrending(): List<HomeVideoItem> =
        withContext(ioDispatcher) {
            try {
                searchVideos("trending music")
            } catch (e: Exception) {
                Log.e("YoutubeDS", "getTrending failed: ${e.message}")
                emptyList()
            }
        }

    // 🎵 Music search (YouTube Music filter)
    suspend fun searchMusic(query: String): List<HomeMusicItem> =
        withContext(ioDispatcher) {
            try {
                val extractor =
                    yt.getSearchExtractor(
                        query,
                        listOf("music_songs"),
                        "",
                    )
                extractor.fetchPage()
                val infoItems: List<InfoItem> = extractor.initialPage.items
                infoItems.filterIsInstance<StreamInfoItem>()
                    .map { item: StreamInfoItem -> item.toHomeMusicItem() }
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
            val rankedLayers = rankingManager.getRankedLayers()
            Log.d("YoutubeDS", "Resolving stream for video ID: $cleanId with layers: $rankedLayers")

            var finalResult: StreamResult? = null

            // Run ALL active layers in parallel (including WebView) for true 0-latency racing
            if (rankedLayers.isNotEmpty()) {
                Log.d("YoutubeDS", "Running parallel extraction for video ID: $cleanId")
                try {
                    finalResult = kotlinx.coroutines.withTimeoutOrNull(15000L) { // Increased timeout to 15s for WebView fallback
                        kotlinx.coroutines.supervisorScope {
                            val scope = this
                            val channel = kotlinx.coroutines.channels.Channel<StreamResult?>(rankedLayers.size)
                            
                            val jobs = rankedLayers.map { layer ->
                                scope.launch {
                                    var result: StreamResult? = null
                                    var lastException: Exception? = null
                                    val maxAttempts = 2
                                    for (attempt in 1..maxAttempts) {
                                        try {
                                            result = when (layer) {
                                                com.deepeye.musicpro.diagnostics.ExtractionRankingManager.Layer.NEWPIPE -> extractNewPipe(cleanId, preferVideo)
                                                com.deepeye.musicpro.diagnostics.ExtractionRankingManager.Layer.ALT_EXTRACTOR -> extractAlternative(cleanId, preferVideo)
                                                com.deepeye.musicpro.diagnostics.ExtractionRankingManager.Layer.PIPED -> extractPiped(cleanId, preferVideo)
                                                com.deepeye.musicpro.diagnostics.ExtractionRankingManager.Layer.INVIDIOUS -> extractInvidious(cleanId, preferVideo)
                                                com.deepeye.musicpro.diagnostics.ExtractionRankingManager.Layer.WEBVIEW_CAPTURE -> {
                                                    val url = headlessExtractor.extractStreamUrl(cleanId, preferVideo)
                                                    if (url != null) {
                                                        rankingManager.recordSuccess(com.deepeye.musicpro.diagnostics.ExtractionRankingManager.Layer.WEBVIEW_CAPTURE)
                                                        StreamResult(url, preferVideo)
                                                    } else {
                                                        rankingManager.recordFailure(com.deepeye.musicpro.diagnostics.ExtractionRankingManager.Layer.WEBVIEW_CAPTURE)
                                                        null
                                                    }
                                                }
                                                else -> null
                                            }
                                            if (result != null) break
                                        } catch (e: Exception) {
                                            lastException = e
                                            Log.w("YoutubeDS", "⚠️ Layer $layer attempt $attempt failed for $cleanId: ${e.message}")
                                        }
                                    }
                                    
                                    if (result != null) {
                                        rankingManager.recordSuccess(layer)
                                        Log.i("YoutubeDS", "✅ Extraction layer $layer SUCCEEDED for $cleanId")
                                        channel.trySend(result)
                                    } else {
                                        rankingManager.recordFailure(layer)
                                        if (lastException != null) {
                                            Log.e("YoutubeDS", "❌ Extraction layer $layer failed completely for $cleanId", lastException)
                                        } else {
                                            Log.w("YoutubeDS", "❌ Extraction layer $layer returned null for $cleanId")
                                        }
                                        channel.trySend(null)
                                    }
                                }
                            }

                            var resResult: StreamResult? = null
                            var failures = 0
                            for (i in 1..rankedLayers.size) {
                                val res = channel.receive()
                                if (res != null) {
                                    resResult = res
                                    // Cancel other ongoing jobs to save resources and bandwidth!
                                    jobs.forEach { it.cancel() }
                                    break
                                } else {
                                    failures++
                                }
                            }
                            channel.close()
                            resResult
                        }
                    }
                    if (finalResult == null) {
                        Log.w("YoutubeDS", "⚠️ Parallel extraction timed out after 15000ms for $cleanId")
                    }
                } catch (e: Exception) {
                    Log.w("YoutubeDS", "⚠️ Parallel extraction failed with error: ${e.message}")
                }
            }

            if (finalResult == null) {
                Log.e("YoutubeDS", "🚨 All extraction layers failed for $cleanId")
            }

            finalResult
        }

    private suspend fun extractNewPipe(videoId: String, preferVideo: Boolean): StreamResult? {
        val url = "https://www.youtube.com/watch?v=$videoId"
        val info = StreamInfo.getInfo(yt, url)

        if (preferVideo) {
            val streamUrl = info.dashMpdUrl?.takeIf { it.isNotEmpty() }
                ?: info.hlsUrl?.takeIf { it.isNotEmpty() }
            if (streamUrl != null) {
                return StreamResult(streamUrl, isVideo = true)
            }
            val audioFallback = info.audioStreams.maxByOrNull { it.bitrate }?.content
            if (audioFallback != null) {
                return StreamResult(audioFallback, isVideo = false)
            }
            throw RuntimeException("NewPipe returned no DASH/HLS or audio stream URLs for $videoId")
        } else {
            val audio = info.audioStreams.maxByOrNull { it.bitrate }
                ?: info.audioStreams.firstOrNull()
            if (audio != null) {
                return StreamResult(audio.content, isVideo = false)
            }
            val dashOrHls = info.dashMpdUrl?.takeIf { it.isNotEmpty() }
                ?: info.hlsUrl?.takeIf { it.isNotEmpty() }
            if (dashOrHls != null) {
                return StreamResult(dashOrHls, isVideo = false)
            }
            throw RuntimeException("NewPipe returned no audio or DASH/HLS stream URLs for $videoId")
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
            "https://api.piped.privacydev.net",
            "https://piped-api.garudalinux.org",
            "https://api-piped.mha.fi",
            "https://pipedapi.colt.top",
            "https://pipedapi.leptons.xyz",
            "https://pipedapi.ox.ci"
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
            "https://yewtu.be",
            "https://invidious.projectsegfau.lt",
            "https://inv.tux.im",
            "https://invidious.privacydev.net",
            "https://invidious.lunar.icu"
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

    // 🔗 Get related music (for autoplay)
    suspend fun getRelatedMusic(
        title: String,
        artist: String,
        isVideo: Boolean = false,
    ): List<com.deepeye.musicpro.domain.model.MediaItem.Remote> =
        withContext(ioDispatcher) {
            try {
                // Search for related music using title and artist
                val query = "related to $title $artist"
                val relatedItems = searchMusic(query)

                relatedItems.map { item ->
                    com.deepeye.musicpro.domain.model.MediaItem.Remote(
                        id = item.id,
                        title = item.title,
                        artist = item.artist,
                        artworkUri = android.net.Uri.parse(item.thumbnailUrl),
                        duration = item.duration * 1000L, // Seconds to ms
                        isVideo = isVideo,
                    )
                }
            } catch (e: Exception) {
                Log.e("YoutubeDS", "getRelatedMusic failed: ${e.message}")
                emptyList()
            }
        }

    // 📱 Shorts — vertical format
    suspend fun getShorts(): List<HomeVideoItem> =
        withContext(ioDispatcher) {
            try {
                val extractor = yt.getSearchExtractor("shorts #trending")
                extractor.fetchPage()
                val infoItems: List<InfoItem> = extractor.initialPage.items
                infoItems.filterIsInstance<StreamInfoItem>()
                    .filter { it.duration in 1..60 }
                    .take(12)
                    .map { item: StreamInfoItem -> item.toHomeVideoItem(isShort = true) }
            } catch (e: Exception) {
                emptyList()
            }
        }

    // 💡 Search Suggestions
    suspend fun getSearchSuggestions(query: String): List<String> =
        withContext(ioDispatcher) {
            if (query.trim().isEmpty()) return@withContext emptyList()
            try {
                val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
                val request =
                    okhttp3.Request.Builder()
                        .url(
                            "https://suggestqueries.google.com/complete/search?client=youtube&ds=yt&q=$encodedQuery"
                        )
                        .build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: ""

                // Parse the response: ["query", ["suggestion1", "suggestion2", ...]]
                // Regex matches: ["query",["s1","s2",...]] or ["query",["s1","s2",...],...]
                val regex = """\["([^"]+)"\s*,\s*\[([^\]]+)\]""".toRegex()
                val match = regex.find(body)
                if (match != null) {
                    val arrayPart = match.groupValues[2]
                    arrayPart.split(",")
                        .map { it.replace("\"", "").trim() }
                        .filter { it.isNotEmpty() }
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e("YoutubeDS", "getSearchSuggestions failed: ${e.message}")
                emptyList()
            }
        }

    // Mappers
    private fun StreamInfoItem.toHomeVideoItem(isShort: Boolean = false) =
        HomeVideoItem(
            id = url.extractVideoId(),
            title = name ?: "Unknown",
            channelName = uploaderName ?: "Unknown",
            channelId = uploaderName ?: "",
            thumbnailUrl = thumbnails.lastOrNull()?.url ?: "",
            duration = duration,
            viewCount = 0L,
            uploadDate = "",
            isShort = isShort || duration in 1..60,
        )

    private fun StreamInfoItem.toHomeMusicItem() =
        HomeMusicItem(
            id = url.extractVideoId(),
            title = name ?: "Unknown",
            artist = uploaderName ?: "Unknown Artist",
            thumbnailUrl = thumbnails.lastOrNull()?.url ?: "",
            duration = duration,
            playCount = 0L,
        )

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
)

data class SearchResultPage(
    val items: List<HomeVideoItem>,
    val nextPage: Page?,
)
