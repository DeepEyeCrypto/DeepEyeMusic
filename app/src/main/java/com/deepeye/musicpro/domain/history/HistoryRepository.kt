// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.domain.history

import com.deepeye.musicpro.data.db.PlayEvent
import com.deepeye.musicpro.data.db.TasteDao
import com.deepeye.musicpro.data.db.TopTrackResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Time period for filtering top tracks.
 */
enum class TimePeriod {
    WEEK,
    MONTH,
    ALL_TIME,
}

/**
 * Repository for listening history and top tracks computation.
 * Records play events and provides aggregated views.
 */
@Singleton
class HistoryRepository @Inject constructor(
    private val tasteDao: TasteDao,
) {
    /**
     * Records a play event to the history database.
     */
    suspend fun recordPlayEvent(event: PlayEvent) {
        withContext(Dispatchers.IO) {
            tasteDao.insertPlayEvent(event)
        }
    }

    /**
     * Returns recent listening history (most recent first).
     */
    fun getRecentHistory(limit: Int = 100): Flow<List<PlayEvent>> {
        return tasteDao.getRecentHistory(limit)
    }

    /**
     * Returns all play events as a flow.
     */
    fun getAllHistory(): Flow<List<PlayEvent>> {
        return tasteDao.getAllPlayEvents()
    }

    /**
     * Returns top tracks ranked by total play time for a given period.
     */
    fun getTopTracks(period: TimePeriod, limit: Int = 50): Flow<List<TopTrackResult>> {
        val sinceTimestamp = when (period) {
            TimePeriod.WEEK -> System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
            TimePeriod.MONTH -> System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L
            TimePeriod.ALL_TIME -> return tasteDao.getTopTracksAllTime(limit)
        }
        return tasteDao.getTopTracks(sinceTimestamp, limit)
    }
}
