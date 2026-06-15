package com.deepeye.musicpro.extractor.plugin

import com.deepeye.musicpro.extractor.bridge.*
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NewPipeExtractorPlugin : IExtractorBridge {

    private val yt: StreamingService by lazy {
        NewPipe.init(PluginDownloader(), Localization.DEFAULT, ContentCountry.DEFAULT)
        ServiceList.YouTube
    }

    override suspend fun initBridge(downloader: Any) {
        // We use our own PluginDownloader to avoid ClassLoader mismatch
        yt // Trigger init
    }

    override suspend fun searchVideosFirstPage(query: String): ExtractorSearchResultPage = withContext(Dispatchers.IO) {
        val extractor = yt.getSearchExtractor(query)
        extractor.fetchPage()
        val page = extractor.initialPage
        val items = page.items.filterIsInstance<StreamInfoItem>().map { it.toExtractorVideoItem() }
        ExtractorSearchResultPage(items, page.nextPage?.url)
    }

    override suspend fun searchVideosNextPage(query: String, nextPageUrl: String): ExtractorSearchResultPage = withContext(Dispatchers.IO) {
        val extractor = yt.getSearchExtractor(query)
        extractor.fetchPage() // fetchPage initialization needed
        // Since nextPageUrl is usually sufficient in Page(nextPageUrl, id)
        val page = extractor.getPage(Page(nextPageUrl, ""))
        val items = page.items.filterIsInstance<StreamInfoItem>().map { it.toExtractorVideoItem() }
        ExtractorSearchResultPage(items, page.nextPage?.url)
    }

    override suspend fun getTrending(): List<ExtractorVideoItem> = withContext(Dispatchers.IO) {
        val extractor = yt.getSearchExtractor("trending music")
        extractor.fetchPage()
        extractor.initialPage.items.filterIsInstance<StreamInfoItem>().map { it.toExtractorVideoItem() }
    }

    override suspend fun searchMusic(query: String): List<ExtractorMusicItem> = withContext(Dispatchers.IO) {
        val extractor = yt.getSearchExtractor(query, listOf("music_songs"), "")
        extractor.fetchPage()
        extractor.initialPage.items.filterIsInstance<StreamInfoItem>().map { it.toExtractorMusicItem() }
    }

    override suspend fun extractStream(videoId: String, preferVideo: Boolean): ExtractorStreamResult? = withContext(Dispatchers.IO) {
        try {
            val streamInfo = StreamInfo.getInfo(yt, "https://www.youtube.com/watch?v=$videoId")
            if (preferVideo) {
                val streamUrl = streamInfo.dashMpdUrl?.takeIf { it.isNotEmpty() }
                    ?: streamInfo.hlsUrl?.takeIf { it.isNotEmpty() }
                if (streamUrl != null) {
                    return@withContext ExtractorStreamResult(
                        url = streamUrl,
                        container = "adaptive",
                        extractorName = "NewPipePlugin"
                    )
                }
            } else {
                val audio = streamInfo.audioStreams.maxByOrNull { it.bitrate } ?: streamInfo.audioStreams.firstOrNull()
                if (audio != null) {
                    return@withContext ExtractorStreamResult(
                        url = audio.content,
                        container = "audio",
                        quality = "${audio.bitrate} kbps",
                        extractorName = "NewPipePlugin"
                    )
                }
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun getRelatedMusic(videoId: String): List<ExtractorMusicItem> = withContext(Dispatchers.IO) {
        try {
            val streamInfo = StreamInfo.getInfo(yt, "https://www.youtube.com/watch?v=$videoId")
            streamInfo.relatedItems.filterIsInstance<StreamInfoItem>().map { it.toExtractorMusicItem() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getShorts(): List<ExtractorVideoItem> = withContext(Dispatchers.IO) {
        val extractor = yt.getSearchExtractor("shorts", listOf("music_videos"), "")
        extractor.fetchPage()
        extractor.initialPage.items.filterIsInstance<StreamInfoItem>().map { it.toExtractorVideoItem(isShort = true) }
    }

    override suspend fun getSearchSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val extractor = yt.getSuggestionExtractor()
            extractor.suggestionList(query)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun StreamInfoItem.toExtractorVideoItem(isShort: Boolean = false) = ExtractorVideoItem(
        id = this.url.substringAfter("v=").substringBefore("&").take(11),
        title = this.name,
        artist = this.uploaderName,
        duration = this.duration,
        thumbnailUrl = this.thumbnails.firstOrNull()?.url ?: "",
        isShort = isShort
    )

    private fun StreamInfoItem.toExtractorMusicItem() = ExtractorMusicItem(
        id = this.url.substringAfter("v=").substringBefore("&").take(11),
        title = this.name,
        artist = this.uploaderName,
        album = "YouTube",
        duration = this.duration,
        thumbnailUrl = this.thumbnails.firstOrNull()?.url ?: ""
    )
}
