package com.example.core.model

import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class HabitLog(
    val id: String,
    val habitId: String,
    val userId: String,
    val date: String, // "YYYY-MM-DD"
    val completed: Boolean = false,
    val value: Double = 0.0,
    val targetValue: Double = 1.0,
    val unit: String = "times",
    val note: String = "",
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class StreakResult(
    val currentStreak: Int,
    val longestStreak: Int
)

fun calculateStreakFromLogs(
    completedDates: List<String>,
    todayDate: String
): StreakResult {
    if (completedDates.isEmpty()) return StreakResult(0, 0)

    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val parsedDates = completedDates.mapNotNull {
        try { LocalDate.parse(it, formatter) } catch (e: Exception) { null }
    }.distinct().sortedDescending()

    if (parsedDates.isEmpty()) return StreakResult(0, 0)

    val today = try { LocalDate.parse(todayDate, formatter) } catch (e: Exception) { LocalDate.now() }
    val yesterday = today.minusDays(1)

    // Current streak
    var currentStreak = 0
    val firstDate = parsedDates.first()

    // Can continue streak if logged today or yesterday
    if (firstDate == today || firstDate == yesterday) {
        var expected = firstDate
        for (date in parsedDates) {
            if (date == expected) {
                currentStreak++
                expected = expected.minusDays(1)
            } else if (date.isBefore(expected)) {
                break
            }
        }
    }

    // Longest streak
    var longestStreak = 0
    var tempStreak = 0
    val ascendingDates = parsedDates.sorted()
    var prevDate: LocalDate? = null

    for (date in ascendingDates) {
        if (prevDate == null || date == prevDate.plusDays(1)) {
            tempStreak++
        } else if (date != prevDate) {
            tempStreak = 1
        }
        if (tempStreak > longestStreak) {
            longestStreak = tempStreak
        }
        prevDate = date
    }

    if (currentStreak > longestStreak) {
        longestStreak = currentStreak
    }

    return StreakResult(currentStreak, longestStreak)
}
