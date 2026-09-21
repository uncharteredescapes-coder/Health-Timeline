package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.CareTask
import com.example.data.LabResult
import com.example.data.Medication
import com.example.notifications.MedicationWorkScheduler
import com.example.ui.HealthViewModel
import com.example.ui.components.BadgeType
import com.example.ui.components.DesignOutlineButton
import com.example.ui.components.DesignPrimaryButton
import com.example.ui.components.GroundingNote
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*

@Composable
fun ExtractionScreen(
    onBack: () -> Unit,
    onConfirm: () -> Unit,
    viewModel: HealthViewModel = viewModel()
) {
    val context = LocalContext.current
    val patient by viewModel.patient.collectAsStateWithLifecycle()
    val extractedData by viewModel.extractedData.collectAsStateWithLifecycle()
    val isExtracting by viewModel.isExtracting.collectAsStateWithLifecycle()
    val extractionStatus by viewModel.extractionStatus.collectAsStateWithLifecycle()

    val language = patient?.language ?: "en"
    val t: (String, String) -> String = { en, bn -> if (language == "en") en else bn }

    // Local editable copies of extracted items
    val labs = remember(extractedData) {
        mutableStateListOf<LabResult>().apply {
            extractedData?.labs?.let { addAll(it) }
        }
    }
    val medications = remember(extractedData) {
        mutableStateListOf<Medication>().apply {
            extractedData?.medications?.let { addAll(it) }
        }
    }
    val careTasks = remember(extractedData) {
        mutableStateListOf<CareTask>().apply {
            extractedData?.careTasks?.let { addAll(it) }
        }
    }

    var editingLabIndex by remember { mutableStateOf<Int?>(null) }

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
                Text(text = "←", fontSize = 20.sp, color = Primary, fontWeight = FontWeight.Bold)
            }
            Text(
                text = t("Review Extraction", "তথ্য যাচাই"),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = BodyFontFamily,
                color = Ink
            )
            Spacer(modifier = Modifier.size(34.dp))
        }

        if (isExtracting) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = t("Analyzing clinical document...", "ডকুমেন্ট বিশ্লেষণ করা হচ্ছে..."),
                        fontSize = 14.sp,
                        color = InkSoft,
                        fontFamily = BodyFontFamily
                    )
                }
            }
        } else if (labs.isEmpty() && medications.isEmpty() && careTasks.isEmpty()) {
            // Empty / Unreadable document state
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Line),
                color = Surface
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "📄", fontSize = 32.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (!extractionStatus.isNullOrBlank()) extractionStatus!! else t("No structured results detected", "কোনো তথ্য শনাক্ত করা যায়নি"),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (extractionStatus?.contains("rejected", ignoreCase = true) == true) Accent else Ink,
                        fontFamily = BodyFontFamily
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = t(
                            "The document may be blurry or not contain clear medical tests. You can retake the photo or enter values manually.",
                            "ডকুমেন্টটি অস্পষ্ট হতে পারে। আবার ছবি তুলতে পারেন অথবা তথ্য নিজে যোগ করতে পারেন।"
                        ),
                        fontSize = 13.sp,
                        color = InkSoft,
                        fontFamily = BodyFontFamily,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    DesignPrimaryButton(
                        text = t("Retake Photo", "আবার ছবি তুলুন"),
                        onClick = onBack
                    )
                }
            }
        } else {
            Text(
                text = t(
                    "Extracted from your medical document. Review, edit, or remove entries before saving to your timeline.",
                    "আপনার ডকুমেন্ট থেকে পাওয়া তথ্য। সংরক্ষণ করার আগে পরীক্ষা করুন বা সম্পাদন করুন।"
                ),
                fontSize = 13.sp,
                color = InkSoft,
                fontFamily = BodyFontFamily,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Extracted Labs section
            if (labs.isNotEmpty()) {
                Text(
                    text = t("Laboratory Tests & Metrics", "ল্যাব টেস্ট ও ফলাফল"),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                    fontFamily = BodyFontFamily,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                labs.forEachIndexed { index, lab ->
                    val badgeType = when (lab.status.lowercase()) {
                        "high" -> BadgeType.WATCH
                        "low" -> BadgeType.WATCH
                        "critical" -> BadgeType.ATTN
                        else -> BadgeType.OK
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .clickable { editingLabIndex = index },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Line),
                        color = Surface
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = lab.testName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Ink,
                                    fontFamily = BodyFontFamily,
                                    modifier = Modifier.weight(1f)
                                )
                                StatusBadge(text = lab.status.uppercase(), type = badgeType)
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = lab.value.toString(),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Primary,
                                    fontFamily = FrauncesFontFamily
                                )
                                Text(
                                    text = lab.unit,
                                    fontSize = 14.sp,
                                    color = InkSoft,
                                    fontFamily = BodyFontFamily
                                )
                            }

                            if (lab.referenceLow != null || lab.referenceHigh != null) {
                                Text(
                                    text = "${t("Ref Range: ", "স্বাভাবিক সীমা: ")}${lab.referenceLow ?: "?"} – ${lab.referenceHigh ?: "?"} ${lab.unit}",
                                    fontSize = 12.sp,
                                    color = InkSoft,
                                    fontFamily = BodyFontFamily,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            if (!lab.sourceDocumentId.isNullOrBlank()) {
                                Text(
                                    text = t("Source Doc: ${lab.sourceDocumentId.take(8)}...", "উৎস ডকুমেন্ট: ${lab.sourceDocumentId.take(8)}..."),
                                    fontSize = 11.sp,
                                    color = Primary,
                                    fontFamily = BodyFontFamily,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val isLowConf = lab.confidence < 0.7
                                Text(
                                    text = t("Confidence: ${(lab.confidence * 100).toInt()}%", "নির্ভুলতা: ${(lab.confidence * 100).toInt()}%") + (if (isLowConf) " ⚠️" else ""),
                                    fontSize = 11.sp,
                                    color = if (isLowConf) Accent else InkSoft,
                                    fontWeight = if (isLowConf) FontWeight.Bold else FontWeight.Normal,
                                    fontFamily = BodyFontFamily
                                )
                                Text(
                                    text = t("Tap to edit / verify", "সম্পাদনা ও যাচাই করতে চাপুন"),
                                    fontSize = 12.sp,
                                    color = Primary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = BodyFontFamily
                                )
                            }
                        }
                    }
                }
            }

            // Extracted Medications section
            if (medications.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = t("Detected Medications", "শনাক্তকৃত ওষুধ"),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                    fontFamily = BodyFontFamily,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                medications.forEach { med ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Line),
                        color = Surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = med.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Ink,
                                    fontFamily = BodyFontFamily
                                )
                                Text(
                                    text = "${med.dose} · ${med.schedule.joinToString(", ")}",
                                    fontSize = 12.sp,
                                    color = InkSoft,
                                    fontFamily = BodyFontFamily
                                )
                            }
                        }
                    }
                }
            }

            // Extracted Follow-ups
            if (careTasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = t("Care Tasks & Follow-ups", "ফলো-আপ ও যত্ন পরিকল্পনা"),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                    fontFamily = BodyFontFamily,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                careTasks.forEach { task ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Line),
                        color = Surface
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = task.title,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Ink,
                                fontFamily = BodyFontFamily
                            )
                            Text(
                                text = t("Category: ${task.category}", "বিভাগ: ${task.category}"),
                                fontSize = 12.sp,
                                color = InkSoft,
                                fontFamily = BodyFontFamily
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            DesignPrimaryButton(
                text = t("Confirm & Save Verified Records", "যাচাইকৃত রেকর্ড সংরক্ষণ করুন"),
                onClick = {
                    val scheduler = MedicationWorkScheduler(context)
                    viewModel.confirmAndSaveExtraction(
                        verifiedLabs = labs.toList(),
                        verifiedMeds = medications.toList(),
                        verifiedTasks = careTasks.toList(),
                        workScheduler = scheduler
                    )
                    onConfirm()
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            GroundingNote(
                text = t(
                    "All extracted metrics are saved to your private encrypted database with complete document provenance.",
                    "সমস্ত তথ্য সম্পূর্ণ ডকুমেন্ট প্রমাণের সাথে আপনার ব্যক্তিগত ডাটাবেসে সুরক্ষিতভাবে সংরক্ষিত হয়।"
                )
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // Edit Lab Dialog
    editingLabIndex?.let { index ->
        val lab = labs.getOrNull(index)
        if (lab != null) {
            var editName by remember { mutableStateOf(lab.testName) }
            var editVal by remember { mutableStateOf(lab.value.toString()) }
            var editUnit by remember { mutableStateOf(lab.unit) }

            AlertDialog(
                onDismissRequest = { editingLabIndex = null },
                title = {
                    Text(
                        text = t("Edit Clinical Metric", "টেস্টের তথ্য সম্পাদনা"),
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text(t("Test Name", "পরীক্ষার নাম")) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editVal,
                            onValueChange = { editVal = it },
                            label = { Text(t("Result Value", "ফলাফল")) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editUnit,
                            onValueChange = { editUnit = it },
                            label = { Text(t("Unit", "একক")) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val parsedVal = editVal.toDoubleOrNull() ?: lab.value
                            labs[index] = lab.copy(
                                testName = editName.trim(),
                                value = parsedVal,
                                unit = editUnit.trim()
                            )
                            editingLabIndex = null
                        }
                    ) {
                        Text(t("Save", "সংরক্ষণ"), color = Primary, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            labs.removeAt(index)
                            editingLabIndex = null
                        }
                    ) {
                        Text(t("Delete Entry", "মুছে ফেলুন"), color = Accent)
                    }
                }
            )
        }
    }
}
