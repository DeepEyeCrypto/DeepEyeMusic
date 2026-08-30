package com.deepeye.musicpro.data

/**
 * SponsorBlock API configuration.
 * Mobile-optimized with caching and user preferences.
 */
object SponsorBlockConfig {
    
    const val API_BASE_URL = "https://sponsor.ajay.app"
    
    const val USER_AGENT = "DeepEyeMusicPro/1.0 (Android)"
    
    /**
     * Categories to skip automatically.
     * Mobile-optimized: Only skip sponsor segments by default.
     */
    val DEFAULT_SKIP_CATEGORIES = setOf("sponsor")
    
    /**
     * All available categories.
     */
    val ALL_CATEGORIES = listOf(
        "sponsor",
        "selfpromo",
        "interaction",
        "intro",
        "outro",
        "preview",
        "music_offtopic",
        "filler"
    )
    
    /**
     * Minimum votes required to auto-skip.
     * Mobile-optimized: Conservative threshold for mobile users.
     */
    const val MIN_VOTES_TO_SKIP = 0
    
    /**
     * Cache duration for segments (in milliseconds).
     */
    const val CACHE_DURATION_MS = 24 * 60 * 60 * 1000L // 24 hours
    
    /**
     * Enable/disable SponsorBlock.
     */
    const val PREF_ENABLED = "sponsorblock_enabled"
    
    /**
     * Selected categories to skip.
     */
    const val PREF_SKIP_CATEGORIES = "sponsorblock_skip_categories"
    
    /**
     * Show toast when segment is skipped.
     */
    const val PREF_SHOW_TOAST = "sponsorblock_show_toast"
    
    /**
     * Auto-skip vs manual skip.
     */
    const val PREF_AUTO_SKIP = "sponsorblock_auto_skip"
    
    /**
     * User ID for submissions.
     */
    const val PREF_USER_ID = "sponsorblock_user_id"
}