package com.cenir.cenirapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "completed_log")
data class CompletedLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val taskTitle: String,
    val completedAt: Long = System.currentTimeMillis()
)
