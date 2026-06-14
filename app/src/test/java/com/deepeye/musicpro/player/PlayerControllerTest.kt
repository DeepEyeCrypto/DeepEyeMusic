package com.deepeye.musicpro.player

import android.content.Context
import android.net.Uri
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.core.app.ApplicationProvider
import com.deepeye.musicpro.data.source.remote.youtube.YoutubeRemoteDataSource
import com.deepeye.musicpro.domain.model.MediaItem
import com.deepeye.musicpro.domain.model.Song
import com.deepeye.musicpro.dsp.engine.DSPEngine
import com.deepeye.musicpro.player.controller.PlayerController
import com.deepeye.musicpro.player.queue.QueueManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PlayerControllerTest {
    private val player = mockk<ExoPlayer>(relaxed = true)
    private val queueManager = QueueManager()
    private val sourceResolverManager = mockk<com.deepeye.musicpro.domain.resolver.SourceResolverManager>(relaxed = true)
    private val historyRepository = mockk<com.deepeye.musicpro.domain.repository.HistoryRepository>(relaxed = true)
    private val audioSessionManager = mockk<com.deepeye.musicpro.dsp.session.AudioSessionManager>(relaxed = true)
    private val dspEngine = mockk<DSPEngine>(relaxed = true)
    private val tasteProfileRepository = mockk<com.deepeye.musicpro.domain.repository.TasteProfileRepository>(
        relaxed = true
    )
    private val musicRepository = mockk<com.deepeye.musicpro.domain.repository.MusicRepository>(relaxed = true)
    private val recommendationEngine = mockk<com.deepeye.musicpro.domain.recommendation.RecommendationEngine>(
        relaxed = true
    )
    private val autoplayRepository = mockk<com.deepeye.musicpro.domain.autoplay.AutoplayRepository>(relaxed = true)
    private val sleepTimerManager = mockk<com.deepeye.musicpro.player.timer.SleepTimerManager>(relaxed = true)
    private val sleepTimerManagerLazy: dagger.Lazy<com.deepeye.musicpro.player.timer.SleepTimerManager> =
        dagger.Lazy { sleepTimerManager }
    private val playbackPathEnforcer = mockk<com.deepeye.musicpro.diagnostics.PlaybackPathEnforcer>(relaxed = true)
    private val audioSessionGuardian = mockk<com.deepeye.musicpro.diagnostics.AudioSessionGuardian>(relaxed = true)
    private val forensics = mockk<com.deepeye.musicpro.diagnostics.ExoPlayerForensics>(relaxed = true)
    private val dspProfileManager = mockk<com.deepeye.musicpro.dsp.profile.DspProfileManager>(relaxed = true)
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
            sourceResolverManager = sourceResolverManager,
            audioSessionManager = audioSessionManager,
            dspEngine = dspEngine,
            tasteProfileRepository = tasteProfileRepository,
            historyRepository = historyRepository,
            musicRepository = musicRepository,
            recommendationEngine = recommendationEngine,
            autoplayRepository = autoplayRepository,
            sleepTimerManager = sleepTimerManagerLazy,
            playbackPathEnforcer = playbackPathEnforcer,
            audioSessionGuardian = audioSessionGuardian,
            forensics = forensics,
            dspProfileManager = dspProfileManager,
            context = context,
        )
        verify { audioSessionManager.attachToPlayer(player) }
    }

    @Test
    fun testPlayMedia_localItem() =
        runTest {
            val controller =
                PlayerController(
                    player = player,
                    queueManager = queueManager,
                    sourceResolverManager = sourceResolverManager,
                    audioSessionManager = audioSessionManager,
                    dspEngine = dspEngine,
                    tasteProfileRepository = tasteProfileRepository,
                    historyRepository = historyRepository,
                    musicRepository = musicRepository,
                    recommendationEngine = recommendationEngine,
                    autoplayRepository = autoplayRepository,
                    sleepTimerManager = sleepTimerManagerLazy,
                    playbackPathEnforcer = playbackPathEnforcer,
                    audioSessionGuardian = audioSessionGuardian,
                    forensics = forensics,
                    dspProfileManager = dspProfileManager,
                    context = context,
                )

            val song =
                mockk<Song>(relaxed = true) {
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
