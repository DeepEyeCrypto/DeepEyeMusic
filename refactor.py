import re

with open("/Users/enayat/Documents/DeepEyeMusicPro/app/src/main/java/com/deepeye/musicpro/data/source/remote/youtube/YoutubeRemoteDataSource.kt", "r") as f:
    content = f.read()

# 1. Imports
content = re.sub(r'import org\.schabi\.newpipe\.extractor\..*\n', '', content)
content = content.replace("private val downloader: NewPipeDownloader,\n", "")

# 2. Remove yt
content = re.sub(r'    private val yt: StreamingService by lazy \{[\s\S]*?ServiceList\.YouTube\n    \}\n', '', content)

# 3. Replace extractNewPipe
new_extract = """    private suspend fun extractNewPipe(videoId: String, preferVideo: Boolean): StreamResult? {
        return try {
            com.deepeye.musicpro.core.plugins.PluginManager.getExtractor(context).extractStream(videoId, preferVideo)?.let {
                StreamResult(it.url, isVideo = it.container != "audio", isAdaptive = it.container == "adaptive")
            }
        } catch (e: Exception) {
            Log.e("YoutubeDS", "NewPipe extract failed", e)
            null
        }
    }"""
content = re.sub(r'    private suspend fun extractNewPipe.*?\}\n        \}', new_extract, content, flags=re.DOTALL)

# 4. Search methods
search_block = """    suspend fun searchVideosFirstPage(query: String): SearchResultPage =
        withContext(ioDispatcher) {
            try {
                val page = com.deepeye.musicpro.core.plugins.PluginManager.getExtractor(context).searchVideosFirstPage(query)
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
                val page = com.deepeye.musicpro.core.plugins.PluginManager.getExtractor(context).searchVideosNextPage(query, nextPageUrl)
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
                com.deepeye.musicpro.core.plugins.PluginManager.getExtractor(context).getTrending().map { it.toHomeVideoItem() }
            } catch (e: Exception) {
                Log.e("YoutubeDS", "getTrending failed: ${e.message}")
                emptyList()
            }
        }

    // 🎵 Music search
    suspend fun searchMusic(query: String): List<HomeMusicItem> =
        withContext(ioDispatcher) {
            try {
                com.deepeye.musicpro.core.plugins.PluginManager.getExtractor(context).searchMusic(query).map { it.toHomeMusicItem() }
            } catch (e: Exception) {
                Log.e("YoutubeDS", "searchMusic failed: ${e.message}", e)
                throw e
            }
        }"""
content = re.sub(r'    suspend fun searchVideosFirstPage.*?throw e\n            \}\n        \}', search_block, content, flags=re.DOTALL)

# 5. getRelatedMusic
related_block = """    suspend fun getRelatedMusic(
        title: String,
        artist: String,
        isVideo: Boolean = false,
    ): List<com.deepeye.musicpro.domain.model.MediaItem.Remote> =
        withContext(ioDispatcher) {
            try {
                com.deepeye.musicpro.core.plugins.PluginManager.getExtractor(context).getRelatedMusic("$title $artist").map { item ->
                    com.deepeye.musicpro.domain.model.MediaItem.Remote(
                        id = item.id,
                        title = item.title,
                        artist = item.artist,
                        artworkUri = android.net.Uri.parse(item.thumbnailUrl),
                        duration = item.duration * 1000L,
                        isVideo = isVideo,
                    )
                }
            } catch (e: Exception) {
                Log.e("YoutubeDS", "getRelatedMusic failed: ${e.message}")
                emptyList()
            }
        }"""
content = re.sub(r'    suspend fun getRelatedMusic.*?emptyList\(\)\n            \}\n        \}', related_block, content, flags=re.DOTALL)

# 6. getShorts
shorts_block = """    suspend fun getShorts(): List<HomeVideoItem> =
        withContext(ioDispatcher) {
            try {
                com.deepeye.musicpro.core.plugins.PluginManager.getExtractor(context).getShorts().map { it.toHomeVideoItem() }
            } catch (e: Exception) {
                emptyList()
            }
        }"""
content = re.sub(r'    suspend fun getShorts\(\).*?emptyList\(\)\n            \}\n        \}', shorts_block, content, flags=re.DOTALL)

# 7. getSearchSuggestions
sugg_block = """    suspend fun getSearchSuggestions(query: String): List<String> =
        withContext(ioDispatcher) {
            try {
                com.deepeye.musicpro.core.plugins.PluginManager.getExtractor(context).getSearchSuggestions(query)
            } catch (e: Exception) {
                Log.e("YoutubeDS", "getSearchSuggestions failed: ${e.message}")
                emptyList()
            }
        }"""
content = re.sub(r'    suspend fun getSearchSuggestions.*?emptyList\(\)\n            \}\n        \}', sugg_block, content, flags=re.DOTALL)

# 8. Mappers
mappers_block = """    // Mappers
    private fun com.deepeye.musicpro.extractor.bridge.ExtractorVideoItem.toHomeVideoItem() = HomeVideoItem(
        id = id,
        title = title,
        channelName = artist,
        thumbnailUrl = thumbnailUrl,
        duration = duration,
        isShort = isShort
    )

    private fun com.deepeye.musicpro.extractor.bridge.ExtractorMusicItem.toHomeMusicItem() = HomeMusicItem(
        id = id,
        title = title,
        artist = artist,
        thumbnailUrl = thumbnailUrl,
        duration = duration
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
    val isAdaptive: Boolean = false,
)

data class SearchResultPage(
    val items: List<HomeVideoItem>,
    val nextPageUrl: String?,
)"""
content = re.sub(r'    // Mappers\n.*?val nextPage: Page\?,\n\)', mappers_block, content, flags=re.DOTALL)

with open("/Users/enayat/Documents/DeepEyeMusicPro/app/src/main/java/com/deepeye/musicpro/data/source/remote/youtube/YoutubeRemoteDataSource.kt", "w") as f:
    f.write(content)
