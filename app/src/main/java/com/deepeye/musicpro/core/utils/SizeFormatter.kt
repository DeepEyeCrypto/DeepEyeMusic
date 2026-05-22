// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.core.utils

import java.util.Locale

/**
 * Formats file sizes for display.
 * Pure Kotlin — zero Android dependencies.
 */
object SizeFormatter {

    private val UNITS = arrayOf("B", "KB", "MB", "GB", "TB")

    /**
     * Formats bytes to a human-readable size string.
     * e.g., 1536 → "1.5 KB", 1048576 → "1.0 MB"
     */
    fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"

        var value = bytes.toDouble()
        var unitIndex = 0

        while (value >= 1024 && unitIndex < UNITS.size - 1) {
            value /= 1024
            unitIndex++
        }

        return String.format(Locale.US, "%.1f %s", value, UNITS[unitIndex])
    }
}
