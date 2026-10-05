package com.example.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE userId = :userId ORDER BY dueDate ASC, createdAt DESC")
    fun getTasksFlow(userId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE userId = :userId AND status != 'COMPLETED' ORDER BY dueDate ASC, priority DESC")
    fun getPendingTasksFlow(userId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE goalId = :goalId ORDER BY dueDate ASC, createdAt DESC")
    fun getTasksForGoalFlow(goalId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTaskById(taskId: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(task: TaskEntity)

    @Update
    suspend fun update(task: TaskEntity)

    @Query("UPDATE tasks SET status = :status, completedAt = :completedAt, updatedAt = :updatedAt WHERE id = :taskId")
    suspend fun updateTaskStatus(taskId: String, status: String, completedAt: Long?, updatedAt: Long)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: String)

    @Query("DELETE FROM tasks WHERE goalId = :goalId")
    suspend fun deleteTasksForGoal(goalId: String)
}
