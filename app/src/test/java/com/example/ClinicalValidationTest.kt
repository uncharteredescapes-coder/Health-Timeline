package com.example

import com.example.ai.GeminiService
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class ClinicalValidationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = false
    }

    @Test
    fun testValidClinicalJsonParsing() {
        val validJson = """
        {
          "patientName": "Ahmed Karim",
          "date": "2026-09-14",
          "laboratoryName": "Popular Diagnostic Center",
          "results": [
            {
              "metric_name": "HbA1c",
              "value": 6.8,
              "unit": "%",
              "referenceLow": 4.0,
              "referenceHigh": 5.6,
              "status": "high",
              "confidence": 0.98
            },
            {
              "metric_name": "Total Cholesterol",
              "value": 185.0,
              "unit": "mg/dL",
              "referenceLow": null,
              "referenceHigh": 200.0,
              "status": "normal",
              "confidence": 0.95
            }
          ],
          "medications": [],
          "careTasks": []
        }
        """.trimIndent()

        val rootObj = json.parseToJsonElement(validJson).jsonObject
        assertNotNull(rootObj)
        assertEquals("Ahmed Karim", rootObj["patientName"]?.jsonPrimitive?.content)
        assertEquals("Popular Diagnostic Center", rootObj["laboratoryName"]?.jsonPrimitive?.content)

        val resultsArray = rootObj["results"]?.jsonArray
        assertNotNull(resultsArray)
        assertEquals(2, resultsArray?.size)

        val firstLab = resultsArray?.get(0)?.jsonObject
        assertEquals("HbA1c", firstLab?.get("metric_name")?.jsonPrimitive?.content)
        assertEquals(6.8, firstLab?.get("value")?.jsonPrimitive?.doubleOrNull ?: 0.0, 0.01)
        assertEquals("high", firstLab?.get("status")?.jsonPrimitive?.content)
    }

    @Test
    fun testMalformedJsonRejection() {
        val corruptedJson = """
        {
          "reportType": "Incomplete,
          "results": [ { "testName": 123
        """.trimIndent()

        try {
            json.parseToJsonElement(corruptedJson)
            fail("Expected syntax exception on malformed JSON")
        } catch (e: Exception) {
            assertTrue(e is kotlinx.serialization.SerializationException)
        }
    }

    @Test
    fun testReferenceRangeClassification() {
        // High
        val valHigh = 135.0
        val refHigh = 99.0
        val refLow = 70.0
        val statusHigh = if (valHigh > refHigh) "high" else if (valHigh < refLow) "low" else "normal"
        assertEquals("high", statusHigh)

        // Normal
        val valNorm = 4.2
        val refNormLow = 3.5
        val refNormHigh = 5.0
        val statusNorm = if (valNorm > refNormHigh) "high" else if (valNorm < refNormLow) "low" else "normal"
        assertEquals("normal", statusNorm)

        // Low
        val valLow = 9.8
        val refLowLow = 13.0
        val refLowHigh = 17.0
        val statusLow = if (valLow > refLowHigh) "high" else if (valLow < refLowLow) "low" else "normal"
        assertEquals("low", statusLow)
    }

    @Test
    fun testAdherenceCalculation() {
        val scheduledDoses = 14
        val takenDoses = 13
        val adherenceRate = (takenDoses.toDouble() / scheduledDoses) * 100.0
        assertEquals(92.85, adherenceRate, 0.01)
    }
}
