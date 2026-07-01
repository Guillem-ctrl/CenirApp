package com.cenir.cenirapp.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cenir.cenirapp.data.database.AppDatabase
import com.cenir.cenirapp.data.model.CompletedLog

class DailyResetWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(applicationContext)
        val taskDao = db.taskDao()
        val logDao  = db.completedLogDao()

        // 1. Guardar en el log las tareas completadas no-diarias antes de borrarlas
        val completedNonDaily = taskDao.getCompletedNonDailySnapshot()
        completedNonDaily.forEach { task ->
            if (task.completedAt != null) {
                logDao.insert(CompletedLog(taskTitle = task.title, completedAt = task.completedAt))
            }
        }

        // 2. Borrar tareas completadas no-diarias
        taskDao.deleteCompletedNonDaily()

        // 3. Reiniciar tareas diarias (poner isCompleted = false)
        taskDao.resetDailyTasks()

        return Result.success()
    }

    companion object {
        const val TAG = "daily_reset"
    }
}
