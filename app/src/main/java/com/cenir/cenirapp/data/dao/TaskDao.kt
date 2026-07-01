package com.cenir.cenirapp.data.dao

import androidx.room.*
import com.cenir.cenirapp.data.model.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("""
        SELECT * FROM tasks
        ORDER BY isCompleted ASC, isUrgent DESC, createdAt ASC
    """)
    fun getAllTasks(): Flow<List<Task>>

    @Query("""
        SELECT * FROM tasks
        ORDER BY isCompleted ASC, isUrgent DESC, createdAt ASC
    """)
    fun getAllSorted(): Flow<List<Task>>

    @Query("""
        SELECT * FROM tasks WHERE isDaily = 1
        ORDER BY isCompleted ASC, createdAt ASC
    """)
    fun getDailyTasks(): Flow<List<Task>>

    @Query("""
        SELECT * FROM tasks WHERE isDaily = 0
        ORDER BY isCompleted ASC, isUrgent DESC, createdAt ASC
    """)
    fun getNonDailyTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 1 AND isDaily = 0")
    suspend fun getCompletedNonDailySnapshot(): List<Task>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Query("DELETE FROM tasks WHERE isCompleted = 1 AND isDaily = 0")
    suspend fun deleteCompletedNonDaily()

    @Query("UPDATE tasks SET isCompleted = 0, completedAt = NULL WHERE isDaily = 1")
    suspend fun resetDailyTasks()

    @Query("""
        SELECT completedAt FROM tasks
        WHERE isCompleted = 1 AND completedAt IS NOT NULL
        ORDER BY completedAt DESC
    """)
    fun getCompletedByDay(): Flow<List<Long>>

    @Query("""
        SELECT COUNT(*) FROM tasks
        WHERE isCompleted = 1
        AND completedAt >= :startOfDay
    """)
    fun getCompletedToday(startOfDay: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 1")
    fun getTotalCompleted(): Flow<Int>

    @Query("DELETE FROM tasks WHERE isCompleted = 1")
    suspend fun deleteAllCompleted()

    // ── OBJETIVOS (sistema de racha) ──────────────────────────────────────

    /** Flow para observar objetivos de hoy en la UI */
    @Query("""
        SELECT * FROM tasks
        WHERE isObjective = 1
        AND scheduledFor = :today
        ORDER BY isCompleted ASC
    """)
    fun getObjectivesForToday(today: Long): Flow<List<Task>>

    /** Snapshot suspend — usado en GamificationManager */
    @Query("""
        SELECT * FROM tasks
        WHERE isObjective = 1
        AND scheduledFor = :today
    """)
    suspend fun getObjectivesForTodayOnce(today: Long): List<Task>

    /** Cuántos objetivos de hoy están completados */
    @Query("""
        SELECT COUNT(*) FROM tasks
        WHERE isObjective = 1
        AND isCompleted  = 1
        AND scheduledFor = :today
    """)
    suspend fun countCompletedObjectivesToday(today: Long): Int
}
