package com.example.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE userId = :userId ORDER BY createdAt DESC")
    fun getHabitsFlow(userId: String): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE userId = :userId AND isActive = 1 ORDER BY createdAt DESC")
    fun getActiveHabitsFlow(userId: String): Flow<List<HabitEntity>>

    @Query("SELECT COUNT(*) FROM habits WHERE userId = :userId AND isActive = 1")
    suspend fun getActiveHabitsCount(userId: String): Int

    @Query("SELECT * FROM habits WHERE goalId = :goalId ORDER BY createdAt DESC")
    fun getHabitsForGoalFlow(goalId: String): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :habitId LIMIT 1")
    suspend fun getHabitById(habitId: String): HabitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(habit: HabitEntity)

    @Update
    suspend fun update(habit: HabitEntity)

    @Query("UPDATE habits SET currentStreak = :currentStreak, longestStreak = :longestStreak, updatedAt = :updatedAt WHERE id = :habitId")
    suspend fun updateStreak(habitId: String, currentStreak: Int, longestStreak: Int, updatedAt: Long)

    @Query("DELETE FROM habits WHERE id = :habitId")
    suspend fun deleteHabit(habitId: String)

    @Query("DELETE FROM habits WHERE goalId = :goalId")
    suspend fun deleteHabitsForGoal(goalId: String)
}
