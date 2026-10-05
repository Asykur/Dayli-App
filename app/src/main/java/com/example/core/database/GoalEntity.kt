package com.example.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.core.model.Goal
import com.example.core.model.GoalCategory
import com.example.core.model.GoalStatus

@Entity(
    tableName = "goals",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["status"])
    ]
)
data class GoalEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val title: String,
    val description: String,
    val why: String,
    val category: String,
    val status: String,
    val startDate: Long,
    val deadline: Long?,
    val progress: Float,
    val icon: String,
    val milestoneCount: Int,
    val completedMilestoneCount: Int,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain(): Goal {
        return Goal(
            id = id,
            userId = userId,
            title = title,
            description = description,
            why = why,
            category = GoalCategory.fromString(category),
            status = try { GoalStatus.valueOf(status) } catch (e: Exception) { GoalStatus.ACTIVE },
            startDate = startDate,
            deadline = deadline,
            progress = progress,
            icon = icon,
            milestoneCount = milestoneCount,
            completedMilestoneCount = completedMilestoneCount,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(goal: Goal): GoalEntity {
            return GoalEntity(
                id = goal.id,
                userId = goal.userId,
                title = goal.title,
                description = goal.description,
                why = goal.why,
                category = goal.category.name,
                status = goal.status.name,
                startDate = goal.startDate,
                deadline = goal.deadline,
                progress = goal.progress,
                icon = goal.icon,
                milestoneCount = goal.milestoneCount,
                completedMilestoneCount = goal.completedMilestoneCount,
                createdAt = goal.createdAt,
                updatedAt = goal.updatedAt
            )
        }
    }
}
