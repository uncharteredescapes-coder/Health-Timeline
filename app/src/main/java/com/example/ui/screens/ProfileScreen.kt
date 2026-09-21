package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.HealthViewModel
import com.example.ui.components.LinkRow
import com.example.ui.theme.*
import com.example.util.BiometricAuthManager
import com.example.util.PdfExportManager

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onNavigateToOnboarding: () -> Unit = {},
    viewModel: HealthViewModel = viewModel()
) {
    val context = LocalContext.current
    val patient by viewModel.patient.collectAsStateWithLifecycle()
    val conditions by viewModel.conditions.collectAsStateWithLifecycle()
    val labResults by viewModel.labResults.collectAsStateWithLifecycle()
    val medications by viewModel.medications.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()

    val authManager = remember { BiometricAuthManager(context) }
    var isBiometricsEnabled by remember { mutableStateOf(authManager.isBiometricLockEnabled) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    val language = patient?.language ?: "en"
    val t: (String, String) -> String = { en, bn -> if (language == "en") en else bn }

    val name = patient?.name ?: "Health User"
    val age = patient?.age ?: 45
    val gender = patient?.gender?.replaceFirstChar { it.uppercase() } ?: "Unspecified"

    val initials = remember(name) {
        name.trim().split("\\s+".toRegex()).take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("").ifEmpty { "HU" }
    }

    val condsSummary = remember(conditions) {
        if (conditions.isEmpty()) t("None selected", "কোনোটি নির্বাচন করা হয়নি")
        else conditions.joinToString(", ") { it.type.replaceFirstChar { c -> c.uppercase() } }
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
                Text(text = "←", fontSize = 20.sp, color = Primary, fontWeight = FontWeight.Bold)
            }
            Text(
                text = t("Profile & Security", "প্রোফাইল ও নিরাপত্তা"),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = BodyFontFamily,
                color = Ink
            )
            Spacer(modifier = Modifier.size(34.dp))
        }

        // Profile details card / Greeting row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = name,
                    style = GreetingNameStyle,
                    color = Ink,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Text(
                    text = "$age years · $gender",
                    fontSize = 13.sp,
                    color = InkSoft,
                    fontFamily = BodyFontFamily
                )
            }
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = BodyFontFamily
                )
            }
        }

        // Link rows enclosed in Surface
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Line),
            color = Surface
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp)) {
                // Language
                LinkRow(
                    title = t("Language", "ভাষা"),
                    meta = if (language == "en") "English" else "বাংলা",
                    onClick = {
                        val next = if (language == "en") "bn" else "en"
                        patient?.let { viewModel.savePatient(it.name, it.age, it.gender, next) }
                        Toast.makeText(context, if (next == "en") "Switched to English" else "বাংলায় পরিবর্তিত", Toast.LENGTH_SHORT).show()
                    }
                )

                HorizontalDivider(color = Line, thickness = 1.dp)

                // Tracked conditions
                LinkRow(
                    title = t("Tracked conditions", "ট্র্যাক করা রোগ"),
                    meta = condsSummary,
                    onClick = onNavigateToOnboarding
                )

                HorizontalDivider(color = Line, thickness = 1.dp)

                // Data & export
                LinkRow(
                    title = t("Export Health Records", "স্বাস্থ্য রেকর্ড রপ্তানি"),
                    meta = t("Generate and share clinical PDF summary", "ক্লিনিক্যাল PDF সারসংক্ষেপ তৈরি করুন"),
                    onClick = {
                        PdfExportManager.generateAndShareSummaryPdf(
                            context = context,
                            patient = patient,
                            conditions = conditions.map { it.type },
                            labs = labResults,
                            medications = medications,
                            summaryText = summary
                        )
                    }
                )

                HorizontalDivider(color = Line, thickness = 1.dp)

                // Privacy & storage
                LinkRow(
                    title = t("Privacy & Storage", "গোপনীয়তা ও সঞ্চয়স্থান"),
                    meta = t("Private on-device storage · Excluded from cloud backup", "ব্যক্তিগত ডিভাইসে সংরক্ষিত · ক্লাউড ব্যাকআপ ব্যতীত"),
                    onClick = {
                        Toast.makeText(
                            context,
                            t("Health database and documents are kept exclusively in private app storage", "স্বাস্থ্য সংক্রান্ত তথ্য শুধুমাত্র ডিভাইসের অভ্যন্তরীণ মেমরিতে সংরক্ষিত"),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )

                HorizontalDivider(color = Line, thickness = 1.dp)

                // Biometrics Authentication
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = t("Biometric App Lock", "বায়োমেট্রিক নিরাপত্তা"),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Ink,
                            fontFamily = BodyFontFamily
                        )
                        Text(
                            text = t("Require fingerprint or face authentication on launch", "অ্যাপ খোলার সময় বায়োমেট্রিক যাচাই প্রয়োজন"),
                            fontSize = 12.sp,
                            color = InkSoft,
                            fontFamily = BodyFontFamily,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Switch(
                        checked = isBiometricsEnabled,
                        onCheckedChange = { enabled ->
                            authManager.isBiometricLockEnabled = enabled
                            isBiometricsEnabled = enabled
                            Toast.makeText(
                                context,
                                if (enabled) t("Biometric lock enabled", "বায়োমেট্রিক লক চালু করা হয়েছে")
                                else t("Biometric lock disabled", "বায়োমেট্রিক লক বন্ধ করা হয়েছে"),
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Primary
                        )
                    )
                }

                HorizontalDivider(color = Line, thickness = 1.dp)

                // Clear Records
                LinkRow(
                    title = t("Clear Lab History", "ল্যাব রেকর্ড মুছে ফেলুন"),
                    meta = t("Permanently delete extracted lab results", "সংরক্ষিত ল্যাব ফলাফল স্থায়ীভাবে মুছুন"),
                    onClick = { showClearConfirmDialog = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(110.dp))
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = {
                Text(
                    text = t("Delete all lab records?", "সব ল্যাব রেকর্ড মুছে ফেলবেন?"),
                    fontWeight = FontWeight.Bold,
                    fontFamily = BodyFontFamily
                )
            },
            text = {
                Text(
                    text = t(
                        "This will permanently delete all extracted and manual lab observations from your local database. This action cannot be undone.",
                        "এটি আপনার ডাটাবেস থেকে সব ল্যাব ফলাফল মুছে ফেলবে। এই কাজটি পূর্বাবস্থায় ফিরিয়ে আনা যাবে না।"
                    ),
                    fontSize = 13.sp,
                    fontFamily = BodyFontFamily
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllLabs()
                        showClearConfirmDialog = false
                        Toast.makeText(context, t("Lab history cleared", "ল্যাব হিস্ট্রি মুছে ফেলা হয়েছে"), Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(t("Delete", "মুছে ফেলুন"), color = Accent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text(t("Cancel", "বাতিল"))
                }
            }
        )
    }
}
