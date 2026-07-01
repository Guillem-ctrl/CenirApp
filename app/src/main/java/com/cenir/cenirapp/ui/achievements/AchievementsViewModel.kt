package com.cenir.cenirapp.ui.achievements

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import com.cenir.cenirapp.data.database.AppDatabase
import com.cenir.cenirapp.data.model.Achievement

class AchievementsViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).achievementDao()

    /** Lista completa: desbloqueados primero (más reciente), luego bloqueados */
    val achievements: LiveData<List<Achievement>> = dao.getAll().asLiveData().map { list ->
        list.sortedWith(
            compareByDescending<Achievement> { it.isUnlocked }
                .thenByDescending { it.unlockedAt ?: 0L }
        )
    }

    /** Número de logros desbloqueados */
    val unlockedCount: LiveData<Int> = achievements.map { list ->
        list.count { it.isUnlocked }
    }

    /** Total de logros */
    val totalCount: LiveData<Int> = achievements.map { it.size }

    /** XP total ganado por logros desbloqueados */
    val totalXpEarned: LiveData<Int> = achievements.map { list ->
        list.filter { it.isUnlocked }.sumOf { it.xpReward }
    }

    /** Porcentaje de progreso (0–100) */
    val progressPercent: LiveData<Int> = achievements.map { list ->
        if (list.isEmpty()) 0
        else (list.count { it.isUnlocked } * 100 / list.size).coerceIn(0, 100)
    }
}
