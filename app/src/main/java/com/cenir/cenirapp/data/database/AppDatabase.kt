package com.cenir.cenirapp.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.cenir.cenirapp.data.dao.AchievementDao
import com.cenir.cenirapp.data.dao.CompletedLogDao
import com.cenir.cenirapp.data.dao.DailyMissionDao
import com.cenir.cenirapp.data.dao.TaskDao
import com.cenir.cenirapp.data.dao.UserSettingsDao
import com.cenir.cenirapp.data.dao.UserStatsDao
import com.cenir.cenirapp.data.model.Achievement
import com.cenir.cenirapp.data.model.CompletedLog
import com.cenir.cenirapp.data.model.DailyMission
import com.cenir.cenirapp.data.model.Task
import com.cenir.cenirapp.data.model.UserSettings
import com.cenir.cenirapp.data.model.UserStats

@Database(
    entities = [
        Task::class,
        CompletedLog::class,
        UserStats::class,
        Achievement::class,
        DailyMission::class,
        UserSettings::class
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun completedLogDao(): CompletedLogDao
    abstract fun userStatsDao(): UserStatsDao
    abstract fun achievementDao(): AchievementDao
    abstract fun dailyMissionDao(): DailyMissionDao
    abstract fun userSettingsDao(): UserSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cenir_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
