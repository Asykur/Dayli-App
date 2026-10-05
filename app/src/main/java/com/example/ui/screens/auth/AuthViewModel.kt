package com.example.ui.screens.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.AppDatabase
import com.example.core.firebase.FirebaseManager
import com.example.core.model.ThemePreference
import com.example.core.model.UiState
import com.example.core.model.UserProfile
import com.example.core.model.UserSettings
import com.example.core.repository.AuthRepository
import com.example.core.repository.AuthRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository: AuthRepository

    val currentUser: StateFlow<UserProfile?>

    private val _uiState = MutableStateFlow<UiState<UserProfile>>(UiState.Idle)
    val uiState: StateFlow<UiState<UserProfile>> = _uiState.asStateFlow()

    private val _passwordResetSuccess = MutableStateFlow(false)
    val passwordResetSuccess: StateFlow<Boolean> = _passwordResetSuccess.asStateFlow()

    init {
        FirebaseManager.initialize(application)
        val database = AppDatabase.getInstance(application)
        authRepository = AuthRepositoryImpl(database.userDao())
        currentUser = authRepository.currentUser

        viewModelScope.launch {
            authRepository.checkActiveSession()
        }
    }

    fun login(email: String, pass: String, onSuccess: () -> Unit) {
        if (email.isBlank() || pass.isBlank()) {
            _uiState.value = UiState.Error("Please enter both email and password")
            return
        }
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = authRepository.signInWithEmail(email, pass)
            result.onSuccess { profile ->
                _uiState.value = UiState.Success(profile)
                onSuccess()
            }.onFailure { error ->
                _uiState.value = UiState.Error(error.localizedMessage ?: "Failed to sign in. Please try again.")
            }
        }
    }

    fun register(
        displayName: String,
        email: String,
        pass: String,
        timezone: String,
        onSuccess: () -> Unit
    ) {
        if (email.isBlank() || pass.isBlank()) {
            _uiState.value = UiState.Error("Email and password are required")
            return
        }
        if (pass.length < 6) {
            _uiState.value = UiState.Error("Password must be at least 6 characters")
            return
        }
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = authRepository.signUpWithEmail(displayName, email, pass, timezone)
            result.onSuccess { profile ->
                _uiState.value = UiState.Success(profile)
                onSuccess()
            }.onFailure { error ->
                _uiState.value = UiState.Error(error.localizedMessage ?: "Failed to create account")
            }
        }
    }

    fun signInGuest(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = authRepository.signInGuest()
            result.onSuccess { profile ->
                _uiState.value = UiState.Success(profile)
                onSuccess()
            }.onFailure { error ->
                _uiState.value = UiState.Error(error.localizedMessage ?: "Guest login failed")
            }
        }
    }

    fun sendPasswordReset(email: String) {
        if (email.isBlank()) {
            _uiState.value = UiState.Error("Please enter your registered email")
            return
        }
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = authRepository.sendPasswordReset(email)
            result.onSuccess {
                _passwordResetSuccess.value = true
                _uiState.value = UiState.Idle
            }.onFailure { error ->
                _uiState.value = UiState.Error(error.localizedMessage ?: "Failed to send reset email")
            }
        }
    }

    fun resetPasswordResetFlag() {
        _passwordResetSuccess.value = false
    }

    fun updateTheme(themePreference: ThemePreference) {
        val user = currentUser.value ?: return
        val newSettings = user.settings.copy(theme = themePreference)
        viewModelScope.launch {
            authRepository.updateSettings(newSettings)
        }
    }

    fun updateNotifications(enabled: Boolean) {
        val user = currentUser.value ?: return
        val newSettings = user.settings.copy(notificationsEnabled = enabled)
        viewModelScope.launch {
            authRepository.updateSettings(newSettings)
        }
    }

    fun updateHaptics(enabled: Boolean) {
        val user = currentUser.value ?: return
        val newSettings = user.settings.copy(hapticEnabled = enabled)
        viewModelScope.launch {
            authRepository.updateSettings(newSettings)
        }
    }

    fun updateTimezone(newTz: String) {
        val user = currentUser.value ?: return
        val updated = user.copy(timezone = newTz)
        viewModelScope.launch {
            authRepository.updateProfile(updated)
        }
    }

    fun completeOnboarding(onSuccess: () -> Unit) {
        viewModelScope.launch {
            authRepository.completeOnboarding()
            onSuccess()
        }
    }

    fun signOut(onSignedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut()
            _uiState.value = UiState.Idle
            onSignedOut()
        }
    }

    fun clearError() {
        if (_uiState.value is UiState.Error) {
            _uiState.value = UiState.Idle
        }
    }
}
