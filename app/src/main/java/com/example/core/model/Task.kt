package com.example.core.model

enum class TaskStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}

enum class TaskPriority(val label: String, val level: Int) {
    LOW("Low", 1),
    MEDIUM("Medium", 2),
    HIGH("High", 3),
    URGENT("Urgent", 4);

    companion object {
        fun fromString(value: String): TaskPriority {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: MEDIUM
        }
    }
}

enum class RecurrenceRule(val label: String) {
    NONE("Does not repeat"),
    DAILY("Every day"),
    WEEKDAYS("Mon to Fri"),
    WEEKLY("Once a week");

    companion object {
        fun fromString(value: String): RecurrenceRule {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: NONE
        }
    }
}

data class Task(
    val id: String,
    val userId: String,
    val title: String,
    val description: String = "",
    val status: TaskStatus = TaskStatus.PENDING,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val dueDate: Long? = null, // Epoch ms of the scheduled date
    val dueTime: String? = null, // e.g. "08:30"
    val estimatedMinutes: Int = 25,
    val category: String = "General",
    val goalId: String? = null,
    val milestoneId: String? = null,
    val goalTitle: String? = null,
    val recurrence: RecurrenceRule = RecurrenceRule.NONE,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
