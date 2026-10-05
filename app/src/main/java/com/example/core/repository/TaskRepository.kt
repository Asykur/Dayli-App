package com.example.core.repository

import com.example.core.model.Task
import com.example.core.model.TaskStatus
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun getTasksFlow(userId: String): Flow<List<Task>>

    fun getPendingTasksFlow(userId: String): Flow<List<Task>>

    fun getTasksForGoalFlow(goalId: String): Flow<List<Task>>

    suspend fun getTaskById(taskId: String): Task?

    suspend fun createTask(task: Task): Result<Task>

    suspend fun updateTask(task: Task): Result<Task>

    suspend fun updateTaskStatus(taskId: String, status: TaskStatus): Result<Unit>

    suspend fun deleteTask(taskId: String): Result<Unit>
}
