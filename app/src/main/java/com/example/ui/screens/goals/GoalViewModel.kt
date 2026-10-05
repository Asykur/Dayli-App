package com.example.ui.screens.goals

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.AppDatabase
import com.example.core.model.Goal
import com.example.core.model.GoalCategory
import com.example.core.model.GoalStatus
import com.example.core.model.Milestone
import com.example.core.model.MilestoneStatus
import com.example.core.model.UiState
import com.example.core.repository.GoalRepository
import com.example.core.repository.GoalRepositoryImpl
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
import java.util.UUID

enum class GoalFilter {
    ALL,
    ACTIVE,
    COMPLETED,
    PAUSED
}

class GoalViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GoalRepository
    private val currentUserId = MutableStateFlow<String>("")

    private val _selectedFilter = MutableStateFlow(GoalFilter.ACTIVE)
    val selectedFilter: StateFlow<GoalFilter> = _selectedFilter.asStateFlow()

    private val _uiState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val uiState: StateFlow<UiState<Unit>> = _uiState.asStateFlow()

    private val _selectedGoalId = MutableStateFlow<String?>(null)

    init {
        val database = AppDatabase.getInstance(application)
        repository = GoalRepositoryImpl(database.goalDao(), database.milestoneDao())
    }

    fun setUserId(userId: String) {
        if (currentUserId.value != userId) {
            currentUserId.value = userId
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val goals: StateFlow<List<Goal>> = currentUserId.flatMapLatest { uid ->
        if (uid.isNotBlank()) {
            repository.getGoalsFlow(uid)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredGoals: StateFlow<List<Goal>> = combine(goals, _selectedFilter) { list, filter ->
        when (filter) {
            GoalFilter.ALL -> list
            GoalFilter.ACTIVE -> list.filter { it.status == GoalStatus.ACTIVE }
            GoalFilter.COMPLETED -> list.filter { it.status == GoalStatus.COMPLETED }
            GoalFilter.PAUSED -> list.filter { it.status == GoalStatus.PAUSED }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeGoalsCount: StateFlow<Int> = goals.combine(flowOf(Unit)) { list, _ ->
        list.count { it.status == GoalStatus.ACTIVE }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedGoal: StateFlow<Goal?> = _selectedGoalId.flatMapLatest { id ->
        if (id != null) repository.getGoalByIdFlow(id) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedGoalMilestones: StateFlow<List<Milestone>> = _selectedGoalId.flatMapLatest { id ->
        if (id != null) repository.getMilestonesForGoalFlow(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectGoal(goalId: String?) {
        _selectedGoalId.value = goalId
    }

    fun setFilter(filter: GoalFilter) {
        _selectedFilter.value = filter
    }

    fun createGoal(
        title: String,
        description: String,
        why: String,
        category: GoalCategory,
        deadline: Long?,
        icon: String = "flag",
        onSuccess: (Goal) -> Unit
    ) {
        val uid = currentUserId.value
        if (uid.isBlank()) {
            _uiState.value = UiState.Error("User not authenticated")
            return
        }
        if (title.isBlank()) {
            _uiState.value = UiState.Error("Goal title cannot be empty")
            return
        }

        val newGoal = Goal(
            id = UUID.randomUUID().toString(),
            userId = uid,
            title = title.trim(),
            description = description.trim(),
            why = why.trim(),
            category = category,
            status = GoalStatus.ACTIVE,
            deadline = deadline,
            icon = icon,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = repository.createGoal(newGoal)
            result.onSuccess {
                _uiState.value = UiState.Success(Unit)
                onSuccess(it)
            }.onFailure { error ->
                _uiState.value = UiState.Error(error.localizedMessage ?: "Failed to create goal")
            }
        }
    }

    fun updateGoal(goal: Goal, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = repository.updateGoal(goal)
            result.onSuccess {
                _uiState.value = UiState.Success(Unit)
                onSuccess()
            }.onFailure { error ->
                _uiState.value = UiState.Error(error.localizedMessage ?: "Failed to update goal")
            }
        }
    }

    fun updateGoalStatus(goal: Goal, newStatus: GoalStatus) {
        viewModelScope.launch {
            val updated = goal.copy(
                status = newStatus,
                progress = if (newStatus == GoalStatus.COMPLETED) 1f else goal.progress
            )
            repository.updateGoal(updated)
        }
    }

    fun deleteGoal(goalId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = repository.deleteGoal(goalId)
            result.onSuccess {
                _uiState.value = UiState.Success(Unit)
                if (_selectedGoalId.value == goalId) {
                    _selectedGoalId.value = null
                }
                onSuccess()
            }.onFailure { error ->
                _uiState.value = UiState.Error(error.localizedMessage ?: "Failed to delete goal")
            }
        }
    }

    fun createMilestone(
        goalId: String,
        title: String,
        description: String,
        targetValue: Double,
        currentValue: Double,
        unit: String,
        deadline: Long?,
        onSuccess: () -> Unit
    ) {
        val uid = currentUserId.value
        if (title.isBlank()) {
            _uiState.value = UiState.Error("Milestone title cannot be empty")
            return
        }

        val order = selectedGoalMilestones.value.size
        val safeTarget = if (targetValue <= 0.0) 1.0 else targetValue
        val safeCurrent = currentValue.coerceAtLeast(0.0)
        val initialStatus = if (safeCurrent >= safeTarget) MilestoneStatus.COMPLETED else MilestoneStatus.PENDING
        val initialProgress = (safeCurrent / safeTarget).toFloat().coerceIn(0f, 1f)

        val milestone = Milestone(
            id = UUID.randomUUID().toString(),
            goalId = goalId,
            userId = uid,
            title = title.trim(),
            description = description.trim(),
            order = order,
            status = initialStatus,
            progress = initialProgress,
            targetValue = safeTarget,
            currentValue = safeCurrent,
            unit = unit.trim(),
            deadline = deadline
        )

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = repository.createMilestone(milestone)
            result.onSuccess {
                _uiState.value = UiState.Success(Unit)
                onSuccess()
            }.onFailure { error ->
                _uiState.value = UiState.Error(error.localizedMessage ?: "Failed to create milestone")
            }
        }
    }

    fun toggleMilestoneCompletion(milestone: Milestone) {
        val isNowCompleted = milestone.status != MilestoneStatus.COMPLETED
        val updated = milestone.copy(
            status = if (isNowCompleted) MilestoneStatus.COMPLETED else MilestoneStatus.PENDING,
            currentValue = if (isNowCompleted) milestone.targetValue else 0.0,
            progress = if (isNowCompleted) 1f else 0f
        )
        viewModelScope.launch {
            repository.updateMilestone(updated)
        }
    }

    fun updateMilestoneCurrentValue(milestone: Milestone, newValue: Double) {
        val safeCurrent = newValue.coerceAtLeast(0.0)
        val isDone = safeCurrent >= milestone.targetValue
        val newProgress = (safeCurrent / milestone.targetValue).toFloat().coerceIn(0f, 1f)
        val updated = milestone.copy(
            currentValue = safeCurrent,
            status = if (isDone) MilestoneStatus.COMPLETED else if (safeCurrent > 0) MilestoneStatus.IN_PROGRESS else MilestoneStatus.PENDING,
            progress = newProgress
        )
        viewModelScope.launch {
            repository.updateMilestone(updated)
        }
    }

    fun updateMilestone(milestone: Milestone, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = repository.updateMilestone(milestone)
            result.onSuccess {
                _uiState.value = UiState.Success(Unit)
                onSuccess()
            }.onFailure { error ->
                _uiState.value = UiState.Error(error.localizedMessage ?: "Failed to update milestone")
            }
        }
    }

    fun deleteMilestone(milestoneId: String, goalId: String) {
        viewModelScope.launch {
            repository.deleteMilestone(milestoneId, goalId)
        }
    }

    fun clearError() {
        if (_uiState.value is UiState.Error) {
            _uiState.value = UiState.Idle
        }
    }
}
