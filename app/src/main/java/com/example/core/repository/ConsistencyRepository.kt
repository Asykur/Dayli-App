package com.example.core.repository

import com.example.core.model.WeeklyConsistencyReport
import kotlinx.coroutines.flow.Flow

interface ConsistencyRepository {
    suspend fun awardXp(userId: String, amount: Long, reason: String): Result<Long>

    suspend fun evaluateDailyStreak(userId: String, todayDate: String, hadCompletionToday: Boolean): Result<Unit>

    suspend fun useStreakShield(userId: String): Result<Boolean>

    fun getWeeklyConsistencyReportFlow(userId: String, timezone: String): Flow<WeeklyConsistencyReport>
}
