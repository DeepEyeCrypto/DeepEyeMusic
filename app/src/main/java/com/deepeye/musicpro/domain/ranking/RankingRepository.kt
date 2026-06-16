// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.domain.ranking

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RankingRepository @Inject constructor() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val usersCollection = firestore.collection("users")

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    suspend fun syncGamificationState(
        points: Int,
        streak: Int,
        songsListened: Int,
        activeDays: Int,
        score: Float
    ) {
        val userId = getCurrentUserId() ?: return
        val user = auth.currentUser ?: return
        
        val updateData = mapOf(
            "displayName" to (user.displayName ?: "User"),
            "photoUrl" to (user.photoUrl?.toString() ?: ""),
            "points" to points,
            "streak" to streak,
            "songsListened" to songsListened,
            "dailyActiveDays" to activeDays,
            "score" to score,
            "lastSync" to System.currentTimeMillis()
        )

        try {
            usersCollection.document(userId).set(updateData, com.google.firebase.firestore.SetOptions.merge()).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun initializeUserIfNew() {
        val userId = getCurrentUserId() ?: return
        val user = auth.currentUser ?: return
        try {
            val doc = usersCollection.document(userId).get().await()
            if (!doc.exists()) {
                val initData = mapOf(
                    "displayName" to (user.displayName ?: "User"),
                    "photoUrl" to (user.photoUrl?.toString() ?: ""),
                    "points" to 0,
                    "streak" to 0,
                    "songsListened" to 0,
                    "dailyActiveDays" to 0,
                    "score" to 0f,
                    "rank" to 999999,
                    "createdAt" to System.currentTimeMillis()
                )
                usersCollection.document(userId).set(initData).await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getUserRank(userId: String): Int {
        try {
            val userDoc = usersCollection.document(userId).get().await()
            val userScore = userDoc.getDouble("score")?.toFloat() ?: 0f
            
            // To find rank, count how many users have a higher score
            val higherScoresQuery = usersCollection.whereGreaterThan("score", userScore).get().await()
            return higherScoresQuery.size() + 1
        } catch (e: Exception) {
            e.printStackTrace()
            return 999999
        }
    }

    fun getTopUsers(limit: Int = 100): Flow<List<UserRank>> = callbackFlow {
        val subscription = usersCollection
            .orderBy("score", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val users = snapshot?.documents?.mapIndexed { index, doc ->
                    UserRank(
                        userId = doc.id,
                        displayName = doc.getString("displayName") ?: "User",
                        photoUrl = doc.getString("photoUrl"),
                        points = doc.getLong("points")?.toInt() ?: 0,
                        streak = doc.getLong("streak")?.toInt() ?: 0,
                        songsListened = doc.getLong("songsListened")?.toInt() ?: 0,
                        dailyActiveDays = doc.getLong("dailyActiveDays")?.toInt() ?: 0,
                        score = doc.getDouble("score")?.toFloat() ?: 0f,
                        rank = index + 1
                    )
                } ?: emptyList()
                
                trySend(users)
            }
            
        awaitClose { subscription.remove() }
    }
    
    fun observeUserRank(userId: String): Flow<UserRank?> = callbackFlow {
        val subscription = usersCollection.document(userId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            
            if (snapshot != null && snapshot.exists()) {
                val user = UserRank(
                    userId = snapshot.id,
                    displayName = snapshot.getString("displayName") ?: "User",
                    photoUrl = snapshot.getString("photoUrl"),
                    points = snapshot.getLong("points")?.toInt() ?: 0,
                    streak = snapshot.getLong("streak")?.toInt() ?: 0,
                    songsListened = snapshot.getLong("songsListened")?.toInt() ?: 0,
                    dailyActiveDays = snapshot.getLong("dailyActiveDays")?.toInt() ?: 0,
                    score = snapshot.getDouble("score")?.toFloat() ?: 0f,
                    // Note: Real-time global rank requires a separate cloud function calculation.
                    // Client side, we might poll this separately.
                    rank = snapshot.getLong("rank")?.toInt() ?: 999999 
                )
                trySend(user)
            } else {
                trySend(null)
            }
        }
        awaitClose { subscription.remove() }
    }
}
