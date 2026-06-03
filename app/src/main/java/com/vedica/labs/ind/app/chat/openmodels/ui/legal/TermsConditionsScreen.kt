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
fun TermsConditionsScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terms & Conditions") },
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
            LegalSection("1. Acceptance", "By using OpenModels, you agree to these terms. If you do not agree, do not use the application.")
            LegalSection("2. User Responsibility", "You are solely responsible for content generated using this application and for compliance with applicable laws.")
            LegalSection("3. User Conduct", "You agree not to use OpenModels for any unlawful purpose or to generate harmful, abusive, or malicious content.")
            LegalSection("4. No Warranty", "This application is provided 'as is' without warranty of any kind, express or implied.")
            LegalSection("5. Limitation of Liability", "The developers shall not be liable for any damages arising from the use or inability to use this application.")
            LegalSection("6. Intellectual Property", "The application code is proprietary. Model weights are subject to their respective licenses.")
            LegalSection("7. Modifications", "We reserve the right to modify these terms at any time. Continued use after changes constitutes acceptance.")
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
