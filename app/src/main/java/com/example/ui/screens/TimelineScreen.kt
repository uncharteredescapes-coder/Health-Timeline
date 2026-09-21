package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.notifications.MedicationWorkScheduler
import com.example.ui.HealthViewModel
import com.example.ui.TimelineEvent
import com.example.ui.components.BadgeType
import com.example.ui.components.MonthHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    onNavigateToCapture: () -> Unit,
    viewModel: HealthViewModel = viewModel()
) {
    val context = LocalContext.current
    val patient by viewModel.patient.collectAsStateWithLifecycle()
    val timelineEvents by viewModel.timelineEvents.collectAsStateWithLifecycle()
    val isSyncingHealthConnect by viewModel.isSyncingHealthConnect.collectAsStateWithLifecycle()
    val healthConnectSyncMessage by viewModel.healthConnectSyncMessage.collectAsStateWithLifecycle()

    val language = patient?.language ?: "en"
    val t: (String, String) -> String = { en, bn -> if (language == "en") en else bn }

    var showManualInput by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("All") }

    val filterOptions = listOf(
        "All" to t("All", "সব"),
        "Labs" to t("Labs", "ল্যাব"),
        "Vitals" to t("Vitals", "ভাইটাল"),
        "Meds" to t("Meds", "ওষুধ")
    )

    val displayEvents = timelineEvents

    val filteredEvents = remember(displayEvents, selectedFilter) {
        when (selectedFilter) {
            "Labs" -> displayEvents.filterIsInstance<TimelineEvent.Lab>()
            "Vitals" -> displayEvents.filterIsInstance<TimelineEvent.VitalEntry>()
            "Meds" -> displayEvents.filterIsInstance<TimelineEvent.MedicationTaken>()
            else -> displayEvents
        }
    }

    // Group events by Month and Year: e.g. "September 2026", "August 2026"
    val monthFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val groupedEvents = remember(filteredEvents) {
        filteredEvents.sortedByDescending { it.timestamp }.groupBy { event ->
            monthFormat.format(Date(event.timestamp))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Canvas)
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
                text = t("Health timeline", "স্বাস্থ্য টাইমলাইন"),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = BodyFontFamily,
                color = Ink
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = { viewModel.syncHealthConnect(context) },
                    enabled = !isSyncingHealthConnect,
                    modifier = Modifier.size(34.dp)
                ) {
                    if (isSyncingHealthConnect) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Primary)
                    } else {
                        Icon(Icons.Default.Sync, contentDescription = "Sync Health Connect", tint = Primary, modifier = Modifier.size(20.dp))
                    }
                }
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .clickable { showManualInput = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✚",
                        fontSize = 18.sp,
                        color = Primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Health Connect message banner if available
        if (healthConnectSyncMessage != null) {
            Surface(
                color = PrimaryTint,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Primary.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "✓", color = Primary, fontWeight = FontWeight.Bold)
                        Text(
                            text = healthConnectSyncMessage!!,
                            fontSize = 12.sp,
                            color = Ink,
                            fontFamily = BodyFontFamily
                        )
                    }
                    IconButton(
                        onClick = { viewModel.clearHealthConnectMessage() },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = InkSoft, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        // Filter Chipbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filterOptions.forEach { (key, label) ->
                val isActive = selectedFilter == key
                Surface(
                    onClick = { selectedFilter = key },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isActive) Ink else Color.Transparent,
                    border = BorderStroke(1.dp, if (isActive) Ink else Line)
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isActive) Surface else InkSoft,
                        fontFamily = BodyFontFamily,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }
        }

        // Timeline Items grouped by Month
        if (groupedEvents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(text = "📅", fontSize = 40.sp)
                    Text(
                        text = t("No health events recorded yet", "এখনও কোনো স্বাস্থ্য ইভেন্ট নেই"),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink,
                        fontFamily = BodyFontFamily
                    )
                    Text(
                        text = t(
                            "Scan a lab report or prescription, or tap '+' above to add your first record.",
                            "নতুন রিপোর্ট স্ক্যান করুন অথবা উপরে '+' চিহ্নে চাপ দিয়ে তথ্য যোগ করুন।"
                        ),
                        fontSize = 13.sp,
                        color = InkSoft,
                        fontFamily = BodyFontFamily,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 110.dp)
            ) {
                groupedEvents.forEach { (monthLabel, eventsInMonth) ->
                    item(key = "header-$monthLabel") {
                        MonthHeader(
                            text = monthLabel,
                            modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
                        )
                    }

                    items(eventsInMonth) { event ->
                        TimelineCard(event = event, t = t)
                    }
                }
            }
        }
    }

    if (showManualInput) {
        val workScheduler = remember { MedicationWorkScheduler(context) }
        ManualInputBottomSheet(
            onDismiss = { showManualInput = false },
            onSaveLab = { name, value, unit, date ->
                viewModel.addManualLabResult(name, value, unit, date)
            },
            onSaveVital = { type, v1, v2, ctx ->
                viewModel.logVital(type, v1, v2, ctx)
            },
            onSaveMed = { name, dose, schedule ->
                viewModel.addManualMedication(name, dose, schedule, workScheduler)
            },
            t = t
        )
    }
}

