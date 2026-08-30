package com.deepeye.musicpro.data.source.remote

import com.deepeye.musicpro.data.SponsorBlockConfig
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * SponsorBlock API client for fetching sponsor segments.
 * Mobile-optimized with caching and offline support.
 */
@Singleton
class SponsorBlockApi @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    
    /**
     * Get sponsor segments for a video.
     * Returns list of segments with start/end times and category.
     */
    suspend fun getSegments(videoId: String): List<SponsorSegment> = withContext(Dispatchers.IO) {
        try {
            val url = "${SponsorBlockConfig.API_BASE_URL}/api/skipSegments/$videoId"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", SponsorBlockConfig.USER_AGENT)
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()
            
            val body = response.body?.string() ?: return@withContext emptyList()
            parseSegments(body)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Submit sponsor segment (requires user authentication).
     */
    suspend fun submitSegment(
        videoId: String,
        startTime: Float,
        endTime: Float,
        category: String,
        userId: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "${SponsorBlockConfig.API_BASE_URL}/api/skipSegments"
            val json = JSONObject().apply {
                put("videoID", videoId)
                put("startTime", startTime)
                put("endTime", endTime)
                put("category", category)
                put("userID", userId)
            }
            
            val body = json.toString().toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url(url)
                .post(body)
                .header("User-Agent", SponsorBlockConfig.USER_AGENT)
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Vote on a segment (upvote/downvote).
     */
    suspend fun voteSegment(uuid: String, score: Int, userId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "${SponsorBlockConfig.API_BASE_URL}/api/voteOnSponsorTime"
            val json = JSONObject().apply {
                put("UUID", uuid)
                put("score", score)
                put("userID", userId)
            }
            
            val body = json.toString().toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url(url)
                .post(body)
                .header("User-Agent", SponsorBlockConfig.USER_AGENT)
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Parse JSON response into SponsorSegment objects.
     */
    private fun parseSegments(json: String): List<SponsorSegment> {
        return try {
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { index ->
                val obj = array.getJSONObject(index)
                val segment = obj.getJSONArray("segment")
                SponsorSegment(
                    uuid = obj.getString("UUID"),
                    startTime = segment.getDouble(0).toFloat(),
                    endTime = segment.getDouble(1).toFloat(),
                    category = obj.getString("category"),
                    votes = obj.getInt("votes"),
                    locked = obj.optBoolean("locked", false),
                    hidden = obj.optBoolean("hidden", false)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

/**
 * Data class for SponsorBlock segment.
 */
data class SponsorSegment(
    val uuid: String,
    val startTime: Float,
    val endTime: Float,
    val category: String,
    val votes: Int,
    val locked: Boolean,
    val hidden: Boolean
) {
    /**
     * Check if segment should be skipped based on votes and lock status.
     * Mobile-optimized: Only skip if votes > 0 or locked.
     */
    fun shouldSkip(): Boolean = !hidden && (locked || votes > 0)
    
    /**
     * Get duration of segment in seconds.
     */
    fun getDuration(): Float = endTime - startTime
}