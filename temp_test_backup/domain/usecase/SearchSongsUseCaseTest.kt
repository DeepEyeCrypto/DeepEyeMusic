package com.deepeye.musicpro.domain.usecase

import com.deepeye.musicpro.domain.model.Song
import com.deepeye.musicpro.domain.repository.MusicRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SearchSongsUseCaseTest {

    private lateinit var searchSongsUseCase: SearchSongsUseCase
    private val repository = mockk<MusicRepository>()

    private val allSongs = listOf(
        Song(1, "Tum Hi Ho", "Arijit Singh", "Aashiqui 2", 1000, "", "", 1),
        Song(2, "Channa Mereya", "Arijit Singh", "Ae Dil Hai Mushkil", 1000, "", "", 1),
        Song(3, "Shape of You", "Ed Sheeran", "Divide", 1000, "", "", 1)
    )

    @Before
    fun setup() {
        searchSongsUseCase = SearchSongsUseCase(repository)
        every { repository.searchSongs(any()) } answers {
            val query = firstArg<String>().lowercase()
            flowOf(allSongs.filter { 
                it.title.lowercase().contains(query) || 
                it.artist.lowercase().contains(query) || 
                it.album.lowercase().contains(query) 
            })
        }
        every { repository.getAllSongs() } returns flowOf(allSongs)
    }

    @Test
    fun `test empty query returns all songs`() = runTest {
        val result = searchSongsUseCase("").first()
        assertEquals(3, result.size)
    }

    @Test
    fun `test query arijit matches title and artist fields`() = runTest {
        val result = searchSongsUseCase("arijit").first()
        assertEquals(2, result.size)
        assertEquals("Tum Hi Ho", result[0].title)
        assertEquals("Channa Mereya", result[1].title)
    }

    @Test
    fun `test special chars dont crash`() = runTest {
        val result = searchSongsUseCase("!@#$%^").first()
        assertEquals(0, result.size)
    }
}
