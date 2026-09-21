package com.example.notifications

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.data.Medication
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MedicationWorkScheduler(private val context: Context) {

    private val workManager = WorkManager.getInstance(context)

    fun scheduleMedicationReminders(medication: Medication) {
        val scheduleList = if (medication.schedule.isEmpty()) listOf("morning") else medication.schedule

        scheduleList.forEach { slot ->
            val initialDelayMinutes = calculateInitialDelayMinutes(slot)
            val workData = workDataOf(
                MedicationReminderWorker.KEY_MED_ID to medication.id,
                MedicationReminderWorker.KEY_MED_NAME to medication.name,
                MedicationReminderWorker.KEY_MED_DOSE to medication.dose,
                MedicationReminderWorker.KEY_MED_SLOT to slot
            )

            val periodicRequest = PeriodicWorkRequestBuilder<MedicationReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
                .setInputData(workData)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(false)
                        .build()
                )
                .build()

            val uniqueWorkName = "med_work_${medication.id}_${slot.lowercase().replace(" ", "_")}"
            workManager.enqueueUniquePeriodicWork(
                uniqueWorkName,
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicRequest
            )
            Log.d("MedicationWorkScheduler", "Scheduled WorkManager reminder for ${medication.name} ($slot) in $initialDelayMinutes mins")
        }
    }

    fun triggerImmediateTestReminder(medication: Medication, slot: String = "Test Schedule") {
        val workData = workDataOf(
            MedicationReminderWorker.KEY_MED_ID to medication.id,
            MedicationReminderWorker.KEY_MED_NAME to medication.name,
            MedicationReminderWorker.KEY_MED_DOSE to medication.dose,
            MedicationReminderWorker.KEY_MED_SLOT to slot
        )

        val oneTimeRequest = OneTimeWorkRequestBuilder<MedicationReminderWorker>()
            .setInputData(workData)
            .setInitialDelay(2, TimeUnit.SECONDS)
            .build()

        workManager.enqueue(oneTimeRequest)
    }

    fun cancelMedicationReminders(medication: Medication) {
        val scheduleList = if (medication.schedule.isEmpty()) listOf("morning") else medication.schedule
        scheduleList.forEach { slot ->
            val uniqueWorkName = "med_work_${medication.id}_${slot.lowercase().replace(" ", "_")}"
            workManager.cancelUniqueWork(uniqueWorkName)
        }
    }

    private fun calculateInitialDelayMinutes(slot: String): Long {
        val normalized = slot.lowercase().trim()
        val targetHour = when {
            normalized.contains("morning") || normalized.contains("সকাল") -> 8
            normalized.contains("afternoon") || normalized.contains("দুপুর") -> 13
            normalized.contains("evening") || normalized.contains("সন্ধ্যা") -> 18
            normalized.contains("night") || normalized.contains("রাত") -> 21
            normalized.contains("bedtime") -> 22
            else -> {
                // Try to parse format like "8:00 AM" or "10:30"
                extractHourFromString(normalized) ?: 9
            }
        }
        val targetMinute = extractMinuteFromString(normalized) ?: 0

        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (target.before(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        val diffMs = target.timeInMillis - now.timeInMillis
        return TimeUnit.MILLISECONDS.toMinutes(diffMs).coerceAtLeast(1)
    }

    private fun extractHourFromString(timeStr: String): Int? {
        val match = Regex("""(\d{1,2}):(\d{2})?\s*(am|pm)?""").find(timeStr) ?: return null
        var hour = match.groupValues[1].toIntOrNull() ?: return null
        val amPm = match.groupValues[3].lowercase()
        if (amPm == "pm" && hour < 12) hour += 12
        if (amPm == "am" && hour == 12) hour = 0
        return hour
    }

    private fun extractMinuteFromString(timeStr: String): Int? {
        val match = Regex("""\d{1,2}:(\d{2})""").find(timeStr) ?: return null
        return match.groupValues[1].toIntOrNull()
    }
}
