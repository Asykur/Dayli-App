package com.example.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.core.model.ThemePreference
import com.example.core.model.UserProfile
import com.example.core.model.UserSettings

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,
    val displayName: String,
    val email: String,
    val photoUrl: String?,
    val timezone: String,
    val locale: String,
    val onboardingCompleted: Boolean,
    val xp: Long,
    val level: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val streakFreezes: Int = 1,
    val theme: String,
    val notificationsEnabled: Boolean,
    val hapticEnabled: Boolean,
    val soundEnabled: Boolean,
    val isCurrentActiveUser: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain(): UserProfile {
        val themePref = try {
            ThemePreference.valueOf(theme)
        } catch (e: Exception) {
            ThemePreference.SYSTEM
        }
        return UserProfile(
            id = id,
            displayName = displayName,
            email = email,
            photoUrl = photoUrl,
            timezone = timezone,
            locale = locale,
            onboardingCompleted = onboardingCompleted,
            xp = xp,
            level = level,
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            streakFreezes = streakFreezes,
            settings = UserSettings(
                theme = themePref,
                notificationsEnabled = notificationsEnabled,
                hapticEnabled = hapticEnabled,
                soundEnabled = soundEnabled
            ),
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(profile: UserProfile, isCurrentActive: Boolean = true): UserEntity {
            return UserEntity(
                id = profile.id,
                displayName = profile.displayName,
                email = profile.email,
                photoUrl = profile.photoUrl,
                timezone = profile.timezone,
                locale = profile.locale,
                onboardingCompleted = profile.onboardingCompleted,
                xp = profile.xp,
                level = profile.level,
                currentStreak = profile.currentStreak,
                longestStreak = profile.longestStreak,
                streakFreezes = profile.streakFreezes,
                theme = profile.settings.theme.name,
                notificationsEnabled = profile.settings.notificationsEnabled,
                hapticEnabled = profile.settings.hapticEnabled,
                soundEnabled = profile.settings.soundEnabled,
                isCurrentActiveUser = isCurrentActive,
                createdAt = profile.createdAt,
                updatedAt = profile.updatedAt
            )
        }
    }
}
