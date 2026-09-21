package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.Document
import com.example.ui.HealthViewModel
import com.example.ui.theme.Canvas
import com.example.ui.theme.Primary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportViewerScreen(
    documentId: String,
    onBack: () -> Unit,
    viewModel: HealthViewModel = viewModel()
) {
    var document by remember { mutableStateOf<Document?>(null) }

    LaunchedEffect(documentId) {
        document = viewModel.getDocument(documentId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Original Report") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Canvas)
            )
        },
        containerColor = Canvas
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            if (document != null) {
                AsyncImage(
                    model = document!!.filePath,
                    contentDescription = "Medical Report Scan",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                CircularProgressIndicator(color = Primary)
            }
        }
    }
}
