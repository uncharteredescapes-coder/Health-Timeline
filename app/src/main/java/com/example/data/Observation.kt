package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Canonical clinical observation entity representing a factual metric extracted from a medical document
 * or recorded by a clinician/patient, maintaining strict provenance.
 */
@Serializable
@Entity(tableName = "observations")
data class Observation(
    @PrimaryKey val id: String,
    val sourceDocumentId: String,
    val sourcePage: Int = 1,
    val sourceText: String = "",
    val testNameRaw: String,
    val testNameNormalized: String,
    val value: Double,
    val unit: String,
    val referenceRangeLow: Double? = null,
    val referenceRangeHigh: Double? = null,
    val referenceRangeText: String? = null,
    val collectionDate: String? = null,
    val confidence: Double = 1.0,
    val verificationStatus: String = "unverified" // "unverified", "verified", "rejected"
) {
    /**
     * Converts to [LabResult] for backward compatibility with existing visualization components.
     */
    fun toLabResult(patientId: String, defaultTimestamp: Long = System.currentTimeMillis()): LabResult {
        val computedStatus = when {
            referenceRangeLow != null && value < referenceRangeLow -> "low"
            referenceRangeHigh != null && value > referenceRangeHigh -> "high"
            else -> "normal"
        }
        return LabResult(
            id = id,
            patientId = patientId,
            testName = testNameNormalized.ifBlank { testNameRaw },
            value = value,
            unit = unit,
            referenceLow = referenceRangeLow,
            referenceHigh = referenceRangeHigh,
            status = computedStatus,
            collectedAt = defaultTimestamp,
            laboratoryName = null,
            reportDate = collectionDate,
            sourceDocumentId = sourceDocumentId,
            confidence = confidence,
            verifiedByUser = (verificationStatus == "verified")
        )
    }
}
