package com.example.data

import kotlinx.coroutines.flow.Flow

class HealthRepository(private val healthDao: HealthDao) {
    fun getPatient(): Flow<Patient?> = healthDao.getPatient()
    suspend fun insertPatient(patient: Patient) = healthDao.insertPatient(patient)

    fun getConditions(patientId: String): Flow<List<Condition>> = healthDao.getConditions(patientId)
    suspend fun insertCondition(condition: Condition) = healthDao.insertCondition(condition)

    fun getLabResults(patientId: String): Flow<List<LabResult>> = healthDao.getLabResults(patientId)
    fun getLabHistory(patientId: String, testName: String): Flow<List<LabResult>> = healthDao.getLabHistory(patientId, testName)
    suspend fun insertLabResult(result: LabResult) = healthDao.insertLabResult(result)

    fun getVitals(patientId: String): Flow<List<Vital>> = healthDao.getVitals(patientId)
    suspend fun insertVital(vital: Vital) = healthDao.insertVital(vital)

    fun getMedications(patientId: String): Flow<List<Medication>> = healthDao.getMedications(patientId)
    suspend fun insertMedication(medication: Medication) = healthDao.insertMedication(medication)

    fun getMedicationLogs(patientId: String): Flow<List<MedicationLog>> = healthDao.getMedicationLogs(patientId)
    suspend fun insertMedicationLog(log: MedicationLog) = healthDao.insertMedicationLog(log)

    fun getCareTasks(patientId: String): Flow<List<CareTask>> = healthDao.getCareTasks(patientId)
    suspend fun insertCareTask(task: CareTask) = healthDao.insertCareTask(task)

    fun getDocuments(patientId: String): Flow<List<Document>> = healthDao.getDocuments(patientId)
    suspend fun insertDocument(document: Document) = healthDao.insertDocument(document)
    suspend fun getDocumentById(id: String): Document? = healthDao.getDocumentById(id)

    fun getHealthSummaries(patientId: String): Flow<List<HealthSummary>> = healthDao.getHealthSummaries(patientId)
    suspend fun insertHealthSummary(summary: HealthSummary) = healthDao.insertHealthSummary(summary)

    suspend fun deleteLabResult(id: String) = healthDao.deleteLabResult(id)
    suspend fun deleteMedication(id: String) = healthDao.deleteMedication(id)
    suspend fun deleteCareTask(id: String) = healthDao.deleteCareTask(id)
    suspend fun updateCareTaskStatus(id: String, status: String, completedAt: Long? = null) =
        healthDao.updateCareTaskStatus(id, status, completedAt)
    suspend fun deleteCondition(id: String) = healthDao.deleteCondition(id)
    suspend fun deleteDocument(id: String) = healthDao.deleteDocument(id)
    suspend fun clearPatientLabs(patientId: String) = healthDao.clearPatientLabs(patientId)

    fun getObservationsForDocument(documentId: String): Flow<List<Observation>> =
        healthDao.getObservationsForDocument(documentId)
    fun getObservationsByStatus(status: String): Flow<List<Observation>> =
        healthDao.getObservationsByStatus(status)
    suspend fun insertObservation(observation: Observation) = healthDao.insertObservation(observation)
    suspend fun insertObservations(observations: List<Observation>) = healthDao.insertObservations(observations)
    suspend fun updateObservationStatus(id: String, status: String) = healthDao.updateObservationStatus(id, status)
    suspend fun deleteObservation(id: String) = healthDao.deleteObservation(id)
}
