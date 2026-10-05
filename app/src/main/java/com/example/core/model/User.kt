package com.example.core.model

import java.util.TimeZone

enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK
}

data class UserSettings(
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val notificationsEnabled: Boolean = true,
    val hapticEnabled: Boolean = true,
    val soundEnabled: Boolean = true
)

data class UserProfile(
    val id: String,
    val displayName: String,
    val email: String,
    val photoUrl: String? = null,
    val timezone: String = TimeZone.getDefault().id,
    val locale: String = "en",
    val onboardingCompleted: Boolean = false,
    val xp: Long = 0L,
    val level: Int = 1,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val streakFreezes: Int = 1,
    val settings: UserSettings = UserSettings(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
