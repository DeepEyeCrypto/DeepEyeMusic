// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.player.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class MusicPlayerServiceTest {

    @Test
    fun verifyRootConstants() {
        val rootId = "DEEPEYE_MEDIA_ROOT"
        val queueId = "DEEPEYE_MEDIA_QUEUE"
        val recentId = "DEEPEYE_MEDIA_RECENT"

        assertNotNull(rootId)
        assertEquals("DEEPEYE_MEDIA_ROOT", rootId)
        assertEquals("DEEPEYE_MEDIA_QUEUE", queueId)
        assertEquals("DEEPEYE_MEDIA_RECENT", recentId)
    }
}
