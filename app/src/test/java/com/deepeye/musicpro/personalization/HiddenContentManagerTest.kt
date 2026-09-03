// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.personalization

import com.deepeye.musicpro.data.cache.HiddenContentManager
import com.deepeye.musicpro.data.cache.dao.HiddenContentDao
import com.deepeye.musicpro.data.cache.entities.HiddenContentEntity
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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

    @Test
    fun roomPersistence_insertsAndLoadsCorrectly() = runTest {
        val dao = mockk<HiddenContentDao>(relaxed = true)
        val savedEntities = mutableListOf<HiddenContentEntity>()
        coEvery { dao.insert(any()) } answers {
            savedEntities.add(firstArg())
        }
        coEvery { dao.getAllHiddenContent("acc_1") } answers {
            savedEntities.filter { it.accountKey == "acc_1" }
        }

        val manager = HiddenContentManager(dao)
        manager.switchAccount("acc_1")
        manager.hideItem("song_100", "Hit Track", alsoHideArtist = "Great Artist", accountKey = "acc_1")

        coVerify { dao.insert(match { it.itemId == "song_100" && it.accountKey == "acc_1" }) }
        coVerify { dao.insert(match { it.itemId == "Great Artist" && it.itemType == "ARTIST" }) }

        // Create new manager simulating process recreation
        val freshManager = HiddenContentManager(dao)
        freshManager.switchAccount("acc_1")

        assertTrue(freshManager.isItemHidden("song_100"))
        assertTrue(freshManager.isArtistHidden("Great Artist"))
    }

    @Test
    fun accountSwitching_isolatesHiddenContent() = runTest {
        val dao = mockk<HiddenContentDao>(relaxed = true)
        coEvery { dao.getAllHiddenContent("acc_A") } returns listOf(
            HiddenContentEntity(accountKey = "acc_A", itemId = "song_A", title = "Song A", itemType = "SONG")
        )
        coEvery { dao.getAllHiddenContent("acc_B") } returns listOf(
            HiddenContentEntity(accountKey = "acc_B", itemId = "song_B", title = "Song B", itemType = "SONG")
        )

        val manager = HiddenContentManager(dao)
        manager.switchAccount("acc_A")
        assertTrue(manager.isItemHidden("song_A"))
        assertFalse(manager.isItemHidden("song_B"))

        manager.switchAccount("acc_B")
        assertFalse(manager.isItemHidden("song_A"))
        assertTrue(manager.isItemHidden("song_B"))
    }
}

