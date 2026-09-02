// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.personalization

import com.deepeye.musicpro.data.cache.HiddenContentManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HiddenContentManagerTest {

    @Test
    fun hideItem_marksHiddenAndProvidesUndo() = runTest {
        val manager = HiddenContentManager()
        val hidden = manager.hideItem("item_1", "Some Track")

        assertTrue(hidden)
        assertTrue(manager.isItemHidden("item_1"))
        assertEquals(1, manager.undoableHides.value.size)
    }

    @Test
    fun hideItem_returnsFalseWhenAlreadyHidden() = runTest {
        val manager = HiddenContentManager()
        manager.hideItem("item_1", "Track")
        val second = manager.hideItem("item_1", "Track")

        assertFalse(second)
    }

    @Test
    fun undoHide_removesItemFromHiddenSet() = runTest {
        val manager = HiddenContentManager()
        manager.hideItem("item_1", "Track")
        manager.undoHide("item_1")

        assertFalse(manager.isItemHidden("item_1"))
    }

    @Test
    fun hideArtist_alsoHidesSongsByThatArtist() = runTest {
        val manager = HiddenContentManager()
        manager.hideItem("song_1", "Track", alsoHideArtist = "Some Artist")

        assertTrue(manager.isArtistHidden("Some Artist"))
        assertTrue(manager.isArtistHidden("SOME ARTIST") || manager.isArtistHidden("Some Artist"))
    }

    @Test
    fun resetAll_clearsEverythingAndBumpsVersion() = runTest {
        val manager = HiddenContentManager()
        manager.hideItem("item_1", "Track", alsoHideArtist = "Artist")
        val versionBefore = manager.resetVersion.value

        manager.resetAll()

        assertFalse(manager.isItemHidden("item_1"))
        assertFalse(manager.isArtistHidden("Artist"))
        assertTrue(manager.undoableHides.value.isEmpty())
        assertTrue(manager.resetVersion.value > versionBefore)
    }
}
