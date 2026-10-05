package com.example.core.model

enum class GoalStatus {
    ACTIVE,
    COMPLETED,
    PAUSED,
    ARCHIVED,
    CANCELLED
}

enum class GoalCategory(val displayName: String, val defaultIcon: String) {
    CAREER("Career & Work", "work"),
    HEALTH("Health & Fitness", "fitness"),
    LEARNING("Learning & Skills", "school"),
    FINANCE("Money & Finance", "payments"),
    MINDSET("Mindset & Habits", "psychology"),
    LIFESTYLE("Lifestyle & Personal", "star");

    companion object {
        fun fromString(value: String): GoalCategory {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: CAREER
        }
    }
}

data class Goal(
    val id: String,
    val userId: String,
    val title: String,
    val description: String = "",
    val why: String = "",
    val category: GoalCategory = GoalCategory.CAREER,
    val status: GoalStatus = GoalStatus.ACTIVE,
    val startDate: Long = System.currentTimeMillis(),
    val deadline: Long? = null,
    val progress: Float = 0f, // 0.0f to 1.0f
    val icon: String = "flag",
    val milestoneCount: Int = 0,
    val completedMilestoneCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
