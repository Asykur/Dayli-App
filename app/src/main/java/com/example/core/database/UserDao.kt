package com.example.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE isCurrentActiveUser = 1 LIMIT 1")
    fun getActiveUserFlow(): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE isCurrentActiveUser = 1 LIMIT 1")
    suspend fun getActiveUser(): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(user: UserEntity)

    @Update
    suspend fun update(user: UserEntity)

    @Query("UPDATE users SET isCurrentActiveUser = 0")
    suspend fun clearActiveUser()

    @Query("UPDATE users SET xp = :xp, level = :level, currentStreak = :currentStreak, longestStreak = :longestStreak, streakFreezes = :streakFreezes WHERE id = :userId")
    suspend fun updateGamification(userId: String, xp: Long, level: Int, currentStreak: Int, longestStreak: Int, streakFreezes: Int)

    @Query("UPDATE users SET currentStreak = :currentStreak, longestStreak = :longestStreak WHERE id = :userId")
    suspend fun updateStreak(userId: String, currentStreak: Int, longestStreak: Int)

    @Query("UPDATE users SET streakFreezes = streakFreezes - 1 WHERE id = :userId AND streakFreezes > 0")
    suspend fun useStreakShield(userId: String): Int

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM users")
    suspend fun deleteAll()
}
