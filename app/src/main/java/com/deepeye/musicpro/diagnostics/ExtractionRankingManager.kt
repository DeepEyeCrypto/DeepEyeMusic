// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.diagnostics

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExtractionRankingManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "deepeye_extraction_stats"
        private const val TAG = "ExtractionRanking"
    }

    enum class Layer(val defaultPriority: Int) {
        NEWPIPE(10),
        ALT_EXTRACTOR(9),
        PIPED(8),
        INVIDIOUS(7),
        WEBVIEW_CAPTURE(6), // Disabled
        EMERGENCY_FALLBACK(5) // Disabled
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Records the success of a specific extraction layer.
     */
    fun recordSuccess(layer: Layer) {
        val successes = prefs.getInt("${layer.name}_success", 0) + 1
        prefs.edit().putInt("${layer.name}_success", successes).apply()
        Log.d(TAG, "Recorded success for $layer (Total successes: $successes)")
    }

    /**
     * Records the failure of a specific extraction layer.
     */
    fun recordFailure(layer: Layer) {
        val failures = prefs.getInt("${layer.name}_failure", 0) + 1
        prefs.edit().putInt("${layer.name}_failure", failures).apply()
        Log.d(TAG, "Recorded failure for $layer (Total failures: $failures)")
    }

    /**
     * Gets the success rate for a layer.
     */
    fun getSuccessRate(layer: Layer): Float {
        // Disabled layers have 0 success rate
        if (layer == Layer.EMERGENCY_FALLBACK) {
            return 0.0f
        }
        val successes = prefs.getInt("${layer.name}_success", 0)
        val failures = prefs.getInt("${layer.name}_failure", 0)
        val total = successes + failures
        if (total == 0) {
            // Return a default weight based on layer priority to bootstrap ranking
            return layer.defaultPriority.toFloat() / 10f
        }
        return successes.toFloat() / total.toFloat()
    }

    /**
     * Returns the list of active layers sorted dynamically by success rate (highest first).
     */
    fun getRankedLayers(): List<Layer> {
        return Layer.values()
            .filter { it != Layer.EMERGENCY_FALLBACK }
            .sortedByDescending { getSuccessRate(it) }
    }
}
