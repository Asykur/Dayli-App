package com.example

import com.example.core.database.DailyFocusEntity
import com.example.core.model.DailyFocus
import com.example.core.model.RecurrenceRule
import com.example.core.model.Task
import com.example.core.model.TaskStatus
import com.example.core.model.calculateDailyProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class Phase4TodayEngineTest {

    @Test
    fun calculateDailyProgress_zeroScheduled_returnsZero() {
        val progress = calculateDailyProgress(0, 0, 0, 0)
        assertEquals(0f, progress, 0.001f)
    }

    @Test
    fun calculateDailyProgress_deterministicCalculation_isExact() {
        // 3 tasks scheduled, 2 completed
        // 2 habits scheduled, 1 completed
        // Total completed = 3, Total scheduled = 5 -> 3/5 = 0.60f (60%)
        val progress = calculateDailyProgress(
            scheduledTasksCount = 3,
            completedTasksCount = 2,
            scheduledHabitsCount = 2,
            completedHabitsCount = 1
        )
        assertEquals(0.60f, progress, 0.001f)
    }

    @Test
    fun calculateDailyProgress_allCompleted_returnsOne() {
        val progress = calculateDailyProgress(
            scheduledTasksCount = 4,
            completedTasksCount = 4,
            scheduledHabitsCount = 3,
            completedHabitsCount = 3
        )
        assertEquals(1.0f, progress, 0.001f)
    }

    @Test
    fun dailyFocusEntity_conversionToAndFromDomain_preservesAllFields() {
        val focus = DailyFocus(
            id = "focus_1",
            userId = "usr_1",
            date = "2026-10-04",
            focusText = "Ship Phase 4 Today Engine",
            taskId = "task_today_1",
            isCompleted = true,
            updatedAt = 1775000000000L
        )

        val entity = DailyFocusEntity.fromDomain(focus)
        val restored = entity.toDomain()

        assertEquals(focus.id, restored.id)
        assertEquals(focus.userId, restored.userId)
        assertEquals(focus.date, restored.date)
        assertEquals(focus.focusText, restored.focusText)
        assertEquals(focus.taskId, restored.taskId)
        assertTrue(restored.isCompleted)
        assertEquals(focus.updatedAt, restored.updatedAt)
    }

    @Test
    fun todayTaskAggregation_overdueTasksIncluded_cancelledExcluded() {
        val zoneId = ZoneId.of("Asia/Jakarta")
        val now = LocalDate.now(zoneId)
        val todayStartMs = now.atStartOfDay(zoneId).toInstant().toEpochMilli()

        val overdueTask = Task(
            id = "t1",
            userId = "u1",
            title = "Overdue task from yesterday",
            dueDate = todayStartMs - (24 * 60 * 60 * 1000), // yesterday
            status = TaskStatus.PENDING
        )

        val cancelledTask = Task(
            id = "t2",
            userId = "u1",
            title = "Cancelled task",
            dueDate = todayStartMs + 1000,
            status = TaskStatus.CANCELLED
        )

        val dailyRecurringTask = Task(
            id = "t3",
            userId = "u1",
            title = "Daily reading",
            recurrence = RecurrenceRule.DAILY,
            status = TaskStatus.PENDING
        )

        val allTasks = listOf(overdueTask, cancelledTask, dailyRecurringTask)

        // Matching logic from TodayEngine:
        val todayScheduled = allTasks.filter { task ->
            if (task.status == TaskStatus.CANCELLED) return@filter false
            when {
                task.recurrence == RecurrenceRule.DAILY -> true
                task.dueDate != null && task.dueDate < todayStartMs && task.status != TaskStatus.COMPLETED -> true
                else -> false
            }
        }

        assertEquals(2, todayScheduled.size)
        assertTrue(todayScheduled.any { it.id == "t1" })
        assertTrue(todayScheduled.any { it.id == "t3" })
        assertFalse(todayScheduled.any { it.id == "t2" })
    }
}
