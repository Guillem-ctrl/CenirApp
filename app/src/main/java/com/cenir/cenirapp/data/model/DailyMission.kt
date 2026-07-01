package com.cenir.cenirapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_missions")
data class DailyMission(
    @PrimaryKey val id: String,
    val description: String,
    val targetCount: Int,
    val currentCount: Int = 0,
    val xpReward: Int,
    val isCompleted: Boolean = false,
    val date: Long
)
