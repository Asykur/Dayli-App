package com.example.core.model

enum class HabitType(val label: String) {
    BOOLEAN("Yes / No Check"),
    DURATION("Timer / Minutes"),
    COUNT("Numeric Counter");

    companion object {
        fun fromString(value: String): HabitType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: BOOLEAN
        }
    }
}

enum class HabitFrequency(val label: String) {
    DAILY("Every Day"),
    WEEKDAYS("Mon - Fri"),
    WEEKLY_TARGET("Weekly Target");

    companion object {
        fun fromString(value: String): HabitFrequency {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: DAILY
        }
    }
}

data class Habit(
    val id: String,
    val userId: String,
    val name: String,
    val description: String = "",
    val icon: String = "repeat",
    val category: String = "General",
    val type: HabitType = HabitType.BOOLEAN,
    val targetValue: Double = 1.0,
    val unit: String = "times",
    val frequency: HabitFrequency = HabitFrequency.DAILY,
    val frequencyDays: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7), // 1=Mon, 7=Sun
    val weeklyTarget: Int = 7,
    val reminder: String? = null,
    val goalId: String? = null,
    val milestoneId: String? = null,
    val goalTitle: String? = null,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val isActive: Boolean = true,
    val startDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
