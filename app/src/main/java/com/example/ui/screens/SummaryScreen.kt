package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.HealthViewModel
import com.example.ui.components.DesignOutlineButton
import com.example.ui.components.DesignPrimaryButton
import com.example.ui.components.GroundingNote
import com.example.ui.components.SummaryBlock
import com.example.ui.theme.*
import com.example.util.PdfExportManager

@Composable
fun SummaryScreen(
    onBack: () -> Unit,
    viewModel: HealthViewModel = viewModel()
) {
    val context = LocalContext.current
    val patient by viewModel.patient.collectAsStateWithLifecycle()
    val conditions by viewModel.conditions.collectAsStateWithLifecycle()
    val labResults by viewModel.labResults.collectAsStateWithLifecycle()
    val medications by viewModel.medications.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val isGeneratingSummary by viewModel.isGeneratingSummary.collectAsStateWithLifecycle()

    val language = patient?.language ?: "en"
    val t: (String, String) -> String = { en, bn -> if (language == "en") en else bn }

    LaunchedEffect(Unit) {
        if (summary.isBlank()) {
            viewModel.generateVisitSummary()
        }
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
                text = t("Visit Summary", "পরিদর্শন সারসংক্ষেপ"),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = BodyFontFamily,
                color = Ink
            )
            Spacer(modifier = Modifier.size(34.dp))
        }

        if (isGeneratingSummary) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = t("Generating clinical summary...", "সারসংক্ষেপ তৈরি হচ্ছে..."),
                        fontSize = 13.sp,
                        color = InkSoft,
                        fontFamily = BodyFontFamily
                    )
                }
            }
        } else {
            // Generated Evidence-based Summary
            SummaryBlock(
                eyebrowTitle = t("Clinical Overview & Findings", "ক্লিনিক্যাল ফলাফল ও সারসংক্ষেপ"),
                body = if (summary.isNotBlank()) summary else t(
                    "No diagnostic records or medications recorded yet. Upload lab reports to generate an automated clinical visit summary.",
                    "এখনও কোনো ডায়াগনস্টিক রিপোর্ট সংরক্ষিত নেই। রিপোর্ট যোগ করে পরিদর্শন সারসংক্ষেপ তৈরি করুন।"
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Discussion Checklist
            val questionsText = if (conditions.isNotEmpty()) {
                t(
                    "1. Review current medication dosage and timing with your physician.\n2. Confirm follow-up schedule for diagnostic lab re-tests.",
                    "১. ডাক্তারের সাথে বর্তমান ওষুধের ডোজ ও সময় পর্যালোচনা করুন।\n২. পরবর্তী ফলো-আপ ল্যাব টেস্টের সময়সূচী নিশ্চিত করুন।"
                )
            } else {
                t(
                    "1. Schedule routine preventive health screening.\n2. Inquire about lifestyle and dietary recommendations.",
                    "১. রুটিন স্বাস্থ্য পরীক্ষার সময় নির্ধারণ করুন।\n২. জীবনধারা ও খাদ্যাভ্যাস সম্পর্কে পরামর্শ গ্রহণ করুন।"
                )
            }

            SummaryBlock(
                eyebrowTitle = t("Questions to ask your clinician", "ডাক্তারকে জিজ্ঞেস করার বিষয়সমূহ"),
                body = questionsText
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        DesignPrimaryButton(
            text = t("Done", "সম্পন্ন"),
            onClick = onBack
        )

        Spacer(modifier = Modifier.height(10.dp))

        DesignOutlineButton(
            text = t("Share with Doctor (PDF)", "ডাক্তারকে পাঠান (PDF)"),
            onClick = {
                val result = PdfExportManager.generateAndShareSummaryPdf(
                    context = context,
                    patient = patient,
                    conditions = conditions.map { it.type },
                    labs = labResults,
                    medications = medications,
                    summaryText = summary
                )
                if (result.isFailure) {
                    Toast.makeText(
                        context,
                        t("Failed to export PDF", "PDF তৈরি করতে ব্যর্থ"),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        GroundingNote(
            text = t(
                "This visit summary is compiled strictly from your verified personal records. It is intended to support, not replace, direct clinical evaluation.",
                "এই সারসংক্ষেপটি আপনার যাচাইকৃত ব্যক্তিগত স্বাস্থ্য রেকর্ড থেকে তৈরি করা হয়েছে। এটি ডাক্তারের সাথে আলোচনার সহায়িকা হিসেবে ব্যবহৃত হবে।"
            )
        )

        Spacer(modifier = Modifier.height(40.dp))
    }
}
