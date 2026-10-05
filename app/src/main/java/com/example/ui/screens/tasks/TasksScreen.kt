package com.example.ui.screens.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.model.Habit
import com.example.core.model.Task
import com.example.core.model.UiState
import com.example.core.model.UserProfile
import com.example.ui.components.EmptyStateView
import com.example.ui.components.THPTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    userProfile: UserProfile?,
    viewModel: TaskHabitViewModel,
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val taskFilter by viewModel.taskFilter.collectAsState()
    val habitFilter by viewModel.habitFilter.collectAsState()

    val tasks by viewModel.filteredTasks.collectAsState()
    val habits by viewModel.filteredHabits.collectAsState()
    val activeHabitsCount by viewModel.activeHabitsCount.collectAsState()
    val todayLogs by viewModel.todayHabitLogs.collectAsState()
    val availableGoals by viewModel.availableGoals.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var showCreateTaskSheet by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }

    var showCreateHabitSheet by remember { mutableStateOf(false) }
    var habitToEdit by remember { mutableStateOf<Habit?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userProfile) {
        if (userProfile != null) {
            viewModel.setUser(userProfile.id, userProfile.timezone)
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is UiState.Error) {
            snackbarHostState.showSnackbar((uiState as UiState.Error).message)
            viewModel.clearError()
        }
    }

    if (showCreateTaskSheet || taskToEdit != null) {
        CreateEditTaskSheet(
            availableGoals = availableGoals,
            taskToEdit = taskToEdit,
            onDismiss = {
                showCreateTaskSheet = false
                taskToEdit = null
            },
            onSave = { title, desc, prio, dueD, dueT, est, cat, gId, mId, gTitle, rec ->
                if (taskToEdit == null) {
                    viewModel.createTask(
                        title = title,
                        description = desc,
                        priority = prio,
                        dueDate = dueD,
                        dueTime = dueT,
                        estimatedMinutes = est,
                        category = cat,
                        goalId = gId,
                        milestoneId = mId,
                        goalTitle = gTitle,
                        recurrence = rec
                    ) {
                        showCreateTaskSheet = false
                    }
                } else {
                    viewModel.updateTask(
                        taskToEdit!!.copy(
                            title = title,
                            description = desc,
                            priority = prio,
                            dueDate = dueD,
                            dueTime = dueT,
                            estimatedMinutes = est,
                            category = cat,
                            goalId = gId,
                            milestoneId = mId,
                            goalTitle = gTitle,
                            recurrence = rec
                        )
                    ) {
                        taskToEdit = null
                    }
                }
            }
        )
    }

    if (showCreateHabitSheet || habitToEdit != null) {
        CreateEditHabitSheet(
            availableGoals = availableGoals,
            habitToEdit = habitToEdit,
            onDismiss = {
                showCreateHabitSheet = false
                habitToEdit = null
            },
            onSave = { name, desc, cat, type, target, unit, freq, rem, gId, mId, gTitle ->
                if (habitToEdit == null) {
                    viewModel.createHabit(
                        name = name,
                        description = desc,
                        category = cat,
                        type = type,
                        targetValue = target,
                        unit = unit,
                        frequency = freq,
                        reminder = rem,
                        goalId = gId,
                        milestoneId = mId,
                        goalTitle = gTitle
                    ) {
                        showCreateHabitSheet = false
                    }
                } else {
                    viewModel.updateHabit(
                        habitToEdit!!.copy(
                            name = name,
                            description = desc,
                            category = cat,
                            type = type,
                            targetValue = target,
                            unit = unit,
                            frequency = freq,
                            reminder = rem,
                            goalId = gId,
                            milestoneId = mId,
                            goalTitle = gTitle
                        )
                    ) {
                        habitToEdit = null
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            THPTopBar(
                title = "Tasks & Habits",
                subtitle = "Turn ambitions into daily execution",
                userProfile = userProfile,
                onProfileClick = onNavigateToProfile,
                onSettingsClick = onNavigateToSettings
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == TasksHabitsTab.TASKS) {
                        showCreateTaskSheet = true
                    } else {
                        showCreateHabitSheet = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("tasks_add_fab")
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = if (selectedTab == TasksHabitsTab.TASKS) "Add Task" else "Add Habit"
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("tasks_screen")
        ) {
            // Tab Header: Tasks vs Habits
            PrimaryTabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == TasksHabitsTab.TASKS,
                    onClick = { viewModel.setTab(TasksHabitsTab.TASKS) },
                    text = { Text("Tasks", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_tasks")
                )
                Tab(
                    selected = selectedTab == TasksHabitsTab.HABITS,
                    onClick = { viewModel.setTab(TasksHabitsTab.HABITS) },
                    text = { Text("Habits", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_habits")
                )
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Spacer(modifier = Modifier.height(12.dp))

                if (selectedTab == TasksHabitsTab.TASKS) {
                    // Task Filters
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TaskFilter.entries.forEach { f ->
                            val isSelected = taskFilter == f
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setTaskFilter(f) },
                                label = { Text(f.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (tasks.isEmpty()) {
                        EmptyStateView(
                            title = "You're all caught up 🎉",
                            subtitle = "No tasks in this view. Create a task to schedule your daily focus.",
                            actionLabel = "Create Task",
                            onAction = { showCreateTaskSheet = true },
                            icon = Icons.AutoMirrored.Outlined.List
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(items = tasks, key = { it.id }) { task ->
                                TaskItemCard(
                                    task = task,
                                    onToggleCompletion = { viewModel.toggleTaskCompletion(task) },
                                    onEdit = { taskToEdit = task },
                                    onDelete = { viewModel.deleteTask(task.id) }
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        }
                    }
                } else {
                    // Habit Filters & Active Limit Indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HabitFilter.entries.forEach { f ->
                                val isSelected = habitFilter == f
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setHabitFilter(f) },
                                    label = { Text(f.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }

                        Text(
                            text = "$activeHabitsCount/5 Habits",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (activeHabitsCount >= 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Free plan 5 habits limit banner
                    if (activeHabitsCount >= 5 && habitFilter == HabitFilter.ACTIVE) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Free plan allows up to 5 active habits. Pause a habit to activate new ones.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (habits.isEmpty()) {
                        EmptyStateView(
                            title = "Small habits become big changes.",
                            subtitle = "Build daily momentum with consistent habits linked to your goals.",
                            actionLabel = "Create Habit",
                            onAction = { showCreateHabitSheet = true },
                            icon = Icons.Filled.Repeat
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(items = habits, key = { it.id }) { habit ->
                                val log = todayLogs[habit.id]
                                HabitItemCard(
                                    habit = habit,
                                    todayLog = log,
                                    onToggleCompletion = { viewModel.toggleHabitCompletionToday(habit) },
                                    onIncrement = { delta -> viewModel.updateHabitValueToday(habit, delta) },
                                    onDecrement = { delta -> viewModel.updateHabitValueToday(habit, -delta) },
                                    onEdit = { habitToEdit = habit },
                                    onDelete = { viewModel.deleteHabit(habit.id) }
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
