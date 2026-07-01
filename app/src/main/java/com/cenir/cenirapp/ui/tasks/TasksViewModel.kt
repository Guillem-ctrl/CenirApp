package com.cenir.cenirapp.ui.tasks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.cenir.cenirapp.data.database.AppDatabase
import com.cenir.cenirapp.data.model.CompletedLog
import com.cenir.cenirapp.data.model.Task
import com.cenir.cenirapp.notifications.TaskReminderWorker
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

enum class TaskFilter { ALL, PENDING, DAILY, COMPLETED }

class TasksViewModel(application: Application) : AndroidViewModel(application) {

    private val dao     = AppDatabase.getInstance(application).taskDao()
    private val logDao  = AppDatabase.getInstance(application).completedLogDao()
    private val workManager = WorkManager.getInstance(application)

    val activeFilter = MutableLiveData(TaskFilter.ALL)

    val filteredTasks: LiveData<List<Task>> = activeFilter.switchMap { filter ->
        dao.getAllSorted().map { list ->
            when (filter) {
                TaskFilter.ALL       -> list
                TaskFilter.PENDING   -> list.filter { !it.isCompleted }
                TaskFilter.DAILY     -> list.filter { it.isDaily }
                TaskFilter.COMPLETED -> list.filter { it.isCompleted }
            }
        }.asLiveData()
    }

    fun addTask(
        title: String,
        subject: String,
        isUrgent: Boolean,
        isDaily: Boolean,
        dueDate: Long?,
        wantsReminder: Boolean
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val task = Task(
                title    = title.trim(),
                subject  = subject.trim(),
                isUrgent = isUrgent,
                dueDate  = dueDate,
                isDaily  = isDaily
            )
            val insertedId = dao.insert(task)
            if (wantsReminder && dueDate != null) {
                scheduleReminder(task.copy(id = insertedId.toInt()))
            }
        }
    }

    fun toggleTask(task: Task) {
        viewModelScope.launch {
            val nowCompleted = !task.isCompleted
            val completedAt  = if (nowCompleted) System.currentTimeMillis() else null
            dao.update(task.copy(isCompleted = nowCompleted, completedAt = completedAt))

            // Registrar en el log solo al completar (no al descompletar)
            if (nowCompleted) {
                logDao.insert(
                    CompletedLog(
                        taskTitle   = task.title,
                        completedAt = completedAt!!
                    )
                )
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            dao.delete(task)
            workManager.cancelAllWorkByTag("reminder_${task.id}")
        }
    }

    fun restoreTask(task: Task) {
        viewModelScope.launch { dao.insert(task) }
    }

    private fun scheduleReminder(task: Task) {
        val due = task.dueDate ?: return
        val delay = due - TWO_HOURS_MS - System.currentTimeMillis()
        if (delay <= 0) return

        val data = Data.Builder()
            .putString(TaskReminderWorker.KEY_TASK_TITLE, task.title)
            .build()

        val request = OneTimeWorkRequestBuilder<TaskReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag("reminder_${task.id}")
            .build()

        workManager.enqueue(request)
    }

    companion object {
        private const val TWO_HOURS_MS = 2 * 60 * 60 * 1000L
    }
}
