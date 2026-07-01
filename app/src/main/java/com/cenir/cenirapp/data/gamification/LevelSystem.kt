package com.cenir.cenirapp.data.gamification

object LevelSystem {

    fun xpForNextLevel(currentLevel: Int): Int = currentLevel * 100

    fun xpForLevel(level: Int): Int =
        (1 until level).sumOf { it * 100 }

    fun levelFromXp(totalXp: Int): Int {
        var level = 1
        var xpNeeded = 0
        while (xpNeeded + xpForNextLevel(level) <= totalXp) {
            xpNeeded += xpForNextLevel(level)
            level++
        }
        return level
    }

    fun xpInCurrentLevel(totalXp: Int): Int {
        val level = levelFromXp(totalXp)
        return totalXp - xpForLevel(level)
    }

    fun progressPercent(totalXp: Int): Float {
        val level = levelFromXp(totalXp)
        val xpIn = xpInCurrentLevel(totalXp)
        return xpIn.toFloat() / xpForNextLevel(level).toFloat()
    }
}
