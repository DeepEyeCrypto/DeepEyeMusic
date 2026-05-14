package com.deepeye.musicpro.core.utils

/**
 * Formats time durations for display in the music player.
 * Pure Kotlin — zero Android dependencies.
 */
object TimeFormatter {

    /**
     * Formats milliseconds to "mm:ss" or "h:mm:ss" format.
     */
    fun formatDuration(durationMs: Long): String {
        if (durationMs < 0) return "0:00"

        val totalSeconds = durationMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }

    /**
     * Formats total duration for album/playlist summaries.
     * e.g., "1h 23m", "45m", "3m 15s"
     */
    fun formatTotalDuration(durationMs: Long): String {
        if (durationMs < 0) return "0s"

        val totalSeconds = durationMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return buildString {
            if (hours > 0) append("${hours}h ")
            if (minutes > 0) append("${minutes}m ")
            if (hours == 0L && minutes == 0L) append("${seconds}s")
        }.trim()
    }
}
