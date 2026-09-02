// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.personalization

import com.deepeye.musicpro.domain.model.personalization.PersonalizedFeedItem
import com.deepeye.musicpro.domain.model.personalization.PersonalizedItemType
import com.deepeye.musicpro.domain.model.personalization.PersonalizedSection
import com.deepeye.musicpro.domain.model.personalization.PersonalizedSectionType
import com.deepeye.musicpro.domain.personalization.DiversityRanker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiversityRankerTest {

    private fun item(id: String, artist: String) = PersonalizedFeedItem(
        id = id,
        title = "Track $id",
        artist = artist,
        itemType = PersonalizedItemType.SONG,
    )

    private fun section(items: List<PersonalizedFeedItem>) = PersonalizedSection(
        id = "sec_test",
        type = PersonalizedSectionType.BASED_ON_LISTENING,
        title = "Test",
        sourceLabel = "Local",
        items = items,
    )

    @Test
    fun `caps repeated artist at the top of the section`() {
        val items = listOf(
            item("a1", "Artist A"),
            item("a2", "Artist A"),
            item("a3", "Artist A"),
            item("b1", "Artist B"),
        )
        val result = DiversityRanker.diversify(section(items), maxRepeatedArtistPerSection = 2).items

        // The first `maxRepeated` positions may belong to Artist A, but the extra
        // Artist A item is pushed toward the end so no single artist dominates the top.
        val topSegment = result.take(2)
        assertTrue(topSegment.count { it.artist == "Artist A" } <= 2)
        // The overflowing Artist A item is moved out of the leading run.
        assertEquals("a3", result.last().id)
        // All items are retained (the ranker redistributes, never discards).
        assertEquals(4, result.size)
    }

    @Test
    fun `keeps order within allowed artist cap`() {
        val items = listOf(
            item("a1", "Artist A"),
            item("b1", "Artist B"),
            item("a2", "Artist A"),
        )
        val result = DiversityRanker.diversify(section(items), maxRepeatedArtistPerSection = 2).items
        assertEquals(listOf("a1", "b1", "a2"), result.map { it.id })
        assertEquals(true, result.size == 3)
    }

    @Test
    fun `moves currently playing item away from first position`() {
        val items = listOf(
            item("now", "Artist A"),
            item("other1", "Artist B"),
            item("other2", "Artist C"),
        )
        val result = DiversityRanker.diversify(section(items), currentPlayingItemId = "now").items

        assertNotEquals("now", result.first().id)
        assertTrue(result.any { it.id == "now" })
        // The currently playing item is inserted just after the original first item.
        assertEquals("other1", result[0].id)
        assertEquals("now", result[1].id)
    }

    @Test
    fun `skipped items are pushed toward the end`() {
        val items = listOf(
            item("s1", "Artist A"),
            item("kept1", "Artist B"),
            item("s2", "Artist C"),
            item("kept2", "Artist D"),
        )
        val result = DiversityRanker.diversify(
            section(items),
            recentlySkippedItemIds = setOf("s1", "s2"),
        ).items

        val kept = result.take(2).map { it.id }
        val skipped = result.takeLast(2).map { it.id }
        assertEquals(setOf("kept1", "kept2"), kept.toSet())
        assertEquals(setOf("s1", "s2"), skipped.toSet())
    }

    @Test
    fun `empty section returns unchanged`() {
        val empty = section(emptyList())
        assertEquals(empty, DiversityRanker.diversify(empty))
    }
}
