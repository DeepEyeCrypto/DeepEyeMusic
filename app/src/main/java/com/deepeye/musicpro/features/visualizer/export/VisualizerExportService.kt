// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.features.visualizer.export

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

/**
 * VisualizerExportService — Foreground service for surface recording visualizer frames to MP4.
 */
class VisualizerExportService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("VisualizerExport", "Export service started")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d("VisualizerExport", "Export service stopped")
    }
}
