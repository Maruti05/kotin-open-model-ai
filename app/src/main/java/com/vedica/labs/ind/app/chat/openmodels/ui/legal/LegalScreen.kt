package com.vedica.labs.ind.app.chat.openmodels.ui.legal

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalScreen(
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About & Legal") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "OpenModels v1.0.3",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Run open-source LLMs locally on Android. Private, offline AI chat with GGUF model support.",
                style = MaterialTheme.typography.bodyMedium
            )
            Divider()
            Text(
                text = "Data Protection",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "All data processing occurs entirely on-device. No data is sent to external servers. Chat history, model files, and file contexts remain on your device.",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "General Purpose Use",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "OpenModels is designed for general-purpose assistance including code generation, writing, analysis, and educational support. It is not a specialized tool for medical, legal, or financial advice.",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Licensing",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "This application is provided as-is. Model files are subject to their respective licenses (Apache 2.0, MIT, CC-BY-NC, etc.). Users are responsible for complying with individual model license terms.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
