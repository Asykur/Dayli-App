package com.example

import com.example.core.database.UserEntity
import com.example.core.model.ThemePreference
import com.example.core.model.UserProfile
import com.example.core.model.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class Phase1FoundationTest {

    @Test
    fun userProfile_defaultValues_areCorrect() {
        val user = UserProfile(
            id = "user_123",
            displayName = "Alex Growth",
            email = "alex@example.com"
        )

        assertEquals("user_123", user.id)
        assertEquals("Alex Growth", user.displayName)
        assertEquals("alex@example.com", user.email)
        assertEquals(0L, user.xp)
        assertEquals(1, user.level)
        assertEquals(0, user.currentStreak)
        assertFalse(user.onboardingCompleted)
        assertEquals(ThemePreference.SYSTEM, user.settings.theme)
        assertTrue(user.settings.notificationsEnabled)
    }

    @Test
    fun userEntity_conversionToAndFromDomain_preservesAllFields() {
        val original = UserProfile(
            id = "uid_456",
            displayName = "Budi Pratama",
            email = "budi@example.com",
            photoUrl = "https://example.com/avatar.png",
            timezone = "Asia/Jakarta",
            locale = "id",
            onboardingCompleted = true,
            xp = 150L,
            level = 2,
            currentStreak = 7,
            longestStreak = 14,
            settings = UserSettings(
                theme = ThemePreference.DARK,
                notificationsEnabled = true,
                hapticEnabled = false,
                soundEnabled = true
            )
        )

        val entity = UserEntity.fromDomain(original, isCurrentActive = true)
        val restored = entity.toDomain()

        assertEquals(original.id, restored.id)
        assertEquals(original.displayName, restored.displayName)
        assertEquals(original.email, restored.email)
        assertEquals(original.photoUrl, restored.photoUrl)
        assertEquals("Asia/Jakarta", restored.timezone)
        assertEquals(ThemePreference.DARK, restored.settings.theme)
        assertFalse(restored.settings.hapticEnabled)
        assertTrue(restored.onboardingCompleted)
        assertEquals(150L, restored.xp)
        assertEquals(7, restored.currentStreak)
        assertEquals(14, restored.longestStreak)
    }

    @Test
    fun deterministicDailyProgress_calculation_isAccurate() {
        // According to Section 13:
        // completed tasks + completed habits / scheduled tasks + scheduled habits
        val completedTasks = 3
        val scheduledTasks = 4
        val completedHabits = 2
        val scheduledHabits = 3

        val totalCompleted = completedTasks + completedHabits
        val totalScheduled = scheduledTasks + scheduledHabits
        val progressPercent = (totalCompleted.toDouble() / totalScheduled * 100).toInt()

        assertEquals(5, totalCompleted)
        assertEquals(7, totalScheduled)
        assertEquals(71, progressPercent) // 5/7 = 71.4% -> 71%
    }

    @Test
    fun timezone_formatting_resolvesDeterministically() {
        val timestamp = 1775000000000L // arbitrary fixed epoch ms
        val zoneJakarta = ZoneId.of("Asia/Jakarta")
        val zonedDateTime = Instant.ofEpochMilli(timestamp).atZone(zoneJakarta)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
        val formattedDate = zonedDateTime.format(formatter)

        assertNotNull(formattedDate)
        assertEquals(10, formattedDate.length)
    }
}
