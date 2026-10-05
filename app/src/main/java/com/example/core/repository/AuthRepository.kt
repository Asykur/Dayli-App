package com.example.core.repository

import com.example.core.model.UserProfile
import com.example.core.model.UserSettings
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val currentUser: StateFlow<UserProfile?>

    suspend fun signInWithEmail(email: String, password: String): Result<UserProfile>

    suspend fun signUpWithEmail(
        displayName: String,
        email: String,
        password: String,
        timezone: String
    ): Result<UserProfile>

    suspend fun signInGuest(displayName: String = "Growth Seeker"): Result<UserProfile>

    suspend fun sendPasswordReset(email: String): Result<Unit>

    suspend fun signOut(): Result<Unit>

    suspend fun updateProfile(profile: UserProfile): Result<UserProfile>

    suspend fun updateSettings(settings: UserSettings): Result<Unit>

    suspend fun completeOnboarding(): Result<Unit>

    suspend fun checkActiveSession(): UserProfile?
}
