package com.cenir.cenirapp.ui.home

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.cenir.cenirapp.data.database.AppDatabase
import com.cenir.cenirapp.data.gamification.GamificationManager
import com.cenir.cenirapp.data.gamification.StreakResult
import com.cenir.cenirapp.data.model.CompletedLog
import com.cenir.cenirapp.data.model.DailyMission
import com.cenir.cenirapp.data.model.Task
import com.cenir.cenirapp.data.model.UserSettings
import com.cenir.cenirapp.data.model.UserStats
import com.cenir.cenirapp.notifications.TaskReminderWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val db          = AppDatabase.getInstance(application)
    private val dao         = db.taskDao()
    private val logDao      = db.completedLogDao()
    private val workManager = WorkManager.getInstance(application)

    val gamification = GamificationManager(db, application.applicationContext)

    // ── Stats ─────────────────────────────────────────────────────────────
    val userStats: LiveData<UserStats?> = db.userStatsDao()
        .getStats().flowOn(Dispatchers.IO).asLiveData()

    // ── Misiones ──────────────────────────────────────────────────────────
    val dailyMissions: LiveData<List<DailyMission>> =
        db.dailyMissionDao().getMissionsForDay(todayMidnight())
            .flowOn(Dispatchers.IO).asLiveData()

    // ── Configuración racha ───────────────────────────────────────────────
    val streakSettings: LiveData<UserSettings?> = db.userSettingsDao()
        .getSettings().flowOn(Dispatchers.IO).asLiveData()

    // ── Objetivos de hoy (isObjective = true) ────────────────────────────
