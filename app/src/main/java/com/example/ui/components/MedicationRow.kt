package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*

import androidx.compose.foundation.clickable

@Composable
fun MedicationRow(name: String, schedule: String, taken: Boolean, t: (String, String) -> String, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(MaterialTheme.shapes.small)
                .background(PrimaryTint),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.MedicalServices, contentDescription = null, modifier = Modifier.size(19.dp), tint = Primary)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, style = MaterialTheme.typography.labelLarge, color = Ink)
            if (schedule.isNotEmpty()) {
                Text(text = schedule, style = MaterialTheme.typography.bodySmall, color = InkSoft)
            }
        }
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(MaterialTheme.shapes.small)
                .background(if (taken) Primary else Color.Transparent)
                .border(1.6.dp, if (taken) Primary else Line, MaterialTheme.shapes.small),
            contentAlignment = Alignment.Center
        ) {
            if (taken) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
            }
        }
    }
}
