package com.cenir.cenirapp.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class DailyMissionsWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        NotificationHelper.showDailyMissionsNotification(applicationContext)
        return Result.success()
    }

    companion object {
        const val TAG = "daily_missions_notif"
    }
}
