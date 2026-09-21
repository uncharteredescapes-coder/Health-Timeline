package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.ui.components.CameraView
import com.example.ui.components.GroundingNote
import com.example.ui.theme.*

@Composable
fun CaptureScreen(
    onDismiss: () -> Unit,
    onNavigateToExtract: () -> Unit,
    onNavigateToManual: () -> Unit = {},
    viewModel: HealthViewModel = viewModel()
) {
    val context = LocalContext.current
    val patient by viewModel.patient.collectAsStateWithLifecycle()
    val language = patient?.language ?: "en"
    val t: (String, String) -> String = { en, bn -> if (language == "en") en else bn }

    var isLiveCameraActive by remember { mutableStateOf(false) }

    // Modern zero-permission Android Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.processDocument(context, uri) {
                onNavigateToExtract()
            }
            onNavigateToExtract()
        }
    }

    // Camera capture launcher
    val takePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            viewModel.extractResultsFromBitmap(bitmap, context) {
                onNavigateToExtract()
            }
            onNavigateToExtract()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isLiveCameraActive = true
        } else {
            // Fallback to system camera intent
            takePhotoLauncher.launch(null)
        }
    }

    if (isLiveCameraActive) {
        Box(modifier = Modifier.fillMaxSize()) {
            CameraView(
                onImageCaptured = { bitmap ->
                    viewModel.extractResultsFromBitmap(bitmap, context) {
                        onNavigateToExtract()
                    }
                    onNavigateToExtract()
                },
                onError = {
                    Toast.makeText(context, t("Camera error", "ক্যামেরা ত্রুটি"), Toast.LENGTH_SHORT).show()
                    isLiveCameraActive = false
                },
                onClose = { isLiveCameraActive = false }
            )
        }
        return
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
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clickable { onDismiss() },
                contentAlignment = Alignment.CenterStart
            ) {
                Text(text = "✕", fontSize = 18.sp, color = Primary, fontWeight = FontWeight.Bold)
            }
            Text(
                text = t("Capture Document", "ডকুমেন্ট স্ক্যান করুন"),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = BodyFontFamily,
                color = Ink
            )
            Spacer(modifier = Modifier.size(34.dp))
        }

        // Viewfinder Frame
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0B140F))
                .clickable {
                    cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                },
            contentAlignment = Alignment.Center
        ) {
            // Corner Reticles
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(18.dp)
                    .size(22.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Primary))
                Box(modifier = Modifier.fillMaxHeight().width(2.dp).background(Primary))
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(18.dp)
                    .size(22.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Primary))
                Box(modifier = Modifier.fillMaxHeight().width(2.dp).align(Alignment.TopEnd).background(Primary))
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(18.dp)
                    .size(22.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.BottomStart).background(Primary))
                Box(modifier = Modifier.fillMaxHeight().width(2.dp).background(Primary))
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(18.dp)
                    .size(22.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.BottomEnd).background(Primary))
                Box(modifier = Modifier.fillMaxHeight().width(2.dp).align(Alignment.BottomEnd).background(Primary))
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "📷",
                    fontSize = 36.sp
                )
                Text(
                    text = t("Tap to start camera scanner", "ক্যামেরা চালু করতে চাপুন"),
                    color = Color(0xFF9EB0A2),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = BodyFontFamily
                )
                Text(
                    text = t("Center lab report or prescription inside frame", "রিপোর্টটি ফ্রেমের মধ্যে রাখুন"),
                    color = Color(0xFF6E8072),
                    fontSize = 11.5.sp,
                    fontFamily = BodyFontFamily
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Capture controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Photo Picker (Gallery)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Surface)
                        .border(1.dp, Line, CircleShape)
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🖼", fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = t("Gallery", "গ্যালারি"),
                    fontSize = 11.5.sp,
                    color = InkSoft,
                    fontFamily = BodyFontFamily
                )
            }

            // Shutter button: triggers CameraX or camera preview
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .border(3.dp, Primary, CircleShape)
                        .clickable {
                            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(Primary)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = t("Scan", "স্ক্যান"),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary,
                    fontFamily = BodyFontFamily
                )
            }

            // Manual Entry
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Surface)
                        .border(1.dp, Line, CircleShape)
                        .clickable { onNavigateToManual() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "✏", fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = t("Manual", "ম্যানুয়াল"),
                    fontSize = 11.5.sp,
                    color = InkSoft,
                    fontFamily = BodyFontFamily
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        GroundingNote(
            text = t(
                "Documents are processed securely with clinical provenance linking.",
                "ডকুমেন্টগুলি সুরক্ষিতভাবে ক্লিনিক্যাল প্রমাণের সাথে সংযুক্ত করা হয়।"
            )
        )
    }
}
