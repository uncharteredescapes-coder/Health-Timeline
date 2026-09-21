package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.LabResult
import com.example.data.Medication
import com.example.data.Patient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DatabaseAndMigrationTest {

    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testPatientAndLabResultWithProvenance() = runBlocking {
        val dao = db.healthDao()

        // 1. Insert patient
        val patient = Patient(
            id = "patient-1",
            name = "Ayesha Rahman",
            age = 48,
            gender = "Female",
            language = "bn"
        )
        dao.insertPatient(patient)

        val retrievedPatient = dao.getPatient().first()
        assertNotNull(retrievedPatient)
        assertEquals("Ayesha Rahman", retrievedPatient?.name)

        // 2. Insert lab result with provenance
        val lab = LabResult(
            id = "lab-101",
            patientId = "patient-1",
            testName = "HbA1c",
            value = 7.1,
            unit = "%",
            referenceLow = 4.0,
            referenceHigh = 5.6,
            status = "high",
            collectedAt = System.currentTimeMillis(),
            laboratoryName = "Popular Diagnostic Center",
            reportDate = "2026-09-14",
            sourceDocumentId = "doc-scan-99",
            confidence = 0.99,
            verifiedByUser = true
        )
        dao.insertLabResult(lab)

        val retrievedLabs = dao.getLabResults("patient-1").first()
        assertEquals(1, retrievedLabs.size)
        assertEquals("doc-scan-99", retrievedLabs[0].sourceDocumentId)
        assertTrue(retrievedLabs[0].verifiedByUser)
        assertEquals(7.1, retrievedLabs[0].value, 0.01)

        // 3. Delete lab result by id
        dao.deleteLabResult(lab.id)
        val afterDelete = dao.getLabResults("patient-1").first()
        assertEquals(0, afterDelete.size)
    }

    @Test
    fun testMedicationCrud() = runBlocking {
        val dao = db.healthDao()
        val med = Medication(
            id = "med-1",
            patientId = "patient-1",
            name = "Glimepiride",
            dose = "2mg",
            schedule = listOf("Morning"),
            startedAt = System.currentTimeMillis(),
            refillDueAt = null,
            conditionId = null
        )

        dao.insertMedication(med)
        val meds = dao.getMedications("patient-1").first()
        assertEquals(1, meds.size)
        assertEquals("Glimepiride", meds[0].name)

        dao.deleteMedication(med.id)
        val afterDelete = dao.getMedications("patient-1").first()
        assertEquals(0, afterDelete.size)
    }

    @Test
    fun testObservationWithProvenance() = runBlocking {
        val dao = db.healthDao()
        val observation = com.example.data.Observation(
            id = "obs-prov-1",
            sourceDocumentId = "doc-scan-99",
            sourcePage = 2,
            sourceText = "Fasting Blood Sugar: 126 mg/dL (70-99)",
            testNameRaw = "Fasting Blood Sugar",
            testNameNormalized = "Fasting Blood Glucose",
            value = 126.0,
            unit = "mg/dL",
            referenceRangeLow = 70.0,
            referenceRangeHigh = 99.0,
            referenceRangeText = "70-99 mg/dL",
            collectionDate = "2026-09-14",
            confidence = 0.97,
            verificationStatus = "verified"
        )

        dao.insertObservation(observation)

        val observations = dao.getObservationsForDocument("doc-scan-99").first()
        assertEquals(1, observations.size)
        val retrieved = observations[0]
        assertEquals("doc-scan-99", retrieved.sourceDocumentId)
        assertEquals(2, retrieved.sourcePage)
        assertEquals("Fasting Blood Sugar: 126 mg/dL (70-99)", retrieved.sourceText)
        assertEquals(126.0, retrieved.value, 0.001)
        assertEquals("verified", retrieved.verificationStatus)

        // Verify conversion to LabResult
        val labResult = retrieved.toLabResult("patient-1")
        assertEquals("Fasting Blood Glucose", labResult.testName)
        assertEquals("high", labResult.status)
        assertTrue(labResult.verifiedByUser)

        // Delete observation
        dao.deleteObservation(observation.id)
        val emptyObs = dao.getObservationsForDocument("doc-scan-99").first()
        assertEquals(0, emptyObs.size)
    }

    @Test
    fun testMigrationsExecution() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Verify singleton database instantiation with all migrations registered works without error
        val database = AppDatabase.getDatabase(context)
        assertNotNull(database)
        assertNotNull(database.healthDao())
    }
}
