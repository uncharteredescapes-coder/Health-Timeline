package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.ui.HealthViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    onNavigateToTimeline: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToCondition: (String) -> Unit,
    viewModel: HealthViewModel = viewModel()
) {
    val patient by viewModel.patient.collectAsStateWithLifecycle()
    val conditions by viewModel.conditions.collectAsStateWithLifecycle()
    val labResults by viewModel.labResults.collectAsStateWithLifecycle()
    val medications by viewModel.medications.collectAsStateWithLifecycle()
    val medicationLogs by viewModel.medicationLogs.collectAsStateWithLifecycle()
    val careTasks by viewModel.careTasks.collectAsStateWithLifecycle()

    val language = patient?.language ?: "en"
    val t: (String, String) -> String = { en, bn -> if (language == "en") en else bn }

    val initials = remember(patient?.name) {
        val n = patient?.name?.trim() ?: "Health User"
        n.split("\\s+".toRegex()).take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("").ifEmpty { "HU" }
    }

    val todayCalendar = remember {
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
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        // Greeting Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = t("Welcome", "স্বাগতম"),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = InkSoft,
                    fontFamily = BodyFontFamily,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Text(
                    text = patient?.name ?: "Health Timeline",
                    style = GreetingNameStyle,
                    color = Ink
                )
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Primary)
                    .clickable { onNavigateToProfile() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = BodyFontFamily
                )
            }
        }

        // Timeline Shortcut Button
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            shape = RoundedCornerShape(14.dp),
            color = PrimaryTint,
            onClick = onNavigateToTimeline
        ) {
            Row(
                modifier = Modifier.padding(vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "◷",
                    color = Primary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = t("View full health timeline", "পুরো স্বাস্থ্য টাইমলাইন দেখুন"),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Primary,
                    fontFamily = BodyFontFamily
                )
            }
        }

        // Primary Condition / Metric Hero Card
        val defaultConditionId = conditions.firstOrNull()?.id ?: "diabetes"
        val diabetesLabs = labResults.filter {
            it.testName.contains("hba1c", ignoreCase = true) || it.testName.contains("glucose", ignoreCase = true)
        }.sortedByDescending { it.collectedAt }

        val latestReading = diabetesLabs.firstOrNull()
        val previousReading = if (diabetesLabs.size >= 2) diabetesLabs[1] else null
        val sparklinePoints = remember(diabetesLabs) {
            if (diabetesLabs.size >= 2) {
                diabetesLabs.sortedBy { it.collectedAt }.map { it.value }
            } else emptyList()
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, Line),
            color = Surface,
            onClick = { onNavigateToCondition(defaultConditionId) }
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.linearGradient(
                            colors = listOf(PrimaryTint, Surface),
                            start = androidx.compose.ui.geometry.Offset(0f, 0f),
                            end = androidx.compose.ui.geometry.Offset(600f, 600f)
                        )
                    )
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "♥", color = InkSoft, fontSize = 14.sp)
                        Text(
                            text = if (conditions.isNotEmpty()) conditions.first().type else t("Metabolic Health", "মেটাবলিক স্বাস্থ্য"),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = InkSoft,
                            fontFamily = BodyFontFamily
                        )
                    }

                    if (latestReading != null) {
                        val badgeType = when (latestReading.status.lowercase()) {
                            "high" -> BadgeType.WATCH
                            "low" -> BadgeType.WATCH
                            else -> BadgeType.OK
                        }
                        StatusBadge(
                            text = latestReading.status.uppercase(),
                            type = badgeType
                        )
                    }
                }

                if (latestReading != null) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(
                            text = latestReading.value.toString(),
                            style = HeroValueStyle,
                            color = Ink
                        )
                        val deltaText = if (previousReading != null) {
                            val diff = latestReading.value - previousReading.value
                            val sign = if (diff >= 0) "+" else ""
                            val diffStr = String.format(Locale.US, "%.1f", diff)
                            "${latestReading.unit} ${latestReading.testName} (${sign}$diffStr vs prev)"
                        } else {
                            "${latestReading.unit} ${latestReading.testName}"
                        }
                        Text(
                            text = deltaText,
                            fontSize = 12.sp,
                            color = InkSoft,
                            fontFamily = BodyFontFamily
                        )
                    }

                    if (sparklinePoints.size >= 2) {
                        Sparkline(
                            points = sparklinePoints,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .padding(top = 8.dp),
                            color = Primary
                        )
                    }
                } else {
                    // Empty state for hero
                    Column(modifier = Modifier.padding(vertical = 12.dp)) {
                        Text(
                            text = t("No Lab Tests Recorded", "কোনো ল্যাব টেস্ট রেকর্ড নেই"),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Ink,
                            fontFamily = BodyFontFamily
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = t(
                                "Scan your diagnostic reports to view longitudinal biomarker trends.",
                                "দীর্ঘমেয়াদী ট্র্যাকিং দেখতে ল্যাব রিপোর্ট স্ক্যান করুন।"
                            ),
                            fontSize = 12.sp,
                            color = InkSoft,
                            fontFamily = BodyFontFamily
                        )
                    }
                }
            }
        }

        // Eyebrow: Today's medication
        Eyebrow(
            text = t("Today's medication", "আজকের ওষুধ"),
            modifier = Modifier.padding(top = 26.dp, bottom = 10.dp)
        )

        // Today's Medication List
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Line),
            color = Surface
        ) {
            if (medications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = t("No scheduled medications today.", "আজকের জন্য কোনো ওষুধ নির্ধারিত নেই।"),
                        fontSize = 13.sp,
                        color = InkSoft,
                        fontFamily = BodyFontFamily
                    )
                }
            } else {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                    medications.forEachIndexed { index, med ->
                        val isTakenToday = medicationLogs.any {
                            it.medicationId == med.id && it.takenAt >= todayCalendar && it.status == "taken"
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CheckCircle(
                                checked = isTakenToday,
                                onToggle = {
                                    if (isTakenToday) {
                                        viewModel.logMedication(med.id, "missed")
                                    } else {
                                        viewModel.logMedication(med.id, "taken")
                                    }
                                }
                            )
                            Column {
                                Text(
                                    text = "${med.name} ${med.dose}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Ink,
                                    fontFamily = BodyFontFamily
                                )
                                Text(
                                    text = med.schedule.joinToString(" · "),
                                    fontSize = 12.sp,
                                    color = InkSoft,
                                    fontFamily = BodyFontFamily,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }

                        if (index < medications.size - 1) {
                            HorizontalDivider(color = Line, thickness = 1.dp)
                        }
                    }
                }
            }
        }

        // Eyebrow: Care Tasks & Follow-ups
        Eyebrow(
            text = t("Care tasks & follow-ups", "যত্ন পরিকল্পনা ও ফলো-আপ"),
            modifier = Modifier.padding(top = 26.dp, bottom = 10.dp)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Line),
            color = Surface
        ) {
            if (careTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = t("All clinical tasks and appointments up to date.", "সব ফলো-আপ ও কাজ সম্পন্ন।"),
                        fontSize = 13.sp,
                        color = InkSoft,
                        fontFamily = BodyFontFamily
                    )
                }
            } else {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                    careTasks.forEachIndexed { index, task ->
                        val isCompleted = task.status == "completed"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(if (isCompleted) PrimaryTint else AccentTint),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (task.category.contains("lab", ignoreCase = true)) "🩸" else "📋",
                                    fontSize = 17.sp
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Ink,
                                    fontFamily = BodyFontFamily
                                )
                                Text(
                                    text = if (task.dueAt != null) {
                                        SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(task.dueAt))
                                    } else task.category,
                                    fontSize = 12.sp,
                                    color = InkSoft,
                                    fontFamily = BodyFontFamily,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    val newStatus = if (isCompleted) "pending" else "completed"
                                    viewModel.updateCareTaskStatus(task.id, newStatus)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text(
                                    text = if (isCompleted) "✓" else "○",
                                    color = if (isCompleted) Primary else InkSoft,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }

                        if (index < careTasks.size - 1) {
                            HorizontalDivider(color = Line, thickness = 1.dp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(110.dp))
    }
}
