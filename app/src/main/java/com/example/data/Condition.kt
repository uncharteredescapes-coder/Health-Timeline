package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "conditions")
data class Condition(
    @PrimaryKey val id: String,
    val patientId: String,
    val type: String, // diabetes, hypertension
    val diagnosedAt: Long?,
    val status: String // active, monitoring, resolved
)
