// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.domain.sync

import android.util.Log
import com.deepeye.musicpro.data.prefs.SettingsDataStore
import com.deepeye.musicpro.data.prefs.TasteProfileDataStore
import com.deepeye.musicpro.domain.repository.HistoryRepository
import com.deepeye.musicpro.domain.repository.PlaylistRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudSyncManager @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val settingsDataStore: SettingsDataStore,
    private val tasteProfileDataStore: TasteProfileDataStore,
    private val playlistRepository: PlaylistRepository,
    private val historyRepository: HistoryRepository
) {
    suspend fun syncAllData() {
        withContext(Dispatchers.IO) {
            val user = auth.currentUser ?: return@withContext

            // Verify the ID token is valid before writing to Firestore.
            // On cold start the auth token may not be propagated to GMS yet.
            try {
                user.getIdToken(false).await()
            } catch (e: Exception) {
                Log.w("CloudSync", "Auth token not ready, skipping cloud sync", e)
                return@withContext
            }

            val uid = user.uid
            
            try {
                // 1. Sync Settings
                val settingsJson = settingsDataStore.exportToJson()
                val settingsData = mapOf(
                    "settings" to settingsJson,
                    "last_synced" to System.currentTimeMillis()
                )
                firestore.collection("users").document(uid).collection("sync").document("settings")
                    .set(settingsData, SetOptions.merge()).await()

                // 2. Sync Playlists
                val playlistsJson = playlistRepository.exportToJson()
                val playlistsData = mapOf(
                    "playlists" to playlistsJson,
                    "last_synced" to System.currentTimeMillis()
                )
                firestore.collection("users").document(uid).collection("sync").document("playlists")
                    .set(playlistsData, SetOptions.merge()).await()

                // 3. Sync History
                val historyJson = historyRepository.exportToJson()
                val historyData = mapOf(
                    "history" to historyJson,
                    "last_synced" to System.currentTimeMillis()
                )
                firestore.collection("users").document(uid).collection("sync").document("history")
                    .set(historyData, SetOptions.merge()).await()

                // 4. Sync Taste Profile
                val tasteProfileJson = tasteProfileDataStore.exportToJson()
                val tasteProfileData = mapOf(
                    "taste_profile" to tasteProfileJson,
                    "last_synced" to System.currentTimeMillis()
                )
                firestore.collection("users").document(uid).collection("sync").document("taste_profile")
                    .set(tasteProfileData, SetOptions.merge()).await()

                Log.d("CloudSync", "Successfully synced all data to cloud for user $uid")
            } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
                if (e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    Log.w("CloudSync", "Firestore rules denied sync for $uid — check Firebase Console security rules")
                } else {
                    Log.e("CloudSync", "Failed to sync data to cloud", e)
                }
            } catch (e: Exception) {
                Log.e("CloudSync", "Failed to sync data to cloud", e)
            }
        }
    }

    suspend fun syncTasteProfile() {
        withContext(Dispatchers.IO) {
            val user = auth.currentUser ?: return@withContext
            try { user.getIdToken(false).await() } catch (_: Exception) { return@withContext }
            val uid = user.uid
            try {
                val tasteProfileJson = tasteProfileDataStore.exportToJson()
                val tasteProfileData = mapOf(
                    "taste_profile" to tasteProfileJson,
                    "last_synced" to System.currentTimeMillis()
                )
                firestore.collection("users").document(uid).collection("sync").document("taste_profile")
                    .set(tasteProfileData, SetOptions.merge()).await()
                Log.d("CloudSync", "Successfully synced Taste Profile to cloud for user $uid")
            } catch (e: Exception) {
                Log.e("CloudSync", "Failed to sync Taste Profile to cloud", e)
            }
        }
    }

    suspend fun syncHistory() {
        withContext(Dispatchers.IO) {
            val user = auth.currentUser ?: return@withContext
            try { user.getIdToken(false).await() } catch (_: Exception) { return@withContext }
            val uid = user.uid
            try {
                val historyJson = historyRepository.exportToJson()
                val historyData = mapOf(
                    "history" to historyJson,
                    "last_synced" to System.currentTimeMillis()
                )
                firestore.collection("users").document(uid).collection("sync").document("history")
                    .set(historyData, SetOptions.merge()).await()
                Log.d("CloudSync", "Successfully synced History to cloud for user $uid")
            } catch (e: Exception) {
                Log.e("CloudSync", "Failed to sync History to cloud", e)
            }
        }
    }
}
