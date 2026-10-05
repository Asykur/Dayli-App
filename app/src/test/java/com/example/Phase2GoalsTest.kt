package com.example

import com.example.core.database.GoalEntity
import com.example.core.database.MilestoneEntity
import com.example.core.model.Goal
import com.example.core.model.GoalCategory
import com.example.core.model.GoalStatus
import com.example.core.model.Milestone
import com.example.core.model.MilestoneStatus
import com.example.core.model.calculateGoalProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase2GoalsTest {

    @Test
    fun calculateGoalProgress_emptyList_returnsZero() {
        val progress = calculateGoalProgress(emptyList())
        assertEquals(0f, progress, 0.001f)
    }

    @Test
    fun calculateGoalProgress_partialValues_calculatesDeterministicAverage() {
        val m1 = Milestone(
            id = "m1",
            goalId = "g1",
            userId = "u1",
            title = "Read 5 chapters",
            currentValue = 2.0,
            targetValue = 5.0, // 2/5 = 0.4
            status = MilestoneStatus.IN_PROGRESS
        )
        val m2 = Milestone(
            id = "m2",
            goalId = "g1",
            userId = "u1",
            title = "Pass Mock Exam",
            currentValue = 1.0,
            targetValue = 1.0, // 1/1 = 1.0
            status = MilestoneStatus.COMPLETED
        )

        val progress = calculateGoalProgress(listOf(m1, m2))
        // Average: (0.4 + 1.0) / 2 = 0.70 (70%)
        assertEquals(0.70f, progress, 0.005f)
    }

    @Test
    fun calculateGoalProgress_completedStatusOverridesRatio() {
        val m1 = Milestone(
            id = "m1",
            goalId = "g1",
            userId = "u1",
            title = "Milestone marked completed manually",
            currentValue = 0.0,
            targetValue = 10.0,
            status = MilestoneStatus.COMPLETED
        )
        val progress = calculateGoalProgress(listOf(m1))
        assertEquals(1.0f, progress, 0.001f)
    }

    @Test
    fun goalEntity_conversionToAndFromDomain_preservesAllFields() {
        val goal = Goal(
            id = "goal_test_1",
            userId = "usr_999",
            title = "Become proficient in Kotlin Coroutines",
            description = "Deep dive into Flow and StateFlow",
            why = "Build high-performance, responsive applications for our users",
            category = GoalCategory.CAREER,
            status = GoalStatus.ACTIVE,
            startDate = 1700000000000L,
            deadline = 1710000000000L,
            progress = 0.65f,
            icon = "work",
            milestoneCount = 4,
            completedMilestoneCount = 2
        )

        val entity = GoalEntity.fromDomain(goal)
        val restored = entity.toDomain()

        assertEquals(goal.id, restored.id)
        assertEquals(goal.userId, restored.userId)
        assertEquals(goal.title, restored.title)
        assertEquals(goal.why, restored.why)
        assertEquals(GoalCategory.CAREER, restored.category)
        assertEquals(GoalStatus.ACTIVE, restored.status)
        assertEquals(goal.deadline, restored.deadline)
        assertEquals(0.65f, restored.progress, 0.001f)
        assertEquals(4, restored.milestoneCount)
        assertEquals(2, restored.completedMilestoneCount)
    }

    @Test
    fun milestoneEntity_conversionToAndFromDomain_preservesAllFields() {
        val milestone = Milestone(
            id = "ms_test_1",
            goalId = "goal_test_1",
            userId = "usr_999",
            title = "Complete 10 LeetCode Mediums",
            description = "Dynamic Programming & Graphs",
            order = 2,
            status = MilestoneStatus.IN_PROGRESS,
            progress = 0.5f,
            targetValue = 10.0,
            currentValue = 5.0,
            unit = "problems",
            deadline = 1720000000000L
        )

        val entity = MilestoneEntity.fromDomain(milestone)
        val restored = entity.toDomain()

        assertEquals(milestone.id, restored.id)
        assertEquals(milestone.goalId, restored.goalId)
        assertEquals(milestone.title, restored.title)
        assertEquals(MilestoneStatus.IN_PROGRESS, restored.status)
        assertEquals(10.0, restored.targetValue, 0.001)
        assertEquals(5.0, restored.currentValue, 0.001)
        assertEquals("problems", restored.unit)
        assertEquals(2, restored.order)
    }
}
