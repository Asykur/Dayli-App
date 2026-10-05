package com.example.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.core.model.HabitLog

@Entity(
    tableName = "habit_logs",
    indices = [
        Index(value = ["habitId", "date"], unique = true),
        Index(value = ["userId"]),
        Index(value = ["date"])
    ]
)
data class HabitLogEntity(
    @PrimaryKey
    val id: String,
    val habitId: String,
    val userId: String,
    val date: String, // "YYYY-MM-DD"
    val completed: Boolean,
    val value: Double,
    val targetValue: Double,
    val unit: String,
    val note: String,
    val completedAt: Long?,
    val createdAt: Long
) {
    fun toDomain(): HabitLog {
        return HabitLog(
            id = id,
            habitId = habitId,
            userId = userId,
            date = date,
            completed = completed,
            value = value,
            targetValue = targetValue,
            unit = unit,
            note = note,
            completedAt = completedAt,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDomain(log: HabitLog): HabitLogEntity {
            return HabitLogEntity(
                id = log.id,
                habitId = log.habitId,
                userId = log.userId,
                date = log.date,
                completed = log.completed,
                value = log.value,
                targetValue = log.targetValue,
                unit = log.unit,
                note = log.note,
                completedAt = log.completedAt,
                createdAt = log.createdAt
            )
        }
    }
}
