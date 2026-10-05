package com.example.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals WHERE userId = :userId ORDER BY createdAt DESC")
    fun getGoalsFlow(userId: String): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE userId = :userId AND status = 'ACTIVE' ORDER BY createdAt DESC")
    fun getActiveGoalsFlow(userId: String): Flow<List<GoalEntity>>

    @Query("SELECT COUNT(*) FROM goals WHERE userId = :userId AND status = 'ACTIVE'")
    suspend fun getActiveGoalsCount(userId: String): Int

    @Query("SELECT * FROM goals WHERE id = :goalId LIMIT 1")
    suspend fun getGoalById(goalId: String): GoalEntity?

    @Query("SELECT * FROM goals WHERE id = :goalId LIMIT 1")
    fun getGoalByIdFlow(goalId: String): Flow<GoalEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(goal: GoalEntity)

    @Update
    suspend fun update(goal: GoalEntity)

    @Query("UPDATE goals SET progress = :progress, milestoneCount = :milestoneCount, completedMilestoneCount = :completedCount, updatedAt = :updatedAt WHERE id = :goalId")
    suspend fun updateGoalProgress(goalId: String, progress: Float, milestoneCount: Int, completedCount: Int, updatedAt: Long)

    @Query("DELETE FROM goals WHERE id = :goalId")
    suspend fun deleteGoal(goalId: String)

    @Query("DELETE FROM goals WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}
