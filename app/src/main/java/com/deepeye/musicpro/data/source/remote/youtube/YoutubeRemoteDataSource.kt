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
        getStreamUrl(videoId, preferVideo = false)?.url

    suspend fun getVideoStreamUrl(videoId: String): String? =
        getStreamUrl(videoId, preferVideo = true)?.url

    // Unified helper that extracts the media stream and dynamically computes whether it contains video
    suspend fun getStreamUrl(videoId: String, preferVideo: Boolean): StreamResult? =
        withContext(ioDispatcher) {
            try {
                val cleanId = videoId.trim().take(11)
                val url = "https://www.youtube.com/watch?v=$cleanId"
                val info = StreamInfo.getInfo(yt, url)
                
                if (preferVideo) {
                    val streamUrl = info.dashMpdUrl?.takeIf { it.isNotEmpty() } 
                        ?: info.hlsUrl?.takeIf { it.isNotEmpty() }
                    
                    Log.d("YoutubeDS", "Video stream extraction for $videoId: DASH=${info.dashMpdUrl?.isNotEmpty()}, HLS=${info.hlsUrl?.isNotEmpty()}")
                    
                    if (streamUrl != null) {
                        StreamResult(streamUrl, isVideo = true)
                    } else {
                        Log.w("YoutubeDS", "getStreamUrl: No valid DASH/HLS URL found for $videoId. Falling back to audio stream.")
                        val audioFallback = info.audioStreams.maxByOrNull { it.bitrate }?.content
                        audioFallback?.let { StreamResult(it, isVideo = false) }
                    }
                } else {
                    val audio = info.audioStreams.maxByOrNull { it.bitrate }
                        ?: info.audioStreams.firstOrNull()
                    audio?.content?.let { StreamResult(it, isVideo = false) }
                }
            } catch (e: Exception) {
                Log.e("YoutubeDS", "getStreamUrl failed for $videoId: ${e.message}", e)
                null
            }
        }

    // 🔗 Get related music (for autoplay)
    suspend fun getRelatedMusic(title: String, artist: String, isVideo: Boolean = false): List<com.deepeye.musicpro.domain.model.MediaItem.Remote> =
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
                        isVideo = isVideo
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
        val id = when {
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
    val isVideo: Boolean
)