// ── Tareas objetivo del día (para mostrar en sección separada) ────────
    val todayObjectiveTasks: LiveData<List<Task>> = dao.getNonDailyTasks()
        .map { list ->
            list.filter { it.isObjective }
                .sortedWith(compareBy { it.isCompleted })
        }
        .flowOn(Dispatchers.IO).asLiveData()

    // ── Eventos ───────────────────────────────────────────────────────────
    private val _levelUpEvent = MutableLiveData<Int?>()
    val levelUpEvent: LiveData<Int?> = _levelUpEvent

    private val _streakResult = MutableLiveData<StreakResult?>()
    val streakResult: LiveData<StreakResult?> = _streakResult

    // ── Tareas ────────────────────────────────────────────────────────────
    val allTasks: LiveData<List<Task>> = dao.getAllTasks()
        .flowOn(Dispatchers.IO).asLiveData()

    val dailyTasks: LiveData<List<Task>> = dao.getDailyTasks()
        .flowOn(Dispatchers.IO).asLiveData()

    // ── Helpers de fecha ──────────────────────────────────────────────────
    private fun startOfToday(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun endOfToday(): Long = startOfToday() + 24 * 60 * 60 * 1000L - 1L

    fun todayMidnight(): Long = startOfToday()

    // ── Grupos de tareas no-diarias ───────────────────────────────────────
    val overdueTasks: LiveData<List<Task>> = dao.getNonDailyTasks()
        .map { list ->
            val start = startOfToday()
            list.filter { it.dueDate != null && it.dueDate < start }
                .sortedWith(compareBy<Task> { it.isCompleted }.thenBy { it.dueDate })
        }
        .flowOn(Dispatchers.IO).asLiveData()

    val todayTasks: LiveData<List<Task>> = dao.getNonDailyTasks()
        .map { list ->
            val start = startOfToday()
            val end   = endOfToday()
            list.filter { task ->
                when {
                    task.dueDate == null       -> true
                    task.dueDate in start..end -> true
                    else                       -> false
                }
            }.sortedWith(
                compareBy<Task> { it.isCompleted }
                    .thenByDescending { it.isUrgent }
                    .thenBy { it.createdAt }
            )
        }
        .flowOn(Dispatchers.IO).asLiveData()

    val upcomingTasks: LiveData<List<Task>> = dao.getNonDailyTasks()
        .map { list ->
            val end = endOfToday()
            list.filter { it.dueDate != null && it.dueDate > end }
                .sortedWith(compareBy<Task> { it.isCompleted }.thenBy { it.dueDate })
        }
        .flowOn(Dispatchers.IO).asLiveData()

    // ── Todas las tareas no completadas (para el selector de objetivos) ───
    val allPendingTasks: LiveData<List<Task>> = dao.getAllTasks()
        .map { list -> list.filter { !it.isCompleted }.sortedBy { it.createdAt } }
        .flowOn(Dispatchers.IO).asLiveData()

    // ── SharedPreferences ─────────────────────────────────────────────────
    private fun prefs(context: Context) =
        context.getSharedPreferences("cenir_prefs", Context.MODE_PRIVATE)

    fun getUserName(context: Context): String =
        prefs(context).getString("user_name", "") ?: ""

    fun saveUserName(context: Context, name: String) =
        prefs(context).edit().putString("user_name", name.trim()).apply()

    // ── CRUD ──────────────────────────────────────────────────────────────
    fun addTask(
        title:         String,
        subject:       String,
        isUrgent:      Boolean = false,
        isDaily:       Boolean = false,
        dueDate:       Long?   = null,
        wantsReminder: Boolean = false,
        isObjective:   Boolean = false
    ) {
        if (title.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val task = Task(
                title        = title.trim(),
                subject      = subject.trim(),
                isUrgent     = isUrgent,
                dueDate      = dueDate,
                isDaily      = isDaily,
                scheduledFor = if (isObjective) todayMidnight() else null,
                isObjective  = isObjective
            )
            val insertedId = dao.insert(task)
            if (wantsReminder && dueDate != null) {
                scheduleReminder(task.copy(id = insertedId.toInt()))
            }
        }
    }

    fun toggleTask(task: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            val nowCompleted = !task.isCompleted
            val completedAt  = if (nowCompleted) System.currentTimeMillis() else null
            dao.update(task.copy(isCompleted = nowCompleted, completedAt = completedAt))

            if (nowCompleted) {
                val levelBefore = db.userStatsDao().getStatsOnce()?.currentLevel ?: 1
                val result = gamification.onTaskCompleted(
                    taskTitle   = task.title,
                    isObjective = task.isObjective
                )
                val levelAfter = db.userStatsDao().getStatsOnce()?.currentLevel ?: 1
                if (levelAfter > levelBefore) _levelUpEvent.postValue(levelAfter)
                if (result is StreakResult.STREAK_UPDATED) _streakResult.postValue(result)
            }
        }
    }

    /**
     * Establece directamente isObjective en una tarea.
     * Si se activa, scheduledFor = hoy (para que aparezca en los objetivos de hoy).
     * Si se desactiva, scheduledFor = null (deja de contar para la racha).
     * El cambio es permanente hasta que el usuario lo cambie manualmente.
     */
    fun setObjective(task: Task, makeObjective: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.update(
                task.copy(
                    isObjective  = makeObjective,
                    scheduledFor = if (makeObjective) todayMidnight() else null
                )
            )
        }
    }

    fun onLevelUpEventHandled() { _levelUpEvent.value = null }
    fun onStreakResultHandled() { _streakResult.value = null }

    fun deleteTask(task: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            // Si era un objetivo YA completado, guardarlo en el log antes de borrar
            if (task.isCompleted && task.completedAt != null) {
                logDao.insert(CompletedLog(taskTitle = task.title, completedAt = task.completedAt))
            }
            dao.delete(task)
            cancelReminder(task.id)
        }
    }

    fun restoreTask(task: Task) {
        viewModelScope.launch(Dispatchers.IO) { dao.insert(task) }
    }

    // ── WorkManager ───────────────────────────────────────────────────────
    fun scheduleReminder(task: Task) {
        val due   = task.dueDate ?: return
        val delay = due - TWO_HOURS_MS - System.currentTimeMillis()
        if (delay <= 0) return

        val data = Data.Builder()
            .putString(TaskReminderWorker.KEY_TASK_TITLE, task.title)
            .build()

        val request = OneTimeWorkRequestBuilder<TaskReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag(reminderTag(task.id))
            .build()

        workManager.enqueue(request)
    }

    fun cancelReminder(taskId: Int) =
        workManager.cancelAllWorkByTag(reminderTag(taskId))

    private fun reminderTag(taskId: Int) = "reminder_$taskId"

    fun deleteAllCompleted() {
        viewModelScope.launch(Dispatchers.IO) { dao.deleteAllCompleted() }
    }

    companion object {
        private const val TWO_HOURS_MS = 2 * 60 * 60 * 1000L
    }
}
