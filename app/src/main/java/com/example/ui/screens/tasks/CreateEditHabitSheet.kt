package com.example.ui.screens.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.model.Goal
import com.example.core.model.Habit
import com.example.core.model.HabitFrequency
import com.example.core.model.HabitType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditHabitSheet(
    availableGoals: List<Goal>,
    habitToEdit: Habit? = null,
    onDismiss: () -> Unit,
    onSave: (
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
        goalTitle: String?
    ) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var name by remember { mutableStateOf(habitToEdit?.name ?: "") }
    var description by remember { mutableStateOf(habitToEdit?.description ?: "") }
    var type by remember { mutableStateOf(habitToEdit?.type ?: HabitType.BOOLEAN) }
    var targetValueStr by remember {
        mutableStateOf(habitToEdit?.targetValue?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: if (type == HabitType.DURATION) "30" else "1")
    }
    var unit by remember {
        mutableStateOf(habitToEdit?.unit ?: if (type == HabitType.DURATION) "mins" else if (type == HabitType.COUNT) "glasses" else "times")
    }
    var frequency by remember { mutableStateOf(habitToEdit?.frequency ?: HabitFrequency.DAILY) }
    var reminder by remember { mutableStateOf(habitToEdit?.reminder ?: "") }

    var selectedGoal by remember {
        mutableStateOf(availableGoals.firstOrNull { it.id == habitToEdit?.goalId })
    }
    var goalsExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
                .testTag("create_habit_sheet")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (habitToEdit == null) "Create New Habit" else "Edit Habit",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Filled.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Habit Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Habit Name *") },
                placeholder = { Text("e.g. Read 20 pages or 30 min Deep Work") },
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.Repeat, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("habit_name_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Habit Type
            Text(
                text = "Habit Type",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HabitType.entries.forEach { t ->
                    FilterChip(
                        selected = type == t,
                        onClick = {
                            type = t
                            if (t == HabitType.DURATION && targetValueStr == "1") {
                                targetValueStr = "30"
                                unit = "mins"
                            } else if (t == HabitType.BOOLEAN) {
                                targetValueStr = "1"
                                unit = "times"
                            }
                        },
                        label = { Text(t.label.split(" ").first()) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Target Value & Unit (for duration or count)
            if (type != HabitType.BOOLEAN) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = targetValueStr,
                        onValueChange = { targetValueStr = it },
                        label = { Text("Target Per Day") },
                        placeholder = { Text("30") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit") },
                        placeholder = { Text("mins, pages") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Frequency Chips
            Text(
                text = "Frequency",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HabitFrequency.entries.forEach { f ->
                    FilterChip(
                        selected = frequency == f,
                        onClick = { frequency = f },
                        label = { Text(f.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Reminder Time
            OutlinedTextField(
                value = reminder,
                onValueChange = { reminder = it },
                label = { Text("Daily Reminder (Optional)") },
                placeholder = { Text("e.g. 07:00") },
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp))
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Goal Linker Dropdown
            if (availableGoals.isNotEmpty()) {
                Text(
                    text = "Link to a Long-Term Goal",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                ExposedDropdownMenuBox(
                    expanded = goalsExpanded,
                    onExpandedChange = { goalsExpanded = !goalsExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedGoal?.title ?: "No Goal (Standalone Habit)",
                        onValueChange = {},
                        readOnly = true,
                        leadingIcon = {
                            Icon(imageVector = Icons.Filled.Flag, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = goalsExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = goalsExpanded,
                        onDismissRequest = { goalsExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None (Standalone)") },
                            onClick = {
                                selectedGoal = null
                                goalsExpanded = false
                            }
                        )
                        availableGoals.forEach { goal ->
                            DropdownMenuItem(
                                text = { Text(goal.title) },
                                onClick = {
                                    selectedGoal = goal
                                    goalsExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Notes / Habit Trigger (Optional)") },
                placeholder = { Text("e.g. Right after making morning coffee") },
                minLines = 2,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val target = targetValueStr.toDoubleOrNull() ?: 1.0
                    if (name.isNotBlank()) {
                        onSave(
                            name,
                            description,
                            selectedGoal?.category?.displayName ?: "Habits",
                            type,
                            target,
                            unit,
                            frequency,
                            reminder.ifBlank { null },
                            selectedGoal?.id,
                            null,
                            selectedGoal?.title
                        )
                    }
                },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_habit_button")
            ) {
                Text(
                    text = if (habitToEdit == null) "Create Habit" else "Save Changes",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
