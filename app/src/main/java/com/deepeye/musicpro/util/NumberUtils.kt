package com.deepeye.musicpro.util

fun formatCompactNumber(number: Long): String {
    if (number < 1000) return number.toString()
    val exp = (Math.log(number.toDouble()) / Math.log(1000.0)).toInt()
    val format = java.text.DecimalFormat("0.#")
    val value = number / Math.pow(1000.0, exp.toDouble())
    return format.format(value) + "kMGTPE"[exp - 1]
}

fun formatUploadDate(dateStr: String): String {
    if (dateStr.isBlank()) return ""
    if (dateStr.contains("ago", ignoreCase = true) || dateStr.contains("yesterday", ignoreCase = true) || dateStr.contains("today", ignoreCase = true)) {
        return dateStr
    }
    return try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val parsedInstant = try {
                java.time.OffsetDateTime.parse(dateStr).toInstant()
            } catch (e: Exception) {
                try {
                    java.time.LocalDate.parse(dateStr).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()
                } catch (e2: Exception) {
                    null
                }
            }
            if (parsedInstant != null) {
                val now = java.time.Instant.now()
                val durationSeconds = java.time.Duration.between(parsedInstant, now).seconds
                when {
                    durationSeconds < 60 -> "Just now"
                    durationSeconds < 3600 -> "${durationSeconds / 60}m ago"
                    durationSeconds < 86400 -> "${durationSeconds / 3600}h ago"
                    durationSeconds < 86400 * 7 -> "${durationSeconds / 86400}d ago"
                    durationSeconds < 86400 * 30 -> "${durationSeconds / (86400 * 7)}w ago"
                    durationSeconds < 86400 * 365 -> "${durationSeconds / (86400 * 30)}mo ago"
                    else -> "${durationSeconds / (86400 * 365)}y ago"
                }
            } else {
                dateStr
            }
        } else {
            dateStr
        }
    } catch (e: Exception) {
        dateStr
    }
}
