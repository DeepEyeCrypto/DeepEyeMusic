package com.deepeye.musicpro.domain.usecase.search

import com.deepeye.musicpro.data.source.remote.youtube.YoutubeRemoteDataSource
import com.deepeye.musicpro.domain.model.Song
import com.deepeye.musicpro.domain.model.home.HomeMusicItem
import com.deepeye.musicpro.domain.model.home.MusicItemType
import com.deepeye.musicpro.domain.repository.MusicRepository
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchHybridUseCaseTest {

    private val musicRepository = mockk<MusicRepository>()
    private val youtubeRemoteDataSource = mockk<YoutubeRemoteDataSource>()
    private lateinit var useCase: SearchHybridUseCase

    @Before
    fun setUp() {
        useCase = SearchHybridUseCase(musicRepository, youtubeRemoteDataSource)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun testEmptyQuery_returnsEmptyLists() = runTest {
        val result = useCase("").first()
        assertTrue(result.localSongs.isEmpty())
        assertTrue(result.remoteItems.isEmpty())
    }

    @Test
    fun testBlankQuery_returnsEmptyLists() = runTest {
        val result = useCase("   ").first()
        assertTrue(result.localSongs.isEmpty())
        assertTrue(result.remoteItems.isEmpty())
    }

    @Test
    fun testValidQuery_returnsMatchedLocalAndRemoteSongs() = runTest {
        val query = "arijit"
        val mockLocalSongs = listOf(
            mockk<Song> {
                every { id } returns 101L
                every { title } returns "Channa Mereya"
                every { artist } returns "Arijit Singh"
            }
        )
        val mockRemoteItems = listOf(
            HomeMusicItem(
                id = "yt_123",
                title = "Tum Hi Ho",
                artist = "Arijit Singh",
                thumbnailUrl = "https://example.com/thumb.jpg",
                duration = 260000L,
                playCount = 1000000L,
                type = MusicItemType.SONG,
                streamUrl = null
            )
        )

        every { musicRepository.searchSongs(query) } returns flowOf(mockLocalSongs)
        coEvery { youtubeRemoteDataSource.searchMusic(query) } returns mockRemoteItems

        val result = useCase(query).first()

        assertEquals(1, result.localSongs.size)
        assertEquals("Channa Mereya", result.localSongs.first().title)
        assertEquals(1, result.remoteItems.size)
        assertEquals("Tum Hi Ho", result.remoteItems.first().title)

        verify(exactly = 1) { musicRepository.searchSongs(query) }
        coVerify(exactly = 1) { youtubeRemoteDataSource.searchMusic(query) }
    }

    @Test
    fun testSpecialCharactersQuery_doesNotCrash() = runTest {
        val query = "arijit@123!#$"
        every { musicRepository.searchSongs(query) } returns flowOf(emptyList())
        coEvery { youtubeRemoteDataSource.searchMusic(query) } returns emptyList()

        val result = useCase(query).first()
        assertTrue(result.localSongs.isEmpty())
        assertTrue(result.remoteItems.isEmpty())
    }
}
