package com.deepeye.musicpro.data

import com.deepeye.musicpro.BuildConfig

/**
 * OAuth 2.0 Configuration for YouTube integration.
 * Mobile-optimized with Chrome Custom Tabs support.
 */
object OAuthConfig {
    // YouTube TV (Limited Input Device) OAuth Client ID
    // This is the SmartTube-style client that works without GMS
    const val CLIENT_ID = "861556708454-d6dlm3lh05idd8npek18k6be8ba3oc6g.apps.googleusercontent.com"
    
    // Client secret for YouTube TV device flow (publicly known, safe to include)
    const val CLIENT_SECRET = "SboVhoG9s0rNafixCSGGKXAT"
    
    // OAuth scopes for YouTube Data API
    const val SCOPE = "https://www.googleapis.com/auth/youtube"
    
    // OAuth endpoints
    const val DEVICE_CODE_URL = "https://oauth2.googleapis.com/device/code"
    const val TOKEN_URL = "https://oauth2.googleapis.com/token"
    const val VERIFICATION_URL = "https://www.youtube.com/activate"
    
    // Mobile optimization: Chrome Custom Tabs
    const val CUSTOM_TABS_PACKAGE = "com.android.chrome"
    const val REDIRECT_SCHEME = "com.deepeye.musicpro"
    const val REDIRECT_HOST = "oauth-callback"
    const val REDIRECT_URI = "$REDIRECT_SCHEME://$REDIRECT_HOST"
    
    // Token refresh settings
    const val TOKEN_EXPIRY_BUFFER_MS = 5 * 60 * 1000L // 5 minutes buffer
    const val POLLING_INTERVAL_MS = 5000L // 5 seconds
    const val POLLING_TIMEOUT_MS = 120000L // 2 minutes timeout
    
    // API endpoints
    const val YOUTUBE_DATA_API_BASE = "https://www.googleapis.com/youtube/v3/"
    const val SPONSORBLOCK_BASE = "https://sponsor.ajay.app/api/"
    const val RETURN_DISLIKE_BASE = "https://returnyoutubedislikeapi.com/"
}
