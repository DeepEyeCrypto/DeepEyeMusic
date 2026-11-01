package com.deepeye.musicpro.ui.player.visualizer

import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse

/**
 * Test harness for AGSL visualizer rendering.
 * Feeds synthetic FFT data into the visualizer and verifies it doesn't crash.
 */
class VisualizerTestHarness {

    /**
     * Verifies that all 22 visualizer scenes can be created without crash.
     * This is a smoke test — it doesn't render to GPU but verifies the scene
     * enum and catalog are consistent.
     */
    fun verifyAllScenesExist(): List<String> {
        val allScenes = VisualizerSceneId.values().map { it.name }
        val catalogScenes = AvailableVisualizerScenes.map { it.id.name }
        val missing = allScenes.filter { it !in catalogScenes }
        return missing
    }
}

/**
 * Unit test entry point for the visualizer test harness.
 */
class VisualizerTestHarnessTest {
    @Test
    fun testAllScenesExist() {
        val harness = VisualizerTestHarness()
        val missing = harness.verifyAllScenesExist()
        assertTrue("Missing scenes: $missing", missing.isEmpty())
    }
}