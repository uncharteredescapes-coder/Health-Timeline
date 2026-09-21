package com.example.data

import kotlinx.serialization.Serializable

/**
 * Strict schema for medical document AI extraction.
 * Every extracted observation must conform to [Observation] with provenance attributes.
 */
@Serializable
data class ClinicalDocumentExtraction(
    val documentId: String? = null,
    val patientName: String? = null,
    val facilityName: String? = null,
    val reportType: String? = null,
    val specimenCollectedAt: String? = null,
    val reportedAt: String? = null,
    val observations: List<Observation> = emptyList(),
    val medications: List<ExtractedMedication> = emptyList(),
    val followUps: List<ExtractedFollowUp> = emptyList()
)

@Serializable
data class ExtractedMedication(
    val name: String,
    val dose: String = "",
    val schedule: List<String> = emptyList()
)

@Serializable
data class ExtractedFollowUp(
    val title: String,
    val category: String = "visit",
    val dueInDays: Int = 30
)
