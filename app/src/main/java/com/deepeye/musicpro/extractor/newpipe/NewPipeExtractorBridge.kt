// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.extractor.newpipe

import android.util.Log
import com.deepeye.musicpro.extractor.*
import com.deepeye.musicpro.player.smarttube.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.util.concurrent.atomic.AtomicBoolean

class NewPipeExtractorBridge(
    private val client: OkHttpClient
) : IExtractorBridge {

    companion object {
        private const val TAG = "NewPipeExtractor"
        private val isInitialized = AtomicBoolean(false)
    }

    init {
        initNewPipe()
    }

    private fun initNewPipe() {
        if (isInitialized.compareAndSet(false, true)) {
            try {
                NewPipe.init(NewPipeDownloader(client))
                Log.i(TAG, "NewPipeExtractor initialized successfully with custom OkHttp downloader")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize NewPipeExtractor", e)
            }
        }
    }

    override suspend fun initBridge(downloader: Any) {
        initNewPipe()
    }

    override suspend fun extractStream(videoId: String, preferVideo: Boolean): ExtractorStreamResult? = withContext(Dispatchers.IO) {
        try {
            initNewPipe()
            val url = "https://www.youtube.com/watch?v=$videoId"
            Log.d(TAG, "Extracting stream via NewPipe for: $url (preferVideo=$preferVideo)")

            val streamInfo = StreamInfo.getInfo(ServiceList.YouTube, url)

            val videoFormats = mutableListOf<DeepEyePlaybackFormat>()
            val audioFormats = mutableListOf<DeepEyePlaybackFormat>()

            // 1. Process Video Only (High-res: 1080p, 1440p, 4K)
            streamInfo.videoOnlyStreams?.forEachIndexed { index, vs ->
                val itag = vs.itag
                val height = vs.height.takeIf { it > 0 } ?: vs.resolution?.filter { it.isDigit() }?.toIntOrNull() ?: 0
                val width = vs.width
                val fps = vs.fps.takeIf { it > 0 } ?: 30
                val format = DeepEyePlaybackFormat(
                    stableId = "newpipe_video_${itag}_$index",
                    smartTubeFormatId = itag.toString(),
                    streamType = DeepEyeStreamType.VIDEO_ONLY,
                    container = vs.format?.suffix ?: "mp4",
                    mimeType = vs.format?.mimeType ?: "video/mp4",
                    videoCodec = if (vs.codec?.contains("avc") == true) DeepEyeVideoCodec.AVC else if (vs.codec?.contains("vp9") == true) DeepEyeVideoCodec.VP9 else DeepEyeVideoCodec.AV1,
                    width = width,
                    height = height,
                    frameRate = fps.toFloat(),
                    bitrate = vs.bitrate.toLong(),
                    isVideoOnly = true,
                    isDeviceCompatible = true,
                    streamUrl = vs.content
                )
                videoFormats.add(format)
            }

            // 2. Process Progressive Video (Muxed audio+video)
            streamInfo.videoStreams?.forEachIndexed { index, vs ->
                val itag = vs.itag
                val height = vs.height.takeIf { it > 0 } ?: vs.resolution?.filter { it.isDigit() }?.toIntOrNull() ?: 0
                val format = DeepEyePlaybackFormat(
                    stableId = "newpipe_prog_${itag}_$index",
                    smartTubeFormatId = itag.toString(),
                    streamType = DeepEyeStreamType.VIDEO_PROGRESSIVE,
                    container = vs.format?.suffix ?: "mp4",
                    mimeType = vs.format?.mimeType ?: "video/mp4",
                    videoCodec = DeepEyeVideoCodec.AVC,
                    width = vs.width,
                    height = height,
                    frameRate = (vs.fps.takeIf { it > 0 } ?: 30).toFloat(),
                    bitrate = vs.bitrate.toLong(),
                    isProgressive = true,
                    isDeviceCompatible = true,
                    streamUrl = vs.content
                )
                videoFormats.add(format)
            }

            // 3. Process Audio Streams
            streamInfo.audioStreams?.forEachIndexed { index, as_ ->
                val itag = as_.itag
                val format = DeepEyePlaybackFormat(
                    stableId = "newpipe_audio_${itag}_$index",
                    smartTubeFormatId = itag.toString(),
                    streamType = DeepEyeStreamType.AUDIO_ONLY,
                    container = as_.format?.suffix ?: "m4a",
                    mimeType = as_.format?.mimeType ?: "audio/mp4",
                    audioCodec = if (as_.codec?.contains("opus") == true) DeepEyeAudioCodec.OPUS else DeepEyeAudioCodec.AAC,
                    bitrate = as_.averageBitrate.toLong().takeIf { it > 0 } ?: as_.bitrate.toLong().takeIf { it > 0 } ?: 128000L,
                    sampleRateHz = 44100,
                    channelCount = 2,
                    isAudioOnly = true,
                    isDeviceCompatible = true,
                    streamUrl = as_.content
                )
                audioFormats.add(format)
            }

            val hlsUrl = streamInfo.hlsUrl
            val dashUrl = streamInfo.dashMpdUrl

            val primaryUrl = if (!hlsUrl.isNullOrBlank()) {
                hlsUrl
            } else if (!dashUrl.isNullOrBlank()) {
                dashUrl
            } else if (preferVideo) {
                videoFormats.firstOrNull { it.isProgressive && it.streamUrl != null }?.streamUrl
                    ?: videoFormats.maxByOrNull { it.height ?: 0 }?.streamUrl
                    ?: audioFormats.maxByOrNull { it.bitrate ?: 0L }?.streamUrl
            } else {
                audioFormats.maxByOrNull { it.bitrate ?: 0L }?.streamUrl
                    ?: videoFormats.firstOrNull { it.isProgressive && it.streamUrl != null }?.streamUrl
            }

            if (primaryUrl.isNullOrBlank()) {
                Log.w(TAG, "No usable stream URL found via NewPipe for $videoId")
                return@withContext null
            }

            Log.i(TAG, "Successfully extracted via NewPipe for $videoId: HLS=${hlsUrl != null} DASH=${dashUrl != null} Videos=${videoFormats.size} Audio=${audioFormats.size}")

            ExtractorStreamResult(
                url = primaryUrl,
                container = if (!hlsUrl.isNullOrBlank()) "hls" else if (!dashUrl.isNullOrBlank()) "dash" else "progressive",
                quality = if (!hlsUrl.isNullOrBlank()) "HLS" else if (!dashUrl.isNullOrBlank()) "DASH" else "HD",
                extractorName = "NewPipeExtractor",
                videoFormats = videoFormats,
                audioFormats = audioFormats,
                dashManifestUrl = dashUrl,
                hlsManifestUrl = hlsUrl
            )
        } catch (e: Exception) {
            Log.e(TAG, "NewPipe extraction failed for $videoId: ${e.message}", e)
            null
        }
    }

    override suspend fun searchVideosFirstPage(query: String): ExtractorSearchResultPage = withContext(Dispatchers.IO) {
        try {
            initNewPipe()
            val searchInfo = SearchInfo.getInfo(
                ServiceList.YouTube,
                ServiceList.YouTube.searchQHFactory.fromQuery(
                    query,
                    listOf(YoutubeSearchQueryHandlerFactory.VIDEOS),
                    ""
                )
            )
            val videos = searchInfo.relatedItems.filterIsInstance<StreamInfoItem>().map {
                ExtractorVideoItem(
                    id = it.url.substringAfter("v=").take(11),
                    title = it.name ?: "",
                    artist = it.uploaderName ?: "",
                    duration = it.duration * 1000L,
                    thumbnailUrl = it.thumbnails?.firstOrNull()?.url ?: "",
                    viewCount = it.viewCount,
                    channelAvatarUrl = it.uploaderAvatars?.firstOrNull()?.url ?: "",
                    channelId = it.uploaderUrl ?: ""
                )
            }
            ExtractorSearchResultPage(videos, searchInfo.nextPage?.url)
        } catch (e: Exception) {
            Log.e(TAG, "NewPipe search failed: ${e.message}", e)
            ExtractorSearchResultPage(emptyList(), null)
        }
    }

    override suspend fun searchVideosNextPage(query: String, nextPageUrl: String): ExtractorSearchResultPage = withContext(Dispatchers.IO) {
        ExtractorSearchResultPage(emptyList(), null)
    }

    override suspend fun getTrending(): List<ExtractorVideoItem> = withContext(Dispatchers.IO) {
        emptyList()
    }

    override suspend fun searchMusic(query: String): List<ExtractorMusicItem> = withContext(Dispatchers.IO) {
        emptyList()
    }

    override suspend fun getRelatedMusic(videoId: String): List<ExtractorMusicItem> = withContext(Dispatchers.IO) {
        emptyList()
    }

    override suspend fun getRelatedVideos(videoId: String): List<ExtractorVideoItem> = withContext(Dispatchers.IO) {
        try {
            initNewPipe()
            val url = "https://www.youtube.com/watch?v=$videoId"
            val streamInfo = StreamInfo.getInfo(ServiceList.YouTube, url)
            streamInfo.relatedItems.filterIsInstance<StreamInfoItem>().map {
                ExtractorVideoItem(
                    id = it.url.substringAfter("v=").take(11),
                    title = it.name ?: "",
                    artist = it.uploaderName ?: "",
                    duration = it.duration * 1000L,
                    thumbnailUrl = it.thumbnails?.firstOrNull()?.url ?: "",
                    viewCount = it.viewCount,
                    channelAvatarUrl = it.uploaderAvatars?.firstOrNull()?.url ?: "",
                    channelId = it.uploaderUrl ?: ""
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "NewPipe getRelatedVideos failed: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun getShorts(): List<ExtractorVideoItem> = withContext(Dispatchers.IO) {
        emptyList()
    }

    override suspend fun getSearchSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        try {
            initNewPipe()
            ServiceList.YouTube.suggestionExtractor.suggestionList(query)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
