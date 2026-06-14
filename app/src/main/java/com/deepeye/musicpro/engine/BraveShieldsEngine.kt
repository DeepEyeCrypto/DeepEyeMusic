package com.deepeye.musicpro.engine

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

/**
 * DeepEye Music Pro - Brave Shields Auto-Update Engine
 * Automatically syncs with Brave's official GitHub adblock repositories
 * for 0-latency tracking protection and cosmetic ad blocking.
 */
object BraveShieldsEngine {
    private const val TAG = "BraveShieldsEngine"
    private const val PREFS_NAME = "brave_shields_engine"
    private const val KEY_NETWORK_LIST = "network_blocklist"
    private const val KEY_COSMETIC_LIST = "cosmetic_selectors"
    private const val KEY_LAST_SYNC = "last_sync_time"
    private const val SYNC_INTERVAL_MS = 24 * 60 * 60 * 1000L // 24 hours

    // Fallback Hardcoded High-Performance Blocklist
    private val DEFAULT_NETWORK_BLOCKLIST = listOf(
        "doubleclick.net", "googleadservices.com", "google-analytics.com",
        "/api/stats/ads", "/pagead/", "youtube.com/ptracking", "youtube.com/error_204",
        "play.google.com/log", "adunit", "googleads", "adservice", "innovid.com",
        "flashtalking.com", "serving-sys.com"
    )

    // Fallback Hardcoded Cosmetic Selectors
    private val DEFAULT_COSMETIC_SELECTORS = listOf(
        ".ytp-ad-module", ".video-ads", ".ytp-ad-image-overlay", ".ytp-ad-overlay-container",
        ".ytp-ad-overlay-slot", ".ad-container", ".ad-display", ".display-ad-container",
        "#player-ads", ".ad-div", ".main-ad-layout", ".ytp-ad-player-overlay"
    )

    var activeNetworkBlocklist: List<String> = DEFAULT_NETWORK_BLOCKLIST
        private set

    var activeCosmeticSelectors: List<String> = DEFAULT_COSMETIC_SELECTORS
        private set

    // Brave Official Repositories
    private val BRAVE_NETWORK_REPOS = listOf(
        "https://raw.githubusercontent.com/brave/adblock-lists/master/brave-lists/brave-specific.txt",
        "https://raw.githubusercontent.com/brave/adblock-lists/master/brave-lists/brave-android-specific.txt"
    )

    private val BRAVE_COSMETIC_REPOS = listOf(
        "https://raw.githubusercontent.com/brave/adblock-lists/master/brave-lists/yt-distracting.txt",
        "https://raw.githubusercontent.com/brave/adblock-lists/master/brave-lists/yt-shorts.txt"
    )

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // Load instantaneously for 0-latency startup
        val cachedNetwork = prefs.getString(KEY_NETWORK_LIST, null)
        if (cachedNetwork != null) {
            activeNetworkBlocklist = cachedNetwork.split(",").filter { it.isNotBlank() }
        }

        val cachedCosmetic = prefs.getString(KEY_COSMETIC_LIST, null)
        if (cachedCosmetic != null) {
            activeCosmeticSelectors = cachedCosmetic.split("|,|").filter { it.isNotBlank() }
        }

        // Trigger background sync if needed
        val lastSync = prefs.getLong(KEY_LAST_SYNC, 0L)
        if (System.currentTimeMillis() - lastSync > SYNC_INTERVAL_MS) {
            CoroutineScope(Dispatchers.IO).launch {
                syncWithBraveUpstream(context)
            }
        }
    }

    private fun syncWithBraveUpstream(context: Context) {
        Log.i(TAG, "Starting background sync with Brave Upstream Repos...")
        try {
            val newNetworkRules = mutableSetOf<String>().apply { addAll(DEFAULT_NETWORK_BLOCKLIST) }
            val newCosmeticRules = mutableSetOf<String>().apply { addAll(DEFAULT_COSMETIC_SELECTORS) }

            // Sync Network Blocklists (parsing ABP syntax ||domain^)
            BRAVE_NETWORK_REPOS.forEach { url ->
                val lines = fetchUrlLines(url)
                lines.forEach { line ->
                    if (line.isNotBlank() && !line.startsWith("!") && line.startsWith("||") && line.contains("^")) {
                        val domain = line.substringAfter("||").substringBefore("^")
                        if (domain.isNotBlank() && !domain.contains("*") && !domain.contains("[")) {
                            newNetworkRules.add(domain)
                        }
                    }
                }
            }

            // Sync Cosmetic Filters (parsing ##selector syntax)
            BRAVE_COSMETIC_REPOS.forEach { url ->
                val lines = fetchUrlLines(url)
                lines.forEach { line ->
                    if (line.isNotBlank() && !line.startsWith("!") && line.contains("##")) {
                        val selector = line.substringAfter("##")
                        if (selector.isNotBlank() && !selector.contains("+js(")) {
                            newCosmeticRules.add(selector)
                        }
                    }
                }
            }

            // Save and apply if changes found
            if (newNetworkRules.size > DEFAULT_NETWORK_BLOCKLIST.size || newCosmeticRules.size > DEFAULT_COSMETIC_SELECTORS.size) {
                activeNetworkBlocklist = newNetworkRules.toList()
                activeCosmeticSelectors = newCosmeticRules.toList()

                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
                    putString(KEY_NETWORK_LIST, activeNetworkBlocklist.joinToString(","))
                    putString(KEY_COSMETIC_LIST, activeCosmeticSelectors.joinToString("|,|"))
                    putLong(KEY_LAST_SYNC, System.currentTimeMillis())
                    apply()
                }
                Log.i(TAG, "Brave Upstream Sync Complete! Network Rules: ${activeNetworkBlocklist.size}, Cosmetic Rules: ${activeCosmeticSelectors.size}")
            } else {
                // Just update sync time
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                    .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
                    .apply()
                Log.i(TAG, "Brave Upstream Sync Complete! No new rules found.")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync with Brave upstream: ${e.message}")
        }
    }

    private fun fetchUrlLines(urlString: String): List<String> {
        return try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

            if (connection.responseCode == 200) {
                connection.inputStream.bufferedReader().readLines()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun shouldBlockNetworkRequest(url: String): Boolean {
        // Fast optimized network check
        return activeNetworkBlocklist.any { url.contains(it) }
    }

    fun getCosmeticCssStyles(): String {
        return activeCosmeticSelectors.joinToString(",\n") { "    $it" } + " {\n        display: none !important;\n        opacity: 0 !important;\n        visibility: hidden !important;\n        pointer-events: none !important;\n    }\n"
    }
}
