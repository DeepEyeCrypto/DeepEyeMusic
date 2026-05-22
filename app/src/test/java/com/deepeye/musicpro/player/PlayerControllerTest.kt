package com.deepeye.musicpro.player

import android.content.Context
import android.net.Uri
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.core.app.ApplicationProvider
import com.deepeye.musicpro.data.source.remote.youtube.YoutubeRemoteDataSource
import com.deepeye.musicpro.domain.model.MediaItem
import com.deepeye.musicpro.domain.model.Song
import com.deepeye.musicpro.dsp.engine.V4AEngine
import com.deepeye.musicpro.player.controller.PlayerController
import com.deepeye.musicpro.player.queue.QueueManager
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PlayerControllerTest {

    private val player = mockk<ExoPlayer>(relaxed = true)
    private val queueManager = QueueManager()
    private val youtubeDataSource = mockk<YoutubeRemoteDataSource>(relaxed = true)
    private val audioSessionManager = mockk<com.deepeye.musicpro.dsp.session.AudioSessionManager>(relaxed = true)
    private val v4aEngine = V4AEngine()
    private val tasteProfileRepository = mockk<com.deepeye.musicpro.domain.repository.TasteProfileRepository>(relaxed = true)
    private val musicRepository = mockk<com.deepeye.musicpro.domain.repository.MusicRepository>(relaxed = true)
    private lateinit var context: Context

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun testInit_attachesAudioSessionManager() {
        PlayerController(
            player = player,
            queueManager = queueManager,
            youtubeDataSource = youtubeDataSource,
            audioSessionManager = audioSessionManager,
            v4aEngine = v4aEngine,
            tasteProfileRepository = tasteProfileRepository,
            musicRepository = musicRepository,
            context = context
        )
        verify { audioSessionManager.attachToPlayer(player) }
    }

    @Test
    fun testPlayMedia_localItem() = runTest {
        val controller = PlayerController(
            player = player,
            queueManager = queueManager,
            youtubeDataSource = youtubeDataSource,
            audioSessionManager = audioSessionManager,
            v4aEngine = v4aEngine,
            tasteProfileRepository = tasteProfileRepository,
            musicRepository = musicRepository,
            context = context
        )
        
        val song = mockk<Song>(relaxed = true) {
            every { id } returns 123L
            every { title } returns "Local Song"
            every { artist } returns "Artist A"
            every { duration } returns 240000L
            every { uri } returns Uri.parse("content://media/external/audio/media/123")
        }
        val localItem = MediaItem.Local(song = song)
        
        controller.playMedia(localItem)
        advanceUntilIdle()

        assertEquals(localItem, controller.playerState.value.currentItem)
        assertFalse(controller.playerState.value.isVideo)
    }
}
