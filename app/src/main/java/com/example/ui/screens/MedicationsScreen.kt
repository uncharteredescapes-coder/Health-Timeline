package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.Medication
import com.example.notifications.MedicationWorkScheduler
import com.example.ui.HealthViewModel
import com.example.ui.components.BadgeType
import com.example.ui.components.CheckCircle
import com.example.ui.components.DesignPrimaryButton
import com.example.ui.components.Eyebrow
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import java.util.*

@Composable
fun MedicationsScreen(viewModel: HealthViewModel = viewModel()) {
    val context = LocalContext.current
    val patient by viewModel.patient.collectAsStateWithLifecycle()
    val medications by viewModel.medications.collectAsStateWithLifecycle()
    val medicationLogs by viewModel.medicationLogs.collectAsStateWithLifecycle()

    val language = patient?.language ?: "en"
    val t: (String, String) -> String = { en, bn -> if (language == "en") en else bn }

    val scheduler = remember { MedicationWorkScheduler(context) }
    var showAddMedDialog by remember { mutableStateOf(false) }

    // Start of today timestamp
    val startOfToday = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Canvas)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        // Topbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = t("Medications", "ওষুধসমূহ"),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = BodyFontFamily,
                color = Ink
            )
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(PrimaryTint)
                    .clickable { showAddMedDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Medication",
                    tint = Primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (medications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "💊", fontSize = 40.sp)
                    Text(
                        text = t("No active medications", "কোনো সক্রিয় ওষুধ নেই"),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink,
                        fontFamily = BodyFontFamily
                    )
                    Text(
                        text = t(
                            "Scan a prescription or tap '+' to add your current medications and schedule daily reminders.",
                            "প্রেসক্রিপশন স্ক্যান করুন অথবা '+' চাপ দিয়ে ওষুধ যোগ করে প্রতিদিনের রিমাইন্ডার সেট করুন।"
                        ),
                        fontSize = 13.sp,
                        color = InkSoft,
                        fontFamily = BodyFontFamily,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    DesignPrimaryButton(
                        text = t("Add Medication", "ওষুধ যোগ করুন"),
                        onClick = { showAddMedDialog = true },
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            // Medication Cards
            medications.forEach { med ->
                val logsForMed = medicationLogs.filter { it.medicationId == med.id }
                val takenCount = logsForMed.count { it.status == "taken" }
                val totalLogged = logsForMed.size

                val adherencePercent = if (totalLogged > 0) {
                    (takenCount * 100) / totalLogged
                } else {
                    100
                }

                val isTakenToday = logsForMed.any { it.takenAt >= startOfToday && it.status == "taken" }

                val badgeType = if (adherencePercent >= 80) BadgeType.OK else BadgeType.WATCH
                val barColor = if (adherencePercent >= 80) Primary else Accent

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Line),
                    color = Surface
                ) {
                    Column(modifier = Modifier.padding(15.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${med.name} ${med.dose}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Ink,
                                    fontFamily = BodyFontFamily,
                                    modifier = Modifier.padding(bottom = 3.dp)
                                )
                                Text(
                                    text = med.schedule.joinToString(" · "),
                                    fontSize = 12.sp,
                                    color = InkSoft,
                                    fontFamily = BodyFontFamily
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                StatusBadge(
                                    text = "$adherencePercent%",
                                    type = badgeType
                                )
                                IconButton(
                                    onClick = { viewModel.deleteMedication(med.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = InkSoft.copy(alpha = 0.5f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Progress Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .padding(vertical = 1.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Line)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = (adherencePercent / 100f).coerceIn(0.05f, 1f))
                                    .fillMaxHeight()
                                    .background(barColor)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Dose Status and Logger
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CheckCircle(
                                    checked = isTakenToday,
                                    onToggle = {
                                        if (isTakenToday) {
                                            viewModel.logMedication(med.id, "missed")
                                        } else {
                                            viewModel.logMedication(med.id, "taken")
                                            Toast.makeText(
                                                context,
                                                t("Dose logged as taken", "ডোজ গ্রহণ চিহ্নিত করা হয়েছে"),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                )
                                Text(
                                    text = if (isTakenToday) {
                                        t("Taken today", "আজ গ্রহণ করা হয়েছে")
                                    } else {
                                        t("Due today · Tap to log", "আজ নির্ধারিত · চিহ্নিত করতে চাপুন")
                                    },
                                    fontSize = 12.5.sp,
                                    fontWeight = if (isTakenToday) FontWeight.Normal else FontWeight.SemiBold,
                                    color = if (isTakenToday) InkSoft else Primary,
                                    fontFamily = BodyFontFamily
                                )
                            }

                            IconButton(
                                onClick = {
                                    viewModel.testMedicationAlert(med, context)
                                    Toast.makeText(
                                        context,
                                        t("Reminder test triggered", "রিমাইন্ডার পাঠানো হয়েছে"),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Test Notification",
                                    tint = Primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }

    // Add Medication Dialog
    if (showAddMedDialog) {
        var medName by remember { mutableStateOf("") }
        var medDose by remember { mutableStateOf("") }
        var isMorning by remember { mutableStateOf(true) }
        var isAfternoon by remember { mutableStateOf(false) }
        var isEvening by remember { mutableStateOf(false) }
        var isNight by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddMedDialog = false },
            title = {
                Text(
                    text = t("Add Medication", "নতুন ওষুধ যোগ করুন"),
                    fontWeight = FontWeight.Bold,
                    fontFamily = BodyFontFamily
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = medName,
                        onValueChange = { medName = it },
                        label = { Text(t("Medication Name (e.g. Metformin)", "ওষুধের নাম")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = medDose,
                        onValueChange = { medDose = it },
                        label = { Text(t("Dosage (e.g. 500mg)", "ডোজ (যেমন: ৫০০ মিলিগ্রাম)")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = t("Daily Schedule", "দৈনিক সময়সূচী"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = InkSoft,
                        fontFamily = BodyFontFamily,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ScheduleCheckbox(label = t("Morn", "সকাল"), checked = isMorning) { isMorning = it }
                        ScheduleCheckbox(label = t("Noon", "দুপুর"), checked = isAfternoon) { isAfternoon = it }
                        ScheduleCheckbox(label = t("Eve", "সন্ধ্যা"), checked = isEvening) { isEvening = it }
                        ScheduleCheckbox(label = t("Night", "রাত"), checked = isNight) { isNight = it }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (medName.isNotBlank()) {
                            val scheduleList = mutableListOf<String>()
                            if (isMorning) scheduleList.add("Morning")
                            if (isAfternoon) scheduleList.add("Afternoon")
                            if (isEvening) scheduleList.add("Evening")
                            if (isNight) scheduleList.add("Night")
                            if (scheduleList.isEmpty()) scheduleList.add("Morning")

                            viewModel.addManualMedication(
                                name = medName,
                                dose = medDose.ifBlank { "1 dose" },
                                schedule = scheduleList,
                                workScheduler = scheduler
                            )
                            showAddMedDialog = false
                        }
                    }
                ) {
                    Text(t("Add", "যোগ করুন"), color = Primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMedDialog = false }) {
                    Text(t("Cancel", "বাতিল"))
                }
            }
        )
    }
}

@Composable
private fun ScheduleCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { onCheckedChange(!checked) }
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = Primary)
        )
        Text(text = label, fontSize = 12.sp, fontFamily = BodyFontFamily)
    }
}
