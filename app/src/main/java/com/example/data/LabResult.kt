package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "lab_results")
data class LabResult(
    @PrimaryKey val id: String,
    val patientId: String,
    val testName: String, // normalized, e.g. "HbA1c"
    val value: Double,
    val unit: String,
    val referenceLow: Double?,
    val referenceHigh: Double?,
    val status: String, // normal, low, high, critical
    val collectedAt: Long,
    val laboratoryName: String? = null,
    val reportDate: String? = null,
    val sourceDocumentId: String?,
    val confidence: Double, // OCR extraction confidence
    val verifiedByUser: Boolean = false
) {
    /**
     * Converts to canonical [Observation] maintaining provenance attributes.
     */
    fun toObservation(page: Int = 1, rawText: String = ""): Observation {
        return Observation(
            id = id,
            sourceDocumentId = sourceDocumentId ?: "",
            sourcePage = page,
            sourceText = rawText.ifBlank { "$testName: $value $unit" },
            testNameRaw = testName,
            testNameNormalized = testName,
            value = value,
            unit = unit,
            referenceRangeLow = referenceLow,
            referenceRangeHigh = referenceHigh,
            referenceRangeText = if (referenceLow != null && referenceHigh != null) "$referenceLow - $referenceHigh $unit" else null,
            collectionDate = reportDate,
            confidence = confidence,
            verificationStatus = if (verifiedByUser) "verified" else "unverified"
        )
    }
}
