// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.domain.sync

import android.util.Log
import com.deepeye.musicpro.data.prefs.SettingsDataStore
import com.deepeye.musicpro.domain.repository.HistoryRepository
import com.deepeye.musicpro.domain.repository.PlaylistRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudRestoreManager @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val settingsDataStore: SettingsDataStore,
    private val playlistRepository: PlaylistRepository,
    private val historyRepository: HistoryRepository
) {
    suspend fun restoreAllData() {
        withContext(Dispatchers.IO) {
            val user = auth.currentUser ?: return@withContext
            val uid = user.uid

            try {
                // 1. Restore Settings
                val settingsDoc = firestore.collection("users").document(uid).collection("sync").document("settings").get().await()
                if (settingsDoc.exists()) {
                    val settingsJson = settingsDoc.getString("settings")
                    if (settingsJson != null) {
                        settingsDataStore.importFromJson(settingsJson)
                        Log.d("CloudRestore", "Successfully restored settings")
                    }
                }

                // 2. Restore Playlists
                val playlistsDoc = firestore.collection("users").document(uid).collection("sync").document("playlists").get().await()
                if (playlistsDoc.exists()) {
                    val playlistsJson = playlistsDoc.getString("playlists")
                    if (playlistsJson != null) {
                        playlistRepository.importFromJson(playlistsJson)
                        Log.d("CloudRestore", "Successfully restored playlists")
                    }
                }

                // 3. Restore History
                val historyDoc = firestore.collection("users").document(uid).collection("sync").document("history").get().await()
                if (historyDoc.exists()) {
                    val historyJson = historyDoc.getString("history")
                    if (historyJson != null) {
                        historyRepository.importFromJson(historyJson)
                        Log.d("CloudRestore", "Successfully restored history")
                    }
                }

                Log.d("CloudRestore", "Successfully completed all cloud restore operations for user $uid")
            } catch (e: Exception) {
                Log.e("CloudRestore", "Failed to restore data from cloud", e)
            }
        }
    }
}
