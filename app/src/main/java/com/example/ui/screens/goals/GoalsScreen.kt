package com.example.ui.screens.goals

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.unit.sp
import com.example.core.model.Goal
import com.example.core.model.GoalStatus
import com.example.core.model.UiState
import com.example.core.model.UserProfile
import com.example.ui.components.EmptyStateView
import com.example.ui.components.THPTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    userProfile: UserProfile?,
    viewModel: GoalViewModel,
    onNavigateToGoalDetail: (String) -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val goals by viewModel.filteredGoals.collectAsState()
    val allGoals by viewModel.goals.collectAsState()
    val activeCount by viewModel.activeGoalsCount.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var showCreateSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userProfile) {
        userProfile?.id?.let { viewModel.setUserId(it) }
    }

    LaunchedEffect(uiState) {
        if (uiState is UiState.Error) {
            snackbarHostState.showSnackbar((uiState as UiState.Error).message)
            viewModel.clearError()
        }
    }

    if (showCreateSheet) {
        CreateEditGoalSheet(
            onDismiss = { showCreateSheet = false },
            onSave = { title, desc, why, cat, deadline ->
                viewModel.createGoal(
                    title = title,
                    description = desc,
                    why = why,
                    category = cat,
                    deadline = deadline
                ) { newGoal ->
                    showCreateSheet = false
                    onNavigateToGoalDetail(newGoal.id)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            THPTopBar(
                title = "Long-Term Goals",
                subtitle = "Turn ambitions into actionable milestones",
                userProfile = userProfile,
                onProfileClick = onNavigateToProfile,
                onSettingsClick = onNavigateToSettings
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("goals_add_fab")
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Add Goal")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .testTag("goals_screen")
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            // Filter Chips Row & Active Counter
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GoalFilter.entries.forEach { filter ->
                        val isSelected = selectedFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setFilter(filter) },
                            label = { Text(filter.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                Text(
                    text = "$activeCount/3 Active",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (activeCount >= 3) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Free Plan Limit Banner (Section 28 & 30)
            if (activeCount >= 3 && selectedFilter == GoalFilter.ACTIVE) {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Free plan includes up to 3 active goals. Pause or complete a goal to create more.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (goals.isEmpty()) {
                val isFilterEmpty = allGoals.isNotEmpty()
                EmptyStateView(
                    title = if (isFilterEmpty) "No ${selectedFilter.name.lowercase()} goals" else "What would you like to improve?",
                    subtitle = if (isFilterEmpty)
                        "Try selecting another filter or create a new goal."
                    else "Connect your ambitions to milestones and realistic daily habits. Consistency beats sporadic intensity.",
                    actionLabel = "Create Goal",
                    onAction = { showCreateSheet = true },
                    icon = Icons.Outlined.Flag
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items = goals, key = { it.id }) { goal ->
                        GoalCard(
                            goal = goal,
                            onClick = {
                                viewModel.selectGoal(goal.id)
                                onNavigateToGoalDetail(goal.id)
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp)) // padding for bottom nav & FAB
                    }
                }
            }
        }
    }
}
