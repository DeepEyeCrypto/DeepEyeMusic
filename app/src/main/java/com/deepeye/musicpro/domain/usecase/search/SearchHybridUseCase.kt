// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.domain.usecase.search

import com.deepeye.musicpro.domain.model.Song
import com.deepeye.musicpro.domain.model.home.HomeMusicItem
import com.deepeye.musicpro.domain.repository.MusicRepository
import com.deepeye.musicpro.data.source.remote.youtube.YoutubeRemoteDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

data class HybridSearchResponse(
    val localSongs: List<Song>,
    val remoteItems: List<HomeMusicItem>
)

class SearchHybridUseCase @Inject constructor(
    private val musicRepository: MusicRepository,
    private val youtubeRemoteDataSource: YoutubeRemoteDataSource
) {
    operator fun invoke(query: String): Flow<HybridSearchResponse> {
        if (query.isBlank()) return flow { emit(HybridSearchResponse(emptyList(), emptyList())) }
        
        val localFlow = musicRepository.searchSongs(query)
        val remoteFlow = flow {
            try {
                val youtubeResults = youtubeRemoteDataSource.searchMusic(query)
                emit(youtubeResults)
            } catch (e: Exception) {
                emit(emptyList())
            }
        }

        return combine(localFlow, remoteFlow) { local, remote ->
            HybridSearchResponse(local, remote)
        }
    }
}
