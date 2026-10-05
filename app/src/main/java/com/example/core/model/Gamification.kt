package com.example.core.model

object XpRewards {
    const val TASK_COMPLETION = 10L
    const val HABIT_COMPLETION = 15L
    const val DAILY_PERFECT_BONUS = 25L
    const val MILESTONE_COMPLETION = 50L
    const val GOAL_COMPLETION = 100L
}

data class LevelInfo(
    val level: Int,
    val rankTitle: String,
    val currentLevelMinXp: Long,
    val nextLevelMinXp: Long,
    val xpInLevel: Long,
    val xpRequiredForNextLevel: Long,
    val progressRatio: Float // 0.0f to 1.0f
)

fun calculateLevelInfo(totalXp: Long): LevelInfo {
    val xp = totalXp.coerceAtLeast(0L)

    // Progressive level formula:
    // Level 1: 0 - 99 (100 xp needed)
    // Level 2: 100 - 249 (150 xp needed)
    // Level 3: 250 - 449 (200 xp needed)
    // Level 4: 450 - 699 (250 xp needed)
    // Level 5: 700 - 999 (300 xp needed)
    // Level 6+: +50 xp per level step
    val thresholds = mutableListOf<Long>()
    thresholds.add(0L)
    var currentThreshold = 0L
    var step = 100L
    for (i in 1..50) {
        currentThreshold += step
        thresholds.add(currentThreshold)
        step += 50L
    }

    var level = 1
    for (i in 0 until thresholds.size - 1) {
        if (xp >= thresholds[i] && xp < thresholds[i + 1]) {
            level = i + 1
            break
        }
    }
    if (xp >= thresholds.last()) {
        level = thresholds.size
    }

    val minXp = thresholds[level - 1]
    val maxXp = thresholds.getOrElse(level) { minXp + 500L }
    val xpInLevel = (xp - minXp).coerceAtLeast(0L)
    val required = maxXp - minXp
    val ratio = (xpInLevel.toFloat() / required.toFloat()).coerceIn(0f, 1f)

    val rankTitle = when (level) {
        1 -> "Novice Builder"
        2 -> "Consistent Seeker"
        3 -> "Momentum Builder"
        4 -> "Habit Artisan"
        5 -> "Master of Focus"
        6 -> "Grand Architect"
        else -> "Legendary Practitioner"
    }

    return LevelInfo(
        level = level,
        rankTitle = rankTitle,
        currentLevelMinXp = minXp,
        nextLevelMinXp = maxXp,
        xpInLevel = xpInLevel,
        xpRequiredForNextLevel = required,
        progressRatio = ratio
    )
}

data class DailyConsistencyPoint(
    val dayOfWeek: String, // e.g. "Mon"
    val date: String, // "YYYY-MM-DD"
    val scheduledCount: Int,
    val completedCount: Int,
    val percentage: Int // 0 to 100
)

data class WeeklyConsistencyReport(
    val weeklyScore: Int, // 0 to 100%
    val totalScheduled: Int,
    val totalCompleted: Int,
    val days: List<DailyConsistencyPoint>,
    val perfectDaysCount: Int
)

fun calculateWeeklyScore(
    totalScheduled: Int,
    totalCompleted: Int
): Int {
    if (totalScheduled == 0) return 0
    return ((totalCompleted.toFloat() / totalScheduled.toFloat()) * 100f).toInt().coerceIn(0, 100)
}
