package com.deepeye.musicpro.data

/**
 * Return YouTube Dislike API configuration.
 * Mobile-optimized with caching and user preferences.
 */
object ReturnDislikeConfig {
    
    const val API_BASE_URL = "https://returnyoutubedislikeapi.com"
    
    const val USER_AGENT = "DeepEyeMusicPro/1.0 (Android)"
    
    /**
     * Enable/disable Return Dislike.
     */
    const val PREF_ENABLED = "return_dislike_enabled"
    
    /**
     * Show dislike count in Now Playing.
     */
    const val PREF_SHOW_COUNT = "return_dislike_show_count"
    
    /**
     * Show dislike percentage bar.
     */
    const val PREF_SHOW_BAR = "return_dislike_show_bar"
    
    /**
     * Show rating label (Excellent, Good, Mixed, Poor, Terrible).
     */
    const val PREF_SHOW_LABEL = "return_dislike_show_label"
    
    /**
     * Cache duration for dislike info (in milliseconds).
     */
    const val CACHE_DURATION_MS = 24 * 60 * 60 * 1000L // 24 hours
    
    /**
     * Minimum votes required to display dislike info.
     * Mobile-optimized: Conservative threshold for mobile users.
     */
    const val MIN_VOTES_TO_SHOW = 10
}