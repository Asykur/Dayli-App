package com.example.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MilestoneDao {
    @Query("SELECT * FROM milestones WHERE goalId = :goalId ORDER BY orderIndex ASC, createdAt ASC")
    fun getMilestonesForGoalFlow(goalId: String): Flow<List<MilestoneEntity>>

    @Query("SELECT * FROM milestones WHERE goalId = :goalId ORDER BY orderIndex ASC, createdAt ASC")
    suspend fun getMilestonesForGoal(goalId: String): List<MilestoneEntity>

    @Query("SELECT * FROM milestones WHERE id = :id LIMIT 1")
    suspend fun getMilestoneById(id: String): MilestoneEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(milestone: MilestoneEntity)

    @Update
    suspend fun update(milestone: MilestoneEntity)

    @Query("DELETE FROM milestones WHERE id = :milestoneId")
    suspend fun deleteMilestone(milestoneId: String)

    @Query("DELETE FROM milestones WHERE goalId = :goalId")
    suspend fun deleteMilestonesForGoal(goalId: String)
}
