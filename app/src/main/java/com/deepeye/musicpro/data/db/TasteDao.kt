package com.deepeye.musicpro.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TasteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayEvent(event: PlayEvent)

    @Query("SELECT * FROM play_events ORDER BY timestamp DESC")
    fun getAllPlayEvents(): Flow<List<PlayEvent>>

    @Query("SELECT * FROM play_events WHERE song_id = :songId ORDER BY timestamp DESC")
    suspend fun getPlayEventsForSong(songId: String): List<PlayEvent>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedback(feedback: UserFeedback)

    @Query("SELECT * FROM user_feedback WHERE song_id = :songId")
    suspend fun getFeedback(songId: String): UserFeedback?

    @Query("SELECT * FROM user_feedback WHERE song_id = :songId")
    fun getFeedbackFlow(songId: String): Flow<UserFeedback?>

    @Query("SELECT * FROM user_feedback")
    fun getAllFeedback(): Flow<List<UserFeedback>>
}
