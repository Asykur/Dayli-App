package com.example.core.repository

import android.util.Log
import com.example.core.database.HabitDao
import com.example.core.database.HabitLogDao
import com.example.core.database.TaskDao
import com.example.core.database.UserDao
import com.example.core.firebase.FirebaseManager
import com.example.core.model.DailyConsistencyPoint
import com.example.core.model.HabitFrequency
import com.example.core.model.TaskStatus
import com.example.core.model.WeeklyConsistencyReport
import com.example.core.model.calculateLevelInfo
import com.example.core.model.calculateWeeklyScore
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ConsistencyRepositoryImpl(
    private val userDao: UserDao,
    private val taskDao: TaskDao,
    private val habitDao: HabitDao,
    private val habitLogDao: HabitLogDao
) : ConsistencyRepository {

    private val tag = "ConsistencyRepository"

    override suspend fun awardXp(userId: String, amount: Long, reason: String): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val user = userDao.getUserById(userId) ?: return@withContext Result.failure(Exception("User not found"))
            val currentXp = user.xp
            val newXp = currentXp + amount
            val levelInfo = calculateLevelInfo(newXp)

            userDao.updateGamification(
                userId = userId,
                xp = newXp,
                level = levelInfo.level,
                currentStreak = user.currentStreak,
                longestStreak = user.longestStreak,
                streakFreezes = user.streakFreezes
            )

            syncUserGamificationToFirestore(userId, newXp, levelInfo.level, user.currentStreak, user.longestStreak, user.streakFreezes)

            Result.success(newXp)
        } catch (e: Exception) {
            Log.e(tag, "Failed to award XP: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun evaluateDailyStreak(
        userId: String,
        todayDate: String,
        hadCompletionToday: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val user = userDao.getUserById(userId) ?: return@withContext Result.failure(Exception("User not found"))

            if (hadCompletionToday) {
                // If user already had a streak increment today, keep it; otherwise increment!
                var currentStreak = user.currentStreak
                var longestStreak = user.longestStreak

                if (currentStreak == 0) {
                    currentStreak = 1
                }

                if (currentStreak > longestStreak) {
                    longestStreak = currentStreak
                }

                userDao.updateStreak(userId, currentStreak, longestStreak)
                syncUserGamificationToFirestore(userId, user.xp, user.level, currentStreak, longestStreak, user.streakFreezes)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to evaluate streak: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun useStreakShield(userId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val rowsUpdated = userDao.useStreakShield(userId)
            val success = rowsUpdated > 0
            if (success) {
                val user = userDao.getUserById(userId)
                if (user != null) {
                    syncUserGamificationToFirestore(
                        userId,
                        user.xp,
                        user.level,
                        user.currentStreak,
                        user.longestStreak,
                        user.streakFreezes
                    )
                }
            }
            Result.success(success)
        } catch (e: Exception) {
            Log.e(tag, "Failed to use streak shield: ${e.message}", e)
            Result.failure(e)
        }
    }

    override fun getWeeklyConsistencyReportFlow(
        userId: String,
        timezone: String
    ): Flow<WeeklyConsistencyReport> {
        val tasksFlow = taskDao.getTasksFlow(userId)
        val habitsFlow = habitDao.getActiveHabitsFlow(userId)

        return combine(tasksFlow, habitsFlow) { taskEntities, habitEntities ->
            val zoneId = try { ZoneId.of(timezone) } catch (e: Exception) { ZoneId.systemDefault() }
            val today = LocalDate.now(zoneId)

            val days = mutableListOf<DailyConsistencyPoint>()
            var totalWeeklyScheduled = 0
            var totalWeeklyCompleted = 0
            var perfectDays = 0

            val tasks = taskEntities.map { it.toDomain() }
            val habits = habitEntities.map { it.toDomain() }

            // Evaluate past 7 days: from (today - 6 days) to today
            for (i in 6 downTo 0) {
                val date = today.minusDays(i.toLong())
                val dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                val dayOfWeekShort = date.format(DateTimeFormatter.ofPattern("EEE"))
                val dayOfWeekNum = date.dayOfWeek.value // 1=Mon, 7=Sun

                val dayStartMs = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
                val dayEndMs = dayStartMs + (24 * 60 * 60 * 1000)

                // Scheduled tasks for this date
                val scheduledTasksForDay = tasks.filter { task ->
                    if (task.status == TaskStatus.CANCELLED) return@filter false
                    when {
                        task.recurrence == com.example.core.model.RecurrenceRule.DAILY -> true
                        task.recurrence == com.example.core.model.RecurrenceRule.WEEKDAYS && dayOfWeekNum in 1..5 -> true
                        task.dueDate != null && task.dueDate in dayStartMs..dayEndMs -> true
                        else -> false
                    }
                }

                val completedTasksForDay = scheduledTasksForDay.count { it.status == TaskStatus.COMPLETED }

                // Scheduled habits for this date
                val scheduledHabitsForDay = habits.filter { habit ->
                    when (habit.frequency) {
                        HabitFrequency.DAILY -> true
                        HabitFrequency.WEEKDAYS -> dayOfWeekNum in 1..5
                        HabitFrequency.WEEKLY_TARGET -> true
                    }
                }

                // Query completed habit logs for this date synchronously in IO thread
                val dayLogs = habitLogDao.getCompletedDatesForHabit("") // placeholder or read
                val completedHabitsCount = scheduledHabitsForDay.count { habit ->
                    habitLogDao.getLog(habit.id, dateStr)?.completed == true
                }

                val dayScheduled = scheduledTasksForDay.size + scheduledHabitsForDay.size
                val dayCompleted = completedTasksForDay + completedHabitsCount
                val percentage = if (dayScheduled > 0) ((dayCompleted.toFloat() / dayScheduled.toFloat()) * 100).toInt().coerceIn(0, 100) else 0

                if (dayScheduled > 0 && dayCompleted >= dayScheduled) {
                    perfectDays++
                }

                totalWeeklyScheduled += dayScheduled
                totalWeeklyCompleted += dayCompleted

                days.add(
                    DailyConsistencyPoint(
                        dayOfWeek = dayOfWeekShort,
                        date = dateStr,
                        scheduledCount = dayScheduled,
                        completedCount = dayCompleted,
                        percentage = percentage
                    )
                )
            }

            val score = calculateWeeklyScore(totalWeeklyScheduled, totalWeeklyCompleted)

            WeeklyConsistencyReport(
                weeklyScore = score,
                totalScheduled = totalWeeklyScheduled,
                totalCompleted = totalWeeklyCompleted,
                days = days,
                perfectDaysCount = perfectDays
            )
        }.flowOn(Dispatchers.IO)
    }

    private suspend fun syncUserGamificationToFirestore(
        userId: String,
        xp: Long,
        level: Int,
        currentStreak: Int,
        longestStreak: Int,
        streakFreezes: Int
    ) {
        val firestore = FirebaseManager.getFirestore() ?: return
        try {
            val map = hashMapOf<String, Any?>(
                "xp" to xp,
                "level" to level,
                "currentStreak" to currentStreak,
                "longestStreak" to longestStreak,
                "streakFreezes" to streakFreezes,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection("users")
                .document(userId)
                .set(map, SetOptions.merge())
                .awaitTask()
        } catch (e: Exception) {
            Log.w(tag, "Failed to sync gamification to Firestore: ${e.message}")
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
