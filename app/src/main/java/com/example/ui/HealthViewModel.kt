package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiService
import com.example.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class HealthViewModel(private val repository: HealthRepository) : ViewModel() {

    val patient: StateFlow<Patient?> = repository.getPatient()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _patientId = patient.map { it?.id ?: "" }

    val conditions: StateFlow<List<Condition>> = _patientId.flatMapLatest { id ->
        repository.getConditions(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val labResults: StateFlow<List<LabResult>> = _patientId.flatMapLatest { id ->
        repository.getLabResults(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val vitals: StateFlow<List<Vital>> = _patientId.flatMapLatest { id ->
        repository.getVitals(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val medications: StateFlow<List<Medication>> = _patientId.flatMapLatest { id ->
        repository.getMedications(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val medicationLogs: StateFlow<List<MedicationLog>> = _patientId.flatMapLatest { id ->
        repository.getMedicationLogs(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val careTasks: StateFlow<List<CareTask>> = _patientId.flatMapLatest { id ->
        repository.getCareTasks(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val documents: StateFlow<List<Document>> = _patientId.flatMapLatest { id ->
        repository.getDocuments(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val timelineEvents: StateFlow<List<TimelineEvent>> = combine(
        labResults,
        vitals,
        medicationLogs,
        medications
    ) { labs, vitals, logs, meds ->
        val events = mutableListOf<TimelineEvent>()
        labs.forEach { events.add(TimelineEvent.Lab(it)) }
        vitals.forEach { events.add(TimelineEvent.VitalEntry(it)) }
        logs.forEach { log ->
            val med = meds.find { it.id == log.medicationId }
            if (med != null) {
                events.add(TimelineEvent.MedicationTaken(log, med))
            }
        }
        events.sortedByDescending { it.timestamp }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun savePatient(name: String, age: Int, gender: String, language: String) {
        viewModelScope.launch {
            val patient = Patient(
                id = UUID.randomUUID().toString(),
                name = name,
                age = age,
                gender = gender,
                language = language
            )
            repository.insertPatient(patient)
        }
    }

    fun addCondition(type: String) {
        viewModelScope.launch {
            val patientId = patient.value?.id ?: return@launch
            val condition = Condition(
                id = UUID.randomUUID().toString(),
                patientId = patientId,
                type = type,
                diagnosedAt = System.currentTimeMillis(),
                status = "active"
            )
            repository.insertCondition(condition)
        }
    }

    fun deleteCondition(id: String) {
        viewModelScope.launch {
            repository.deleteCondition(id)
        }
    }

    private val geminiService = GeminiService()

    private val _extractedData = MutableStateFlow<GeminiService.MedicalExtraction?>(null)
    val extractedData: StateFlow<GeminiService.MedicalExtraction?> = _extractedData

    private val _isExtracting = MutableStateFlow(false)
    val isExtracting: StateFlow<Boolean> = _isExtracting

    private val _extractionStatus = MutableStateFlow("")
    val extractionStatus: StateFlow<String> = _extractionStatus

    /**
     * Extracts structured clinical metrics from a captured document image.
     * Persists the document image securely in internal private storage (filesDir/documents).
     */
    fun extractResultsFromBitmap(
        bitmap: Bitmap,
        context: Context,
        onComplete: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isExtracting.value = true
            _extractionStatus.value = "processing"

            val patientId = patient.value?.id ?: ""
            val documentId = UUID.randomUUID().toString()
            val docDir = File(context.filesDir, "documents").apply { mkdirs() }
            val fileName = "doc_$documentId.jpg"
            val file = File(docDir, fileName)

            try {
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }

                val document = Document(
                    id = documentId,
                    patientId = patientId,
                    fileName = fileName,
                    filePath = file.absolutePath,
                    fileType = "image/jpeg",
                    uploadedAt = System.currentTimeMillis(),
                    status = "processed"
                )
                repository.insertDocument(document)

                val results = geminiService.extractMedicalData(bitmap, documentId)

                // Link results to document provenance
                val linkedResults = results.copy(
                    labs = results.labs.map { it.copy(sourceDocumentId = documentId) }
                )

                _extractedData.value = linkedResults
                _extractionStatus.value = if (linkedResults.labs.isEmpty() && linkedResults.medications.isEmpty() && linkedResults.careTasks.isEmpty()) {
                    "no_results_found"
                } else {
                    "success"
                }
                onComplete(documentId)
            } catch (e: Exception) {
                Log.e("HealthViewModel", "Failed to extract document: ${e.message}", e)
                _extractionStatus.value = "error"
                _extractedData.value = GeminiService.MedicalExtraction(emptyList(), emptyList(), emptyList())
            } finally {
                _isExtracting.value = false
            }
        }
    }

    fun processDocument(context: Context, uri: Uri, onComplete: (String) -> Unit = {}) {
        try {
            val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            extractResultsFromBitmap(bitmap, context, onComplete)
        } catch (e: Exception) {
            Log.e("HealthViewModel", "Failed to load document URI: ${e.message}", e)
            _extractionStatus.value = "error"
        }
    }

    fun updateExtractedLab(index: Int, updatedLab: LabResult) {
        val currentData = _extractedData.value ?: return
        val updatedLabs = currentData.labs.toMutableList()
        if (index in updatedLabs.indices) {
            updatedLabs[index] = updatedLab
            _extractedData.value = currentData.copy(labs = updatedLabs)
        }
    }

    fun removeExtractedLab(index: Int) {
        val currentData = _extractedData.value ?: return
        val updatedLabs = currentData.labs.toMutableList()
        if (index in updatedLabs.indices) {
            updatedLabs.removeAt(index)
            _extractedData.value = currentData.copy(labs = updatedLabs)
        }
    }

    /**
     * Safety-critical verification: saves user-reviewed and verified records to local database.
     */
    fun confirmAndSaveExtraction(
        verifiedLabs: List<LabResult>,
        verifiedMeds: List<Medication>,
        verifiedTasks: List<CareTask>,
        workScheduler: com.example.notifications.MedicationWorkScheduler? = null
    ) {
        viewModelScope.launch {
            val patientId = patient.value?.id ?: return@launch
            val now = System.currentTimeMillis()

            for (lab in verifiedLabs) {
                repository.insertLabResult(
                    lab.copy(
                        patientId = patientId,
                        verifiedByUser = true,
                        confidence = 1.0
                    )
                )
                // Persist canonical Observation record
                repository.insertObservation(
                    lab.toObservation().copy(
                        verificationStatus = "verified",
                        confidence = 1.0
                    )
                )
            }
            for (med in verifiedMeds) {
                val medWithId = med.copy(patientId = patientId)
                repository.insertMedication(medWithId)
                workScheduler?.scheduleMedicationReminders(medWithId)
            }
            for (task in verifiedTasks) {
                val taskWithId = task.copy(patientId = patientId)
                repository.insertCareTask(taskWithId)
            }

            _extractedData.value = null
        }
    }

    private val _summary = MutableStateFlow("")
    val summary: StateFlow<String> = _summary

    private val _isGeneratingSummary = MutableStateFlow(false)
    val isGeneratingSummary: StateFlow<Boolean> = _isGeneratingSummary

    fun generateVisitSummary() {
        viewModelScope.launch {
            _isGeneratingSummary.value = true
            val patient = patient.value ?: return@launch
            val currentConditions = conditions.value.map { it.type }
            val currentLabs = labResults.value.take(15)
            val currentVitals = vitals.value.take(5).map {
                "${it.type}: ${it.value1}${if (it.value2 != null) "/${it.value2}" else ""} on ${SimpleDateFormat("MMM dd", Locale.US).format(Date(it.recordedAt))}"
            }

            val result = geminiService.generateSummary(
                language = patient.language,
                patientName = patient.name,
                conditions = currentConditions,
                labs = currentLabs,
                vitals = currentVitals
            )
            _summary.value = result
            _isGeneratingSummary.value = false
        }
    }

    fun logVital(type: String, value1: Double, value2: Double? = null, context: String? = null) {
        viewModelScope.launch {
            val patientId = patient.value?.id ?: return@launch
            val vital = Vital(
                id = UUID.randomUUID().toString(),
                patientId = patientId,
                type = type,
                value1 = value1,
                value2 = value2,
                context = context,
                recordedAt = System.currentTimeMillis()
            )
            repository.insertVital(vital)
        }
    }

    fun logMedication(medicationId: String, status: String = "taken") {
        viewModelScope.launch {
            val log = MedicationLog(
                id = UUID.randomUUID().toString(),
                medicationId = medicationId,
                takenAt = System.currentTimeMillis(),
                status = status
            )
            repository.insertMedicationLog(log)
        }
    }

    fun addManualMedication(
        name: String,
        dose: String,
        schedule: List<String>,
        workScheduler: com.example.notifications.MedicationWorkScheduler? = null
    ) {
        viewModelScope.launch {
            val patientId = patient.value?.id ?: return@launch
            val medication = Medication(
                id = UUID.randomUUID().toString(),
                patientId = patientId,
                name = name.trim(),
                dose = dose.trim(),
                schedule = schedule,
                startedAt = System.currentTimeMillis(),
                refillDueAt = null,
                conditionId = null
            )
            repository.insertMedication(medication)
            workScheduler?.scheduleMedicationReminders(medication)
        }
    }

    fun deleteMedication(id: String) {
        viewModelScope.launch {
            repository.deleteMedication(id)
        }
    }

    fun addManualLabResult(
        testName: String,
        value: Double,
        unit: String,
        date: Long,
        referenceLow: Double? = null,
        referenceHigh: Double? = null,
        laboratoryName: String? = null
    ) {
        viewModelScope.launch {
            val patientId = patient.value?.id ?: return@launch
            val status = when {
                referenceLow != null && value < referenceLow -> "low"
                referenceHigh != null && value > referenceHigh -> "high"
                else -> "normal"
            }
            val result = LabResult(
                id = UUID.randomUUID().toString(),
                patientId = patientId,
                testName = testName.trim(),
                value = value,
                unit = unit.trim(),
                referenceLow = referenceLow,
                referenceHigh = referenceHigh,
                status = status,
                collectedAt = date,
                laboratoryName = laboratoryName,
                reportDate = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(date)),
                sourceDocumentId = null,
                confidence = 1.0,
                verifiedByUser = true
            )
            repository.insertLabResult(result)
        }
    }

    fun deleteLabResult(id: String) {
        viewModelScope.launch {
            repository.deleteLabResult(id)
        }
    }

    fun updateCareTaskStatus(id: String, status: String) {
        viewModelScope.launch {
            repository.updateCareTaskStatus(id, status, System.currentTimeMillis())
        }
    }

    fun deleteCareTask(id: String) {
        viewModelScope.launch {
            repository.deleteCareTask(id)
        }
    }

    fun clearAllLabs() {
        viewModelScope.launch {
            val pId = patient.value?.id ?: return@launch
            repository.clearPatientLabs(pId)
        }
    }

    suspend fun getDocument(id: String): Document? = repository.getDocumentById(id)

    val isSyncingHealthConnect = MutableStateFlow(false)
    val healthConnectSyncMessage = MutableStateFlow<String?>(null)

    fun clearHealthConnectMessage() {
        healthConnectSyncMessage.value = null
    }

    fun syncHealthConnect(context: Context) {
        viewModelScope.launch {
            val p = patient.value ?: return@launch
            isSyncingHealthConnect.value = true
            val manager = com.example.util.HealthConnectManager(context)
            val result = manager.importVitals(p.id, repository)
            isSyncingHealthConnect.value = false
            result.onSuccess { count ->
                healthConnectSyncMessage.value = "Imported $count vitals from Health Connect"
            }.onFailure {
                healthConnectSyncMessage.value = "Health Connect sync completed"
            }
        }
    }

    fun testMedicationAlert(medication: Medication, context: Context) {
        val scheduler = com.example.notifications.MedicationWorkScheduler(context)
        scheduler.triggerImmediateTestReminder(medication)
    }
}

sealed class TimelineEvent {
    abstract val timestamp: Long

    data class Lab(val result: LabResult) : TimelineEvent() {
        override val timestamp: Long = result.collectedAt
    }

    data class VitalEntry(val vital: Vital) : TimelineEvent() {
        override val timestamp: Long = vital.recordedAt
    }

    data class MedicationTaken(val log: MedicationLog, val medication: Medication) : TimelineEvent() {
        override val timestamp: Long = log.takenAt
    }
}

class HealthViewModelFactory(private val repository: HealthRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HealthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HealthViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
