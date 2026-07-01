package com.cenir.cenirapp.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class TaskReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val taskTitle = inputData.getString(KEY_TASK_TITLE)
            ?: return Result.failure()

        NotificationHelper.showDueNotification(context, taskTitle)
        return Result.success()
    }

    companion object {
        const val KEY_TASK_TITLE = "task_title"
    }
}
