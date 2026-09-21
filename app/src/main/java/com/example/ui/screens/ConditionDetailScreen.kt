package com.example.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.HealthViewModel
import com.example.ui.components.LinkRow
import com.example.ui.components.Sparkline
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ConditionDetailScreen(
    conditionId: String,
    onBack: () -> Unit,
    onViewReport: (String) -> Unit = {},
    viewModel: HealthViewModel = viewModel()
) {
    val patient by viewModel.patient.collectAsStateWithLifecycle()
    val conditions by viewModel.conditions.collectAsStateWithLifecycle()
    val labResults by viewModel.labResults.collectAsStateWithLifecycle()
    val medications by viewModel.medications.collectAsStateWithLifecycle()
    val careTasks by viewModel.careTasks.collectAsStateWithLifecycle()

    val condition = conditions.find { it.id == conditionId } ?: conditions.firstOrNull()
    val language = patient?.language ?: "en"
    val t: (String, String) -> String = { en, bn -> if (language == "en") en else bn }

    val conditionName = when (condition?.type?.lowercase()) {
        "diabetes" -> t("Diabetes", "ডায়াবেটিস")
        "hypertension" -> t("Hypertension", "উচ্চ রক্তচাপ")
        "cholesterol" -> t("High cholesterol", "উচ্চ কোলেস্টেরল")
        "kidney" -> t("Kidney disease", "কিডনি রোগ")
        "heart" -> t("Heart disease", "হৃদরোগ")
        else -> condition?.type?.replaceFirstChar { it.uppercase() } ?: t("Metabolic Health", "মেটাবলিক স্বাস্থ্য")
    }

    // Filter relevant labs for this condition
    val relevantLabs = labResults.filter {
        val name = it.testName.lowercase()
        when (condition?.type?.lowercase()) {
            "hypertension" -> name.contains("blood pressure") || name.contains("pressure")
            "cholesterol" -> name.contains("cholesterol") || name.contains("lipid") || name.contains("ldl") || name.contains("hdl")
            "kidney" -> name.contains("creatinine") || name.contains("egfr") || name.contains("urea") || name.contains("albumin")
            else -> name.contains("hba1c") || name.contains("glucose") || name.contains("sugar")
        }
    }.sortedByDescending { it.collectedAt }

    val latestReading = relevantLabs.firstOrNull()
    val earliestReading = relevantLabs.lastOrNull()
    val sparklinePoints = remember(relevantLabs) {
        if (relevantLabs.size >= 2) {
            relevantLabs.sortedBy { it.collectedAt }.map { it.value }
        } else emptyList()
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
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clickable { onBack() },
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "←",
                    fontSize = 22.sp,
                    color = Primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = conditionName,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = BodyFontFamily,
                color = Ink
            )
            Spacer(modifier = Modifier.size(34.dp))
        }

        // Detail Hero Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 22.dp),
            shape = RoundedCornerShape(22.dp),
            color = PrimaryDeep
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${relevantLabs.size} ${t("recorded readings", "পরীক্ষার ফলাফল")}",
                        fontSize = 12.5.sp,
                        color = Color.White.copy(alpha = 0.75f),
                        fontFamily = BodyFontFamily
                    )
                    if (latestReading != null) {
                        Surface(
                            color = Color.White.copy(alpha = 0.16f),
                            shape = CircleShape
                        ) {
                            Text(
                                text = latestReading.status.uppercase(),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = BodyFontFamily,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                if (latestReading != null) {
                    Text(
                        text = "${latestReading.value} ${latestReading.unit}",
                        style = DetailHeroValueStyle,
                        color = Color.White,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )

                    val summaryNote = if (relevantLabs.size >= 2 && earliestReading != null) {
                        val delta = latestReading.value - earliestReading.value
                        val sign = if (delta >= 0) "+" else ""
                        "${latestReading.testName} · ${sign}${String.format(Locale.US, "%.1f", delta)} since first recorded reading"
                    } else {
                        "${latestReading.testName} · Latest reading recorded"
                    }

                    Text(
                        text = summaryNote,
                        fontSize = 12.5.sp,
                        color = Color.White.copy(alpha = 0.75f),
                        fontFamily = BodyFontFamily,
                        lineHeight = 17.sp
                    )

                    if (sparklinePoints.size >= 2) {
                        Sparkline(
                            points = sparklinePoints,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .padding(top = 14.dp),
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                } else {
                    Text(
                        text = t("No lab readings yet", "এখনও কোনো পরীক্ষার ফলাফল নেই"),
                        fontSize = 16.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                    Text(
                        text = t("Scan diagnostic reports to monitor this condition.", "রিপোর্ট স্ক্যান করে তথ্য সংরক্ষণ করুন।"),
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // Section: Linked medications
        Text(
            text = t("Linked medications", "সংযুক্ত ওষুধ"),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Ink,
            fontFamily = BodyFontFamily,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, Line)
        ) {
            if (medications.isEmpty()) {
                Text(
                    text = t("No medications linked to this condition", "এই রোগের জন্য কোনো ওষুধ যুক্ত নেই"),
                    fontSize = 13.sp,
                    color = InkSoft,
                    fontFamily = BodyFontFamily,
                    modifier = Modifier.padding(14.dp)
                )
            } else {
                Column(modifier = Modifier.padding(horizontal = 14.dp)) {
                    medications.forEachIndexed { index, med ->
                        LinkRow(
                            title = "${med.name} ${med.dose}",
                            meta = med.schedule.joinToString(" · "),
                            trailingChevron = true
                        )
                        if (index < medications.size - 1) {
                            HorizontalDivider(color = Line, thickness = 1.dp)
                        }
                    }
                }
            }
        }

        // Section: Related care tasks
        if (careTasks.isNotEmpty()) {
            Text(
                text = t("Related care tasks", "সংশ্লিষ্ট কাজ"),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Ink,
                fontFamily = BodyFontFamily,
                modifier = Modifier.padding(top = 22.dp, bottom = 6.dp)
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, Line)
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp)) {
                    careTasks.forEachIndexed { index, task ->
                        val dueStr = if (task.dueAt != null) {
                            SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(task.dueAt))
                        } else task.category
                        LinkRow(
                            title = task.title,
                            meta = "${t("Due", "নির্ধারিত")}: $dueStr",
                            trailingChevron = false
                        )
                        if (index < careTasks.size - 1) {
                            HorizontalDivider(color = Line, thickness = 1.dp)
                        }
                    }
                }
            }
        }

        // Section: All readings
        Text(
            text = t("All recorded readings", "সব সংরক্ষিত ফলাফল"),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Ink,
            fontFamily = BodyFontFamily,
            modifier = Modifier.padding(top = 22.dp, bottom = 6.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, Line)
        ) {
            if (relevantLabs.isEmpty()) {
                Text(
                    text = t("No readings recorded yet", "এখনও কোনো ফলাফল রেকর্ড নেই"),
                    fontSize = 13.sp,
                    color = InkSoft,
                    fontFamily = BodyFontFamily,
                    modifier = Modifier.padding(14.dp)
                )
            } else {
                Column(modifier = Modifier.padding(horizontal = 14.dp)) {
                    relevantLabs.forEachIndexed { index, lab ->
                        val dateFormatted = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(lab.collectedAt))
                        val refInfo = if (lab.referenceLow != null || lab.referenceHigh != null) {
                            " · ref ${lab.referenceLow ?: "?"}–${lab.referenceHigh ?: "?"}"
                        } else ""
                        LinkRow(
                            title = "${lab.value} ${lab.unit}",
                            meta = "$dateFormatted · ${lab.status}$refInfo",
                            actionText = if (lab.sourceDocumentId != null) t("View scan", "স্ক্যান দেখুন") else null,
                            onClick = {
                                lab.sourceDocumentId?.let { onViewReport(it) }
                            }
                        )
                        if (index < relevantLabs.size - 1) {
                            HorizontalDivider(color = Line, thickness = 1.dp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(110.dp))
    }
}
