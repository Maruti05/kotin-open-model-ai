package com.vedica.labs.ind.app.chat.openmodels.ui.settings

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.vedica.labs.ind.app.chat.openmodels.data.model.InferenceParams
import com.vedica.labs.ind.app.chat.openmodels.data.model.PromptPreset
import com.vedica.labs.ind.app.chat.openmodels.ui.components.StyledCard
import com.vedica.labs.ind.app.chat.openmodels.ui.theme.NeonCyan
import com.vedica.labs.ind.app.chat.openmodels.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(
        checkNotNull<ViewModelStoreOwner>(
            LocalViewModelStoreOwner.current
        ) {
                "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
            }, null
    )
) {
    val params by viewModel.inferenceParams.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val activePresetId by viewModel.activePresetId.collectAsState()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .then(
                if (Build.VERSION.SDK_INT >= 35) Modifier.statusBarsPadding() else Modifier
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        // ── Header ──
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Configure your experience",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ── Appearance & Theme ──
        item {
            SettingsSectionCard(
                icon = Icons.Outlined.Palette,
                title = "Appearance"
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Theme",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("dark" to "Dark", "light" to "Light", "system" to "System").forEach { (mode, label) ->
                        FilterChip(
                            selected = themeMode == mode,
                            onClick = { viewModel.setThemeMode(mode) },
                            label = { Text(label) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan.copy(alpha = 0.2f)
                            )
                        )
                    }
                }
            }
        }

        // ── Inference Profile ──
        item {
            SettingsSectionCard(
                icon = Icons.Outlined.Tune,
                title = "Inference Profile"
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Quick Presets",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Triple("Precise", InferenceParams.PRECISE) { viewModel.applyInferencePreset(InferenceParams.PRECISE) },
                        Triple("Balanced", InferenceParams.BALANCED) { viewModel.applyInferencePreset(InferenceParams.BALANCED) },
                        Triple("Creative", InferenceParams.CREATIVE) { viewModel.applyInferencePreset(InferenceParams.CREATIVE) }
                    ).forEach { (label, _, onClick) ->
                        FilterChip(
                            selected = when (label) {
                                "Precise" -> params.temperature == 0.1
                                "Balanced" -> params.temperature == 0.7
                                "Creative" -> params.temperature == 1.2
                                else -> false
                            },
                            onClick = onClick,
                            label = { Text(label) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan.copy(alpha = 0.2f)
                            )
                        )
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                ParameterSlider(
                    title = "Temperature",
                    description = "Controls randomness in responses. Lower values (0.1-0.5) make output more focused and deterministic. Higher values (1.0-2.0) produce more creative and varied results.",
                    value = params.temperature.toFloat(),
                    valueRange = 0.0f..2.0f,
                    displayValue = "%.2f".format(params.temperature),
                    onValueChange = { viewModel.setTemperature(it.toDouble()) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                ParameterSlider(
                    title = "Top-P",
                    description = "Nucleus sampling — considers only the top tokens whose cumulative probability reaches this threshold. Lower values (0.1-0.5) make output more focused. Higher values (0.8-1.0) allow more diversity.",
                    value = params.topP.toFloat(),
                    valueRange = 0.0f..1.0f,
                    displayValue = "%.2f".format(params.topP),
                    onValueChange = { viewModel.setTopP(it.toDouble()) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                ParameterSlider(
                    title = "Top-K",
                    description = "Limits the model to the K most likely next tokens at each step. Lower values (1-20) produce more predictable text. Higher values (40-100) allow more surprising choices.",
                    value = params.topK.toFloat(),
                    valueRange = 1f..100f,
                    displayValue = "${params.topK}",
                    onValueChange = { viewModel.setTopK(it.toInt()) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                ParameterSlider(
                    title = "Max Tokens",
                    description = "Maximum number of tokens (words/punctuation marks) the model can generate in a single response. Higher values allow longer replies but use more memory and time.",
                    value = params.maxTokens.toFloat(),
                    valueRange = 64f..2048f,
                    displayValue = "${params.maxTokens}",
                    onValueChange = { viewModel.setMaxTokens(it.toInt()) }
                )
            }
        }

        // ── System Prompt ──
        item {
            SettingsSectionCard(
                icon = Icons.Outlined.TextFields,
                title = "System Prompt"
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggleRow(
                    label = "Enable System Prompt",
                    subtitle = "When off, no system prompt is sent to the model",
                    checked = params.systemPromptEnabled,
                    onCheckedChange = { viewModel.setSystemPromptEnabled(it) }
                )
                if (params.systemPromptEnabled) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    OutlinedTextField(
                        value = params.systemPrompt,
                        onValueChange = { viewModel.setSystemPrompt(it) },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        placeholder = { Text("Custom system prompt...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${params.systemPrompt.length} characters",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ── Display Options ──
        item {
            SettingsSectionCard(
                icon = Icons.Outlined.Visibility,
                title = "Display Options"
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggleRow(
                    label = "Show Thinking",
                    subtitle = "Display model reasoning steps",
                    checked = params.showThinking,
                    onCheckedChange = { viewModel.setShowThinking(it) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                SettingsToggleRow(
                    label = "Show Reasoning",
                    subtitle = "Display chain-of-thought output",
                    checked = params.showReasoning,
                    onCheckedChange = { viewModel.setShowReasoning(it) }
                )
            }
        }

        // ── Prompt Presets ──
        item {
            SettingsSectionCard(
                icon = Icons.Outlined.Bookmark,
                title = "Prompt Presets",
                subtitle = "Quickly switch between predefined system prompts"
            ) {}
        }
        items(viewModel.presets) { preset ->
            PromptPresetCard(
                preset = preset,
                isActive = activePresetId == preset.id,
                onApply = { viewModel.applyPreset(preset) }
            )
        }

        // ── Support ──
        item {
            SettingsSectionCard(
                icon = Icons.Outlined.FavoriteBorder,
                title = "Support"
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsActionRow(
                    icon = Icons.Outlined.Star,
                    label = "Rate the App",
                    onClick = { openPlayStore(context, context.packageName) }
                )
                SettingsActionRow(
                    icon = Icons.Outlined.Share,
                    label = "Share the App",
                    onClick = { shareApp(context) }
                )
            }
        }

        // ── Legal ──
        item {
            SettingsSectionCard(
                icon = Icons.Outlined.Shield,
                title = "Legal"
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsActionRow(
                    icon = Icons.Outlined.Description,
                    label = "Privacy Policy",
                    onClick = { openUrl(context, "https://openmodels.app/privacy") }
                )
                SettingsActionRow(
                    icon = Icons.Outlined.Gavel,
                    label = "Terms & Conditions",
                    onClick = { openUrl(context, "https://openmodels.app/terms") }
                )
            }
        }

        // ── About ──
        item {
            val appVersion = remember {
                try {
                    val pkg = context.packageManager.getPackageInfo(context.packageName, 0)
                    "${pkg.versionName} (${pkg.longVersionCode})"
                } catch (_: Exception) { "1.0.3" }
            }
            SettingsSectionCard(
                icon = Icons.Outlined.Info,
                title = "About"
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "OpenModels",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Version $appVersion",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NeonCyan
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Run open-source LLMs locally on Android. " +
                                "Privacy-first, offline-first, and completely free.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ─── Reusable Components ───

@Composable
private fun SettingsSectionCard(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    StyledCard {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = NeonCyan.copy(alpha = 0.12f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            content()
        }
    }
}

@Composable
private fun ParameterSlider(
    title: String,
    description: String? = null,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    displayValue: String,
    onValueChange: (Float) -> Unit
) {
    var showInfoDialog by remember { mutableStateOf(false) }

    if (showInfoDialog && description != null) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text(title, fontWeight = FontWeight.Bold) },
            text = { Text(description) },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("Got it")
                }
            }
        )
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (description != null) {
                    IconButton(
                        onClick = { showInfoDialog = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "Info about $title",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = NeonCyan.copy(alpha = 0.12f)
            ) {
                Text(
                    text = displayValue,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = NeonCyan,
                activeTrackColor = NeonCyan,
                inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
            )
        )
    }
}

@Composable
private fun SettingsToggleRow(
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun PromptPresetCard(
    preset: PromptPreset,
    isActive: Boolean,
    onApply: () -> Unit
) {
    StyledCard(
        onClick = onApply
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = preset.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = preset.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(2.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = NeonCyan.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = preset.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }
            if (isActive) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = "Active",
                    tint = SuccessGreen,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// ─── External helpers ───

private fun openPlayStore(context: android.content.Context, packageName: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = android.net.Uri.parse("market://details?id=$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: android.content.ActivityNotFoundException) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = android.net.Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

private fun shareApp(context: android.content.Context) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, "Check out OpenModels - run open-source LLMs locally on Android!\nhttps://play.google.com/store/apps/details?id=${context.packageName}")
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share OpenModels"))
}

private fun openUrl(context: android.content.Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
