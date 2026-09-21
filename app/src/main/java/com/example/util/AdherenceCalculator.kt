package com.example.util

import com.example.data.Medication
import com.example.data.MedicationLog
import java.util.concurrent.TimeUnit

object AdherenceCalculator {
    fun calculateAdherence(medication: Medication, logs: List<MedicationLog>, windowDays: Int = 14): Int {
        val now = System.currentTimeMillis()
        val windowStart = now - TimeUnit.DAYS.toMillis(windowDays.toLong())
        val effectiveStart = maxOf(medication.startedAt, windowStart)
        
        val relevantLogs = logs.filter { 
            it.medicationId == medication.id && 
            it.status == "taken" && 
            it.takenAt >= effectiveStart 
        }
        
        val daysInWindow = TimeUnit.MILLISECONDS.toDays(now - effectiveStart).coerceAtLeast(1)
        val expectedDosesPerDay = medication.schedule.size.coerceAtLeast(1)
        val totalExpectedDoses = (daysInWindow * expectedDosesPerDay).coerceAtLeast(1)
        
        return ((relevantLogs.size.toDouble() / totalExpectedDoses.toDouble()) * 100).toInt().coerceIn(0, 100)
    }
}
