package com.deepeye.musicpro.data.source.remote.update

import android.app.DownloadManager
import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoUpdateManagerTest {

    private val context: Context = mockk(relaxed = true) {
        every { getSystemService(Context.DOWNLOAD_SERVICE) } returns mockk<DownloadManager>(relaxed = true)
    }

    private val updateManager = AutoUpdateManager(
        context = context,
        okHttpClient = mockk(relaxed = true),
        gson = mockk(relaxed = true)
    )

    @Test
    fun testIsNewerVersion_newerMajor() {
        assertTrue(updateManager.isNewerVersion("2.0.0", "1.0.0"))
    }

    @Test
    fun testIsNewerVersion_newerMinor() {
        assertTrue(updateManager.isNewerVersion("1.1.0", "1.0.0"))
    }

    @Test
    fun testIsNewerVersion_newerPatch() {
        assertTrue(updateManager.isNewerVersion("1.0.1", "1.0.0"))
    }

    @Test
    fun testIsNewerVersion_older() {
        assertFalse(updateManager.isNewerVersion("1.0.0", "2.0.0"))
        assertFalse(updateManager.isNewerVersion("1.0.0", "1.1.0"))
        assertFalse(updateManager.isNewerVersion("1.0.0", "1.0.1"))
    }

    @Test
    fun testIsNewerVersion_same() {
        assertFalse(updateManager.isNewerVersion("1.0.0", "1.0.0"))
    }

    @Test
    fun testIsNewerVersion_differentLength() {
        assertTrue(updateManager.isNewerVersion("1.0.0.1", "1.0.0"))
        assertFalse(updateManager.isNewerVersion("1.0.0", "1.0.0.1"))
    }
}
