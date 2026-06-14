package com.deepeye.musicpro.domain.resolver

import android.util.Log
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SourceResolverManagerTest {

    private lateinit var resolverOne: SourceResolver
    private lateinit var resolverTwo: SourceResolver
    private lateinit var resolverThree: SourceResolver
    private lateinit var manager: SourceResolverManager

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0

        resolverOne = mockk()
        every { resolverOne.name } returns "ResolverOne"
        every { resolverOne.priority } returns 1

        resolverTwo = mockk()
        every { resolverTwo.name } returns "ResolverTwo"
        every { resolverTwo.priority } returns 2

        resolverThree = mockk()
        every { resolverThree.name } returns "ResolverThree"
        every { resolverThree.priority } returns 3

        val resolvers = setOf(resolverTwo, resolverThree, resolverOne)
        manager = SourceResolverManager(resolvers)
    }

    @Test
    fun `resolve uses highest priority resolver first`() = runTest {
        coEvery { resolverOne.resolveStreamUrl("vid1", false) } returns "http://url1"

        val result = manager.resolve("vid1", false)

        assertEquals("http://url1", result)
        coVerify(exactly = 1) { resolverOne.resolveStreamUrl("vid1", false) }
        coVerify(exactly = 0) { resolverTwo.resolveStreamUrl(any(), any()) }
    }

    @Test
    fun `resolve falls back to lower priority resolver on null return`() = runTest {
        coEvery { resolverOne.resolveStreamUrl("vid2", true) } returns null
        coEvery { resolverTwo.resolveStreamUrl("vid2", true) } returns "http://url2"

        val result = manager.resolve("vid2", true)

        assertEquals("http://url2", result)
        coVerify(exactly = 1) { resolverOne.resolveStreamUrl("vid2", true) }
        coVerify(exactly = 1) { resolverTwo.resolveStreamUrl("vid2", true) }
    }

    @Test
    fun `resolve falls back on exception thrown by primary resolver`() = runTest {
        coEvery { resolverOne.resolveStreamUrl("vid3", false) } throws RuntimeException("Network Error")
        coEvery { resolverTwo.resolveStreamUrl("vid3", false) } returns "http://url-fallback"

        val result = manager.resolve("vid3", false)

        assertEquals("http://url-fallback", result)
    }

    @Test
    fun `resolve caches successful extraction`() = runTest {
        coEvery { resolverOne.resolveStreamUrl("vid4", true) } returns "http://cached-url"

        val firstResult = manager.resolve("vid4", true)
        val secondResult = manager.resolve("vid4", true)

        assertEquals("http://cached-url", firstResult)
        assertEquals("http://cached-url", secondResult)
        
        // Ensure resolver was only called once due to caching
        coVerify(exactly = 1) { resolverOne.resolveStreamUrl("vid4", true) }
    }

    @Test
    fun `forceRefresh bypasses cache and resolves again`() = runTest {
        coEvery { resolverOne.resolveStreamUrl("vid5", false) } returns "http://url-first" andThen "http://url-refreshed"

        val firstResult = manager.resolve("vid5", false)
        val secondResult = manager.resolve("vid5", false, forceRefresh = true)

        assertEquals("http://url-first", firstResult)
        assertEquals("http://url-refreshed", secondResult)
        
        coVerify(exactly = 2) { resolverOne.resolveStreamUrl("vid5", false) }
    }

    @Test
    fun `resolve returns null if all resolvers fail`() = runTest {
        coEvery { resolverOne.resolveStreamUrl("vid6", false) } returns null
        coEvery { resolverTwo.resolveStreamUrl("vid6", false) } throws Exception("Failed")
        coEvery { resolverThree.resolveStreamUrl("vid6", false) } returns null

        val result = manager.resolve("vid6", false)

        assertNull(result)
        coVerify(exactly = 1) { resolverOne.resolveStreamUrl(any(), any()) }
        coVerify(exactly = 1) { resolverTwo.resolveStreamUrl(any(), any()) }
        coVerify(exactly = 1) { resolverThree.resolveStreamUrl(any(), any()) }
    }
}
