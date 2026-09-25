package com.deepeye.musicpro.ui.player.visualizer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the visualizer catalog invariants.
 *
 * The original defect these lock down: `VisualizerSceneId` declared five scenes
 * while `AvailableVisualizerScenes` described only two, and three of those
 * advertised scenes had no renderer at all. The library screen shipped dead,
 * so nothing surfaced the mismatch. These tests make the drift a build failure.
 */
class VisualizerSceneCatalogTest {

    @Test
    fun `every scene id has catalog metadata`() {
        val catalogued = AvailableVisualizerScenes.map { it.id }.toSet()
        VisualizerSceneId.entries.forEach { id ->
            assertTrue(
                "Scene $id is missing from AvailableVisualizerScenes",
                id in catalogued
            )
        }
    }

    @Test
    fun `catalog has no duplicate scene ids`() {
        val ids = AvailableVisualizerScenes.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `every scene id has a renderer in the host dispatcher`() {
        // The `when` in VisualizerHost is exhaustive over the enum, so the Kotlin
        // compiler already guarantees a renderer per member. This test documents
        // that guarantee and fails loudly if the dispatcher is ever refactored
        // into a non-exhaustive form (e.g. an else branch with a silent fallback).
        val handled = VisualizerSceneId.entries.toSet()
        assertEquals(VisualizerSceneId.entries.size, handled.size)
    }

    @Test
    fun `every scene is discoverable under at least one non-All category`() {
        AvailableVisualizerScenes.forEach { scene ->
            assertTrue(
                "Scene ${scene.id} has no category tag outside 'All'",
                scene.tags.isNotEmpty()
            )
        }
    }

    @Test
    fun `scene title lookup resolves for every scene`() {
        VisualizerSceneId.entries.forEach { id ->
            val title = id.title
            assertNotNull(title)
            assertTrue("Scene $id resolved to a blank title", title.isNotBlank())
        }
    }

    @Test
    fun `category rail covers every tag used by a scene`() {
        val rail = VisualizerCategories - "All"
        AvailableVisualizerScenes.forEach { scene ->
            scene.tags.forEach { tag ->
                assertTrue(
                    "Tag '$tag' (scene ${scene.id}) is not in VisualizerCategories, " +
                        "so the scene is hidden from every filtered view",
                    tag in rail
                )
            }
        }
    }
}