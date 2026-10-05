package com.example.ui.screens.progress

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.AppDatabase
import com.example.core.model.LevelInfo
import com.example.core.model.UserProfile
import com.example.core.model.WeeklyConsistencyReport
import com.example.core.model.calculateLevelInfo
import com.example.core.repository.ConsistencyRepository
import com.example.core.repository.ConsistencyRepositoryImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProgressViewModel(application: Application) : AndroidViewModel(application) {

    private val consistencyRepository: ConsistencyRepository
    private val currentUserId = MutableStateFlow<String>("")
    private val currentTimezone = MutableStateFlow<String>("Asia/Jakarta")

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    init {
        val db = AppDatabase.getInstance(application)
        consistencyRepository = ConsistencyRepositoryImpl(
            userDao = db.userDao(),
            taskDao = db.taskDao(),
            habitDao = db.habitDao(),
            habitLogDao = db.habitLogDao()
        )
    }

    fun setUser(profile: UserProfile?) {
        _userProfile.value = profile
        if (profile != null) {
            currentUserId.value = profile.id
            currentTimezone.value = profile.timezone
        }
    }

    val levelInfo: StateFlow<LevelInfo> = _userProfile.combine(flowOf(Unit)) { profile, _ ->
        calculateLevelInfo(profile?.xp ?: 0L)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        calculateLevelInfo(0L)
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val weeklyReport: StateFlow<WeeklyConsistencyReport> = combine(currentUserId, currentTimezone) { uid, tz ->
        Pair(uid, tz)
    }.flatMapLatest { (uid, tz) ->
        if (uid.isNotBlank()) {
            consistencyRepository.getWeeklyConsistencyReportFlow(uid, tz)
        } else {
            flowOf(
                WeeklyConsistencyReport(
                    weeklyScore = 0,
                    totalScheduled = 0,
                    totalCompleted = 0,
                    days = emptyList(),
                    perfectDaysCount = 0
                )
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        WeeklyConsistencyReport(
            weeklyScore = 0,
            totalScheduled = 0,
            totalCompleted = 0,
            days = emptyList(),
            perfectDaysCount = 0
        )
    )

    fun useStreakShield(onResult: (Boolean) -> Unit) {
        val uid = currentUserId.value
        if (uid.isBlank()) {
            onResult(false)
            return
        }

        viewModelScope.launch {
            val result = consistencyRepository.useStreakShield(uid)
            onResult(result.getOrDefault(false))
        }
    }
}
