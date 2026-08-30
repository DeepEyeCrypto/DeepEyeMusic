package com.deepeye.musicpro.ui.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.ui.graphics.toArgb
import androidx.core.net.toUri
import com.deepeye.musicpro.data.OAuthConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for Chrome Custom Tabs integration.
 * Provides seamless OAuth login flow without leaving the app.
 */
@Singleton
class ChromeCustomTabsHelper @Inject constructor() {
    
    /**
     * Create Chrome Custom Tabs intent for OAuth login.
     * Uses the device code flow with automatic redirect handling.
     */
    fun createCustomTabsIntent(context: Context, deviceCode: String): CustomTabsIntent {
        val builder = CustomTabsIntent.Builder()
        
        // Set toolbar color to match app theme
        builder.setToolbarColor(com.deepeye.musicpro.ui.theme.DeepEyePrimary.toArgb())
        
        // Enable instant apps
        builder.setInstantAppsEnabled(true)
        
        // Show title in toolbar
        builder.setShowTitle(true)
        
        // Add share button
        builder.addDefaultShareMenuItem()
        
        return builder.build()
    }
    
    /**
     * Launch OAuth login in Chrome Custom Tabs.
     * Opens the verification URL with user code pre-filled.
     */
    fun launchOAuthLogin(context: Context, deviceCode: String) {
        val verificationUrl = "${OAuthConfig.VERIFICATION_URL}?user_code=${deviceCode}"
        val customTabsIntent = createCustomTabsIntent(context, deviceCode)
        
        // Try to open in Chrome first, fallback to default browser
        try {
            val chromeIntent = Intent().apply {
                setPackage(OAuthConfig.CUSTOM_TABS_PACKAGE)
                action = Intent.ACTION_VIEW
                data = verificationUrl.toUri()
            }
            context.startActivity(chromeIntent)
        } catch (e: Exception) {
            // Chrome not available, use default browser
            customTabsIntent.launchUrl(context, verificationUrl.toUri())
        }
    }
    
    /**
     * Check if Chrome Custom Tabs is available on the device.
     */
    fun isChromeCustomTabsAvailable(context: Context): Boolean {
        return try {
            context.packageManager.resolveService(
                Intent("android.support.customtabs.action.CustomTabsService"),
                0
            ) != null
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Warm up Custom Tabs connection for faster loading.
     * Call this when user is likely to login (e.g., on login screen load).
     */
    fun warmupCustomTabs(context: Context) {
        if (!isChromeCustomTabsAvailable(context)) return
        
        try {
            val serviceIntent = Intent("android.support.customtabs.action.CustomTabsService").apply {
                setPackage(OAuthConfig.CUSTOM_TABS_PACKAGE)
            }
            context.bindService(serviceIntent, object : android.content.ServiceConnection {
                override fun onServiceConnected(name: android.content.ComponentName?, service: android.os.IBinder?) {
                    // Custom Tabs service connected, ready for fast loading
                }
                
                override fun onServiceDisconnected(name: android.content.ComponentName?) {
                    // Service disconnected
                }
            }, android.content.Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            // Ignore binding errors
        }
    }
}
