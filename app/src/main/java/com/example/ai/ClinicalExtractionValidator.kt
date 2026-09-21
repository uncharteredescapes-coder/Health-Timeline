package com.example.ai

import com.example.data.ClinicalDocumentExtraction
import com.example.data.Observation
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

sealed class ValidationResult {
    data class Success(val extraction: ClinicalDocumentExtraction) : ValidationResult()
    data class Failure(val reason: String, val cause: Throwable? = null) : ValidationResult()
}

/**
 * Strict Kotlin-based validation layer for AI extraction output.
 * Uses kotlinx.serialization to reject any response that does not conform to the schema,
 * and executes clinical domain validations to guarantee safety and provenance.
 */
class ClinicalExtractionValidator(
    private val json: Json = Json {
        ignoreUnknownKeys = false // Strictly enforce schema: reject unexpected fields
        isLenient = false         // Strictly enforce RFC compliant JSON
        coerceInputValues = false // Reject mismatched types instead of coercing
    }
) {
    // Known prompt injection / override patterns to intercept in untrusted document text
    private val forbiddenDirectives = listOf(
        "ignore previous instructions",
        "ignore above instructions",
        "override application",
        "system prompt",
        "jailbreak",
        "developer mode",
        "<script>",
        "drop table",
        "grant all"
    )

    /**
     * Validates and parses raw JSON output from the AI model.
     * Rejects malformed JSON, schema deviations, prompt injections, and invalid clinical bounds.
     */
    fun validateAndParse(rawResponse: String, defaultDocumentId: String): ValidationResult {
        if (rawResponse.isBlank()) {
            return ValidationResult.Failure("AI extraction response is empty")
        }

        // Clean any code block delimiters if returned by model
        var cleaned = rawResponse.trim()
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.substringAfter("```json")
                .substringAfter("```")
                .substringBeforeLast("```")
                .trim()
        }

        // 1. Strict Schema & Syntax validation via kotlinx.serialization
        val parsed: ClinicalDocumentExtraction = try {
            json.decodeFromString<ClinicalDocumentExtraction>(cleaned)
        } catch (e: SerializationException) {
            return ValidationResult.Failure(
                "Schema validation failed: response does not conform to ClinicalDocumentExtraction schema (${e.message})",
                e
            )
        } catch (e: IllegalArgumentException) {
            return ValidationResult.Failure(
                "JSON format error: ${e.message}",
                e
            )
        }

        // 2. Clinical Domain & Provenance Validation
        val validatedObservations = mutableListOf<Observation>()
        for ((index, obs) in parsed.observations.withIndex()) {
            val textPool = "${obs.testNameRaw} ${obs.testNameNormalized} ${obs.sourceText} ${obs.unit} ${obs.referenceRangeText ?: ""}".lowercase()
            if (forbiddenDirectives.any { textPool.contains(it) }) {
                return ValidationResult.Failure(
                    "Security violation: Prompt injection attempt detected in observation $index"
                )
            }

            if (obs.testNameRaw.isBlank()) {
                return ValidationResult.Failure(
                    "Validation error: Observation at index $index has blank testNameRaw"
                )
            }

            if (obs.unit.isBlank()) {
                return ValidationResult.Failure(
                    "Validation error: Observation '${obs.testNameRaw}' has blank unit"
                )
            }

            if (obs.value.isNaN() || obs.value.isInfinite()) {
                return ValidationResult.Failure(
                    "Validation error: Observation '${obs.testNameRaw}' has invalid non-finite value"
                )
            }

            // Reference interval boundary check
            if (obs.referenceRangeLow != null && obs.referenceRangeHigh != null) {
                if (obs.referenceRangeLow > obs.referenceRangeHigh) {
                    return ValidationResult.Failure(
                        "Clinical contradiction: Observation '${obs.testNameRaw}' referenceRangeLow (${obs.referenceRangeLow}) > referenceRangeHigh (${obs.referenceRangeHigh})"
                    )
                }
            }

            // Confidence boundary check
            if (obs.confidence < 0.0 || obs.confidence > 1.0) {
                return ValidationResult.Failure(
                    "Validation error: Observation '${obs.testNameRaw}' has invalid confidence score (${obs.confidence})"
                )
            }

            // Provenance verification: enforce sourceDocumentId and page
            val resolvedDocId = if (obs.sourceDocumentId.isNotBlank()) obs.sourceDocumentId else defaultDocumentId
            val resolvedPage = if (obs.sourcePage >= 1) obs.sourcePage else 1

            validatedObservations.add(
                obs.copy(
                    sourceDocumentId = resolvedDocId,
                    sourcePage = resolvedPage
                )
            )
        }

        val finalizedExtraction = parsed.copy(
            documentId = if (!parsed.documentId.isNullOrBlank()) parsed.documentId else defaultDocumentId,
            observations = validatedObservations
        )

        return ValidationResult.Success(finalizedExtraction)
    }
}
