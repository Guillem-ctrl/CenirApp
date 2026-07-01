package com.cenir.cenirapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val subject: String = "",
    val isCompleted: Boolean = false,
    val isUrgent: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val dueDate: Long? = null,
    val completedAt: Long? = null,
    val isDaily: Boolean = false,
    val scheduledFor: Long? = null,
    val isObjective: Boolean = false
)