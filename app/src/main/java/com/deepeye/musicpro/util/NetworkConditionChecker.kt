// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.util

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NetworkConditionChecker — Single source of truth for metered-network detection.
 *
 * Wraps [ConnectivityManager.isActiveNetworkMetered] so callers (e.g. the autoplay
 * pre-fetch guard) can make data-saving decisions without re-implementing the
 * connectivity lookup. `isActiveNetworkMetered` returns true for cellular and for
 * metered/hotspot Wi-Fi, and false for unmetered Wi-Fi/ethernet.
 */
@Singleton
class NetworkConditionChecker
@Inject
constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val TAG = "NetworkConditionChecker"
    }

    private val connectivityManager: ConnectivityManager?
        get() = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    /**
     * True when the active network is metered (cellular / metered Wi-Fi) and pre-fetching
     * should be treated as consuming the user's mobile data.
     */
    fun isNetworkMetered(): Boolean {
        val cm = connectivityManager
        if (cm == null) {
            Log.w(TAG, "event=metered_check_failed reason=connectivity_manager_unavailable result=metered")
            // Fail closed: treat as metered so we never silently burn mobile data.
            return true
        }
        return try {
            cm.isActiveNetworkMetered
        } catch (e: Exception) {
            Log.e(TAG, "event=metered_check_failed reason=\"${e.message}\" result=metered", e)
            true
        }
    }
}