@Composable
fun TimelineCard(event: TimelineEvent, t: (String, String) -> String) {
    val timeFormat = remember { SimpleDateFormat("MMM d · h:mm a", Locale.getDefault()) }
    val metaText = timeFormat.format(Date(event.timestamp))

    val iconEmoji: String
    val iconBg: Color
    val title: String
    val displayValue: String
    val valueColor: Color
    val badgeText: String
    val badgeType: BadgeType

    when (event) {
        is TimelineEvent.Lab -> {
            iconEmoji = "🧪"
            iconBg = AlertTint
            title = event.result.testName
            displayValue = "${event.result.value} ${event.result.unit}"
            valueColor = Primary
            badgeText = t("Lab Report", "ল্যাব রিপোর্ট")
            badgeType = BadgeType.ATTN
        }
        is TimelineEvent.VitalEntry -> {
            val isBP = event.vital.type.contains("pressure", ignoreCase = true)
            iconEmoji = "❤"
            iconBg = PrimaryTint
            title = if (isBP) t("Blood pressure", "রক্তচাপ") else event.vital.type.replaceFirstChar { it.uppercase() }
            displayValue = if (isBP && event.vital.value2 != null) {
                "${event.vital.value1.toInt()}/${event.vital.value2!!.toInt()}"
            } else {
                "${event.vital.value1}"
            }
            valueColor = Ink
            badgeText = t("Vital", "ভাইটাল")
            badgeType = BadgeType.OK
        }
        is TimelineEvent.MedicationTaken -> {
            iconEmoji = "💊"
            iconBg = AccentTint
            title = event.medication.name
            displayValue = t("Dose taken", "ডোজ নেওয়া হয়েছে")
            valueColor = Accent
            badgeText = t("Medication", "ওষুধ")
            badgeType = BadgeType.WATCH
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Line),
        color = Surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 42x42dp circular icon chip
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Text(text = iconEmoji, fontSize = 18.sp)
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink,
                        fontFamily = BodyFontFamily,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    StatusBadge(
                        text = badgeText,
                        type = badgeType
                    )
                }
                Text(
                    text = metaText,
                    fontSize = 11.5.sp,
                    color = InkSoft,
                    fontFamily = BodyFontFamily,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Text(
                text = displayValue,
                style = CardValueStyle,
                color = valueColor,
                fontSize = if (displayValue.contains("taken")) 13.sp else 16.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualInputBottomSheet(
    onDismiss: () -> Unit,
    onSaveLab: (String, Double, String, Long) -> Unit,
    onSaveVital: (String, Double, Double?, String?) -> Unit,
    onSaveMed: (String, String, List<String>) -> Unit,
    t: (String, String) -> String
) {
    val sheetState = rememberModalBottomSheetState()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Lab, 1: Vital, 2: Med

    // Lab fields
    var testName by remember { mutableStateOf("") }
    var labValue by remember { mutableStateOf("") }
    var labUnit by remember { mutableStateOf("") }

    // Vital fields
    var vitalType by remember { mutableStateOf("blood_pressure") }
    var vitalVal1 by remember { mutableStateOf("") }
    var vitalVal2 by remember { mutableStateOf("") }
    var vitalContext by remember { mutableStateOf("") }

    // Med fields
    var medName by remember { mutableStateOf("") }
    var medDose by remember { mutableStateOf("") }
    var medSchedule by remember { mutableStateOf("Morning · Evening") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Canvas,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 36.dp)
                .padding(horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = t("Manual entry", "ম্যানুয়াল এন্ট্রি"),
                    style = GreetingNameStyle,
                    fontSize = 20.sp,
                    color = Ink
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Text(text = "✕", fontSize = 16.sp, color = InkSoft)
                }
            }

            // Chipbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(0 to t("Lab", "ল্যাব"), 1 to t("Vital", "ভাইটাল"), 2 to t("Med", "ওষুধ")).forEach { (idx, label) ->
                    val isActive = selectedTab == idx
                    Surface(
                        onClick = { selectedTab = idx },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isActive) Ink else Color.Transparent,
                        border = BorderStroke(1.dp, if (isActive) Ink else Line)
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isActive) Surface else InkSoft,
                            fontFamily = BodyFontFamily,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            Text(
                text = t(
                    "Add a value your device didn't scan — schedules update automatically.",
                    "আপনার ডিভাইস স্ক্যান করেনি এমন মান যোগ করুন — সময়সূচী স্বয়ংক্রিয়ভাবে আপডেট হয়।"
                ),
                fontSize = 12.sp,
                color = InkSoft,
                fontFamily = BodyFontFamily,
                modifier = Modifier.padding(bottom = 18.dp)
            )

            when (selectedTab) {
                0 -> {
                    OutlinedTextField(
                        value = testName,
                        onValueChange = { testName = it },
                        label = { Text(t("Test Name", "পরীক্ষার নাম")) },
                        placeholder = { Text("e.g. HbA1c") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = labValue,
                            onValueChange = { labValue = it },
                            label = { Text(t("Value", "মান")) },
                            placeholder = { Text("e.g. 6.8") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = labUnit,
                            onValueChange = { labUnit = it },
                            label = { Text(t("Unit", "একক")) },
                            placeholder = { Text("e.g. %") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
                1 -> {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = vitalVal1,
                            onValueChange = { vitalVal1 = it },
                            label = { Text(t("Systolic", "সিস্টোলিক")) },
                            placeholder = { Text("128") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = vitalVal2,
                            onValueChange = { vitalVal2 = it },
                            label = { Text(t("Diastolic", "ডায়াস্টোলিক")) },
                            placeholder = { Text("82") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
                2 -> {
                    OutlinedTextField(
                        value = medName,
                        onValueChange = { medName = it },
                        label = { Text(t("Medication Name", "ওষুধের নাম")) },
                        placeholder = { Text("e.g. Metformin") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = medDose,
                        onValueChange = { medDose = it },
                        label = { Text(t("Dose", "ডোজ")) },
                        placeholder = { Text("e.g. 500mg") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    when (selectedTab) {
                        0 -> {
                            val v = labValue.toDoubleOrNull() ?: 0.0
                            if (testName.isNotBlank() && v > 0) {
                                onSaveLab(testName, v, labUnit.ifBlank { "%" }, System.currentTimeMillis())
                                onDismiss()
                            }
                        }
                        1 -> {
                            val v1 = vitalVal1.toDoubleOrNull() ?: 0.0
                            val v2 = vitalVal2.toDoubleOrNull()
                            if (v1 > 0) {
                                onSaveVital(vitalType, v1, v2, vitalContext.ifBlank { null })
                                onDismiss()
                            }
                        }
                        2 -> {
                            if (medName.isNotBlank()) {
                                onSaveMed(medName, medDose.ifBlank { "500mg" }, listOf(medSchedule))
                                onDismiss()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text(
                    text = t("Save result", "ফলাফল সংরক্ষণ করুন"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}
