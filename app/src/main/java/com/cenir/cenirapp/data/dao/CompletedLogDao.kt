package com.cenir.cenirapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cenir.cenirapp.data.model.CompletedLog
import kotlinx.coroutines.flow.Flow

@Dao
interface CompletedLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: CompletedLog)

    @Query("SELECT completedAt FROM completed_log ORDER BY completedAt DESC")
    fun getAllCompletedTimestamps(): Flow<List<Long>>

    @Query("SELECT COUNT(*) FROM completed_log WHERE completedAt >= :startOfDay")
    fun getCompletedToday(startOfDay: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM completed_log")
    fun getTotalCompleted(): Flow<Int>
}
