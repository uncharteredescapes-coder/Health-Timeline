package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "health_summaries")
data class HealthSummary(
    @PrimaryKey val id: String,
    val patientId: String,
    val content: String, // AI generated JSON or text
    val generatedAt: Long,
    val status: String, // draft, approved
    val approvedAt: Long? = null,
    val clinicianName: String? = null,
    val auditTrail: String? = null // Store original AI draft if edited
)
