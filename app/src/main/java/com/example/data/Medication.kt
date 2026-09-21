package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "medications")
data class Medication(
    @PrimaryKey val id: String,
    val patientId: String,
    val name: String,
    val dose: String,
    val schedule: List<String>, // ["morning", "evening"]
    val startedAt: Long,
    val refillDueAt: Long?,
    val conditionId: String?
)

@Serializable
@Entity(tableName = "medication_logs")
data class MedicationLog(
    @PrimaryKey val id: String,
    val medicationId: String,
    val takenAt: Long,
    val status: String // taken, missed, skipped
)
