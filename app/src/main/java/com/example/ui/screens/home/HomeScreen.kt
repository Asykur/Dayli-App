package com.example.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.HabitType
import com.example.core.model.Task
import com.example.core.model.UserProfile
import com.example.ui.components.THPTopBar
import com.example.ui.screens.goals.GoalCard
import com.example.ui.screens.tasks.HabitItemCard
import com.example.ui.screens.tasks.TaskItemCard
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CoralRose
import com.example.ui.theme.Emerald500
import com.example.ui.theme.SoftXpPurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userProfile: UserProfile?,
    viewModel: HomeViewModel,
    onNavigateToGoals: () -> Unit,
    onNavigateToGoalDetail: (String) -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val todayData by viewModel.todayAggregate.collectAsState()
    val focus by viewModel.todayFocus.collectAsState()

    var showSetFocusDialog by remember { mutableStateOf(false) }
    var quickAddText by remember { mutableStateOf("") }
    var quickAddIsTask by remember { mutableStateOf(true) }

    LaunchedEffect(userProfile) {
        if (userProfile != null) {
            viewModel.setUser(userProfile.id, userProfile.timezone)
        }
    }

    if (showSetFocusDialog) {
        SetDailyFocusDialog(
            currentFocus = focus?.focusText,
            todayTasks = todayData.scheduledTasks,
            onDismiss = { showSetFocusDialog = false },
            onSaveFocus = { text, taskId ->
                viewModel.setDailyFocus(text, taskId)
                showSetFocusDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            THPTopBar(
                title = "${todayData.greeting}, ${userProfile?.displayName?.split(" ")?.firstOrNull() ?: "Builder"} 👋",
                subtitle = todayData.formattedDate,
                userProfile = userProfile,
                onProfileClick = onNavigateToProfile,
                onSettingsClick = onNavigateToSettings
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToTasks,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("home_quick_add_fab")
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Add New Action")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .testTag("home_screen")
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            // 1. TODAY ENGINE HERO CARD (Deterministic Daily Progress)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("today_engine_hero_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "TODAY'S EXECUTION",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${todayData.progressPercentage}% Complete",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Circular / Tag Indicator
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "${todayData.totalCompleted} / ${todayData.totalScheduled} Done",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { todayData.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val encouragement = when {
                        todayData.totalScheduled == 0 -> "No scheduled actions yet. Capture your first action below."
                        todayData.totalCompleted == todayData.totalScheduled -> "Outstanding! 100% of today's targets achieved. Consistency wins."
                        todayData.progressPercentage >= 50 -> "Over halfway there. Maintain your momentum."
                        else -> "One action at a time. The first step creates the day's flow."
                    }

                    Text(
                        text = encouragement,
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. TODAY'S CORE FOCUS (DAILY HIGHLIGHT)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("today_focus_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (focus?.isCompleted == true)
                        Emerald500.copy(alpha = 0.12f)
                    else MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Filled.Star, contentDescription = null, tint = AmberGold, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "TODAY'S HIGHLIGHT",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                color = AmberGold
                            )
                        }

                        IconButton(
                            onClick = { showSetFocusDialog = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.Edit, contentDescription = "Edit Highlight", modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (focus != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (focus!!.isCompleted) Emerald500 else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { viewModel.toggleDailyFocus() },
                                contentAlignment = Alignment.Center
                            ) {
                                if (focus!!.isCompleted) {
                                    Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = focus!!.focusText,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = if (focus!!.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                ),
                                color = if (focus!!.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showSetFocusDialog = true },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "What is your #1 priority today?",
                                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = { showSetFocusDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AmberGold)
                            ) {
                                Text("Set Highlight", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color.Black)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. QUICK ADD BAR
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick_add_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Quick Capture",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilterChip(
                                selected = quickAddIsTask,
                                onClick = { quickAddIsTask = true },
                                label = { Text("Task") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            FilterChip(
                                selected = !quickAddIsTask,
                                onClick = { quickAddIsTask = false },
                                label = { Text("Habit") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = quickAddText,
                            onValueChange = { quickAddText = it },
                            placeholder = { Text(if (quickAddIsTask) "Add a task for today..." else "Add a daily habit...") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                if (quickAddText.isNotBlank()) {
                                    if (quickAddIsTask) {
                                        viewModel.quickAddTask(quickAddText)
                                    } else {
                                        viewModel.quickAddHabit(quickAddText)
                                    }
                                    quickAddText = ""
                                }
                            }),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_add_input")
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                if (quickAddText.isNotBlank()) {
                                    if (quickAddIsTask) {
                                        viewModel.quickAddTask(quickAddText)
                                    } else {
                                        viewModel.quickAddHabit(quickAddText)
                                    }
                                    quickAddText = ""
                                }
                            },
                            enabled = quickAddText.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(56.dp)
                                .testTag("quick_add_submit_button")
                        ) {
                            Text("Add")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. TODAY'S TASKS
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Today's Tasks (${todayData.completedTasks.size}/${todayData.scheduledTasks.size})",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                TextButton(onClick = onNavigateToTasks) {
                    Text("View Tasks")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (todayData.scheduledTasks.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No tasks scheduled for today.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(6.dp))
                        TextButton(onClick = onNavigateToTasks) {
                            Text("Schedule a Task")
                        }
                    }
                }
            } else {
                todayData.scheduledTasks.forEach { task ->
                    TaskItemCard(
                        task = task,
                        onToggleCompletion = { viewModel.toggleTask(task) },
                        onEdit = onNavigateToTasks,
                        onDelete = { /* handled in TasksScreen */ },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5. TODAY'S HABITS
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Today's Habits (${todayData.completedHabits.size}/${todayData.scheduledHabits.size})",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = onNavigateToTasks) {
                    Text("Manage Habits")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (todayData.scheduledHabits.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No habits scheduled for today.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(6.dp))
                        TextButton(onClick = onNavigateToTasks) {
                            Text("Add a Daily Habit")
                        }
                    }
                }
            } else {
                todayData.scheduledHabits.forEach { habit ->
                    val log = todayData.todayLogs[habit.id]
                    HabitItemCard(
                        habit = habit,
                        todayLog = log,
                        onToggleCompletion = { viewModel.toggleHabit(habit) },
                        onIncrement = { delta -> viewModel.updateHabitValue(habit, delta) },
                        onDecrement = { delta -> viewModel.updateHabitValue(habit, -delta) },
                        onEdit = onNavigateToTasks,
                        onDelete = { /* handled in TasksScreen */ },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 6. ACTIVE GOALS SUMMARY
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Active Goals (${todayData.activeGoals.size})",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = onNavigateToGoals) {
                    Text("View Goals")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (todayData.activeGoals.isNotEmpty()) {
                todayData.activeGoals.take(2).forEach { goal ->
                    GoalCard(
                        goal = goal,
                        onClick = { onNavigateToGoalDetail(goal.id) },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateToGoals),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Outlined.Flag, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Define what truly matters", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text("Create long-term goals and milestones to guide your daily actions.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(70.dp))
        }
    }
}
