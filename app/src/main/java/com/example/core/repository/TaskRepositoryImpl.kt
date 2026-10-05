package com.example.core.repository

import android.util.Log
import com.example.core.database.TaskDao
import com.example.core.database.TaskEntity
import com.example.core.firebase.FirebaseManager
import com.example.core.model.Task
import com.example.core.model.TaskStatus
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class TaskRepositoryImpl(
    private val taskDao: TaskDao
) : TaskRepository {

    private val tag = "TaskRepository"

    override fun getTasksFlow(userId: String): Flow<List<Task>> {
        return taskDao.getTasksFlow(userId).map { list -> list.map { it.toDomain() } }
    }

    override fun getPendingTasksFlow(userId: String): Flow<List<Task>> {
        return taskDao.getPendingTasksFlow(userId).map { list -> list.map { it.toDomain() } }
    }

    override fun getTasksForGoalFlow(goalId: String): Flow<List<Task>> {
        return taskDao.getTasksForGoalFlow(goalId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getTaskById(taskId: String): Task? = withContext(Dispatchers.IO) {
        taskDao.getTaskById(taskId)?.toDomain()
    }

    override suspend fun createTask(task: Task): Result<Task> = withContext(Dispatchers.IO) {
        try {
            taskDao.insertOrUpdate(TaskEntity.fromDomain(task))
            syncTaskToFirestore(task)
            Result.success(task)
        } catch (e: Exception) {
            Log.e(tag, "Failed to create task: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun updateTask(task: Task): Result<Task> = withContext(Dispatchers.IO) {
        try {
            val updated = task.copy(updatedAt = System.currentTimeMillis())
            taskDao.insertOrUpdate(TaskEntity.fromDomain(updated))
            syncTaskToFirestore(updated)
            Result.success(updated)
        } catch (e: Exception) {
            Log.e(tag, "Failed to update task: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun updateTaskStatus(taskId: String, status: TaskStatus): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            val completedAt = if (status == TaskStatus.COMPLETED) now else null
            taskDao.updateTaskStatus(taskId, status.name, completedAt, now)

            val task = taskDao.getTaskById(taskId)?.toDomain()
            if (task != null) {
                syncTaskToFirestore(task)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to update task status: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteTask(taskId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val task = taskDao.getTaskById(taskId)
            taskDao.deleteTask(taskId)
            if (task != null) {
                deleteTaskFromFirestore(task.userId, taskId)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to delete task: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun syncTaskToFirestore(task: Task) {
        val firestore = FirebaseManager.getFirestore() ?: return
        try {
            val map = hashMapOf<String, Any?>(
                "title" to task.title,
                "description" to task.description,
                "status" to task.status.name,
                "priority" to task.priority.name,
                "dueDate" to task.dueDate,
                "dueTime" to task.dueTime,
                "estimatedMinutes" to task.estimatedMinutes,
                "category" to task.category,
                "goalId" to task.goalId,
                "milestoneId" to task.milestoneId,
                "goalTitle" to task.goalTitle,
                "recurrence" to task.recurrence.name,
                "completedAt" to task.completedAt,
                "createdAt" to task.createdAt,
                "updatedAt" to task.updatedAt
            )
            firestore.collection("users")
                .document(task.userId)
                .collection("tasks")
                .document(task.id)
                .set(map, SetOptions.merge())
                .awaitTask()
        } catch (e: Exception) {
            Log.w(tag, "Failed to sync task to Firestore: ${e.message}")
        }
    }

    private suspend fun deleteTaskFromFirestore(userId: String, taskId: String) {
        val firestore = FirebaseManager.getFirestore() ?: return
        try {
            firestore.collection("users")
                .document(userId)
                .collection("tasks")
                .document(taskId)
                .delete()
                .awaitTask()
        } catch (e: Exception) {
            Log.w(tag, "Failed to delete task from Firestore: ${e.message}")
        }
    }

    private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                cont.resume(task.result)
            } else {
                cont.resumeWithException(task.exception ?: RuntimeException("Task failed"))
            }
        }
    }
}
