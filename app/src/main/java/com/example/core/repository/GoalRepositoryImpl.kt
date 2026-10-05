package com.example.core.repository

import android.util.Log
import com.example.core.database.GoalDao
import com.example.core.database.GoalEntity
import com.example.core.database.MilestoneDao
import com.example.core.database.MilestoneEntity
import com.example.core.firebase.FirebaseManager
import com.example.core.model.Goal
import com.example.core.model.GoalStatus
import com.example.core.model.Milestone
import com.example.core.model.MilestoneStatus
import com.example.core.model.calculateGoalProgress
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class GoalLimitExceededException(message: String) : Exception(message)

class GoalRepositoryImpl(
    private val goalDao: GoalDao,
    private val milestoneDao: MilestoneDao
) : GoalRepository {

    private val tag = "GoalRepository"

    override fun getGoalsFlow(userId: String): Flow<List<Goal>> {
        return goalDao.getGoalsFlow(userId).map { list -> list.map { it.toDomain() } }
    }

    override fun getGoalByIdFlow(goalId: String): Flow<Goal?> {
        return goalDao.getGoalByIdFlow(goalId).map { it?.toDomain() }
    }

    override suspend fun getGoalById(goalId: String): Goal? = withContext(Dispatchers.IO) {
        goalDao.getGoalById(goalId)?.toDomain()
    }

    override fun getMilestonesForGoalFlow(goalId: String): Flow<List<Milestone>> {
        return milestoneDao.getMilestonesForGoalFlow(goalId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getActiveGoalsCount(userId: String): Int = withContext(Dispatchers.IO) {
        goalDao.getActiveGoalsCount(userId)
    }

    override suspend fun createGoal(goal: Goal): Result<Goal> = withContext(Dispatchers.IO) {
        try {
            // Check Free limit: max 3 active goals
            if (goal.status == GoalStatus.ACTIVE) {
                val activeCount = goalDao.getActiveGoalsCount(goal.userId)
                if (activeCount >= 3) {
                    return@withContext Result.failure(
                        GoalLimitExceededException("Free plan allows up to 3 active goals. Complete or pause an existing goal to activate this one.")
                    )
                }
            }

            goalDao.insertOrUpdate(GoalEntity.fromDomain(goal))
            syncGoalToFirestore(goal)
            Result.success(goal)
        } catch (e: Exception) {
            Log.e(tag, "Failed to create goal: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun updateGoal(goal: Goal): Result<Goal> = withContext(Dispatchers.IO) {
        try {
            val updated = goal.copy(updatedAt = System.currentTimeMillis())
            goalDao.insertOrUpdate(GoalEntity.fromDomain(updated))
            syncGoalToFirestore(updated)
            Result.success(updated)
        } catch (e: Exception) {
            Log.e(tag, "Failed to update goal: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteGoal(goalId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val goal = goalDao.getGoalById(goalId)
            goalDao.deleteGoal(goalId)
            milestoneDao.deleteMilestonesForGoal(goalId)

            if (goal != null) {
                deleteGoalFromFirestore(goal.userId, goalId)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to delete goal: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun createMilestone(milestone: Milestone): Result<Milestone> = withContext(Dispatchers.IO) {
        try {
            milestoneDao.insertOrUpdate(MilestoneEntity.fromDomain(milestone))
            syncMilestoneToFirestore(milestone)
            recalculateGoalProgress(milestone.goalId)
            Result.success(milestone)
        } catch (e: Exception) {
            Log.e(tag, "Failed to create milestone: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun updateMilestone(milestone: Milestone): Result<Milestone> = withContext(Dispatchers.IO) {
        try {
            val updated = milestone.copy(updatedAt = System.currentTimeMillis())
            milestoneDao.insertOrUpdate(MilestoneEntity.fromDomain(updated))
            syncMilestoneToFirestore(updated)
            recalculateGoalProgress(milestone.goalId)
            Result.success(updated)
        } catch (e: Exception) {
            Log.e(tag, "Failed to update milestone: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteMilestone(milestoneId: String, goalId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val milestone = milestoneDao.getMilestoneById(milestoneId)
            milestoneDao.deleteMilestone(milestoneId)
            if (milestone != null) {
                deleteMilestoneFromFirestore(milestone.userId, milestoneId)
            }
            recalculateGoalProgress(goalId)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to delete milestone: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun recalculateGoalProgress(goalId: String): Result<Float> = withContext(Dispatchers.IO) {
        try {
            val milestoneEntities = milestoneDao.getMilestonesForGoal(goalId)
            val milestones = milestoneEntities.map { it.toDomain() }
            val progress = calculateGoalProgress(milestones)
            val completedCount = milestones.count { it.status == MilestoneStatus.COMPLETED }
            val now = System.currentTimeMillis()

            goalDao.updateGoalProgress(
                goalId = goalId,
                progress = progress,
                milestoneCount = milestones.size,
                completedCount = completedCount,
                updatedAt = now
            )

            val updatedGoal = goalDao.getGoalById(goalId)?.toDomain()
            if (updatedGoal != null) {
                syncGoalToFirestore(updatedGoal)
            }

            Result.success(progress)
        } catch (e: Exception) {
            Log.e(tag, "Failed to recalculate progress: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun syncGoalToFirestore(goal: Goal) {
        val firestore = FirebaseManager.getFirestore() ?: return
        try {
            val map = hashMapOf<String, Any?>(
                "title" to goal.title,
                "description" to goal.description,
                "why" to goal.why,
                "category" to goal.category.name,
                "status" to goal.status.name,
                "startDate" to goal.startDate,
                "deadline" to goal.deadline,
                "progress" to goal.progress,
                "icon" to goal.icon,
                "milestoneCount" to goal.milestoneCount,
                "completedMilestoneCount" to goal.completedMilestoneCount,
                "updatedAt" to goal.updatedAt,
                "createdAt" to goal.createdAt
            )
            firestore.collection("users")
                .document(goal.userId)
                .collection("goals")
                .document(goal.id)
                .set(map, SetOptions.merge())
                .awaitTask()
        } catch (e: Exception) {
            Log.w(tag, "Failed to sync goal to Firestore: ${e.message}")
        }
    }

    private suspend fun deleteGoalFromFirestore(userId: String, goalId: String) {
        val firestore = FirebaseManager.getFirestore() ?: return
        try {
            firestore.collection("users")
                .document(userId)
                .collection("goals")
                .document(goalId)
                .delete()
                .awaitTask()
        } catch (e: Exception) {
            Log.w(tag, "Failed to delete goal from Firestore: ${e.message}")
        }
    }

    private suspend fun syncMilestoneToFirestore(milestone: Milestone) {
        val firestore = FirebaseManager.getFirestore() ?: return
        try {
            val map = hashMapOf<String, Any?>(
                "goalId" to milestone.goalId,
                "title" to milestone.title,
                "description" to milestone.description,
                "order" to milestone.order,
                "status" to milestone.status.name,
                "progress" to milestone.progress,
                "targetValue" to milestone.targetValue,
                "currentValue" to milestone.currentValue,
                "unit" to milestone.unit,
                "deadline" to milestone.deadline,
                "updatedAt" to milestone.updatedAt,
                "createdAt" to milestone.createdAt
            )
            firestore.collection("users")
                .document(milestone.userId)
                .collection("milestones")
                .document(milestone.id)
                .set(map, SetOptions.merge())
                .awaitTask()
        } catch (e: Exception) {
            Log.w(tag, "Failed to sync milestone to Firestore: ${e.message}")
        }
    }

    private suspend fun deleteMilestoneFromFirestore(userId: String, milestoneId: String) {
        val firestore = FirebaseManager.getFirestore() ?: return
        try {
            firestore.collection("users")
                .document(userId)
                .collection("milestones")
                .document(milestoneId)
                .delete()
                .awaitTask()
        } catch (e: Exception) {
            Log.w(tag, "Failed to delete milestone from Firestore: ${e.message}")
        }
    }

    private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                cont.resume(task.result)
            } else {
                cont.resumeWithException(task.exception ?: RuntimeException("Firestore task failed"))
            }
        }
    }
}
