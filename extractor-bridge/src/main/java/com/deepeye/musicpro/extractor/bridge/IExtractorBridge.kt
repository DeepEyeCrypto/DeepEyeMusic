package com.deepeye.musicpro.extractor.bridge

data class ExtractorVideoItem(
    val id: String,
    val title: String,
    val artist: String,
    val duration: Long,
    val thumbnailUrl: String,
    val isShort: Boolean = false
)

data class ExtractorMusicItem(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val thumbnailUrl: String
)

data class ExtractorSearchResultPage(
    val videos: List<ExtractorVideoItem>,
    val nextPageUrl: String?
)

data class ExtractorStreamResult(
    val url: String,
    val itag: Int = 0,
    val container: String = "",
    val quality: String = "",
    val isDrm: Boolean = false,
    val extractorName: String = "NewPipe"
)

interface IExtractorBridge {
    suspend fun initBridge(downloader: Any) 
    suspend fun searchVideosFirstPage(query: String): ExtractorSearchResultPage
    suspend fun searchVideosNextPage(query: String, nextPageUrl: String): ExtractorSearchResultPage
    suspend fun getTrending(): List<ExtractorVideoItem>
    suspend fun searchMusic(query: String): List<ExtractorMusicItem>
    suspend fun extractStream(videoId: String, preferVideo: Boolean): ExtractorStreamResult?
    suspend fun getRelatedMusic(videoId: String): List<ExtractorMusicItem>
    suspend fun getShorts(): List<ExtractorVideoItem>
    suspend fun getSearchSuggestions(query: String): List<String>
}
