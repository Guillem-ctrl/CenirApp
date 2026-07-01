package com.cenir.cenirapp.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cenir.cenirapp.data.database.AppDatabase
import java.util.Calendar

class StreakWarningWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db    = AppDatabase.getInstance(applicationContext)
        val stats = db.userStatsDao().getStatsOnce() ?: return Result.success()

        val today = todayMidnight()

        if (stats.lastActiveDate != today && stats.currentStreak >= 2) {
            NotificationHelper.showStreakWarningNotification(
                applicationContext,
                stats.currentStreak
            )
        }

        return Result.success()
    }

    private fun todayMidnight(): Long {
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE,      0)
        c.set(Calendar.SECOND,      0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    companion object {
        const val TAG = "streak_warning_notif"
    }
}
