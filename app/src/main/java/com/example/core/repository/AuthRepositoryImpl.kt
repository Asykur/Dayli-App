package com.example.core.repository

import android.util.Log
import com.example.core.database.UserDao
import com.example.core.database.UserEntity
import com.example.core.firebase.FirebaseManager
import com.example.core.model.ThemePreference
import com.example.core.model.UserProfile
import com.example.core.model.UserSettings
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.TimeZone
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AuthRepositoryImpl(
    private val userDao: UserDao,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : AuthRepository {

    private val tag = "AuthRepository"
    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    override val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    init {
        scope.launch {
            userDao.getActiveUserFlow().collect { entity ->
                _currentUser.value = entity?.toDomain()
            }
        }
    }

    override suspend fun checkActiveSession(): UserProfile? = withContext(Dispatchers.IO) {
        val localUser = userDao.getActiveUser()?.toDomain()
        if (localUser != null) {
            _currentUser.value = localUser
            return@withContext localUser
        }

        val auth = FirebaseManager.getAuth()
        val firebaseUser = auth?.currentUser
        if (firebaseUser != null) {
            val firestoreUser = fetchUserFromFirestore(firebaseUser.uid)
            val profile = firestoreUser ?: UserProfile(
                id = firebaseUser.uid,
                displayName = firebaseUser.displayName ?: firebaseUser.email?.substringBefore("@") ?: "User",
                email = firebaseUser.email ?: "",
                photoUrl = firebaseUser.photoUrl?.toString(),
                timezone = TimeZone.getDefault().id
            )
            saveUserLocally(profile)
            return@withContext profile
        }

        null
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val auth = FirebaseManager.getAuth()
            if (auth != null) {
                val authResult = auth.signInWithEmailAndPassword(email.trim(), password).awaitTask()
                val uid = authResult.user?.uid ?: throw IllegalStateException("User ID not returned by Firebase Auth")
                val firestoreProfile = fetchUserFromFirestore(uid)
                val profile = firestoreProfile ?: UserProfile(
                    id = uid,
                    displayName = authResult.user?.displayName ?: email.substringBefore("@"),
                    email = email.trim(),
                    timezone = TimeZone.getDefault().id
                )
                saveUserLocally(profile)
                Result.success(profile)
            } else {
                // Standalone / Local Auth fallback
                val existing = userDao.getActiveUser()
                if (existing != null && existing.email.equals(email.trim(), ignoreCase = true)) {
                    val profile = existing.toDomain()
                    _currentUser.value = profile
                    Result.success(profile)
                } else {
                    val profile = UserProfile(
                        id = UUID.randomUUID().toString(),
                        displayName = email.substringBefore("@").replaceFirstChar { it.uppercase() },
                        email = email.trim(),
                        timezone = TimeZone.getDefault().id
                    )
                    saveUserLocally(profile)
                    Result.success(profile)
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Sign in failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun signUpWithEmail(
        displayName: String,
        email: String,
        password: String,
        timezone: String
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val auth = FirebaseManager.getAuth()
            val cleanName = displayName.trim().ifBlank { email.substringBefore("@") }
            val cleanTimezone = timezone.ifBlank { TimeZone.getDefault().id }

            if (auth != null) {
                val authResult = auth.createUserWithEmailAndPassword(email.trim(), password).awaitTask()
                val uid = authResult.user?.uid ?: throw IllegalStateException("User ID not found")
                val profile = UserProfile(
                    id = uid,
                    displayName = cleanName,
                    email = email.trim(),
                    timezone = cleanTimezone,
                    onboardingCompleted = false
                )
                saveUserLocally(profile)
                syncUserToFirestore(profile)
                Result.success(profile)
            } else {
                val profile = UserProfile(
                    id = UUID.randomUUID().toString(),
                    displayName = cleanName,
                    email = email.trim(),
                    timezone = cleanTimezone,
                    onboardingCompleted = false
                )
                saveUserLocally(profile)
                Result.success(profile)
            }
        } catch (e: Exception) {
            Log.e(tag, "Sign up failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun signInGuest(displayName: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val auth = FirebaseManager.getAuth()
            val userId = if (auth != null) {
                try {
                    val result = auth.signInAnonymously().awaitTask()
                    result.user?.uid ?: "guest_${UUID.randomUUID()}"
                } catch (e: Exception) {
                    "guest_${UUID.randomUUID()}"
                }
            } else {
                "guest_${UUID.randomUUID()}"
            }

            val profile = UserProfile(
                id = userId,
                displayName = displayName.ifBlank { "Growth Seeker" },
                email = "guest@thptracker.local",
                timezone = TimeZone.getDefault().id,
                onboardingCompleted = false
            )
            saveUserLocally(profile)
            syncUserToFirestore(profile)
            Result.success(profile)
        } catch (e: Exception) {
            Log.e(tag, "Guest sign in failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val auth = FirebaseManager.getAuth()
            if (auth != null) {
                auth.sendPasswordResetEmail(email.trim()).awaitTask()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Password reset failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val auth = FirebaseManager.getAuth()
            auth?.signOut()
            userDao.clearActiveUser()
            _currentUser.value = null
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Sign out failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun updateProfile(profile: UserProfile): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val updated = profile.copy(updatedAt = System.currentTimeMillis())
            saveUserLocally(updated)
            syncUserToFirestore(updated)
            Result.success(updated)
        } catch (e: Exception) {
            Log.e(tag, "Profile update failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun updateSettings(settings: UserSettings): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val current = _currentUser.value ?: return@withContext Result.failure(IllegalStateException("No active user"))
            val updated = current.copy(
                settings = settings,
                updatedAt = System.currentTimeMillis()
            )
            saveUserLocally(updated)
            syncUserToFirestore(updated)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Settings update failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun completeOnboarding(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val current = _currentUser.value ?: return@withContext Result.failure(IllegalStateException("No active user"))
            val updated = current.copy(
                onboardingCompleted = true,
                updatedAt = System.currentTimeMillis()
            )
            saveUserLocally(updated)
            syncUserToFirestore(updated)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Complete onboarding failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun saveUserLocally(profile: UserProfile) {
        userDao.clearActiveUser()
        userDao.insertOrUpdate(UserEntity.fromDomain(profile, isCurrentActive = true))
        _currentUser.value = profile
    }

    private suspend fun fetchUserFromFirestore(userId: String): UserProfile? {
        val firestore = FirebaseManager.getFirestore() ?: return null
        return try {
            val snapshot = firestore.collection("users").document(userId).get().awaitTask()
            if (snapshot.exists()) {
                val data = snapshot.data ?: return null
                val settingsMap = data["settings"] as? Map<*, *>
                val themeString = settingsMap?.get("theme") as? String ?: ThemePreference.SYSTEM.name
                val theme = try {
                    ThemePreference.valueOf(themeString)
                } catch (e: Exception) {
                    ThemePreference.SYSTEM
                }

                UserProfile(
                    id = userId,
                    displayName = data["displayName"] as? String ?: "User",
                    email = data["email"] as? String ?: "",
                    photoUrl = data["photoUrl"] as? String,
                    timezone = data["timezone"] as? String ?: TimeZone.getDefault().id,
                    locale = data["locale"] as? String ?: "en",
                    onboardingCompleted = data["onboardingCompleted"] as? Boolean ?: false,
                    xp = (data["xp"] as? Number)?.toLong() ?: 0L,
                    level = (data["level"] as? Number)?.toInt() ?: 1,
                    currentStreak = (data["currentStreak"] as? Number)?.toInt() ?: 0,
                    longestStreak = (data["longestStreak"] as? Number)?.toInt() ?: 0,
                    settings = UserSettings(
                        theme = theme,
                        notificationsEnabled = settingsMap?.get("notificationsEnabled") as? Boolean ?: true,
                        hapticEnabled = settingsMap?.get("hapticEnabled") as? Boolean ?: true,
                        soundEnabled = settingsMap?.get("soundEnabled") as? Boolean ?: true
                    ),
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to fetch user from Firestore: ${e.message}")
            null
        }
    }

    private suspend fun syncUserToFirestore(profile: UserProfile) {
        val firestore = FirebaseManager.getFirestore() ?: return
        try {
            val userMap = hashMapOf<String, Any>(
                "displayName" to profile.displayName,
                "email" to profile.email,
                "timezone" to profile.timezone,
                "locale" to profile.locale,
                "onboardingCompleted" to profile.onboardingCompleted,
                "xp" to profile.xp,
                "level" to profile.level,
                "currentStreak" to profile.currentStreak,
                "longestStreak" to profile.longestStreak,
                "settings" to hashMapOf(
                    "theme" to profile.settings.theme.name,
                    "notificationsEnabled" to profile.settings.notificationsEnabled,
                    "hapticEnabled" to profile.settings.hapticEnabled,
                    "soundEnabled" to profile.settings.soundEnabled
                ),
                "updatedAt" to profile.updatedAt
            )
            profile.photoUrl?.let { userMap["photoUrl"] = it }
            firestore.collection("users").document(profile.id).set(userMap, SetOptions.merge()).awaitTask()
        } catch (e: Exception) {
            Log.w(tag, "Failed to sync user to Firestore: ${e.message}")
        }
    }

    private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                cont.resume(task.result)
            } else {
                cont.resumeWithException(task.exception ?: RuntimeException("Task failed with unknown exception"))
            }
        }
    }
}
