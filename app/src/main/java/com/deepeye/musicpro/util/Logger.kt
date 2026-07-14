// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.util

import android.util.Log

object Logger {
    private const val GLOBAL_TAG = "DeepEyeMusic"

    enum class Category(val tag: String) {
        PLAYER("Player"),
        HOME_FEED("HomeFeed"),
        AUTOPLAY("Autoplay"),
        SYNC("Sync"),
        UI("UI")
    }

    fun d(category: Category, message: String) {
        Log.d("${GLOBAL_TAG}_${category.tag}", message)
    }

    fun e(category: Category, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e("${GLOBAL_TAG}_${category.tag}", message, throwable)
        } else {
            Log.e("${GLOBAL_TAG}_${category.tag}", message)
        }
    }

    fun i(category: Category, message: String) {
        Log.i("${GLOBAL_TAG}_${category.tag}", message)
    }
}
