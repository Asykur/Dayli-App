package com.example.core.model

enum class MilestoneStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED;

    companion object {
        fun fromString(value: String): MilestoneStatus {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: PENDING
        }
    }
}

data class Milestone(
    val id: String,
    val goalId: String,
    val userId: String,
    val title: String,
    val description: String = "",
    val order: Int = 0,
    val status: MilestoneStatus = MilestoneStatus.PENDING,
    val progress: Float = 0f, // 0.0f to 1.0f
    val targetValue: Double = 1.0,
    val currentValue: Double = 0.0,
    val unit: String = "",
    val deadline: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

fun calculateGoalProgress(milestones: List<Milestone>): Float {
    if (milestones.isEmpty()) return 0f
    val sum = milestones.fold(0.0) { acc, m ->
        val milestoneRatio = when {
            m.status == MilestoneStatus.COMPLETED -> 1.0
            m.targetValue > 0.0 -> (m.currentValue / m.targetValue).coerceIn(0.0, 1.0)
            else -> m.progress.toDouble().coerceIn(0.0, 1.0)
        }
        acc + milestoneRatio
    }
    val avg = sum / milestones.size
    return avg.toFloat().coerceIn(0f, 1f)
}
