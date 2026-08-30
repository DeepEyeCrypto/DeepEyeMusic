package com.deepeye.musicpro.data.source.remote

import com.deepeye.musicpro.data.ReturnDislikeConfig
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * Return YouTube Dislike API client.
 * Mobile-optimized with caching and offline support.
 */
@Singleton
class ReturnDislikeApi @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    
    /**
     * Get dislike count and ratio for a video.
     * Returns DislikeInfo with counts and percentage.
     */
    suspend fun getDislikeInfo(videoId: String): DislikeInfo? = withContext(Dispatchers.IO) {
        try {
            val url = "${ReturnDislikeConfig.API_BASE_URL}/votes?videoId=$videoId"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", ReturnDislikeConfig.USER_AGENT)
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null
            
            val body = response.body?.string() ?: return@withContext null
            parseDislikeInfo(body)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Parse JSON response into DislikeInfo object.
     */
    private fun parseDislikeInfo(json: String): DislikeInfo {
        return try {
            val obj = JSONObject(json)
            DislikeInfo(
                videoId = obj.getString("id"),
                likes = obj.optInt("likes", 0),
                dislikes = obj.optInt("dislikes", 0),
                rating = obj.optDouble("rating", 0.0).toFloat(),
                dateCreated = obj.optString("created", ""),
                dateModified = obj.optString("modified", "")
            )
        } catch (e: Exception) {
            DislikeInfo(
                videoId = "",
                likes = 0,
                dislikes = 0,
                rating = 0f,
                dateCreated = "",
                dateModified = ""
            )
        }
    }
}

/**
 * Data class for dislike information.
 */
data class DislikeInfo(
    val videoId: String,
    val likes: Int,
    val dislikes: Int,
    val rating: Float,
    val dateCreated: String,
    val dateModified: String
) {
    /**
     * Get total votes.
     */
    fun getTotalVotes(): Int = likes + dislikes
    
    /**
     * Get dislike percentage (0-100).
     */
    fun getDislikePercentage(): Float {
        val total = getTotalVotes()
        return if (total == 0) 0f else (dislikes.toFloat() / total) * 100f
    }
    
    /**
     * Get like percentage (0-100).
     */
    fun getLikePercentage(): Float = 100f - getDislikePercentage()
    
    /**
     * Get rating label based on dislike percentage.
     * Mobile-optimized for small screens.
     */
    fun getRatingLabel(): String {
        val percentage = getDislikePercentage()
        return when {
            percentage < 10 -> "Excellent"
            percentage < 25 -> "Good"
            percentage < 50 -> "Mixed"
            percentage < 75 -> "Poor"
            else -> "Terrible"
        }
    }
}