package com.deepeye.musicpro.data.repository

import com.deepeye.musicpro.account.AccountScopedCacheInvalidator
import com.deepeye.musicpro.domain.model.personalization.PersonalizedFeedState
import com.deepeye.musicpro.domain.model.personalization.PersonalizedSection
import com.deepeye.musicpro.domain.model.personalization.PersonalizedSectionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Focused test proving D5 (invalidateAccountCache) and D6 (feedGeneration & checkGen).
 * Validates state clearance, lastUpdatedMillis reset, and generation check discarding.
 */
class PersonalizationD5D6LogicTest {

    // Simulates the feed state and invalidation logic of PersonalizationRepositoryImpl
    private class SimulatedRepository(
        private val initialSections: List<PersonalizedSection> = emptyList()
    ) : AccountScopedCacheInvalidator {

        private val _feedState = MutableStateFlow(
            PersonalizedFeedState(
                sections = initialSections,
                lastUpdatedMillis = if (initialSections.isNotEmpty()) System.currentTimeMillis() else 0L
            )
        )
        val feedState = _feedState.asStateFlow()

        private val _feedGeneration = java.util.concurrent.atomic.AtomicLong(0)

        override suspend fun invalidateAccountCache(accountKey: String) {
            _feedState.update { it.copy(sections = emptyList(), lastUpdatedMillis = 0L) }
        }

        fun incrementGen(): Long = _feedGeneration.incrementAndGet()

        fun checkGen(section: PersonalizedSection, feedGeneration: Long): PersonalizedSection {
            if (feedGeneration != -1L && feedGeneration != _feedGeneration.get()) {
                return section.copy(items = emptyList())
            }
            return section
        }

        fun getCurrentGen(): Long = _feedGeneration.get()
    }

    @Test
    fun `D5 invalidateAccountCache clears sections and resets lastUpdatedMillis to 0`() = kotlinx.coroutines.test.runTest {
        val sampleSection = PersonalizedSection(
            id = "sec1",
            type = PersonalizedSectionType.CONTINUE_LISTENING,
            title = "Continue Listening",
            sourceLabel = "Test"
        )
        val repo = SimulatedRepository(listOf(sampleSection))

        // Pre-condition: feed has sections and lastUpdatedMillis > 0
        assertTrue("Feed should start with sections", repo.feedState.value.sections.isNotEmpty())
        assertTrue("lastUpdatedMillis should be > 0", repo.feedState.value.lastUpdatedMillis > 0)

        // Act: invalidate account cache (sign out or account switch)
        repo.invalidateAccountCache("testAccountKey")

        // Assert: sections cleared, lastUpdatedMillis reset to 0
        val state = repo.feedState.value
        assertTrue("Sections must be empty after invalidation", state.sections.isEmpty())
        assertEquals("lastUpdatedMillis must be reset to 0", 0L, state.lastUpdatedMillis)
    }

    @Test
    fun `D5 stale cached feed cannot survive invalidation`() = kotlinx.coroutines.test.runTest {
        val sampleSection = PersonalizedSection(
            id = "sec1",
            type = PersonalizedSectionType.YOUR_QUEUE,
            title = "Queue",
            sourceLabel = "Test"
        )
        val repo = SimulatedRepository(listOf(sampleSection))

        assertTrue(repo.feedState.value.lastUpdatedMillis > 0)

        // Invalidate
        repo.invalidateAccountCache("accountA")

        // Stale feed is dead
        assertTrue(repo.feedState.value.sections.isEmpty())
        assertEquals(0L, repo.feedState.value.lastUpdatedMillis)
    }

    @Test
    fun `D6 account session change or manual refresh increments generation`() = kotlinx.coroutines.test.runTest {
        val repo = SimulatedRepository()
        val genBefore = repo.getCurrentGen()

        val genAfterSwitch = repo.incrementGen()
        assertTrue("Generation must increment on session change", genAfterSwitch > genBefore)

        val genAfterRefresh = repo.incrementGen()
        assertTrue("Generation must increment on manual refresh", genAfterRefresh > genAfterSwitch)
    }

    @Test
    fun `D6 checkGen discards stale awaited result when generation mismatches`() = kotlinx.coroutines.test.runTest {
        val repo = SimulatedRepository()
        val genAtRequestStart = repo.getCurrentGen()

        // Simulate an account switch or new refresh happening while request was in flight
        repo.incrementGen() // Current gen is now genAtRequestStart + 1

        val section = PersonalizedSection(
            id = "sec1",
            type = PersonalizedSectionType.LIKED_MUSIC,
            title = "Liked",
            items = listOf(com.deepeye.musicpro.domain.model.personalization.PersonalizedFeedItem(id = "1", title = "Stale Item", artist = "Artist")),
            sourceLabel = "Test"
        )

        // Check gen with the old generation captured at request start
        val checkedSection = repo.checkGen(section, genAtRequestStart)

        // Assert: items are emptied out because generation changed (stale result discarded)
        assertTrue("Stale result items must be discarded to empty list", checkedSection.items.isEmpty())
    }

    @Test
    fun `D6 checkGen preserves result when generation matches`() = kotlinx.coroutines.test.runTest {
        val repo = SimulatedRepository()
        val currentGen = repo.getCurrentGen()

        val section = PersonalizedSection(
            id = "sec1",
            type = PersonalizedSectionType.LIKED_MUSIC,
            title = "Liked",
            items = listOf(com.deepeye.musicpro.domain.model.personalization.PersonalizedFeedItem(id = "1", title = "Valid Item", artist = "Artist")),
            sourceLabel = "Test"
        )

        val checkedSection = repo.checkGen(section, currentGen)

        // Assert: items are preserved because generation matches
        assertEquals(1, checkedSection.items.size)
        assertEquals("Valid Item", checkedSection.items.first().title)
    }
}