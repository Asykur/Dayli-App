package com.example.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.core.model.DailyFocus

@Entity(
    tableName = "daily_focus",
    indices = [
        Index(value = ["userId", "date"], unique = true)
    ]
)
data class DailyFocusEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val date: String,
    val focusText: String,
    val taskId: String?,
    val isCompleted: Boolean,
    val updatedAt: Long
) {
    fun toDomain(): DailyFocus {
        return DailyFocus(
            id = id,
            userId = userId,
            date = date,
            focusText = focusText,
            taskId = taskId,
            isCompleted = isCompleted,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(focus: DailyFocus): DailyFocusEntity {
            return DailyFocusEntity(
                id = focus.id,
                userId = focus.userId,
                date = focus.date,
                focusText = focus.focusText,
                taskId = focus.taskId,
                isCompleted = focus.isCompleted,
                updatedAt = focus.updatedAt
            )
        }
    }
}
