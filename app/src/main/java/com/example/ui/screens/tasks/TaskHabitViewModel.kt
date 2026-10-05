package com.example.ui.screens.tasks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.AppDatabase
import com.example.core.model.Goal
import com.example.core.model.Habit
import com.example.core.model.HabitFrequency
import com.example.core.model.HabitLog
import com.example.core.model.HabitType
import com.example.core.model.Milestone
import com.example.core.model.RecurrenceRule
import com.example.core.model.Task
import com.example.core.model.TaskPriority
import com.example.core.model.TaskStatus
import com.example.core.model.UiState
import com.example.core.repository.GoalRepository
import com.example.core.repository.GoalRepositoryImpl
import com.example.core.repository.HabitRepository
import com.example.core.repository.HabitRepositoryImpl
import com.example.core.repository.TaskRepository
import com.example.core.repository.TaskRepositoryImpl
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

enum class TasksHabitsTab {
    TASKS,
    HABITS
}

enum class TaskFilter {
    ALL,
    PENDING,
    TODAY,
    COMPLETED
}

enum class HabitFilter {
    ALL,
    ACTIVE,
    TODAY
}

class TaskHabitViewModel(application: Application) : AndroidViewModel(application) {

    private val taskRepository: TaskRepository
    private val habitRepository: HabitRepository
    private val goalRepository: GoalRepository
    private val consistencyRepository: com.example.core.repository.ConsistencyRepository

    private val currentUserId = MutableStateFlow<String>("")
    private val currentTimezone = MutableStateFlow<String>("Asia/Jakarta")

    private val _selectedTab = MutableStateFlow(TasksHabitsTab.TASKS)
    val selectedTab: StateFlow<TasksHabitsTab> = _selectedTab.asStateFlow()

    private val _taskFilter = MutableStateFlow(TaskFilter.PENDING)
    val taskFilter: StateFlow<TaskFilter> = _taskFilter.asStateFlow()

    private val _habitFilter = MutableStateFlow(HabitFilter.ACTIVE)
    val habitFilter: StateFlow<HabitFilter> = _habitFilter.asStateFlow()

    private val _uiState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val uiState: StateFlow<UiState<Unit>> = _uiState.asStateFlow()

    init {
        val database = AppDatabase.getInstance(application)
        taskRepository = TaskRepositoryImpl(database.taskDao())
        habitRepository = HabitRepositoryImpl(database.habitDao(), database.habitLogDao())
        goalRepository = GoalRepositoryImpl(database.goalDao(), database.milestoneDao())
        consistencyRepository = com.example.core.repository.ConsistencyRepositoryImpl(
            userDao = database.userDao(),
            taskDao = database.taskDao(),
            habitDao = database.habitDao(),
            habitLogDao = database.habitLogDao()
        )
    }

    fun setUser(userId: String, timezone: String) {
        if (currentUserId.value != userId) {
            currentUserId.value = userId
        }
        if (currentTimezone.value != timezone) {
            currentTimezone.value = timezone
        }
    }

    private fun getTodayDateString(): String {
        return try {
            val zoneId = ZoneId.of(currentTimezone.value)
            LocalDate.now(zoneId).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        } catch (e: Exception) {
            LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        }
    }

