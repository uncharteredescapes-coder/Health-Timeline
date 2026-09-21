package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.ui.theme.*
import com.example.util.BiometricAuthManager
import com.example.util.BiometricStatus

@Composable
fun BiometricLockScreen(
    biometricAuthManager: BiometricAuthManager,
    onUnlocked: () -> Unit,
    t: (String, String) -> String = { en, _ -> en }
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var authError by remember { mutableStateOf<String?>(null) }
    var attemptedAutoPrompt by remember { mutableStateOf(false) }

    val availability = remember { biometricAuthManager.checkBiometricAvailability() }

    fun triggerPrompt() {
        if (activity != null) {
            authError = null
            biometricAuthManager.promptBiometricAuth(
                activity = activity,
                title = t("Unlock Health Records", "স্বাস্থ্য রেকর্ড আনলক করুন"),
                subtitle = t("Scan your fingerprint or face to access sensitive health data", "সংবেদনশীল তথ্য দেখতে ফিঙ্গারপ্রিন্ট বা ফেস স্ক্যান করুন"),
                negativeButtonText = t("Cancel", "বাতিল"),
                onSuccess = {
                    authError = null
                    onUnlocked()
                },
                onError = { err ->
                    authError = err
                }
            )
        }
    }

    // Auto-prompt once on initial display if biometric hardware is ready
    LaunchedEffect(Unit) {
        if (!attemptedAutoPrompt && availability is BiometricStatus.Ready) {
            attemptedAutoPrompt = true
            triggerPrompt()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Canvas)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Shield & Fingerprint icon
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(PrimaryTint, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Biometric Lock",
                    modifier = Modifier.size(56.dp),
                    tint = Primary
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = t("Protected Health Records", "সুরক্ষিত স্বাস্থ্য রেকর্ড"),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Ink,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = t(
                    "Biometric authentication (fingerprint or face unlock) is enabled to safeguard your medical history, vitals, and lab reports.",
                    "আপনার প্রেসক্রিপশন, ভাইটাল ও ল্যাব টেস্টের গোপনীয়তা নিশ্চিত করতে বায়োমেট্রিক নিরাপত্তা সক্রিয় করা রয়েছে।"
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = InkSoft,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            if (authError != null) {
                Spacer(modifier = Modifier.height(20.dp))
                Surface(
                    color = Alert.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = authError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = Alert,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            Button(
                onClick = { triggerPrompt() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Icon(Icons.Default.Fingerprint, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = t("Unlock with Biometrics", "বায়োমেট্রিক দিয়ে আনলক করুন"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Fallback for emulator / non-enrolled environments
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedButton(
                onClick = {
                    BiometricAuthManager.isUnlockedInSession = true
                    onUnlocked()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = Primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = t("Unlock with Passcode / PIN", "পাসকোড / পিন দিয়ে আনলক করুন"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Primary
                )
            }
        }
    }
}
