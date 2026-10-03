// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.domain.personalization

import com.deepeye.musicpro.domain.model.personalization.PersonalizedFeedItem
import com.deepeye.musicpro.domain.model.personalization.PersonalizedSection

/**
 * Applies diversity-aware ranking within personalized sections so the same artist
 * doesn't dominate a row.  The logic runs locally after sections are built and
 * never touches remote data.
 */
object DiversityRanker {

    /**
     * Reranks items inside [section] to respect the user's configured limits while
     * preserving overall relevance ordering as much as possible.
     *
     * @param section                     the section to diversify.
     * @param maxRepeatedArtistPerSection hard cap on how many times one artist may appear.
     * @param currentPlayingItemId        if set, the currently playing item is moved away from
     *                                    index 0 to reduce immediate repeats.
     * @param recentlySkippedItemIds      items skipped in the last few minutes are penalized
     *                                    by being shifted toward the end.
     */
    fun diversify(
        section: PersonalizedSection,
        maxRepeatedArtistPerSection: Int = 2,
        currentPlayingItemId: String? = null,
        recentlySkippedItemIds: Set<String> = emptySet(),
    ): PersonalizedSection {
        if (section.items.isEmpty()) return section

        val maxRepeated = maxRepeatedArtistPerSection.coerceAtLeast(1)

        // Split into artist-frequency buckets, maintaining relative insertion order
        val artistCounts = mutableMapOf<String, Int>()
        val result = mutableListOf<PersonalizedFeedItem>()
        val overflow = mutableListOf<PersonalizedFeedItem>()

        for (item in section.items) {
            val artistKey = item.artist.lowercase()
            val count = artistCounts.getOrDefault(artistKey, 0)
            if (count < maxRepeated) {
                result.add(item)
                artistCounts[artistKey] = count + 1
            } else {
                overflow.add(item)
            }
        }

        // Append overflow items at end of section
        result.addAll(overflow)

        // Penalize recently skipped items: push toward the tail
        if (recentlySkippedItemIds.isNotEmpty()) {
            val (skipped, kept) = result.partition { it.id in recentlySkippedItemIds }
            result.clear()
            result.addAll(kept)
            result.addAll(skipped)
        }

        // Move currently playing item away from position 0 to reduce immediate repeats
        if (currentPlayingItemId != null) {
            val cpIndex = result.indexOfFirst { it.id == currentPlayingItemId }
            if (cpIndex == 0 && result.size > 1) {
                val item = result.removeAt(0)
                // Insert after the first non-skipped item
                val insertAt = if (recentlySkippedItemIds.isEmpty()) 1 else {
                    val firstKept = result.indexOfFirst { it.id !in recentlySkippedItemIds }
                    if (firstKept == -1) 1 else (firstKept + 1).coerceAtMost(result.size)
                }
                result.add(insertAt, item)
            }
        }

        return section.copy(items = result)
    }
}