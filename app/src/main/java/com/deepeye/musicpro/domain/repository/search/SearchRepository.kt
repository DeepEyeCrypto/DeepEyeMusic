package com.deepeye.musicpro.domain.repository.search

import com.deepeye.musicpro.data.cache.CacheManager
import com.deepeye.musicpro.data.source.remote.youtube.SmartTubeEngine
import com.deepeye.musicpro.data.source.remote.youtube.YoutubeRemoteDataSource
import com.deepeye.musicpro.domain.model.search.SearchFilter
import com.deepeye.musicpro.domain.model.search.SearchResultItem
import com.deepeye.musicpro.domain.model.search.SearchSort
import com.deepeye.musicpro.domain.recommendation.ContentFetcher
import com.deepeye.musicpro.domain.recommendation.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SearchRepository
@Inject
constructor(
    private val smartTubeEngine: SmartTubeEngine,
    private val contentFetcher: ContentFetcher,
    private val cacheManager: CacheManager,
    private val youtubeRemoteDataSource: YoutubeRemoteDataSource,
) {
    // In-memory search history is no longer used here; HistoryRepository manages it.
    suspend fun search(
        query: String,
        filter: SearchFilter,
        sort: SearchSort,
    ): List<SearchResultItem> =
        withContext(Dispatchers.IO) {
            if (query.isBlank()) return@withContext emptyList()

            // 1. Primary: SmartTube TVHTML5 /youtubei/v1/search endpoint with Bearer token
            val smartTubeResult = smartTubeEngine.searchTracks(query)
            if (smartTubeResult.isSuccess) {
                val items = smartTubeResult.getOrNull().orEmpty()
                if (items.isNotEmpty()) {
                    return@withContext items
                        .let { applyFilter(it, filter) }
                        .let { applySort(it, sort) }
                }
            }

            // 2. Secondary fallback: Local cache & remote data source
            val cachedVideoItems = cacheManager.loadSearchResults(query).orEmpty()
            val cached =
                cachedVideoItems.map { video ->
                    SearchResultItem(
                        id = video.videoId,
                        title = video.title,
                        subtitle = video.artist,
                        type = SearchFilter.VIDEOS, // Simplify mapping
                        thumbnailUrl = "https://img.youtube.com/vi/${video.videoId}/hqdefault.jpg",
                        artist = video.artist,
                        videoId = video.videoId,
                        channelId = video.channelId,
                    )
                }

            val remote =
                try {
                    val fetched = fetchFromInnertubeDataSource(query, filter)
                    cacheManager.saveSearchResults(query, fetched)
                    fetched.map { video ->
                        SearchResultItem(
                            id = video.videoId,
                            title = video.title,
                            subtitle = video.artist,
                            type = SearchFilter.VIDEOS,
                            thumbnailUrl = "https://img.youtube.com/vi/${video.videoId}/hqdefault.jpg",
                            artist = video.artist,
                            videoId = video.videoId,
                            channelId = video.channelId,
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.e("SearchRepo", "Search mapping/caching fallback failed: ${e.message}", e)
                    emptyList()
                }

            val merged =
                (cached + remote)
                    .distinctBy { it.id }
                    .let { applyFilter(it, filter) }
                    .let { applySort(it, sort) }

            merged
        }

    private suspend fun fetchFromInnertubeDataSource(
        query: String,
        filter: SearchFilter,
    ): List<VideoItem> {
        return try {
            android.util.Log.d("SearchRepo", "fetchFromInnertubeDataSource query='$query' filter=$filter")
            if (filter == SearchFilter.VIDEOS) {
                youtubeRemoteDataSource.searchVideos("$query music video").map { video ->
                    VideoItem(
                        videoId = video.id,
                        title = video.title,
                        artist = video.channelName,
                        channelId = video.channelId,
                        duration = "${video.duration / 60}:${(video.duration % 60).toString().padStart(2, '0')}",
                    )
                }
            } else {
                val searchQuery = when (filter) {
                    SearchFilter.SONGS -> "$query song"
                    SearchFilter.ALBUMS -> "$query album"
                    else -> query
                }
                android.util.Log.d(
                    "SearchRepo",
                    "Calling youtubeRemoteDataSource.searchMusic with query='$searchQuery'"
                )
                val results = youtubeRemoteDataSource.searchMusic(searchQuery)
                android.util.Log.d("SearchRepo", "youtubeRemoteDataSource.searchMusic returned ${results.size} items")
                results.map { music ->
                    VideoItem(
                        videoId = music.id,
                        title = music.title,
                        artist = music.artist,
                        channelId = "",
                        duration = "${music.duration / 60}:${(music.duration % 60).toString().padStart(2, '0')}",
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SearchRepo", "fetchFromInnertubeDataSource failed: ${e.message}", e)
            emptyList()
        }
    }

    private fun applyFilter(
        items: List<SearchResultItem>,
        filter: SearchFilter,
    ): List<SearchResultItem> {
        if (filter == SearchFilter.ALL) return items
        // Simple client-side filter logic (in reality mostly handled by server request)
        return items
    }

    private fun applySort(
        items: List<SearchResultItem>,
        sort: SearchSort,
    ): List<SearchResultItem> {
        return when (sort) {
            SearchSort.ALPHABETICAL -> items.sortedBy { it.title }
            else -> items // Let remote ordering take precedence for RELEVANCE / POPULARITY
        }
    }

    suspend fun buildSuggestions(prefs: Any? = null): List<String> {
        val base = mutableListOf(
            "trending songs",
            "new releases",
            "top music videos",
            "hindi songs",
            "punjabi songs",
            "english hits"
        )
        return base.distinct().take(12)
    }


}
