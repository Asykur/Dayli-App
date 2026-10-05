package com.example.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.core.model.Milestone
import com.example.core.model.MilestoneStatus

@Entity(
    tableName = "milestones",
    indices = [
        Index(value = ["goalId"]),
        Index(value = ["userId"])
    ]
)
data class MilestoneEntity(
    @PrimaryKey
    val id: String,
    val goalId: String,
    val userId: String,
    val title: String,
    val description: String,
    val orderIndex: Int,
    val status: String,
    val progress: Float,
    val targetValue: Double,
    val currentValue: Double,
    val unit: String,
    val deadline: Long?,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain(): Milestone {
        return Milestone(
            id = id,
            goalId = goalId,
            userId = userId,
            title = title,
            description = description,
            order = orderIndex,
            status = MilestoneStatus.fromString(status),
            progress = progress,
            targetValue = targetValue,
            currentValue = currentValue,
            unit = unit,
            deadline = deadline,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(m: Milestone): MilestoneEntity {
            return MilestoneEntity(
                id = m.id,
                goalId = m.goalId,
                userId = m.userId,
                title = m.title,
                description = m.description,
                orderIndex = m.order,
                status = m.status.name,
                progress = m.progress,
                targetValue = m.targetValue,
                currentValue = m.currentValue,
                unit = m.unit,
                deadline = m.deadline,
                createdAt = m.createdAt,
                updatedAt = m.updatedAt
            )
        }
    }
}
