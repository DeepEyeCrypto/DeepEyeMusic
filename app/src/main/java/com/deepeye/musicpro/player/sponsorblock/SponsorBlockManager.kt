package com.deepeye.musicpro.player.sponsorblock

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.deepeye.musicpro.data.SponsorBlockConfig
import com.deepeye.musicpro.data.source.remote.SponsorBlockApi
import com.deepeye.musicpro.data.source.remote.SponsorSegment
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * SponsorBlock manager for Media3 player.
 * Handles segment detection, skipping, and user preferences.
 * Mobile-optimized with efficient background processing.
 */
@UnstableApi
@Singleton
class SponsorBlockManager @Inject constructor(
    private val sponsorBlockApi: SponsorBlockApi
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private val _segments = MutableStateFlow<List<SponsorSegment>>(emptyList())
    val segments = _segments.asStateFlow()
    
    private val _currentVideoId = MutableStateFlow<String?>(null)
    private val currentVideoId = _currentVideoId.asStateFlow()
    
    private val _isSkipEnabled = MutableStateFlow(true)
    val isSkipEnabled = _isSkipEnabled.asStateFlow()
    
    private val _skipCategories = MutableStateFlow(SponsorBlockConfig.DEFAULT_SKIP_CATEGORIES)
    val skipCategories = _skipCategories.asStateFlow()
    
    private val _lastSkipTime = MutableStateFlow(0L)
    
    private var skipJob: Job? = null
    
    /**
     * Initialize SponsorBlock for a video.
     * Fetches segments and sets up monitoring.
     */
    fun initForVideo(videoId: String) {
        if (videoId == currentVideoId.value) return
        
        _currentVideoId.value = videoId
        fetchSegments(videoId)
    }
    
    /**
     * Fetch segments for a video from SponsorBlock API.
     */
    private fun fetchSegments(videoId: String) {
        scope.launch {
            try {
                val fetched = sponsorBlockApi.getSegments(videoId)
                _segments.value = fetched.filter { segment ->
                    segment.shouldSkip() && segment.category in skipCategories.value
                }
            } catch (e: Exception) {
                _segments.value = emptyList()
            }
        }
    }
    
    /**
     * Monitor playback position and skip segments automatically.
     * Mobile-optimized: Only checks when enabled and has segments.
     */
    fun monitorPlayback(player: Player) {
        if (skipJob?.isActive == true) return
        
        skipJob = scope.launch {
            while (true) {
                delay(500) // Check every 500ms (mobile optimization)
                
                if (!_isSkipEnabled.value || segments.value.isEmpty()) continue
                
                val currentPosition = player.currentPosition / 1000f // Convert to seconds
                val videoId = currentVideoId.value ?: continue
                
                // Check if current position is within any segment
                for (segment in segments.value) {
                    if (currentPosition in segment.startTime..segment.endTime) {
                        // Skip segment
                        val now = System.currentTimeMillis()
                        if (now - _lastSkipTime.value > 1000) { // Prevent rapid skipping
                            player.seekTo((segment.endTime * 1000).toLong())
                            _lastSkipTime.value = now
                            // TODO: Show toast notification
                        }
                        break
                    }
                }
            }
        }
    }
    
    /**
     * Stop monitoring playback.
     */
    fun stopMonitoring() {
        skipJob?.cancel()
        skipJob = null
    }
    
    /**
     * Toggle skip enabled state.
     */
    fun setSkipEnabled(enabled: Boolean) {
        _isSkipEnabled.update { enabled }
    }
    
    /**
     * Update skip categories.
     */
    fun setSkipCategories(categories: Set<String>) {
        _skipCategories.update { categories }
        // Re-fetch segments with new categories
        currentVideoId.value?.let { fetchSegments(it) }
    }
    
    /**
     * Get segment at current position.
     */
    fun getSegmentAtPosition(positionMs: Long): SponsorSegment? {
        val positionSec = positionMs / 1000f
        return segments.value.find { positionSec in it.startTime..it.endTime }
    }
    
    /**
     * Get upcoming segment (within next 5 seconds).
     */
    fun getUpcomingSegment(positionMs: Long): SponsorSegment? {
        val positionSec = positionMs / 1000f
        return segments.value.find { 
            it.startTime > positionSec && it.startTime <= positionSec + 5f 
        }
    }
    
    /**
     * Clear cached data.
     */
    fun clearCache() {
        _segments.value = emptyList()
        _currentVideoId.value = null
    }
    
    /**
     * Release resources.
     */
    fun release() {
        stopMonitoring()
        scope.launch {
            delay(100)
            // Cleanup
        }
    }
}