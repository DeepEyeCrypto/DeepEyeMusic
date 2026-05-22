// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * DeepEyeMusicPro Application class.
 * Entry point for Hilt dependency injection.
 */
@HiltAndroidApp
class DeepEyeApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
