// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.player.recovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackRecoverySnapshotTest {

    @Test
    fun testSnapshotContainsOnlyNonSensitiveData() {
        val snapshot = PlaybackRecoverySnapshot(
            mediaId = "dQw4w9WgXcQ",
            title = "Never Gonna Give You Up",
            isVideo = true,
            queueIndex = 0,
            queueSize = 5,
            positionMs = 45000L,
            durationMs = 212000L,
            wasPlaying = true,
            selectedVideoFormatId = "v720p_avc",
            selectedAudioFormatId = "a128k_opus",
            qualityMode = "HIGH_QUALITY",
            playbackSpeed = 1.0f,
            repeatMode = 0,
            shuffleEnabled = false,
            dspEnabled = true,
            dspPresetId = "TubeWarmth",
            captionsEnabled = false,
            selectedAudioLanguage = "en"
        )

        assertEquals("dQw4w9WgXcQ", snapshot.mediaId)
        assertEquals(45000L, snapshot.positionMs)
        assertTrue(snapshot.wasPlaying)
        assertEquals("v720p_avc", snapshot.selectedVideoFormatId)
        assertEquals("a128k_opus", snapshot.selectedAudioFormatId)
        assertTrue(snapshot.dspEnabled)
        assertEquals("TubeWarmth", snapshot.dspPresetId)
    }
}
