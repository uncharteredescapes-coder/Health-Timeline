package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthDao {
    // Patient
    @Query("SELECT * FROM patients LIMIT 1")
    fun getPatient(): Flow<Patient?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatient(patient: Patient)

    // Conditions
    @Query("SELECT * FROM conditions WHERE patientId = :patientId")
    fun getConditions(patientId: String): Flow<List<Condition>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCondition(condition: Condition)

    // Lab Results
    @Query("SELECT * FROM lab_results WHERE patientId = :patientId ORDER BY collectedAt DESC")
    fun getLabResults(patientId: String): Flow<List<LabResult>>

    @Query("SELECT * FROM lab_results WHERE patientId = :patientId AND testName = :testName ORDER BY collectedAt DESC")
    fun getLabHistory(patientId: String, testName: String): Flow<List<LabResult>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLabResult(result: LabResult)

    // Vitals
    @Query("SELECT * FROM vitals WHERE patientId = :patientId ORDER BY recordedAt DESC")
    fun getVitals(patientId: String): Flow<List<Vital>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVital(vital: Vital)

    // Medications
    @Query("SELECT * FROM medications WHERE patientId = :patientId")
    fun getMedications(patientId: String): Flow<List<Medication>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedication(medication: Medication)

    @Query("SELECT * FROM medication_logs WHERE medicationId IN (SELECT id FROM medications WHERE patientId = :patientId) ORDER BY takenAt DESC")
    fun getMedicationLogs(patientId: String): Flow<List<MedicationLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicationLog(log: MedicationLog)

    // Care Tasks
    @Query("SELECT * FROM care_tasks WHERE patientId = :patientId ORDER BY dueAt ASC")
    fun getCareTasks(patientId: String): Flow<List<CareTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCareTask(task: CareTask)

    // Documents
    @Query("SELECT * FROM documents WHERE patientId = :patientId ORDER BY uploadedAt DESC")
    fun getDocuments(patientId: String): Flow<List<Document>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: Document)

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getDocumentById(id: String): Document?

    // Health Summaries
    @Query("SELECT * FROM health_summaries WHERE patientId = :patientId ORDER BY generatedAt DESC")
    fun getHealthSummaries(patientId: String): Flow<List<HealthSummary>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHealthSummary(summary: HealthSummary)

    // Deletions & Updates
    @Query("DELETE FROM lab_results WHERE id = :id")
    suspend fun deleteLabResult(id: String)

    @Query("DELETE FROM medications WHERE id = :id")
    suspend fun deleteMedication(id: String)

    @Query("DELETE FROM care_tasks WHERE id = :id")
    suspend fun deleteCareTask(id: String)

    @Query("UPDATE care_tasks SET status = :status, lastCompletedAt = :completedAt WHERE id = :id")
    suspend fun updateCareTaskStatus(id: String, status: String, completedAt: Long?)

    @Query("DELETE FROM conditions WHERE id = :id")
    suspend fun deleteCondition(id: String)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteDocument(id: String)

    @Query("DELETE FROM lab_results WHERE patientId = :patientId")
    suspend fun clearPatientLabs(patientId: String)

    // Observations (Canonical Provenance Records)
    @Query("SELECT * FROM observations WHERE sourceDocumentId = :documentId")
    fun getObservationsForDocument(documentId: String): Flow<List<Observation>>

    @Query("SELECT * FROM observations WHERE verificationStatus = :status")
    fun getObservationsByStatus(status: String): Flow<List<Observation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObservation(observation: Observation)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObservations(observations: List<Observation>)

    @Query("UPDATE observations SET verificationStatus = :status WHERE id = :id")
    suspend fun updateObservationStatus(id: String, status: String)

    @Query("DELETE FROM observations WHERE id = :id")
    suspend fun deleteObservation(id: String)
}
