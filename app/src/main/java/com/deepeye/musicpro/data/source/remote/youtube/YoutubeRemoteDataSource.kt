package com.deepeye.musicpro.data.source.remote.youtube

import android.util.Log
import com.deepeye.musicpro.domain.model.home.HomeMusicItem
import com.deepeye.musicpro.domain.model.home.HomeVideoItem
import com.yushosei.newpipe.extractor.NewPipe
import com.yushosei.newpipe.extractor.ServiceList
import com.yushosei.newpipe.extractor.stream.StreamInfo
import com.yushosei.newpipe.extractor.stream.StreamInfoItem
import com.yushosei.newpipe.extractor.localization.Localization
import com.yushosei.newpipe.extractor.localization.ContentCountry
import com.yushosei.newpipe.extractor.InfoItem
import com.yushosei.newpipe.extractor.StreamingService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YoutubeRemoteDataSource @Inject constructor(
    private val downloader: NewPipeDownloader
) {
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    private val yt: StreamingService by lazy {
        NewPipe.init(downloader, Localization.DEFAULT, ContentCountry.DEFAULT)
        ServiceList.YouTube
    }

    // 🔍 Search videos
    suspend fun searchVideos(query: String): List<HomeVideoItem> =
        withContext(ioDispatcher) {
            try {
                val extractor = yt.getSearchExtractor(query)
                extractor.fetchPage()
                val infoItems: List<InfoItem> = extractor.initialPage.items
                infoItems.filterIsInstance<StreamInfoItem>()
                     .map { item: StreamInfoItem -> item.toHomeVideoItem() }
            } catch (e: Exception) {
                Log.e("YoutubeDS", "searchVideos failed: ${e.message}")
                emptyList()
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
                val extractor = yt.getSearchExtractor(
                    query,
                    listOf("music_songs"),
                    ""
                )
                extractor.fetchPage()
                val infoItems: List<InfoItem> = extractor.initialPage.items
                infoItems.filterIsInstance<StreamInfoItem>()
                     .map { item: StreamInfoItem -> item.toHomeMusicItem() }
            } catch (e: Exception) {
                Log.e("YoutubeDS", "searchMusic failed: ${e.message}")
                emptyList()
            }
        }

    // ▶️ Get stream URL (audio only — best bitrate)
    suspend fun getAudioStreamUrl(videoId: String): String? =
        withContext(ioDispatcher) {
            try {
                val url = if (videoId.startsWith("http")) videoId else "https://www.youtube.com/watch?v=$videoId"
                val info = StreamInfo.getInfo(yt, url)
                val audio = info.audioStreams.maxByOrNull { it.bitrate }
                audio?.content
            } catch (e: Exception) {
                Log.e("YoutubeDS", "getStreamUrl failed: ${e.message}")
                null
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

    // Mappers
    private fun StreamInfoItem.toHomeVideoItem(
        isShort: Boolean = false
    ) = HomeVideoItem(
        id            = url.extractVideoId(),
        title         = name ?: "Unknown",
        channelName   = uploaderName ?: "Unknown",
        channelId     = uploaderName ?: "", 
        thumbnailUrl  = thumbnails.lastOrNull()?.url ?: "",
        duration      = duration,
        viewCount     = 0L, 
        uploadDate    = "", 
        isShort       = isShort || duration in 1..60
    )

    private fun StreamInfoItem.toHomeMusicItem() = HomeMusicItem(
        id           = url.extractVideoId(),
        title        = name ?: "Unknown",
        artist       = uploaderName ?: "Unknown Artist",
        thumbnailUrl = thumbnails.lastOrNull()?.url ?: "",
        duration     = duration,
        playCount    = 0L
    )

    private fun String.extractVideoId(): String {
        return when {
            contains("v=") -> substringAfter("v=").substringBefore("&")
            contains("youtu.be/") -> substringAfterLast("/")
            contains("/shorts/") -> substringAfterLast("/")
            else -> this
        }.take(11)
    }
}
