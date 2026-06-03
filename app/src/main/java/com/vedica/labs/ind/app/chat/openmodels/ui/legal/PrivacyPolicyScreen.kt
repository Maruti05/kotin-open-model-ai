package com.vedica.labs.ind.app.chat.openmodels.ui.legal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LegalSection("Data Collection", "OpenModels does not collect any personal data. All processing is performed entirely on-device.")
            LegalSection("On-Device Processing", "All AI inference, data storage, and computation happens locally on your device. No data is transmitted to external servers.")
            LegalSection("Generated Content", "Chat history, model outputs, and file contexts are stored locally in an SQLite database on your device.")
            LegalSection("User Responsibility", "You are responsible for the content you generate and process using this application.")
            LegalSection("Model Files", "Downloaded model files (GGUF format) are stored in your device's app-specific storage and are not shared with any third party.")
            LegalSection("Third-Party Services", "Model downloads are performed via direct HTTP connections to HuggingFace. No analytics or tracking services are used.")
            LegalSection("Data Deletion", "You may delete your chat history, downloaded models, and file contexts at any time through the application interface.")
            LegalSection("Changes", "This privacy policy may be updated. Continued use after changes constitutes acceptance of the updated policy.")
        }
    }
}

@Composable
private fun LegalSection(title: String, body: String) {
    Column {
        Text(text = title, style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
