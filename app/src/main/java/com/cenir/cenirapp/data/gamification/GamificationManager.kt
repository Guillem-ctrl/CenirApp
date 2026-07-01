package com.cenir.cenirapp.data.gamification

import android.content.Context
import com.cenir.cenirapp.data.database.AppDatabase
import com.cenir.cenirapp.data.model.Achievement
import com.cenir.cenirapp.data.model.CompletedLog
import com.cenir.cenirapp.data.model.DailyMission
import com.cenir.cenirapp.data.model.UserSettings
import com.cenir.cenirapp.data.model.UserStats
import com.cenir.cenirapp.notifications.NotificationHelper
import kotlinx.coroutines.flow.first
import java.util.Calendar

// ── Resultado del sistema de racha ─────────────────────────────────────────
sealed class StreakResult {
    /** El usuario tiene menos de N objetivos creados hoy */
    object NOT_ENOUGH_OBJECTIVES : StreakResult()
    /** Tiene objetivos creados pero no los ha completado todos */
    object OBJECTIVES_PENDING    : StreakResult()
    /** La racha ya se contó hoy, no se duplica */
    object ALREADY_COUNTED       : StreakResult()
    /** Racha actualizada correctamente */
    data class STREAK_UPDATED(val newStreak: Int) : StreakResult()
}

