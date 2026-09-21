package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Health Timeline", appName)
  }

  @Test
  fun `test biometric auth preferences toggle`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val biometricManager = com.example.util.BiometricAuthManager(context)
    biometricManager.isBiometricLockEnabled = true
    assertEquals(true, biometricManager.isBiometricLockEnabled)
    biometricManager.isBiometricLockEnabled = false
    assertEquals(false, biometricManager.isBiometricLockEnabled)
  }

  @Test
  fun `test medication scheduler delay calculation`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val config = androidx.work.Configuration.Builder()
      .setMinimumLoggingLevel(android.util.Log.DEBUG)
      .setExecutor(java.util.concurrent.Executors.newSingleThreadExecutor())
      .build()
    try {
      androidx.work.WorkManager.initialize(context, config)
    } catch (_: Exception) {}
    val scheduler = com.example.notifications.MedicationWorkScheduler(context)
    val med = com.example.data.Medication(
      id = "test-med",
      patientId = "test-patient",
      name = "Metformin",
      dose = "500mg",
      schedule = listOf("Morning", "Bedtime"),
      startedAt = System.currentTimeMillis(),
      refillDueAt = null,
      conditionId = null
    )
    scheduler.scheduleMedicationReminders(med)
  }
}
