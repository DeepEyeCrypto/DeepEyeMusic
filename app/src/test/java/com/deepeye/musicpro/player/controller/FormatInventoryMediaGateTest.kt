package com.deepeye.musicpro.player.controller

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Media-identity gate tests: verify that the stale-inventory reset contract (test-8
 * requirement: item A's rich SmartTube catalog must NEVER be shown as item B's inventory)
 * is enforced across genuine media transitions while same-media continuation is preserved.
 *
 * The gate is the first decision point; [FormatInventoryMergePolicy] runs AFTER the reset
 * and layer-tests in this file compose both to prove end-to-end behavior.
 */
class FormatInventoryMediaGateTest {
    private lateinit var gate: FormatInventoryMediaGate

    @Before
    fun setup() {
        gate = FormatInventoryMediaGate()
    }

    @Test fun test1_mediaTransitionFromAtoBForcesReset() {
        val keyA = gate.materializeMediaKey(mediaId = "media_A", positionMs = 0L)
        val keyB = gate.materializeMediaKey(mediaId = "media_B", positionMs = 0L)
        assertNotEquals(keyA, keyB)
        assertTrue("item A -> item B runtime transition must force a stale-inventory reset", gate.shouldReset(keyA, keyB))
    }

    @Test fun test2_sameMediaContinuationDoesNotReset() {
        val near = gate.materializeMediaKey(mediaId = "media_A", positionMs = 8_000L)
        val continued = gate.materializeMediaKey(mediaId = "media_A", positionMs = 27_000L) // same 30s bucket
        assertEquals(near, continued)
        assertFalse("same-media continuation (same 30s position bucket) must never reset", gate.shouldReset(near, continued))
    }

    @Test fun test3_recoveryRestoreOfSameMediaDoesNotReset() {
        // Recovery restores position within a 30s tolerance window; media ID is unchanged.
        val stored = gate.materializeMediaKey(mediaId = "media_A", positionMs = 61_000L)
        val restored = gate.materializeMediaKey(mediaId = "media_A", positionMs = 74_000L)
        assertFalse("same-media recovery restore must not reset the restored catalog", gate.shouldReset(stored, restored))
    }

    @Test fun test4_nullOrBlankKeysAreFailSafe() {
        assertFalse(gate.shouldReset(null, "media_B"))
        assertFalse(gate.shouldReset("media_A", null))
        assertFalse(gate.shouldReset("", "media_B"))
        assertFalse(gate.shouldReset("media_A", "  "))
    }

    @Test fun test5_staleCatalogResetThenMergeNeverLeaksItemAtoItemB() {
        val policy = FormatInventoryMergePolicy()

        // Controller flow on a genuine media transition:
        // (1) gate detects item A -> item B (sparse runtime), forces reset,
        // (2) merge policy runs with RESET (empty) existing + B's incoming sparse tracks.
        val keyA = gate.materializeMediaKey(mediaId = "media_A", positionMs = 12_000L)
        val keyB = gate.materializeMediaKey(mediaId = "media_B", positionMs = 12_000L)
        assertTrue(gate.shouldReset(keyA, keyB))

        val emptyV: ImmutableList<com.deepeye.musicpro.player.format.DeepEyeFormat> = persistentListOf()
        val emptyA: ImmutableList<com.deepeye.musicpro.player.format.DeepEyeFormat> = persistentListOf()

        val r = policy.merge(
            existingVideo = emptyV,
            existingAudio = emptyA,
            incomingVideo = sparseVideoList(),
            incomingAudio = sparseAudioList(),
        )

        assertTrue("item A video rows (v*) must never leak into item B",
            r.videoFormats.none { !it.isAuto && it.id.startsWith("v") })
        assertTrue("item A audio rows (a*) must never leak into item B",
            r.audioFormats.none { !it.isAuto && it.id.startsWith("a") })
        // After a genuine transition reset, the catalog is a clean/loading state: the policy
        // keeps the (empty) existing list when incoming is still sparse (<=1 real), so item B
        // shows NO formats until its resolver catalog arrives — never item A's stale rows.
        assertEquals("item B shows a clean/loading catalog, not item A's 20/13 rows",
            0, r.videoFormats.size)
        assertEquals("item B shows a clean/loading catalog, not item A's 20/13 rows",
            0, r.audioFormats.size)
        assertEquals("stale-reset must surface as existingReal=0 for observability",
            0, r.existingRealVideo + r.existingRealAudio)
    }

    @Test fun test6_sameMediaTransitionKeepsRichCatalog() {
        val policy = FormatInventoryMergePolicy()
        val key = gate.materializeMediaKey(mediaId = "media_A", positionMs = 5_000L)
        assertFalse("same-media re-prepare must keep the rich catalog", gate.shouldReset(key, key))

        val r = policy.merge(richVideoList(20), richAudioList(13), sparseVideoList(), sparseAudioList())
        assertTrue(r.preserveExistingVideoInventory); assertTrue(r.preserveExistingAudioInventory)
        assertEquals(21, r.videoFormats.size)
        assertEquals(14, r.audioFormats.size)
    }
}