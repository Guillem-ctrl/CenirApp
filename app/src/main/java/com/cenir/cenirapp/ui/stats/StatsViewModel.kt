package com.cenir.cenirapp.ui.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.cenir.cenirapp.data.database.AppDatabase
import kotlinx.coroutines.flow.map
import java.util.Calendar

class StatsViewModel(application: Application) : AndroidViewModel(application) {

    // ✅ Usa completedLogDao, NO taskDao
    private val logDao = AppDatabase.getInstance(application).completedLogDao()

    private fun startOfToday(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    val completedToday: LiveData<Int> =
        logDao.getCompletedToday(startOfToday()).asLiveData()

    val totalCompleted: LiveData<Int> =
        logDao.getTotalCompleted().asLiveData()

    val currentStreak: LiveData<Int> =
        logDao.getAllCompletedTimestamps().map { timestamps ->
            calculateStreak(timestamps)
        }.asLiveData()

    val lastSevenDays: LiveData<List<Boolean>> =
        logDao.getAllCompletedTimestamps().map { timestamps ->
            buildSevenDayActivity(timestamps)
        }.asLiveData()

    private fun calculateStreak(timestamps: List<Long>): Int {
        if (timestamps.isEmpty()) return 0
        val days = timestamps.map { it.toEpochDay() }.toSortedSet().toList().sortedDescending()
        val today = System.currentTimeMillis().toEpochDay()
        var streak = 0
        var expected = today
        for (day in days) {
            if (day == expected) {
                streak++
                expected--
            } else if (day < expected) {
                break
            }
        }
        return streak
    }

    private fun buildSevenDayActivity(timestamps: List<Long>): List<Boolean> {
        val activeDays = timestamps.map { it.toEpochDay() }.toSet()
        val today = System.currentTimeMillis().toEpochDay()
        return (6 downTo 0).map { offset ->
            (today - offset) in activeDays
        }
    }

    private fun Long.toEpochDay(): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = this
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis / (1000 * 60 * 60 * 24)
    }
}
