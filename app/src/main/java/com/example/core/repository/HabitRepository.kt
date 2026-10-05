package com.example.core.repository

import com.example.core.model.Habit
import com.example.core.model.HabitLog
import kotlinx.coroutines.flow.Flow

class HabitLimitExceededException(message: String) : Exception(message)

interface HabitRepository {
    fun getHabitsFlow(userId: String): Flow<List<Habit>>

    fun getActiveHabitsFlow(userId: String): Flow<List<Habit>>

    suspend fun getActiveHabitsCount(userId: String): Int

    fun getHabitsForGoalFlow(goalId: String): Flow<List<Habit>>

    suspend fun getHabitById(habitId: String): Habit?

    suspend fun createHabit(habit: Habit): Result<Habit>

    suspend fun updateHabit(habit: Habit): Result<Habit>

    suspend fun deleteHabit(habitId: String): Result<Unit>

    suspend fun logHabit(
        habitId: String,
        userId: String,
        date: String,
        completed: Boolean,
        value: Double,
        targetValue: Double,
        unit: String,
        note: String = ""
    ): Result<HabitLog>

    fun getHabitLogFlow(habitId: String, date: String): Flow<HabitLog?>

    fun getLogsForDateFlow(userId: String, date: String): Flow<List<HabitLog>>

    suspend fun recalculateHabitStreak(habitId: String, todayDate: String): Result<Unit>
}
