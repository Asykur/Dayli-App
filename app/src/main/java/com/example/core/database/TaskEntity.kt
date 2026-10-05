package com.example.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.core.model.RecurrenceRule
import com.example.core.model.Task
import com.example.core.model.TaskPriority
import com.example.core.model.TaskStatus

@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["status"]),
        Index(value = ["goalId"]),
        Index(value = ["dueDate"])
    ]
)
data class TaskEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val title: String,
    val description: String,
    val status: String,
    val priority: String,
    val dueDate: Long?,
    val dueTime: String?,
    val estimatedMinutes: Int,
    val category: String,
    val goalId: String?,
    val milestoneId: String?,
    val goalTitle: String?,
    val recurrence: String,
    val completedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain(): Task {
        return Task(
            id = id,
            userId = userId,
            title = title,
            description = description,
            status = try { TaskStatus.valueOf(status) } catch (e: Exception) { TaskStatus.PENDING },
            priority = TaskPriority.fromString(priority),
            dueDate = dueDate,
            dueTime = dueTime,
            estimatedMinutes = estimatedMinutes,
            category = category,
            goalId = goalId,
            milestoneId = milestoneId,
            goalTitle = goalTitle,
            recurrence = RecurrenceRule.fromString(recurrence),
            completedAt = completedAt,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(task: Task): TaskEntity {
            return TaskEntity(
                id = task.id,
                userId = task.userId,
                title = task.title,
                description = task.description,
                status = task.status.name,
                priority = task.priority.name,
                dueDate = task.dueDate,
                dueTime = task.dueTime,
                estimatedMinutes = task.estimatedMinutes,
                category = task.category,
                goalId = task.goalId,
                milestoneId = task.milestoneId,
                goalTitle = task.goalTitle,
                recurrence = task.recurrence.name,
                completedAt = task.completedAt,
                createdAt = task.createdAt,
                updatedAt = task.updatedAt
            )
        }
    }
}
