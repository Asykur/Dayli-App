package com.example

import com.example.core.database.HabitEntity
import com.example.core.database.HabitLogEntity
import com.example.core.database.TaskEntity
import com.example.core.model.Habit
import com.example.core.model.HabitFrequency
import com.example.core.model.HabitLog
import com.example.core.model.HabitType
import com.example.core.model.RecurrenceRule
import com.example.core.model.Task
import com.example.core.model.TaskPriority
import com.example.core.model.TaskStatus
import com.example.core.model.calculateStreakFromLogs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase3TasksHabitsTest {

    @Test
    fun calculateStreak_consecutiveDaysEndingToday_returnsAccurateStreak() {
        val today = "2026-10-04"
        val logs = listOf("2026-10-04", "2026-10-03", "2026-10-02", "2026-10-01")

        val result = calculateStreakFromLogs(logs, today)
        assertEquals(4, result.currentStreak)
        assertEquals(4, result.longestStreak)
    }

    @Test
    fun calculateStreak_consecutiveDaysEndingYesterday_keepsStreakActive() {
        // User logged yesterday, hasn't logged today yet -> streak is still active at 3
        val today = "2026-10-04"
        val logs = listOf("2026-10-03", "2026-10-02", "2026-10-01")

        val result = calculateStreakFromLogs(logs, today)
        assertEquals(3, result.currentStreak)
        assertEquals(3, result.longestStreak)
    }

    @Test
    fun calculateStreak_brokenStreak_resetsCurrentStreakPreservesLongest() {
        // Last log was 3 days ago (missed yesterday and today)
        val today = "2026-10-04"
        val logs = listOf("2026-10-01", "2026-09-30", "2026-09-29", "2026-09-28", "2026-09-27")

        val result = calculateStreakFromLogs(logs, today)
        assertEquals(0, result.currentStreak)
        assertEquals(5, result.longestStreak)
    }

    @Test
    fun calculateStreak_emptyList_returnsZero() {
        val result = calculateStreakFromLogs(emptyList(), "2026-10-04")
        assertEquals(0, result.currentStreak)
        assertEquals(0, result.longestStreak)
    }

    @Test
    fun taskEntity_conversionToAndFromDomain_preservesAllFields() {
        val task = Task(
            id = "task_100",
            userId = "usr_100",
            title = "Implement Phase 3 Tasks & Habits",
            description = "Ensure clean MVVM architecture and Room mapping",
            status = TaskStatus.IN_PROGRESS,
            priority = TaskPriority.URGENT,
            dueDate = 1775000000000L,
            dueTime = "14:30",
            estimatedMinutes = 45,
            category = "Development",
            goalId = "goal_99",
            milestoneId = "ms_99",
            goalTitle = "Master Android",
            recurrence = RecurrenceRule.DAILY
        )

        val entity = TaskEntity.fromDomain(task)
        val restored = entity.toDomain()

        assertEquals(task.id, restored.id)
        assertEquals(task.userId, restored.userId)
        assertEquals(task.title, restored.title)
        assertEquals(task.description, restored.description)
        assertEquals(TaskStatus.IN_PROGRESS, restored.status)
        assertEquals(TaskPriority.URGENT, restored.priority)
        assertEquals(45, restored.estimatedMinutes)
        assertEquals("goal_99", restored.goalId)
        assertEquals("Master Android", restored.goalTitle)
        assertEquals(RecurrenceRule.DAILY, restored.recurrence)
    }

    @Test
    fun habitEntity_conversionToAndFromDomain_preservesAllFields() {
        val habit = Habit(
            id = "habit_100",
            userId = "usr_100",
            name = "Morning Meditation",
            description = "10 minutes of breathing focus",
            icon = "self_improvement",
            category = "Mindset",
            type = HabitType.DURATION,
            targetValue = 10.0,
            unit = "mins",
            frequency = HabitFrequency.DAILY,
            frequencyDays = listOf(1, 2, 3, 4, 5, 6, 7),
            weeklyTarget = 7,
            reminder = "06:30",
            goalId = "goal_mindset",
            goalTitle = "Cultivate Calm",
            currentStreak = 12,
            longestStreak = 25,
            isActive = true
        )

        val entity = HabitEntity.fromDomain(habit)
        val restored = entity.toDomain()

        assertEquals(habit.id, restored.id)
        assertEquals(habit.name, restored.name)
        assertEquals(HabitType.DURATION, restored.type)
        assertEquals(10.0, restored.targetValue, 0.001)
        assertEquals("mins", restored.unit)
        assertEquals(HabitFrequency.DAILY, restored.frequency)
        assertEquals("06:30", restored.reminder)
        assertEquals("goal_mindset", restored.goalId)
        assertEquals(12, restored.currentStreak)
        assertEquals(25, restored.longestStreak)
        assertTrue(restored.isActive)
    }

    @Test
    fun habitLogEntity_conversionToAndFromDomain_preservesAllFields() {
        val log = HabitLog(
            id = "log_100",
            habitId = "habit_100",
            userId = "usr_100",
            date = "2026-10-04",
            completed = true,
            value = 10.0,
            targetValue = 10.0,
            unit = "mins",
            note = "Felt calm and refreshed"
        )

        val entity = HabitLogEntity.fromDomain(log)
        val restored = entity.toDomain()

        assertEquals(log.id, restored.id)
        assertEquals(log.habitId, restored.habitId)
        assertEquals(log.date, restored.date)
        assertTrue(restored.completed)
        assertEquals(10.0, restored.value, 0.001)
        assertEquals("mins", restored.unit)
        assertEquals("Felt calm and refreshed", restored.note)
    }
}
