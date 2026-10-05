package com.example.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.core.model.Habit
import com.example.core.model.HabitFrequency
import com.example.core.model.HabitType

@Entity(
    tableName = "habits",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["isActive"]),
        Index(value = ["goalId"])
    ]
)
data class HabitEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val name: String,
    val description: String,
    val icon: String,
    val category: String,
    val type: String,
    val targetValue: Double,
    val unit: String,
    val frequency: String,
    val frequencyDays: String, // comma separated "1,2,3,4,5,6,7"
    val weeklyTarget: Int,
    val reminder: String?,
    val goalId: String?,
    val milestoneId: String?,
    val goalTitle: String?,
    val currentStreak: Int,
    val longestStreak: Int,
    val isActive: Boolean,
    val startDate: Long,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain(): Habit {
        val days = frequencyDays.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .ifEmpty { listOf(1, 2, 3, 4, 5, 6, 7) }

        return Habit(
            id = id,
            userId = userId,
            name = name,
            description = description,
            icon = icon,
            category = category,
            type = HabitType.fromString(type),
            targetValue = targetValue,
            unit = unit,
            frequency = HabitFrequency.fromString(frequency),
            frequencyDays = days,
            weeklyTarget = weeklyTarget,
            reminder = reminder,
            goalId = goalId,
            milestoneId = milestoneId,
            goalTitle = goalTitle,
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            isActive = isActive,
            startDate = startDate,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(h: Habit): HabitEntity {
            return HabitEntity(
                id = h.id,
                userId = h.userId,
                name = h.name,
                description = h.description,
                icon = h.icon,
                category = h.category,
                type = h.type.name,
                targetValue = h.targetValue,
                unit = h.unit,
                frequency = h.frequency.name,
                frequencyDays = h.frequencyDays.joinToString(","),
                weeklyTarget = h.weeklyTarget,
                reminder = h.reminder,
                goalId = h.goalId,
                milestoneId = h.milestoneId,
                goalTitle = h.goalTitle,
                currentStreak = h.currentStreak,
                longestStreak = h.longestStreak,
                isActive = h.isActive,
                startDate = h.startDate,
                createdAt = h.createdAt,
                updatedAt = h.updatedAt
            )
        }
    }
}
