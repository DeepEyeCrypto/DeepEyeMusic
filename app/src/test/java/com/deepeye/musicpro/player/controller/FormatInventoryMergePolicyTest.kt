package com.deepeye.musicpro.player.controller

import com.deepeye.musicpro.player.format.DeepEyeFormat
import com.deepeye.musicpro.player.format.FormatType
import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FormatInventoryMergePolicyTest {
    private lateinit var policy: FormatInventoryMergePolicy

    @Before
    fun setup() {
        policy = FormatInventoryMergePolicy()
    }

    private fun aV() = DeepEyeFormat(id = "auto_v", groupIndex = 0, trackIndex = 0, type = FormatType.VIDEO,
        mimeType = "video/avc", codecName = "Auto", isAuto = true)
    private fun aA() = DeepEyeFormat(id = "auto_a", groupIndex = 0, trackIndex = 0, type = FormatType.AUDIO,
        mimeType = "audio/opus", codecName = "Auto", isAuto = true)
    private fun v(id: String, h: Int, c: String) = DeepEyeFormat(
        id = id, groupIndex = 0, trackIndex = 0, type = FormatType.VIDEO, mimeType = "video/$c",
        codecName = c, height = h, qualityLabel = "${h}p", isAuto = false)
    private fun a(id: String, c: String) = DeepEyeFormat(
        id = id, groupIndex = 0, trackIndex = 0, type = FormatType.AUDIO, mimeType = "audio/$c",
        codecName = c, isAuto = false)

    private fun richV(n: Int = 20) = (listOf(aV()) + (0 until n).map { i ->
        v(id = "v$i", h = 480 + (i % 5) * 360, c = if (i % 3 == 0) "avc" else if (i % 3 == 1) "vp9" else "av1")
    }).toImmutableList()

    private fun richA(n: Int = 13) = (listOf(aA()) + (0 until n).map { i ->
        a(id = "a$i", c = if (i % 2 == 0) "opus" else "aac")
    }).toImmutableList()

    private fun sparseV() = listOf(aV(), v(id = "s_v", h = 720, c = "avc")).toImmutableList()
    private fun sparseA() = listOf(aA(), a(id = "s_a", c = "aac")).toImmutableList()

    @Test fun test1_keepRichSmartTubeWhenMedia3Sparse() {
        val r = policy.merge(richV(20), richA(13), sparseV(), sparseA())
        assertTrue(r.preserveExistingVideoInventory); assertTrue(r.preserveExistingAudioInventory)
        assertEquals(21, r.videoFormats.size); assertEquals(14, r.audioFormats.size)
        assertEquals("existing 20 real video rows", 20, r.existingRealVideo)
        assertEquals("existing 13 real audio rows", 13, r.existingRealAudio)
        assertEquals("incoming 1 real video row", 1, r.incomingRealVideo)
        assertEquals("incoming 1 real audio row", 1, r.incomingRealAudio)
    }

    @Test fun test2_adoptRicherMedia3() {
        val inV = (listOf(aV()) + (0..4).map { v(id = "in_v$it", h = 480 + it * 180, c = "avc") }).toImmutableList()
        val inA = (listOf(aA()) + (0..2).map { a(id = "in_a$it", c = if (it % 2 == 0) "opus" else "aac") }).toImmutableList()
        val r = policy.merge(listOf(aV()).toImmutableList(), listOf(aA()).toImmutableList(), inV, inA)
        assertEquals(6, r.videoFormats.size); assertEquals(4, r.audioFormats.size)
    }

    @Test fun test3_preserveVideoAdoptAudio() {
        val inA = (listOf(aA()) + (0..4).map { a(id = "rich_a$it", c = if (it % 2 == 0) "opus" else "aac") }).toImmutableList()
        val r = policy.merge(richV(20), listOf(aA(), a(id = "weak", c = "aac")).toImmutableList(), sparseV(), inA)
        assertTrue(r.preserveExistingVideoInventory); assertFalse(r.preserveExistingAudioInventory)
    }

    @Test fun test4_preserveAudioAdoptVideo() {
        val inV = (listOf(aV()) + (0..4).map { v(id = "rich_v$it", h = 480 + it * 200, c = "vp9") }).toImmutableList()
        val r = policy.merge(listOf(aV(), v(id = "weak", h = 480, c = "avc")).toImmutableList(), richA(13), inV, sparseA())
        assertFalse(r.preserveExistingVideoInventory); assertTrue(r.preserveExistingAudioInventory)
    }

    @Test fun test5_autoNotCounted() {
        val r = policy.merge(listOf(aV()).toImmutableList(), listOf(aA()).toImmutableList(),
            listOf(aV(), v(id = "one", h = 720, c = "avc")).toImmutableList(),
            listOf(aA(), a(id = "one", c = "opus")).toImmutableList())
        // Auto rows are never counted as real formats:
        assertEquals(1, r.incomingRealVideo); assertEquals(1, r.incomingRealAudio)
        assertEquals(0, r.existingRealVideo); assertEquals(0, r.existingRealAudio)
        // A singleton incoming (<=1 real) is NOT adopted into the catalog: the fallback path
        // keeps the existing list (auto row only) — identical to pre-policy controller logic.
        assertEquals(1, r.videoFormats.size); assertEquals(1, r.videoFormats.count { it.isAuto })
        assertEquals(1, r.audioFormats.size); assertEquals(1, r.audioFormats.count { it.isAuto })
    }

    @Test fun test6_noDuplicateAuto() {
        val eV = (listOf(aV()) + (0..2).map { v(id = "ex$it", h = 480 + it * 200, c = "avc") }).toImmutableList()
        val inV = (listOf(aV()) + (0..4).map { v(id = "in$it", h = 720 + it * 100, c = "vp9") }).toImmutableList()
        val r = policy.merge(eV, listOf(aA()).toImmutableList(), inV, listOf(aA()).toImmutableList())
        assertEquals(1, r.videoFormats.count { it.isAuto })
    }

    @Test fun test7_variantsPreserved() {
        val sV = listOf(aV(), v(id = "720_avc", h = 720, c = "avc"), v(id = "720_vp9", h = 720, c = "vp9"),
            v(id = "1080_av1", h = 1080, c = "av1")).toImmutableList()
        val r = policy.merge(sV, richA(2), sparseV(), sparseA())
        assertTrue(r.videoFormats.any { it.id == "720_vp9" }); assertTrue(r.videoFormats.any { it.id == "1080_av1" })
    }

    @Test fun test8_noStaleInventory() {
        val r = policy.merge(listOf(aV()).toImmutableList(), listOf(aA()).toImmutableList(),
            listOf(aV()).toImmutableList(), listOf(aA()).toImmutableList())
        assertEquals(1, r.videoFormats.size); assertEquals(1, r.audioFormats.size)
        assertEquals(0, r.existingRealVideo); assertEquals(0, r.existingRealAudio)
    }

    @Test fun test9_recoveryDoesntBlank() {
        val r = policy.merge(richV(20), richA(13), sparseV(), sparseA())
        assertEquals(21, r.videoFormats.size); assertEquals(14, r.audioFormats.size)
        assertFalse(r.videoFormats.isEmpty()); assertFalse(r.audioFormats.isEmpty())
        assertEquals(20, r.existingRealVideo); assertEquals(13, r.existingRealAudio)
        assertEquals(1, r.incomingRealVideo); assertEquals(1, r.incomingRealAudio)
    }

    @Test fun test10_manualSelectionSurvives() {
        val sV = (listOf(aV()) + (0..4).map { i ->
            val f = v(id = "v$i", h = 480 + i * 180, c = if (i == 2) "vp9" else "avc")
            if (i == 2) f.copy(isSelected = true) else f
        }).toImmutableList()
        val r = policy.merge(sV, richA(5), sparseV(), sparseA())
        assertTrue(r.videoFormats.any { it.id == "v2" && it.isSelected }); assertFalse(r.videoFormats.any { it.id == "s_v" })
    }
}
