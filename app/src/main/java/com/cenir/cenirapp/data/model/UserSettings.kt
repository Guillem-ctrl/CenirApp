package com.cenir.cenirapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey val id: Int = 1,
    val streakMinObjectives: Int = 3   // mínimo de objetivos completados para sumar racha
)
