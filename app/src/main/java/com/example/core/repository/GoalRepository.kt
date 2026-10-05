package com.example.core.repository

import com.example.core.model.Goal
import com.example.core.model.Milestone
import kotlinx.coroutines.flow.Flow

interface GoalRepository {
    fun getGoalsFlow(userId: String): Flow<List<Goal>>

    fun getGoalByIdFlow(goalId: String): Flow<Goal?>

    suspend fun getGoalById(goalId: String): Goal?

    fun getMilestonesForGoalFlow(goalId: String): Flow<List<Milestone>>

    suspend fun getActiveGoalsCount(userId: String): Int

    suspend fun createGoal(goal: Goal): Result<Goal>

    suspend fun updateGoal(goal: Goal): Result<Goal>

    suspend fun deleteGoal(goalId: String): Result<Unit>

    suspend fun createMilestone(milestone: Milestone): Result<Milestone>

    suspend fun updateMilestone(milestone: Milestone): Result<Milestone>

    suspend fun deleteMilestone(milestoneId: String, goalId: String): Result<Unit>

    suspend fun recalculateGoalProgress(goalId: String): Result<Float>
}
