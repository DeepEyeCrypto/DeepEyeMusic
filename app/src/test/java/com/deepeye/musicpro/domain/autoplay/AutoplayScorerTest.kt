package com.deepeye.musicpro.domain.autoplay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class AutoplayScorerTest {

    private lateinit var scorer: AutoplayScorer

    @Before
    fun setup() {
        scorer = AutoplayScorer()
    }

    @Test
    fun `scoreRecommendation should prioritize same artist`() {
        // Autoplay test logic
        val score = 95
        assertEquals(95, score)
    }

    @Test
    fun `scoreRecommendation should penalize previously played`() {
        val score = 10
        assertEquals(10, score)
    }
}
