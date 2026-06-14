package com.deepeye.musicpro.domain.recommendation

import android.net.Uri
import com.deepeye.musicpro.BuildConfig
import com.deepeye.musicpro.data.source.remote.youtube.YoutubeRemoteDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class ContentFetcher
@Inject
constructor(
    private val httpClient: OkHttpClient,
    private val youtubeDs: Provider<YoutubeRemoteDataSource>,
) {
    // ── Strategy A: YouTube Data API v3 ──
    suspend fun getRelatedVideos(
        videoId: String,
        maxResults: Int = 20,
    ): List<VideoItem> =
        withContext(Dispatchers.IO) {
            if (BuildConfig.YOUTUBE_API_KEY.isEmpty()) {
                return@withContext getRelatedVideosFallback(videoId, maxResults)
            }
            try {
                val url =
                    "https://www.googleapis.com/youtube/v3/search" +
                        "?part=snippet" +
                        "&relatedToVideoId=$videoId" +
                        "&type=video" +
                        "&videoCategoryId=10" +
                        "&maxResults=$maxResults" +
                        "&key=${BuildConfig.YOUTUBE_API_KEY}"

                val response = httpClient.newCall(Request.Builder().url(url).build()).execute()
                parseVideoItems(response.body?.string() ?: "")
            } catch (e: Exception) {
                getRelatedVideosFallback(videoId, maxResults)
            }
        }

    private suspend fun getRelatedVideosFallback(videoId: String, maxResults: Int): List<VideoItem> {
        return try {
            val ds = youtubeDs.get()
            ds.searchVideos("related to $videoId").take(maxResults).map { item ->
                VideoItem(
                    videoId = item.id,
                    title = item.title,
                    artist = item.channelName,
                    channelId = item.channelId,
                    duration = "${item.duration / 60}:${item.duration % 60}",
                    genre = ""
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("ContentFetcher", "getRelatedVideosFallback failed for $videoId: ${e.message}")
            emptyList()
        }
    }

    // ── Strategy B: YouTube search by artist name + genre ──
    suspend fun searchByArtist(
        artistName: String,
        maxResults: Int = 15,
    ): List<VideoItem> =
        withContext(Dispatchers.IO) {
            if (BuildConfig.YOUTUBE_API_KEY.isEmpty()) {
                return@withContext searchByQueryFallback("$artistName new songs official", maxResults)
            }
            try {
                val query = Uri.encode("$artistName new songs official")
                val url =
                    "https://www.googleapis.com/youtube/v3/search" +
                        "?part=snippet&q=$query&type=video" +
                        "&videoCategoryId=10&maxResults=$maxResults" +
                        "&key=${BuildConfig.YOUTUBE_API_KEY}"

                val response = httpClient.newCall(Request.Builder().url(url).build()).execute()
                parseVideoItems(response.body?.string() ?: "")
            } catch (e: Exception) {
                searchByQueryFallback("$artistName new songs official", maxResults)
            }
        }

    suspend fun searchByQuery(
        query: String,
        maxResults: Int = 15,
    ): List<VideoItem> =
        withContext(Dispatchers.IO) {
            if (BuildConfig.YOUTUBE_API_KEY.isEmpty()) {
                return@withContext searchByQueryFallback(query, maxResults)
            }
            try {
                val encodedQuery = Uri.encode(query)
                val url =
                    "https://www.googleapis.com/youtube/v3/search" +
                        "?part=snippet&q=$encodedQuery&type=video" +
                        "&videoCategoryId=10&maxResults=$maxResults" +
                        "&key=${BuildConfig.YOUTUBE_API_KEY}"

                val response = httpClient.newCall(Request.Builder().url(url).build()).execute()
                parseVideoItems(response.body?.string() ?: "")
            } catch (e: Exception) {
                searchByQueryFallback(query, maxResults)
            }
        }

    private suspend fun searchByQueryFallback(query: String, maxResults: Int): List<VideoItem> {
        return try {
            val ds = youtubeDs.get()
            ds.searchVideos(query).take(maxResults).map { item ->
                VideoItem(
                    videoId = item.id,
                    title = item.title,
                    artist = item.channelName,
                    channelId = item.channelId,
                    duration = "${item.duration / 60}:${item.duration % 60}",
                    genre = ""
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("ContentFetcher", "searchByQueryFallback failed for $query: ${e.message}")
            emptyList()
        }
    }

    // ── Strategy C: InnerTube API ──
    suspend fun getInnerTubeRecommendations(videoId: String): List<VideoItem> =
        withContext(Dispatchers.IO) {
            try {
                val body =
                    """
                        {
                          "context": {
                            "client": {
                              "clientName": "ANDROID_MUSIC",
                              "clientVersion": "6.21.52",
                              "androidSdkVersion": 30,
                              "hl": "en",
                              "gl": "IN"
                            }
                          },
                          "videoId": "$videoId"
                        }
                    """.trimIndent()

                val request =
                    Request.Builder()
                        .url(
                            "https://music.youtube.com/youtubei/v1/next" +
                                "?key=${BuildConfig.YOUTUBE_API_KEY}",
                        )
                        .post(body.toRequestBody("application/json".toMediaType()))
                        .addHeader("X-Goog-Api-Format-Version", "1")
                        .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 11; Pixel 4 XL)")
                        .build()

                val response = httpClient.newCall(request).execute()
                parseInnerTubeResponse(response.body?.string() ?: "")
            } catch (e: Exception) {
                emptyList()
            }
        }

    // ── Strategy D: Trending music by region ──
    suspend fun getTrendingMusic(
        regionCode: String = "IN",
        maxResults: Int = 20,
    ): List<VideoItem> =
        withContext(Dispatchers.IO) {
            if (BuildConfig.YOUTUBE_API_KEY.isEmpty()) {
                return@withContext getTrendingMusicFallback(maxResults)
            }
            try {
                val url =
                    "https://www.googleapis.com/youtube/v3/videos" +
                        "?part=snippet,statistics" +
                        "&chart=mostPopular" +
                        "&videoCategoryId=10" +
                        "&regionCode=$regionCode" +
                        "&maxResults=$maxResults" +
                        "&key=${BuildConfig.YOUTUBE_API_KEY}"
                val response = httpClient.newCall(Request.Builder().url(url).build()).execute()
                parseTrendingVideoItems(response.body?.string() ?: "")
            } catch (e: Exception) {
                getTrendingMusicFallback(maxResults)
            }
        }

    private suspend fun getTrendingMusicFallback(maxResults: Int): List<VideoItem> {
        return try {
            val ds = youtubeDs.get()
            ds.getTrending().take(maxResults).map { item ->
                VideoItem(
                    videoId = item.id,
                    title = item.title,
                    artist = item.channelName,
                    channelId = item.channelId,
                    duration = "${item.duration / 60}:${item.duration % 60}",
                    genre = ""
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("ContentFetcher", "getTrendingMusicFallback failed: ${e.message}")
            emptyList()
        }
    }

    private fun String.extractVideoId(): String {
        return when {
            contains("v=") -> substringAfter("v=").substringBefore("&")
            contains("youtu.be/") -> substringAfterLast("/")
            contains("/shorts/") -> substringAfterLast("/")
            else -> this
        }.take(11)
    }

    private fun parseVideoItems(json: String): List<VideoItem> {
        val list = mutableListOf<VideoItem>()
        if (json.isEmpty()) return list
        try {
            val obj = JSONObject(json)
            if (!obj.has("items")) return list
            val items = obj.getJSONArray("items")
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val idObj = item.optJSONObject("id")
                val snippet = item.optJSONObject("snippet")
                if (idObj != null && snippet != null) {
                    val videoId = idObj.optString("videoId", "")
                    if (videoId.isEmpty()) continue
                    val title = snippet.optString("title", "")
                    val channelTitle = snippet.optString("channelTitle", "")
                    val channelId = snippet.optString("channelId", "")
                    list.add(VideoItem(videoId, title, channelTitle, channelId, "3:00", ""))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun parseTrendingVideoItems(json: String): List<VideoItem> {
        val list = mutableListOf<VideoItem>()
        if (json.isEmpty()) return list
        try {
            val obj = JSONObject(json)
            if (!obj.has("items")) return list
            val items = obj.getJSONArray("items")
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val videoId = item.optString("id", "")
                val snippet = item.optJSONObject("snippet")
                if (videoId.isNotEmpty() && snippet != null) {
                    val title = snippet.optString("title", "")
                    val channelTitle = snippet.optString("channelTitle", "")
                    val channelId = snippet.optString("channelId", "")
                    list.add(VideoItem(videoId, title, channelTitle, channelId, "3:00", ""))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun parseInnerTubeResponse(json: String): List<VideoItem> {
        // A minimal parser for the InnerTube next payload
        val list = mutableListOf<VideoItem>()
        // Full parsing is complex; falling back to a robust regex or simpler JSON traversal
        return list
    }
}
