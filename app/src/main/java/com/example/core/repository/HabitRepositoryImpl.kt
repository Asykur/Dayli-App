package com.example.core.repository

import android.util.Log
import com.example.core.database.HabitDao
import com.example.core.database.HabitEntity
import com.example.core.database.HabitLogDao
import com.example.core.database.HabitLogEntity
import com.example.core.firebase.FirebaseManager
import com.example.core.model.Habit
import com.example.core.model.HabitLog
import com.example.core.model.calculateStreakFromLogs
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class HabitRepositoryImpl(
    private val habitDao: HabitDao,
    private val habitLogDao: HabitLogDao
) : HabitRepository {

    private val tag = "HabitRepository"

    override fun getHabitsFlow(userId: String): Flow<List<Habit>> {
        return habitDao.getHabitsFlow(userId).map { list -> list.map { it.toDomain() } }
    }

    override fun getActiveHabitsFlow(userId: String): Flow<List<Habit>> {
        return habitDao.getActiveHabitsFlow(userId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getActiveHabitsCount(userId: String): Int = withContext(Dispatchers.IO) {
        habitDao.getActiveHabitsCount(userId)
    }

    override fun getHabitsForGoalFlow(goalId: String): Flow<List<Habit>> {
        return habitDao.getHabitsForGoalFlow(goalId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getHabitById(habitId: String): Habit? = withContext(Dispatchers.IO) {
        habitDao.getHabitById(habitId)?.toDomain()
    }

    override suspend fun createHabit(habit: Habit): Result<Habit> = withContext(Dispatchers.IO) {
        try {
            // Free plan limit: max 5 active habits
            if (habit.isActive) {
                val activeCount = habitDao.getActiveHabitsCount(habit.userId)
                if (activeCount >= 5) {
                    return@withContext Result.failure(
                        HabitLimitExceededException("Free plan allows up to 5 active habits. Pause an existing habit to activate this one.")
                    )
                }
            }

            habitDao.insertOrUpdate(HabitEntity.fromDomain(habit))
            syncHabitToFirestore(habit)
            Result.success(habit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to create habit: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun updateHabit(habit: Habit): Result<Habit> = withContext(Dispatchers.IO) {
        try {
            val updated = habit.copy(updatedAt = System.currentTimeMillis())
            habitDao.insertOrUpdate(HabitEntity.fromDomain(updated))
            syncHabitToFirestore(updated)
            Result.success(updated)
        } catch (e: Exception) {
            Log.e(tag, "Failed to update habit: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteHabit(habitId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val habit = habitDao.getHabitById(habitId)
            habitDao.deleteHabit(habitId)
            habitLogDao.deleteLogsForHabit(habitId)
            if (habit != null) {
                deleteHabitFromFirestore(habit.userId, habitId)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to delete habit: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun logHabit(
        habitId: String,
        userId: String,
        date: String,
        completed: Boolean,
        value: Double,
        targetValue: Double,
        unit: String,
        note: String
    ): Result<HabitLog> = withContext(Dispatchers.IO) {
        try {
            val existing = habitLogDao.getLog(habitId, date)
            val now = System.currentTimeMillis()
            val log = HabitLog(
                id = existing?.id ?: UUID.randomUUID().toString(),
                habitId = habitId,
                userId = userId,
                date = date,
                completed = completed,
                value = value,
                targetValue = targetValue,
                unit = unit,
                note = note,
                completedAt = if (completed) now else null,
                createdAt = existing?.createdAt ?: now
            )

            habitLogDao.insertOrUpdate(HabitLogEntity.fromDomain(log))
            syncHabitLogToFirestore(log)
            recalculateHabitStreak(habitId, date)

            Result.success(log)
        } catch (e: Exception) {
            Log.e(tag, "Failed to log habit: ${e.message}", e)
            Result.failure(e)
        }
    }

    override fun getHabitLogFlow(habitId: String, date: String): Flow<HabitLog?> {
        return habitLogDao.getLogFlow(habitId, date).map { it?.toDomain() }
    }

    override fun getLogsForDateFlow(userId: String, date: String): Flow<List<HabitLog>> {
        return habitLogDao.getLogsForDateFlow(userId, date).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun recalculateHabitStreak(habitId: String, todayDate: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val completedDates = habitLogDao.getCompletedDatesForHabit(habitId)
            val streakResult = calculateStreakFromLogs(completedDates, todayDate)
            val now = System.currentTimeMillis()

            habitDao.updateStreak(
                habitId = habitId,
                currentStreak = streakResult.currentStreak,
                longestStreak = streakResult.longestStreak,
                updatedAt = now
            )

            val updatedHabit = habitDao.getHabitById(habitId)?.toDomain()
            if (updatedHabit != null) {
                syncHabitToFirestore(updatedHabit)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to recalculate streak: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun syncHabitToFirestore(habit: Habit) {
        val firestore = FirebaseManager.getFirestore() ?: return
        try {
            val map = hashMapOf<String, Any?>(
                "name" to habit.name,
                "description" to habit.description,
                "icon" to habit.icon,
                "category" to habit.category,
                "type" to habit.type.name,
                "targetValue" to habit.targetValue,
                "unit" to habit.unit,
                "frequency" to habit.frequency.name,
                "frequencyDays" to habit.frequencyDays,
                "weeklyTarget" to habit.weeklyTarget,
                "reminder" to habit.reminder,
                "goalId" to habit.goalId,
                "milestoneId" to habit.milestoneId,
                "goalTitle" to habit.goalTitle,
                "currentStreak" to habit.currentStreak,
                "longestStreak" to habit.longestStreak,
                "isActive" to habit.isActive,
                "startDate" to habit.startDate,
                "createdAt" to habit.createdAt,
                "updatedAt" to habit.updatedAt
            )
            firestore.collection("users")
                .document(habit.userId)
                .collection("habits")
                .document(habit.id)
                .set(map, SetOptions.merge())
                .awaitTask()
        } catch (e: Exception) {
            Log.w(tag, "Failed to sync habit to Firestore: ${e.message}")
        }
    }

    private suspend fun deleteHabitFromFirestore(userId: String, habitId: String) {
        val firestore = FirebaseManager.getFirestore() ?: return
        try {
            firestore.collection("users")
                .document(userId)
                .collection("habits")
                .document(habitId)
                .delete()
                .awaitTask()
        } catch (e: Exception) {
            Log.w(tag, "Failed to delete habit from Firestore: ${e.message}")
        }
    }

    private suspend fun syncHabitLogToFirestore(log: HabitLog) {
        val firestore = FirebaseManager.getFirestore() ?: return
        try {
            val map = hashMapOf<String, Any?>(
                "habitId" to log.habitId,
                "date" to log.date,
                "completed" to log.completed,
                "value" to log.value,
                "targetValue" to log.targetValue,
                "unit" to log.unit,
                "note" to log.note,
                "completedAt" to log.completedAt,
                "createdAt" to log.createdAt
            )
            firestore.collection("users")
                .document(log.userId)
                .collection("habit_logs")
                .document(log.id)
                .set(map, SetOptions.merge())
                .awaitTask()
        } catch (e: Exception) {
            Log.w(tag, "Failed to sync habit log to Firestore: ${e.message}")
        }
    }

    private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                cont.resume(task.result)
            } else {
                cont.resumeWithException(task.exception ?: RuntimeException("Task failed"))
            }
        }
    }
}
