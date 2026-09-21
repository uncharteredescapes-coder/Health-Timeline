package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "vitals")
data class Vital(
    @PrimaryKey val id: String,
    val patientId: String,
    val type: String, // blood_pressure, glucose, weight
    val value1: Double, // systolic or mg_dl or kg
    val value2: Double? = null, // diastolic
    val context: String? = null, // fasting, post_meal, random
    val recordedAt: Long
)
