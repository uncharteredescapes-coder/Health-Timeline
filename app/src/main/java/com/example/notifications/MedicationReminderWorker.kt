package com.example.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.MainActivity

class MedicationReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val CHANNEL_ID = "medication_reminders"
        const val CHANNEL_NAME = "Medication Reminders"
        const val KEY_MED_ID = "med_id"
        const val KEY_MED_NAME = "med_name"
        const val KEY_MED_DOSE = "med_dose"
        const val KEY_MED_SLOT = "med_slot"
    }

    override suspend fun doWork(): Result {
        val medId = inputData.getString(KEY_MED_ID) ?: return Result.failure()
        val medName = inputData.getString(KEY_MED_NAME) ?: "Medication"
        val medDose = inputData.getString(KEY_MED_DOSE) ?: ""
        val medSlot = inputData.getString(KEY_MED_SLOT) ?: "Scheduled"

        sendNotification(medId, medName, medDose, medSlot)
        return Result.success()
    }

    private fun sendNotification(medId: String, name: String, dose: String, slot: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Timely alerts when scheduled medication times arrive"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "medications")
            putExtra("med_id", medId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            medId.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Medication Time: $name")
            .setContentText("It's time for your $dose dose ($slot). Tap to mark as taken.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("It's time to take $name ($dose) for your $slot schedule. Keeping to your routine supports steady clinical progress.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationId = (medId + slot).hashCode()
        notificationManager.notify(notificationId, notification)
    }
}
