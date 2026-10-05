package com.example.core.model

data class DailyFocus(
    val id: String,
    val userId: String,
    val date: String, // "YYYY-MM-DD"
    val focusText: String,
    val taskId: String? = null,
    val isCompleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

data class TodayAggregate(
    val date: String,
    val formattedDate: String,
    val greeting: String,
    val scheduledTasks: List<Task>,
    val completedTasks: List<Task>,
    val scheduledHabits: List<Habit>,
    val completedHabits: List<Habit>,
    val todayLogs: Map<String, HabitLog>,
    val totalScheduled: Int,
    val totalCompleted: Int,
    val progress: Float, // 0.0f to 1.0f
    val progressPercentage: Int, // 0 to 100
    val dailyFocus: DailyFocus?,
    val activeGoals: List<Goal>
)

fun calculateDailyProgress(
    scheduledTasksCount: Int,
    completedTasksCount: Int,
    scheduledHabitsCount: Int,
    completedHabitsCount: Int
): Float {
    val totalScheduled = scheduledTasksCount + scheduledHabitsCount
    if (totalScheduled == 0) return 0f
    val totalCompleted = completedTasksCount + completedHabitsCount
    val ratio = totalCompleted.toFloat() / totalScheduled.toFloat()
    return ratio.coerceIn(0f, 1f)
}
