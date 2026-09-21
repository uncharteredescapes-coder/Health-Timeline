package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "care_tasks")
data class CareTask(
    @PrimaryKey val id: String,
    val patientId: String,
    val conditionId: String?,
    val title: String,
    val category: String, // screening, lab, visit
    val dueAt: Long,
    val status: String, // upcoming, due, overdue, done
    val lastCompletedAt: Long?
)
