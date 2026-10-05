package com.example.ui.screens.goals

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.PlaylistAddCheck
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.Goal
import com.example.core.model.GoalStatus
import com.example.core.model.Milestone
import com.example.core.model.MilestoneStatus
import com.example.ui.components.EmptyStateView
import com.example.ui.components.LoadingView
import com.example.ui.theme.AmberGold
import com.example.ui.theme.Emerald500
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailScreen(
    goalId: String,
    viewModel: GoalViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val goal by viewModel.selectedGoal.collectAsState()
    val milestones by viewModel.selectedGoalMilestones.collectAsState()

    var showEditSheet by remember { mutableStateOf(false) }
    var showAddMilestoneSheet by remember { mutableStateOf(false) }
    var milestoneToEdit by remember { mutableStateOf<Milestone?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Goal") },
            text = { Text("Are you sure you want to delete this goal and all its milestones? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deleteGoal(goalId) {
                            onNavigateBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showEditSheet && goal != null) {
        CreateEditGoalSheet(
            goalToEdit = goal,
            onDismiss = { showEditSheet = false },
            onSave = { title, desc, why, cat, deadline ->
                viewModel.updateGoal(
                    goal!!.copy(
                        title = title,
                        description = desc,
                        why = why,
                        category = cat,
                        deadline = deadline
                    )
                ) {
                    showEditSheet = false
                }
            }
        )
    }

    if (showAddMilestoneSheet || milestoneToEdit != null) {
        CreateMilestoneSheet(
            goalId = goalId,
            milestoneToEdit = milestoneToEdit,
            onDismiss = {
                showAddMilestoneSheet = false
                milestoneToEdit = null
            },
            onSave = { title, desc, target, current, unit, deadline ->
                if (milestoneToEdit == null) {
                    viewModel.createMilestone(
                        goalId = goalId,
                        title = title,
                        description = desc,
                        targetValue = target,
                        currentValue = current,
                        unit = unit,
                        deadline = deadline
                    ) {
                        showAddMilestoneSheet = false
                    }
                } else {
                    viewModel.updateMilestone(
                        milestoneToEdit!!.copy(
                            title = title,
                            description = desc,
                            targetValue = target,
                            currentValue = current,
                            unit = unit,
                            deadline = deadline
                        )
                    ) {
                        milestoneToEdit = null
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = goal?.category?.displayName ?: "Goal Detail",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showEditSheet = true }) {
                            Icon(imageVector = Icons.Filled.Edit, contentDescription = "Edit Goal")
                        }

                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "More Options")
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                if (goal?.status != GoalStatus.COMPLETED) {
                                    DropdownMenuItem(
                                        text = { Text("Mark as Completed") },
                                        leadingIcon = { Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Emerald500) },
                                        onClick = {
                                            showMenu = false
                                            goal?.let { viewModel.updateGoalStatus(it, GoalStatus.COMPLETED) }
                                        }
                                    )
                                }
                                if (goal?.status == GoalStatus.ACTIVE) {
                                    DropdownMenuItem(
                                        text = { Text("Pause Goal") },
                                        leadingIcon = { Icon(Icons.Filled.PauseCircle, contentDescription = null, tint = AmberGold) },
                                        onClick = {
                                            showMenu = false
                                            goal?.let { viewModel.updateGoalStatus(it, GoalStatus.PAUSED) }
                                        }
                                    )
                                } else if (goal?.status == GoalStatus.PAUSED) {
                                    DropdownMenuItem(
                                        text = { Text("Resume Goal") },
                                        leadingIcon = { Icon(Icons.Filled.PlayCircle, contentDescription = null, tint = Emerald500) },
                                        onClick = {
                                            showMenu = false
                                            goal?.let { viewModel.updateGoalStatus(it, GoalStatus.ACTIVE) }
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Delete Goal", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showMenu = false
                                        showDeleteConfirmDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (goal == null) {
            LoadingView(message = "Loading goal details...")
        } else {
            val currentGoal = goal!!
            val progressPercent = (currentGoal.progress * 100).toInt()
            val formattedDeadline = currentGoal.deadline?.let {
                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault()))
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
                    .testTag("goal_detail_screen")
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Title & Status
                Text(
                    text = currentGoal.title,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (formattedDeadline != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Filled.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Target: $formattedDeadline",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // The "Why" Card (Core Motivator)
                if (currentGoal.why.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.FormatQuote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "THE \"WHY\" BEHIND THIS GOAL",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = currentGoal.why,
                                style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Progress Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Deterministic Goal Progress",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$progressPercent%",
                                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "${currentGoal.completedMilestoneCount} / ${currentGoal.milestoneCount} milestones",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        LinearProgressIndicator(
                            progress = { currentGoal.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Milestones Section Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Milestones (${milestones.size})",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Button(
                        onClick = { showAddMilestoneSheet = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("add_milestone_button")
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Milestone")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Milestones List
                if (milestones.isEmpty()) {
                    EmptyStateView(
                        title = "No milestones yet",
                        subtitle = "Break this big goal down into smaller, verifiable milestones.",
                        actionLabel = "Add First Milestone",
                        onAction = { showAddMilestoneSheet = true }
                    )
                } else {
                    milestones.forEach { milestone ->
                        MilestoneItemCard(
                            milestone = milestone,
                            onToggleCompletion = { viewModel.toggleMilestoneCompletion(milestone) },
                            onIncrement = {
                                viewModel.updateMilestoneCurrentValue(milestone, milestone.currentValue + 1.0)
                            },
                            onDecrement = {
                                viewModel.updateMilestoneCurrentValue(milestone, (milestone.currentValue - 1.0).coerceAtLeast(0.0))
                            },
                            onEdit = { milestoneToEdit = milestone },
                            onDelete = { viewModel.deleteMilestone(milestone.id, goalId) }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Connected Daily Actions & Habits Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PlaylistAddCheck,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Connected Daily Actions & Habits",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Tasks and habits linked to this goal appear on your Today dashboard and automatically contribute to your real-life consistency.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}

@Composable
private fun MilestoneItemCard(
    milestone: Milestone,
    onToggleCompletion: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCompleted = milestone.status == MilestoneStatus.COMPLETED
    val progressPercent = (milestone.progress * 100).toInt()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("milestone_item_${milestone.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (isCompleted) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable(onClick = onToggleCompletion),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompleted) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Completed",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = milestone.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                            ),
                            color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        )
                        if (milestone.description.isNotBlank()) {
                            Text(
                                text = milestone.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Filled.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Outlined.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Stepper and Target unit
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Stepper Buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    IconButton(onClick = onDecrement, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Filled.Remove, contentDescription = "Decrement", modifier = Modifier.size(14.dp))
                    }
                    val currentValStr = if (milestone.currentValue % 1.0 == 0.0) milestone.currentValue.toLong().toString() else milestone.currentValue.toString()
                    val targetValStr = if (milestone.targetValue % 1.0 == 0.0) milestone.targetValue.toLong().toString() else milestone.targetValue.toString()

                    Text(
                        text = "$currentValStr / $targetValStr ${milestone.unit}".trim(),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                    IconButton(onClick = onIncrement, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = "Increment", modifier = Modifier.size(14.dp))
                    }
                }

                Text(
                    text = "$progressPercent%",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { milestone.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}
