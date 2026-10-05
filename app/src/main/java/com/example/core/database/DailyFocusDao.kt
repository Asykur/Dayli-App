package com.example.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyFocusDao {
    @Query("SELECT * FROM daily_focus WHERE userId = :userId AND date = :date LIMIT 1")
    fun getFocusFlow(userId: String, date: String): Flow<DailyFocusEntity?>

    @Query("SELECT * FROM daily_focus WHERE userId = :userId AND date = :date LIMIT 1")
    suspend fun getFocus(userId: String, date: String): DailyFocusEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: DailyFocusEntity)

    @Query("UPDATE daily_focus SET isCompleted = :completed, updatedAt = :updatedAt WHERE userId = :userId AND date = :date")
    suspend fun updateCompletion(userId: String, date: String, completed: Boolean, updatedAt: Long)

    @Query("DELETE FROM daily_focus WHERE userId = :userId AND date = :date")
    suspend fun deleteFocus(userId: String, date: String)
}
