package com.deepeye.musicpro.updates

/**
 * Represents one release entry in the changelog.
 */
data class ChangelogEntry(
    val versionCode: Int,
    val versionName: String,
    val releaseDate: String = "",
    val title: String,
    val items: List<String>,
    val highlight: Boolean = false,
)

/**
 * UI state for the changelog dialog system.
 */
data class UpdateState(
    val currentVersionCode: Int = 0,
    val lastShownVersionCode: Int = 0,
    val shouldShowChangelog: Boolean = false,
    val changelogEntries: List<ChangelogEntry> = emptyList(),
)