// ──────────────────────────────────────────────────────────────────────────
class GamificationManager(
    private val db: AppDatabase,
    private val context: Context
) {

    private val statsDao        = db.userStatsDao()
    private val achievementDao  = db.achievementDao()
    private val missionDao      = db.dailyMissionDao()
    private val taskDao         = db.taskDao()
    private val settingsDao     = db.userSettingsDao()
    private val completedLogDao = db.completedLogDao()

    // ── INICIALIZACIÓN ──────────────────────────────────────────────────────

    suspend fun initIfNeeded() {
        if (statsDao.getStatsOnce() == null) {
            statsDao.upsert(UserStats())
        }
        if (settingsDao.getSettingsOnce() == null) {
            settingsDao.upsert(UserSettings())
        }
        achievementDao.insertAll(defaultAchievements())
        ensureDailyMissions()
    }

    // ── XP ──────────────────────────────────────────────────────────────────

    /** Añade XP. Devuelve true si el usuario subió de nivel. */
    suspend fun addXp(amount: Int): Boolean {
        val stats    = statsDao.getStatsOnce() ?: return false
        val oldLevel = LevelSystem.levelFromXp(stats.totalXp)
        val newXp    = stats.totalXp + amount
        val newLevel = LevelSystem.levelFromXp(newXp)

        statsDao.upsert(stats.copy(totalXp = newXp, currentLevel = newLevel))

        if (newLevel >= 5)  unlockAchievementInternal("level_5")
        if (newLevel >= 10) unlockAchievementInternal("level_10")

        return newLevel > oldLevel
    }

    // ── RACHA (basada en objetivos diarios) ─────────────────────────────────

    /**
     * Evalúa si el usuario cumplió sus objetivos del día y actualiza la racha.
     * Devuelve un [StreakResult] que describe el estado actual.
     */
    suspend fun checkStreakFromObjectives(): StreakResult {
        val today    = todayMidnight()
        val stats    = statsDao.getStatsOnce() ?: return StreakResult.NOT_ENOUGH_OBJECTIVES
        val settings = settingsDao.getSettingsOnce() ?: UserSettings()
        val minRequired = settings.streakMinObjectives

        // Total de objetivos creados para hoy
        val totalObjectivesToday = taskDao.getObjectivesForTodayOnce(today).size

        if (totalObjectivesToday < minRequired) {
            return StreakResult.NOT_ENOUGH_OBJECTIVES
        }

        // Cuántos de esos objetivos están completados
        val completedToday = taskDao.countCompletedObjectivesToday(today)

        if (completedToday < minRequired) {
            return StreakResult.OBJECTIVES_PENDING
        }

        // ✅ Cumplió los objetivos — comprobar si ya se contó hoy
        if (stats.lastActiveDate == today) {
            return StreakResult.ALREADY_COUNTED
        }

        // Calcular nueva racha
        val yesterday = today - 86_400_000L
        val newStreak = if (stats.lastActiveDate == yesterday) {
            stats.currentStreak + 1
        } else {
            1   // racha rota, reinicia
        }
        val longest = maxOf(newStreak, stats.longestStreak)

        // Bonus XP por hitos
        val bonusXp = when {
            newStreak == 7            -> XpValues.STREAK_7_DAYS
            newStreak == 30           -> XpValues.STREAK_30_DAYS
            newStreak % 10 == 0       -> 30   // cada 10 días: bonus extra
            else                      -> 0
        }
        if (bonusXp > 0) addXp(bonusXp)

        // Logros de racha
        if (newStreak >= 7)  unlockAchievementInternal("streak_7")
        if (newStreak >= 30) unlockAchievementInternal("streak_30")

        statsDao.upsert(
            stats.copy(
                currentStreak  = newStreak,
                longestStreak  = longest,
                lastActiveDate = today
            )
        )

        return StreakResult.STREAK_UPDATED(newStreak)
    }

    /**
     * Llamado por DailyResetWorker cada noche.
     * Si el último día activo no fue ayer ni hoy → la racha se rompió.
     */
    suspend fun checkStreakBreak() {
        val stats     = statsDao.getStatsOnce() ?: return
        val today     = todayMidnight()
        val yesterday = today - 86_400_000L
        if (stats.lastActiveDate < yesterday && stats.currentStreak > 0) {
            statsDao.upsert(stats.copy(currentStreak = 0))
        }
    }

    // ── EVENTOS EXTERNOS ────────────────────────────────────────────────────

    /**
     * Llamar al completar una tarea.
     * @param taskTitle   Título para el log persistente de estadísticas.
     * @param isObjective Si la tarea es un objetivo del día.
     * @return [StreakResult] con el estado de la racha tras completar.
     */
    suspend fun onTaskCompleted(
        taskTitle: String,
        isObjective: Boolean = false
    ): StreakResult {
        // Log persistente de estadísticas
        completedLogDao.insert(CompletedLog(taskTitle = taskTitle))

        // XP, misiones y logros base
        addXp(XpValues.TASK_COMPLETED)
        updateMissionProgress("mission_tasks")
        unlockAchievementInternal("first_task")
        checkTaskCountAchievements()

        // Evaluar racha (siempre, porque aunque no sea objetivo puede haberse
        // cambiado el mínimo a 0 o el usuario puede tener más objetivos)
        return checkStreakFromObjectives()
    }

    /**
     * Llamar al terminar una sesión de foco.
     */
    suspend fun onFocusSessionCompleted(minutes: Int) {
        addXp(XpValues.FOCUS_SESSION)
        updateMissionProgress("mission_focus")
        checkFocusAchievements()
    }

    // ── MISIONES DIARIAS ────────────────────────────────────────────────────

    suspend fun ensureDailyMissions() {
        val today    = todayMidnight()
        val existing = missionDao.getMissionsForDayOnce(today)
        if (existing.isNotEmpty()) return

        missionDao.upsertAll(
            listOf(
                DailyMission(
                    id           = "mission_tasks_$today",
                    description  = "Completa 3 tareas",
                    targetCount  = 3,
                    currentCount = 0,
                    xpReward     = XpValues.DAILY_MISSION,
                    isCompleted  = false,
                    date         = today
                ),
                DailyMission(
                    id           = "mission_focus_$today",
                    description  = "Finaliza 2 sesiones de foco",
                    targetCount  = 2,
                    currentCount = 0,
                    xpReward     = XpValues.DAILY_MISSION,
                    isCompleted  = false,
                    date         = today
                )
            )
        )
    }

    private suspend fun updateMissionProgress(type: String) {
        val today    = todayMidnight()
        val missions = missionDao.getMissionsForDayOnce(today)

        missions
            .filter { it.id.contains(type) && !it.isCompleted }
            .forEach { mission ->
                val newCount = mission.currentCount + 1
                if (newCount >= mission.targetCount) {
                    missionDao.upsert(mission.copy(currentCount = newCount, isCompleted = true))
                    // XP directo para evitar recursión con addXp/unlockAchievementInternal
                    val stats    = statsDao.getStatsOnce() ?: return@forEach
                    val newXp    = stats.totalXp + mission.xpReward
                    val newLevel = LevelSystem.levelFromXp(newXp)
                    statsDao.upsert(stats.copy(totalXp = newXp, currentLevel = newLevel))
                    unlockAchievementInternal("missions_completed")
                    NotificationHelper.showDailyMissionsNotification(context)
                } else {
                    missionDao.upsert(mission.copy(currentCount = newCount))
                }
            }
    }

    // ── LOGROS ──────────────────────────────────────────────────────────────

    /** Desbloquea un logro desde el exterior (con notificación). */
    suspend fun unlockAchievement(id: String) = unlockAchievementInternal(id)

    /**
     * Uso interno: desbloquea + XP + notificación.
     * XP se aplica directamente (sin llamar a addXp) para evitar recursión.
     */
    private suspend fun unlockAchievementInternal(id: String) {
        val achievement = achievementDao.getById(id) ?: return
        if (achievement.isUnlocked) return

        achievementDao.upsert(
            achievement.copy(
                isUnlocked = true,
                unlockedAt = System.currentTimeMillis()
            )
        )

        val stats    = statsDao.getStatsOnce() ?: return
        val newXp    = stats.totalXp + XpValues.ACHIEVEMENT
        val newLevel = LevelSystem.levelFromXp(newXp)
        statsDao.upsert(stats.copy(totalXp = newXp, currentLevel = newLevel))

        NotificationHelper.showAchievementNotification(
            context, achievement.title, achievement.xpReward
        )
    }

    private suspend fun checkTaskCountAchievements() {
        val total = completedLogDao.getTotalCompleted().first()
        if (total >= 10) unlockAchievementInternal("tasks_10")
        if (total >= 50) unlockAchievementInternal("tasks_50")
    }

    private suspend fun checkFocusAchievements() {
        // TODO: registrar sesiones de foco del día y desbloquear "focus_5_day"
    }

    // ── CONFIGURACIÓN DE RACHA ──────────────────────────────────────────────

    /**
     * Cambia el mínimo de objetivos necesarios para sumar racha (1–10).
     * Llamado desde la pantalla de ajustes.
     */
    suspend fun setStreakMinObjectives(min: Int) {
        val current = settingsDao.getSettingsOnce() ?: UserSettings()
        settingsDao.upsert(current.copy(streakMinObjectives = min.coerceIn(1, 10)))
    }

    /** Devuelve el mínimo actual configurado. */
    suspend fun getStreakMinObjectives(): Int =
        settingsDao.getSettingsOnce()?.streakMinObjectives ?: 3

    // ── LOGROS PREDEFINIDOS ─────────────────────────────────────────────────

    private fun defaultAchievements(): List<Achievement> = listOf(
        Achievement("first_task",         "Primera tarea",       "Completa tu primera tarea",                 XpValues.ACHIEVEMENT),
        Achievement("tasks_10",           "En racha",            "Completa 10 tareas en total",               XpValues.ACHIEVEMENT),
        Achievement("tasks_50",           "Imparable",           "Completa 50 tareas en total",               XpValues.ACHIEVEMENT),
        Achievement("streak_7",           "Semana perfecta",     "7 días seguidos cumpliendo objetivos",      XpValues.ACHIEVEMENT),
        Achievement("streak_30",          "Constancia de acero", "30 días seguidos cumpliendo objetivos",     XpValues.ACHIEVEMENT),
        Achievement("focus_5_day",        "En zona",             "5 sesiones de foco en un mismo día",        XpValues.ACHIEVEMENT),
        Achievement("missions_completed", "Misión cumplida",     "Completa una misión diaria",                XpValues.ACHIEVEMENT),
        Achievement("level_5",            "Estudiante sólido",   "Alcanza el nivel 5",                        XpValues.ACHIEVEMENT),
        Achievement("level_10",           "Experto",             "Alcanza el nivel 10",                       XpValues.ACHIEVEMENT)
    )

    // ── UTILIDADES ──────────────────────────────────────────────────────────

    fun todayMidnight(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE,      0)
            set(Calendar.SECOND,      0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
