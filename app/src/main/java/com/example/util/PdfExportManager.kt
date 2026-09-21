package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.LabResult
import com.example.data.Medication
import com.example.data.Patient
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfExportManager {

    fun generateAndShareSummaryPdf(
        context: Context,
        patient: Patient?,
        conditions: List<String>,
        labs: List<LabResult>,
        medications: List<Medication>,
        summaryText: String
    ): Result<File> {
        return try {
            val doc = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 standard
            val page = doc.startPage(pageInfo)
            val canvas = page.canvas

            val titlePaint = Paint().apply {
                color = Color.rgb(20, 50, 35)
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val subtitlePaint = Paint().apply {
                color = Color.rgb(100, 110, 105)
                textSize = 10f
                typeface = Typeface.DEFAULT
            }

            val headerPaint = Paint().apply {
                color = Color.rgb(30, 40, 35)
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val bodyPaint = Paint().apply {
                color = Color.rgb(40, 45, 42)
                textSize = 10f
                typeface = Typeface.DEFAULT
            }

            val linePaint = Paint().apply {
                color = Color.rgb(220, 225, 222)
                strokeWidth = 1f
            }

            var y = 45f

            // Document Header
            canvas.drawText("HEALTH TIMELINE · CLINICAL SUMMARY", 40f, y, titlePaint)
            y += 18f

            val dateStr = SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.US).format(Date())
            canvas.drawText("Generated on $dateStr · Patient Copy", 40f, y, subtitlePaint)
            y += 15f
            canvas.drawLine(40f, y, 555f, y, linePaint)
            y += 20f

            // Patient Demographics
            val pName = patient?.name ?: "Ahmed Karim"
            val pAge = patient?.age?.let { "$it yrs" } ?: "N/A"
            val pGender = patient?.gender ?: "N/A"
            canvas.drawText("PATIENT DEMOGRAPHICS", 40f, y, headerPaint)
            y += 15f
            canvas.drawText("Name: $pName   |   Age: $pAge   |   Gender: $pGender", 40f, y, bodyPaint)
            if (conditions.isNotEmpty()) {
                y += 14f
                canvas.drawText("Active Conditions: ${conditions.joinToString(", ")}", 40f, y, bodyPaint)
            }
            y += 20f

            // Verified Lab Results
            if (labs.isNotEmpty()) {
                canvas.drawLine(40f, y, 555f, y, linePaint)
                y += 16f
                canvas.drawText("RECENT VERIFIED LAB RESULTS", 40f, y, headerPaint)
                y += 15f

                // Table header
                canvas.drawText("Test Name", 40f, y, subtitlePaint)
                canvas.drawText("Result", 220f, y, subtitlePaint)
                canvas.drawText("Reference Range", 330f, y, subtitlePaint)
                canvas.drawText("Date", 460f, y, subtitlePaint)
                y += 14f

                labs.take(10).forEach { lab ->
                    val dateFormatted = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(lab.collectedAt))
                    val refRange = if (lab.referenceLow != null || lab.referenceHigh != null) {
                        "${lab.referenceLow ?: "?"} – ${lab.referenceHigh ?: "?"} ${lab.unit}"
                    } else "N/A"

                    canvas.drawText(lab.testName.take(28), 40f, y, bodyPaint)
                    canvas.drawText("${lab.value} ${lab.unit} (${lab.status})", 220f, y, bodyPaint)
                    canvas.drawText(refRange, 330f, y, bodyPaint)
                    canvas.drawText(dateFormatted, 460f, y, bodyPaint)
                    y += 14f
                }
                y += 10f
            }

            // Current Medications
            if (medications.isNotEmpty()) {
                canvas.drawLine(40f, y, 555f, y, linePaint)
                y += 16f
                canvas.drawText("CURRENT MEDICATIONS", 40f, y, headerPaint)
                y += 15f
                medications.forEach { med ->
                    canvas.drawText("• ${med.name} ${med.dose} — Schedule: ${med.schedule.joinToString(", ")}", 40f, y, bodyPaint)
                    y += 14f
                }
                y += 10f
            }

            // Clinical Summary / Doctor's Notes
            if (summaryText.isNotBlank()) {
                canvas.drawLine(40f, y, 555f, y, linePaint)
                y += 16f
                canvas.drawText("STRUCTURED VISIT SUMMARY & DISCUSSION POINTS", 40f, y, headerPaint)
                y += 15f

                // Split into lines for wrapping
                val words = summaryText.split(" ")
                var currentLine = StringBuilder()
                for (word in words) {
                    if (y > 780f) break // Avoid overflowing page bottom
                    if (bodyPaint.measureText(currentLine.toString() + word) < 510f) {
                        currentLine.append("$word ")
                    } else {
                        canvas.drawText(currentLine.toString(), 40f, y, bodyPaint)
                        y += 13f
                        currentLine = StringBuilder("$word ")
                    }
                }
                if (currentLine.isNotEmpty() && y <= 780f) {
                    canvas.drawText(currentLine.toString(), 40f, y, bodyPaint)
                    y += 18f
                }
            }

            // Footer Disclaimer
            y = 805f
            canvas.drawLine(40f, y, 555f, y, linePaint)
            y += 12f
            canvas.drawText(
                "DISCLAIMER: This record is compiled by the patient from their personal verified health records for clinician discussion.",
                40f,
                y,
                subtitlePaint
            )

            doc.finishPage(page)

            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val pdfFile = File(exportDir, "Visit_Summary_${System.currentTimeMillis()}.pdf")
            FileOutputStream(pdfFile).use { out ->
                doc.writeTo(out)
            }
            doc.close()

            // Launch share sheet
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Clinical Health Summary - $pName")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Clinical Summary"))

            Result.success(pdfFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
