package com.example.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.AppDatabase
import com.example.core.database.DailyFocusDao
import com.example.core.database.DailyFocusEntity
import com.example.core.model.DailyFocus
import com.example.core.model.Goal
import com.example.core.model.Habit
import com.example.core.model.HabitFrequency
import com.example.core.model.HabitLog
import com.example.core.model.HabitType
import com.example.core.model.RecurrenceRule
import com.example.core.model.Task
import com.example.core.model.TaskPriority
import com.example.core.model.TaskStatus
import com.example.core.model.TodayAggregate
import com.example.core.model.UiState
import com.example.core.model.calculateDailyProgress
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
import java.util.Locale
import java.util.UUID

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val taskRepository: TaskRepository
    private val habitRepository: HabitRepository
    private val goalRepository: GoalRepository
    private val dailyFocusDao: DailyFocusDao
    private val consistencyRepository: com.example.core.repository.ConsistencyRepository

    private val currentUserId = MutableStateFlow<String>("")
    private val currentTimezone = MutableStateFlow<String>("Asia/Jakarta")

    private val _uiState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val uiState: StateFlow<UiState<Unit>> = _uiState.asStateFlow()

    init {
        val db = AppDatabase.getInstance(application)
        taskRepository = TaskRepositoryImpl(db.taskDao())
        habitRepository = HabitRepositoryImpl(db.habitDao(), db.habitLogDao())
        goalRepository = GoalRepositoryImpl(db.goalDao(), db.milestoneDao())
        dailyFocusDao = db.dailyFocusDao()
        consistencyRepository = com.example.core.repository.ConsistencyRepositoryImpl(
            userDao = db.userDao(),
            taskDao = db.taskDao(),
            habitDao = db.habitDao(),
            habitLogDao = db.habitLogDao()
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

    private fun getGreeting(): String {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 4..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val todayFocus: StateFlow<DailyFocus?> = combine(currentUserId, currentTimezone) { uid, tz ->
        Pair(uid, tz)
    }.flatMapLatest { (uid, tz) ->
        if (uid.isNotBlank()) {
            val todayDate = getTodayDateString()
            dailyFocusDao.getFocusFlow(uid, todayDate)
        } else {
            flowOf(null)
        }
    }.combine(flowOf(Unit)) { entity, _ ->
        entity?.toDomain()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeGoals: StateFlow<List<Goal>> = currentUserId.flatMapLatest { uid ->
        if (uid.isNotBlank()) goalRepository.getGoalsFlow(uid) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val allTasks: StateFlow<List<Task>> = currentUserId.flatMapLatest { uid ->
        if (uid.isNotBlank()) taskRepository.getTasksFlow(uid) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val allHabits: StateFlow<List<Habit>> = currentUserId.flatMapLatest { uid ->
        if (uid.isNotBlank()) habitRepository.getActiveHabitsFlow(uid) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val todayLogs: StateFlow<Map<String, HabitLog>> = combine(currentUserId, currentTimezone) { uid, _ ->
        uid
    }.flatMapLatest { uid ->
        if (uid.isNotBlank()) {
            val todayDate = getTodayDateString()
            habitRepository.getLogsForDateFlow(uid, todayDate)
        } else {
            flowOf(emptyList())
        }
    }.combine(flowOf(Unit)) { logs, _ ->
        logs.associateBy { it.habitId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Aggregated Today Engine State
    val todayAggregate: StateFlow<TodayAggregate> = combine(
        allTasks,
        allHabits,
        todayLogs,
        todayFocus,
        activeGoals
    ) { tasks, habits, logs, focus, goals ->
        val zoneId = try { ZoneId.of(currentTimezone.value) } catch (e: Exception) { ZoneId.systemDefault() }
        val now = LocalDate.now(zoneId)
        val todayStartMs = now.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val todayEndMs = todayStartMs + (24 * 60 * 60 * 1000)
        val dayOfWeek = now.dayOfWeek.value // 1=Mon, 7=Sun
        val todayStr = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val formattedDate = now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault()))

        // Filter Today's Tasks:
        // Due today, OR overdue and not completed, OR daily recurring
        val scheduledTasks = tasks.filter { task ->
            if (task.status == TaskStatus.CANCELLED) return@filter false
            when {
                task.recurrence == RecurrenceRule.DAILY -> true
                task.recurrence == RecurrenceRule.WEEKDAYS && dayOfWeek in 1..5 -> true
                task.dueDate != null && task.dueDate in todayStartMs..todayEndMs -> true
                // Overdue incomplete tasks
                task.dueDate != null && task.dueDate < todayStartMs && task.status != TaskStatus.COMPLETED -> true
                // No due date, but in progress or pending today
                task.dueDate == null && task.status == TaskStatus.IN_PROGRESS -> true
                else -> false
            }
        }

        val completedTasks = scheduledTasks.filter { it.status == TaskStatus.COMPLETED }

        // Filter Today's Habits:
        val scheduledHabits = habits.filter { habit ->
            if (!habit.isActive) return@filter false
            when (habit.frequency) {
                HabitFrequency.DAILY -> true
                HabitFrequency.WEEKDAYS -> dayOfWeek in 1..5
                HabitFrequency.WEEKLY_TARGET -> true
            }
        }

        val completedHabits = scheduledHabits.filter { habit ->
            logs[habit.id]?.completed == true
        }

        val progress = calculateDailyProgress(
            scheduledTasksCount = scheduledTasks.size,
            completedTasksCount = completedTasks.size,
            scheduledHabitsCount = scheduledHabits.size,
            completedHabitsCount = completedHabits.size
        )

        val progressPercentage = (progress * 100).toInt()

        TodayAggregate(
            date = todayStr,
            formattedDate = formattedDate,
            greeting = getGreeting(),
            scheduledTasks = scheduledTasks,
            completedTasks = completedTasks,
            scheduledHabits = scheduledHabits,
            completedHabits = completedHabits,
            todayLogs = logs,
            totalScheduled = scheduledTasks.size + scheduledHabits.size,
            totalCompleted = completedTasks.size + completedHabits.size,
            progress = progress,
            progressPercentage = progressPercentage,
            dailyFocus = focus,
            activeGoals = goals.filter { it.status == com.example.core.model.GoalStatus.ACTIVE }
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        TodayAggregate(
            date = "",
            formattedDate = "Today",
            greeting = getGreeting(),
            scheduledTasks = emptyList(),
            completedTasks = emptyList(),
            scheduledHabits = emptyList(),
            completedHabits = emptyList(),
            todayLogs = emptyMap(),
            totalScheduled = 0,
            totalCompleted = 0,
            progress = 0f,
            progressPercentage = 0,
            dailyFocus = null,
            activeGoals = emptyList()
        )
    )

    fun toggleTask(task: Task) {
        val newStatus = if (task.status == TaskStatus.COMPLETED) TaskStatus.PENDING else TaskStatus.COMPLETED
        viewModelScope.launch {
            taskRepository.updateTaskStatus(task.id, newStatus)
            if (newStatus == TaskStatus.COMPLETED) {
                consistencyRepository.awardXp(currentUserId.value, com.example.core.model.XpRewards.TASK_COMPLETION, "Task completion")
                consistencyRepository.evaluateDailyStreak(currentUserId.value, getTodayDateString(), true)
            }
            // If this task was today's focus, sync focus completion!
            val focus = todayFocus.value
            if (focus != null && focus.taskId == task.id) {
                dailyFocusDao.updateCompletion(
                    userId = currentUserId.value,
                    date = getTodayDateString(),
                    completed = (newStatus == TaskStatus.COMPLETED),
                    updatedAt = System.currentTimeMillis()
                )
            }
        }
    }

    fun toggleHabit(habit: Habit) {
        val today = getTodayDateString()
        val currentLog = todayLogs.value[habit.id]
        val isCompleted = !(currentLog?.completed ?: false)
        val value = if (isCompleted) habit.targetValue else 0.0

        viewModelScope.launch {
            habitRepository.logHabit(
                habitId = habit.id,
                userId = currentUserId.value,
                date = today,
                completed = isCompleted,
                value = value,
                targetValue = habit.targetValue,
                unit = habit.unit
            )
            if (isCompleted) {
                consistencyRepository.awardXp(currentUserId.value, com.example.core.model.XpRewards.HABIT_COMPLETION, "Habit completion")
                consistencyRepository.evaluateDailyStreak(currentUserId.value, today, true)
            }
        }
    }

    fun updateHabitValue(habit: Habit, delta: Double) {
        val today = getTodayDateString()
        val currentLog = todayLogs.value[habit.id]
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

    fun setDailyFocus(text: String, taskId: String? = null) {
        val uid = currentUserId.value
        if (uid.isBlank() || text.isBlank()) return
        val date = getTodayDateString()
        val existing = todayFocus.value

        val entity = DailyFocusEntity(
            id = existing?.id ?: UUID.randomUUID().toString(),
            userId = uid,
            date = date,
            focusText = text.trim(),
            taskId = taskId,
            isCompleted = existing?.isCompleted ?: false,
            updatedAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            dailyFocusDao.insertOrUpdate(entity)
        }
    }

    fun toggleDailyFocus() {
        val focus = todayFocus.value ?: return
        val newStatus = !focus.isCompleted
        viewModelScope.launch {
            dailyFocusDao.updateCompletion(
                userId = currentUserId.value,
                date = focus.date,
                completed = newStatus,
                updatedAt = System.currentTimeMillis()
            )
            // If linked to a task, toggle that task as well
            if (focus.taskId != null) {
                taskRepository.updateTaskStatus(
                    focus.taskId,
                    if (newStatus) TaskStatus.COMPLETED else TaskStatus.PENDING
                )
            }
        }
    }

    fun quickAddTask(
        title: String,
        priority: TaskPriority = TaskPriority.MEDIUM,
        estimatedMinutes: Int = 25,
        goalId: String? = null,
        goalTitle: String? = null
    ) {
        val uid = currentUserId.value
        if (title.isBlank() || uid.isBlank()) return

        val zoneId = try { ZoneId.of(currentTimezone.value) } catch (e: Exception) { ZoneId.systemDefault() }
        val todayMs = LocalDate.now(zoneId).atStartOfDay(zoneId).toInstant().toEpochMilli()

        val task = Task(
            id = UUID.randomUUID().toString(),
            userId = uid,
            title = title.trim(),
            status = TaskStatus.PENDING,
            priority = priority,
            dueDate = todayMs,
            estimatedMinutes = estimatedMinutes,
            goalId = goalId,
            goalTitle = goalTitle
        )

        viewModelScope.launch {
            taskRepository.createTask(task)
        }
    }

    fun quickAddHabit(
        name: String,
        type: HabitType = HabitType.BOOLEAN,
        targetValue: Double = 1.0,
        unit: String = "times",
        goalId: String? = null,
        goalTitle: String? = null
    ) {
        val uid = currentUserId.value
        if (name.isBlank() || uid.isBlank()) return

        val habit = Habit(
            id = UUID.randomUUID().toString(),
            userId = uid,
            name = name.trim(),
            type = type,
            targetValue = targetValue,
            unit = unit,
            frequency = HabitFrequency.DAILY,
            goalId = goalId,
            goalTitle = goalTitle,
            isActive = true
        )

        viewModelScope.launch {
            habitRepository.createHabit(habit)
        }
    }

    fun clearError() {
        if (_uiState.value is UiState.Error) {
            _uiState.value = UiState.Idle
        }
    }
}
