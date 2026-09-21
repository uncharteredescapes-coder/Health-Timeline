package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.data.CareTask
import com.example.data.Medication
import java.util.Calendar

class ReminderManager(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleMedicationReminders(medication: Medication) {
        medication.schedule.forEachIndexed { index, timeStr ->
            val calendar = parseTimeToCalendar(timeStr) ?: return@forEachIndexed
            
            // If the time has already passed today, schedule for tomorrow
            if (calendar.timeInMillis < System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra("title", "Medication Reminder")
                putExtra("message", "Time to take ${medication.name} (${medication.dose})")
                putExtra("type", "medication")
                putExtra("id", medication.id.hashCode() + index)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                medication.id.hashCode() + index,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun scheduleCareTaskReminder(task: CareTask) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("title", "Appointment/Task Reminder")
            putExtra("message", task.title)
            putExtra("type", "appointment")
            putExtra("id", task.id.hashCode())
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Schedule for 9 AM on the due date, or immediately if dueAt is in the future
        val calendar = Calendar.getInstance().apply {
            timeInMillis = task.dueAt
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
        }

        if (calendar.timeInMillis > System.currentTimeMillis()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    private fun parseTimeToCalendar(timeStr: String): Calendar? {
        val calendar = Calendar.getInstance()
        val normalizedTime = timeStr.lowercase().trim()
        
        when {
            normalizedTime.contains("morning") -> {
                calendar.set(Calendar.HOUR_OF_DAY, 8)
                calendar.set(Calendar.MINUTE, 0)
            }
            normalizedTime.contains("afternoon") -> {
                calendar.set(Calendar.HOUR_OF_DAY, 13)
                calendar.set(Calendar.MINUTE, 0)
            }
            normalizedTime.contains("evening") -> {
                calendar.set(Calendar.HOUR_OF_DAY, 18)
                calendar.set(Calendar.MINUTE, 0)
            }
            normalizedTime.contains("night") -> {
                calendar.set(Calendar.HOUR_OF_DAY, 21)
                calendar.set(Calendar.MINUTE, 0)
            }
            else -> return null // Could add more robust parsing for "10:00 AM" etc.
        }
        
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar
    }
}
