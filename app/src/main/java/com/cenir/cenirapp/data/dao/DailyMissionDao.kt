package com.cenir.cenirapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cenir.cenirapp.data.model.DailyMission
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyMissionDao {

    @Query("SELECT * FROM daily_missions WHERE date = :date")
    fun getMissionsForDay(date: Long): Flow<List<DailyMission>>

    @Query("SELECT * FROM daily_missions WHERE date = :date")
    suspend fun getMissionsForDayOnce(date: Long): List<DailyMission>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(mission: DailyMission)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(missions: List<DailyMission>)
}
