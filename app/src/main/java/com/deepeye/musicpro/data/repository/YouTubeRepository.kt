package com.deepeye.musicpro.data.repository

import android.util.Log
import com.deepeye.musicpro.data.prefs.TasteProfile
import com.deepeye.musicpro.data.source.remote.youtube.AuthenticatedYouTubeClient
import com.deepeye.musicpro.domain.model.home.HomeVideoItem
import com.deepeye.musicpro.domain.model.search.SearchFilter
import com.deepeye.musicpro.domain.model.search.SearchResultItem
import com.deepeye.musicpro.domain.model.search.SearchSort
import com.deepeye.musicpro.domain.repository.search.SearchRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Mobile-optimized YouTube repository.
 *
 * Wraps [AuthenticatedYouTubeClient] (SmartTube-style Innertube client) and the existing
 * [SearchRepository] with a small in-memory TTL cache so pull-to-refresh and screen
 * re-entry do not re-hit the network every time.
 *
 * All methods are safe to call from the main thread; heavy I/O runs on Dispatchers.IO.
 */
@Singleton
class YouTubeRepository @Inject constructor(
    private val authenticatedClient: AuthenticatedYouTubeClient,
    private val searchRepository: SearchRepository,
) {
    private data class CacheEntry<T>(
        val value: T,
        val fetchedAtMillis: Long,
    )

    private val searchCache = LinkedHashMap<String, CacheEntry<List<SearchResultItem>>>()
    private val railCache = mutableMapOf<String, CacheEntry<List<HomeVideoItem>>>()
    private val cacheLock = Mutex()

    companion object {
        private const val TAG = "YouTubeRepository"
        private const val SEARCH_TTL_MS = 5 * 60 * 1000L      // 5 minutes
        private const val RAIL_TTL_MS = 3 * 60 * 1000L        // 3 minutes
        private const val MAX_CACHE_ENTRIES = 32
    }

    suspend fun search(
        query: String,
        filter: SearchFilter,
        sort: SearchSort,
        forceRefresh: Boolean = false,
    ): List<SearchResultItem> {
        if (query.isBlank()) return emptyList()
        val key = "${query.lowercase()}::${filter.name}::${sort.name}"

        if (!forceRefresh) {
            cacheLock.withLock { searchCache[key] }
                ?.takeIf { System.currentTimeMillis() - it.fetchedAtMillis < SEARCH_TTL_MS }
                ?.let { cached ->
                    Log.d(TAG, "search cacheHit=true query='$query' results=${cached.value.size}")
                    return cached.value
                }
        }

        val results = withContext(Dispatchers.IO) {
            try {
                searchRepository.search(query, filter, sort)
            } catch (e: Exception) {
                Log.e(TAG, "search event=failed query='$query' reason=\"${e.message}\"", e)
                emptyList()
            }
        }

        cacheLock.withLock {
            searchCache[key] = CacheEntry(results, System.currentTimeMillis())
            if (searchCache.size > MAX_CACHE_ENTRIES) {
                val oldest = searchCache.keys.firstOrNull()
                if (oldest != null) searchCache.remove(oldest)
            }
        }
        Log.d(TAG, "search cacheHit=false query='$query' results=${results.size}")
        return results
    }

    suspend fun getQueue(): List<HomeVideoItem> = cachedRail("queue") {
        authenticatedClient.getWatchLater()
    }

    suspend fun getLikedVideos(): List<HomeVideoItem> = cachedRail("liked") {
        authenticatedClient.getLikedVideos()
    }

    suspend fun getSubscriptions(): List<HomeVideoItem> = cachedRail("subscriptions") {
        authenticatedClient.getSubscriptionsFeed()
    }

    suspend fun getHistory(): List<HomeVideoItem> = cachedRail("history") {
        authenticatedClient.getHistory()
    }

    suspend fun getRecommended(): List<HomeVideoItem> = cachedRail("recommended") {
        authenticatedClient.getHomeFeed().filter { !it.isShort }
    }

    suspend fun getHomeFeed(): List<HomeVideoItem> = cachedRail("homeFeed") {
        authenticatedClient.getHomeFeed()
    }

    suspend fun getTrending(): List<HomeVideoItem> = cachedRail("trending") {
        authenticatedClient.getTrending()
    }

    suspend fun buildSuggestions(prefs: TasteProfile?): List<String> =
        searchRepository.buildSuggestions(prefs)

    suspend fun clearCache() {
        cacheLock.withLock {
            searchCache.clear()
            railCache.clear()
        }
        Log.d(TAG, "cache event=cleared")
    }

    private suspend fun cachedRail(
        key: String,
        fetch: suspend () -> List<HomeVideoItem>,
    ): List<HomeVideoItem> {
        cacheLock.withLock { railCache[key] }
            ?.takeIf { System.currentTimeMillis() - it.fetchedAtMillis < RAIL_TTL_MS }
            ?.let { cached ->
                Log.d(TAG, "rail cacheHit=true key=$key items=${cached.value.size}")
                return cached.value
            }

        val items = withContext(Dispatchers.IO) {
            try {
                fetch()
            } catch (e: Exception) {
                Log.e(TAG, "rail event=failed key=$key reason=\"${e.message}\"", e)
                emptyList()
            }
        }

        cacheLock.withLock { railCache[key] = CacheEntry(items, System.currentTimeMillis()) }
        Log.d(TAG, "rail cacheHit=false key=$key items=${items.size}")
        return items
    }
}
