package com.deepeye.musicpro.domain.repository.search

import com.deepeye.musicpro.data.cache.CacheManager
import com.deepeye.musicpro.data.prefs.TasteProfile
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
    private val contentFetcher: ContentFetcher,
    private val cacheManager: CacheManager,
    private val youtubeRemoteDataSource: YoutubeRemoteDataSource,
) {
    // In-memory search history (LRU, max 20 entries)
    private val searchHistory = mutableListOf<String>()
    suspend fun search(
        query: String,
        filter: SearchFilter,
        sort: SearchSort,
    ): List<SearchResultItem> =
        withContext(Dispatchers.IO) {
            if (query.isBlank()) return@withContext emptyList()

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
                    val fetched =
                        if (com.deepeye.musicpro.BuildConfig.YOUTUBE_API_KEY.isNotEmpty()) {
                            try {
                                android.util.Log.d(
                                    "SearchRepo",
                                    "YOUTUBE_API_KEY is non-empty. Fetching from contentFetcher..."
                                )
                                when (filter) {
                                    SearchFilter.ARTISTS -> contentFetcher.searchByArtist(query, 20)
                                    SearchFilter.VIDEOS -> contentFetcher.searchByQuery("$query music video", 20)
                                    SearchFilter.SONGS -> contentFetcher.searchByQuery("$query song", 20)
                                    SearchFilter.ALBUMS -> contentFetcher.searchByQuery("$query album", 20)
                                    else -> contentFetcher.searchByQuery(query, 30)
                                }
                            } catch (e: Exception) {
                                android.util.Log.e(
                                    "SearchRepo",
                                    "ContentFetcher failed, falling back to NewPipe: ${e.message}",
                                    e
                                )
                                fetchFromNewPipe(query, filter)
                            }
                        } else {
                            android.util.Log.d("SearchRepo", "YOUTUBE_API_KEY is empty, calling NewPipe directly...")
                            fetchFromNewPipe(query, filter)
                        }

                    android.util.Log.d("SearchRepo", "Fetched ${fetched.size} items from remote source")
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
                    android.util.Log.e("SearchRepo", "Search mapping/caching failed: ${e.message}", e)
                    emptyList()
                }

            val merged =
                (cached + remote)
                    .distinctBy { it.id }
                    .let { applyFilter(it, filter) }
                    .let { applySort(it, sort) }

            android.util.Log.d(
                "SearchRepo",
                "Search returned ${merged.size} items in total (cached=${cached.size}, remote=${remote.size})"
            )
            merged
        }

    private suspend fun fetchFromNewPipe(
        query: String,
        filter: SearchFilter,
    ): List<VideoItem> {
        return try {
            android.util.Log.d("SearchRepo", "fetchFromNewPipe query='$query' filter=$filter")
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
            android.util.Log.e("SearchRepo", "fetchFromNewPipe failed: ${e.message}", e)
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

    suspend fun buildSuggestions(prefs: TasteProfile?): List<String> {
        val base = mutableListOf<String>()
        if (prefs != null) {
            base += prefs.preferredLanguages.map { "$it songs" }
            base += prefs.favoriteArtists.map { it }
            base += prefs.preferredGenres.map { "$it music" }
        }
        base += listOf("trending songs", "new releases", "top music videos")
        return base.distinct().take(12)
    }

    suspend fun getRecentSearches(): List<String> {
        return searchHistory.toList()
    }

    fun saveRecentSearch(query: String) {
        if (query.isBlank()) return
        searchHistory.remove(query)
        searchHistory.add(0, query)
        if (searchHistory.size > 20) {
            searchHistory.removeAt(searchHistory.lastIndex)
        }
    }

    fun clearSearchHistory() {
        searchHistory.clear()
    }
}