    // Available Goals for linking
    @OptIn(ExperimentalCoroutinesApi::class)
    val availableGoals: StateFlow<List<Goal>> = currentUserId.flatMapLatest { uid ->
        if (uid.isNotBlank()) goalRepository.getGoalsFlow(uid) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All Tasks
    @OptIn(ExperimentalCoroutinesApi::class)
    val allTasks: StateFlow<List<Task>> = currentUserId.flatMapLatest { uid ->
        if (uid.isNotBlank()) taskRepository.getTasksFlow(uid) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All Habits
    @OptIn(ExperimentalCoroutinesApi::class)
    val allHabits: StateFlow<List<Habit>> = currentUserId.flatMapLatest { uid ->
        if (uid.isNotBlank()) habitRepository.getHabitsFlow(uid) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeHabitsCount: StateFlow<Int> = allHabits.combine(flowOf(Unit)) { list, _ ->
        list.count { it.isActive }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Today's Habit Logs
    @OptIn(ExperimentalCoroutinesApi::class)
    val todayHabitLogs: StateFlow<Map<String, HabitLog>> = currentUserId.flatMapLatest { uid ->
        if (uid.isNotBlank()) {
            val todayDate = getTodayDateString()
            habitRepository.getLogsForDateFlow(uid, todayDate)
        } else {
            flowOf(emptyList())
        }
    }.combine(flowOf(Unit)) { logs, _ ->
        logs.associateBy { it.habitId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Filtered Tasks
    val filteredTasks: StateFlow<List<Task>> = combine(allTasks, _taskFilter) { list, filter ->
        val todayStart = try {
            val zoneId = ZoneId.of(currentTimezone.value)
            LocalDate.now(zoneId).atStartOfDay(zoneId).toInstant().toEpochMilli()
        } catch (e: Exception) {
            0L
        }
        val todayEnd = todayStart + (24 * 60 * 60 * 1000)

        when (filter) {
            TaskFilter.ALL -> list
            TaskFilter.PENDING -> list.filter { it.status == TaskStatus.PENDING || it.status == TaskStatus.IN_PROGRESS }
            TaskFilter.TODAY -> list.filter { task ->
                task.dueDate != null && task.dueDate in todayStart..todayEnd
            }
            TaskFilter.COMPLETED -> list.filter { it.status == TaskStatus.COMPLETED }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Habits
    val filteredHabits: StateFlow<List<Habit>> = combine(allHabits, _habitFilter) { list, filter ->
        when (filter) {
            HabitFilter.ALL -> list
            HabitFilter.ACTIVE -> list.filter { it.isActive }
            HabitFilter.TODAY -> {
                val currentDayOfWeek = try {
                    val zoneId = ZoneId.of(currentTimezone.value)
                    LocalDate.now(zoneId).dayOfWeek.value // 1=Mon, 7=Sun
                } catch (e: Exception) { 1 }

                list.filter { habit ->
                    habit.isActive && when (habit.frequency) {
                        HabitFrequency.DAILY -> true
                        HabitFrequency.WEEKDAYS -> currentDayOfWeek in 1..5
                        HabitFrequency.WEEKLY_TARGET -> true
                    }
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setTab(tab: TasksHabitsTab) {
        _selectedTab.value = tab
    }

    fun setTaskFilter(filter: TaskFilter) {
        _taskFilter.value = filter
    }

    fun setHabitFilter(filter: HabitFilter) {
        _habitFilter.value = filter
    }

    fun createTask(
        title: String,
        description: String,
        priority: TaskPriority,
        dueDate: Long?,
        dueTime: String?,
        estimatedMinutes: Int,
        category: String,
        goalId: String?,
        milestoneId: String?,
        goalTitle: String?,
        recurrence: RecurrenceRule,
        onSuccess: () -> Unit
    ) {
        val uid = currentUserId.value
        if (title.isBlank()) {
            _uiState.value = UiState.Error("Task title cannot be empty")
            return
        }

        val task = Task(
            id = UUID.randomUUID().toString(),
            userId = uid,
            title = title.trim(),
            description = description.trim(),
            status = TaskStatus.PENDING,
            priority = priority,
            dueDate = dueDate,
            dueTime = dueTime,
            estimatedMinutes = estimatedMinutes,
            category = category,
            goalId = goalId,
            milestoneId = milestoneId,
            goalTitle = goalTitle,
            recurrence = recurrence,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = taskRepository.createTask(task)
            result.onSuccess {
                _uiState.value = UiState.Success(Unit)
                onSuccess()
            }.onFailure { error ->
                _uiState.value = UiState.Error(error.localizedMessage ?: "Failed to create task")
            }
        }
    }

    fun updateTask(task: Task, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = taskRepository.updateTask(task)
            result.onSuccess {
                _uiState.value = UiState.Success(Unit)
                onSuccess()
            }.onFailure { error ->
                _uiState.value = UiState.Error(error.localizedMessage ?: "Failed to update task")
            }
        }
    }

    fun toggleTaskCompletion(task: Task) {
        val newStatus = if (task.status == TaskStatus.COMPLETED) TaskStatus.PENDING else TaskStatus.COMPLETED
        viewModelScope.launch {
            taskRepository.updateTaskStatus(task.id, newStatus)
            if (newStatus == TaskStatus.COMPLETED) {
                consistencyRepository.awardXp(currentUserId.value, com.example.core.model.XpRewards.TASK_COMPLETION, "Task completion")
                consistencyRepository.evaluateDailyStreak(currentUserId.value, getTodayDateString(), true)
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            taskRepository.deleteTask(taskId)
        }
    }

    fun createHabit(
        name: String,
        description: String,
        category: String,
        type: HabitType,
        targetValue: Double,
        unit: String,
        frequency: HabitFrequency,
        reminder: String?,
        goalId: String?,
        milestoneId: String?,
        goalTitle: String?,
        onSuccess: () -> Unit
    ) {
        val uid = currentUserId.value
        if (name.isBlank()) {
            _uiState.value = UiState.Error("Habit name cannot be empty")
            return
        }

        val habit = Habit(
            id = UUID.randomUUID().toString(),
            userId = uid,
            name = name.trim(),
            description = description.trim(),
            category = category,
            type = type,
            targetValue = if (targetValue <= 0.0) 1.0 else targetValue,
            unit = unit.trim().ifBlank { if (type == HabitType.DURATION) "mins" else "times" },
            frequency = frequency,
            reminder = reminder,
            goalId = goalId,
            milestoneId = milestoneId,
            goalTitle = goalTitle,
            isActive = true
        )

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = habitRepository.createHabit(habit)
            result.onSuccess {
                _uiState.value = UiState.Success(Unit)
                onSuccess()
            }.onFailure { error ->
                _uiState.value = UiState.Error(error.localizedMessage ?: "Failed to create habit")
            }
        }
    }

    fun updateHabit(habit: Habit, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = habitRepository.updateHabit(habit)
            result.onSuccess {
                _uiState.value = UiState.Success(Unit)
                onSuccess()
            }.onFailure { error ->
                _uiState.value = UiState.Error(error.localizedMessage ?: "Failed to update habit")
            }
        }
    }

    fun toggleHabitCompletionToday(habit: Habit) {
        val today = getTodayDateString()
        val currentLog = todayHabitLogs.value[habit.id]
        val isNowCompleted = !(currentLog?.completed ?: false)
        val value = if (isNowCompleted) habit.targetValue else 0.0

        viewModelScope.launch {
            habitRepository.logHabit(
                habitId = habit.id,
                userId = currentUserId.value,
                date = today,
                completed = isNowCompleted,
                value = value,
                targetValue = habit.targetValue,
                unit = habit.unit
            )
            if (isNowCompleted) {
                consistencyRepository.awardXp(currentUserId.value, com.example.core.model.XpRewards.HABIT_COMPLETION, "Habit completion")
                consistencyRepository.evaluateDailyStreak(currentUserId.value, today, true)
            }
        }
    }

    fun updateHabitValueToday(habit: Habit, delta: Double) {
        val today = getTodayDateString()
        val currentLog = todayHabitLogs.value[habit.id]
        val currentValue = currentLog?.value ?: 0.0
        val newValue = (currentValue + delta).coerceAtLeast(0.0)
        val isCompleted = newValue >= habit.targetValue

        viewModelScope.launch {
            habitRepository.logHabit(
                habitId = habit.id,
                userId = currentUserId.value,
                date = today,
                completed = isCompleted,
                value = newValue,
                targetValue = habit.targetValue,
                unit = habit.unit
            )
        }
    }

    fun deleteHabit(habitId: String) {
        viewModelScope.launch {
            habitRepository.deleteHabit(habitId)
        }
    }

    fun clearError() {
        if (_uiState.value is UiState.Error) {
            _uiState.value = UiState.Idle
        }
    }
}
