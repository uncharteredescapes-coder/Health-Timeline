package com.example.util

import com.example.data.LabResult
import java.text.SimpleDateFormat
import java.util.*

data class TrendInfo(
    val status: String,
    val deltaCaption: String,
    val statusColorType: String // "Primary", "Alert", "InkSoft"
)

object HealthTrends {
    fun computeTrend(testName: String, results: List<LabResult>, language: String): TrendInfo {
        if (results.isEmpty()) return TrendInfo(
            if (language == "en") "No data" else "কোনো তথ্য নেই",
            "",
            "InkSoft"
        )
        
        if (results.size == 1) return TrendInfo(
            if (language == "en") "Baseline recorded" else "ভিত্তি রেকর্ড করা হয়েছে",
            "",
            "InkSoft"
        )

        // Ensure sorted by date
        val sortedResults = results.sortedBy { it.collectedAt }
        val latest = sortedResults.last()
        val previous = sortedResults[sortedResults.size - 2]
        val delta = latest.value - previous.value
        
        // HbA1c and BP: down is better
        val isDownBetter = when (testName.lowercase()) {
            "hba1c", "blood sugar", "glucose", "systolic", "diastolic" -> true
            else -> true 
        }

        val status = when {
            delta < -0.05 -> if (isDownBetter) "Improving" else "Needs attention"
            delta > 0.05 -> if (isDownBetter) "Needs attention" else "Improving"
            else -> "Stable"
        }
        
        val localizedStatus = if (language == "en") {
            status
        } else {
            when (status) {
                "Improving" -> "উন্নতি হচ্ছে"
                "Stable" -> "স্থির আছে"
                "Needs attention" -> "মনোযোগ প্রয়োজন"
                else -> status
            }
        }

        val sign = if (delta > 0) "↑" else if (delta < 0) "↓" else "→"
        val absDelta = kotlin.math.abs(delta)
        val formattedDelta = String.format("%.1f", absDelta)
        val month = SimpleDateFormat("MMM", Locale.getDefault()).format(Date(previous.collectedAt))
        
        val caption = if (language == "en") {
            "$sign $formattedDelta since $month"
        } else {
            "$sign $formattedDelta $month থেকে"
        }

        val colorType = when (status) {
            "Improving" -> "Primary"
            "Needs attention" -> "Alert"
            else -> "InkSoft"
        }

        return TrendInfo(localizedStatus, caption, colorType)
    }
}
