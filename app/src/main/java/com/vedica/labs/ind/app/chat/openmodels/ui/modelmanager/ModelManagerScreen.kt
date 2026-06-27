package com.vedica.labs.ind.app.chat.openmodels.ui.modelmanager

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.vedica.labs.ind.app.chat.openmodels.data.model.BackendType
import com.vedica.labs.ind.app.chat.openmodels.data.model.ModelDownloadState
import com.vedica.labs.ind.app.chat.openmodels.data.model.ModelInfo
import com.vedica.labs.ind.app.chat.openmodels.ui.components.CollapsibleSection
import com.vedica.labs.ind.app.chat.openmodels.ui.components.StyledCard
import com.vedica.labs.ind.app.chat.openmodels.ui.theme.ErrorRed
import com.vedica.labs.ind.app.chat.openmodels.ui.theme.NeonCyan
import com.vedica.labs.ind.app.chat.openmodels.ui.theme.SuccessGreen
import com.vedica.labs.ind.app.chat.openmodels.ui.theme.WarningAmber

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ModelManagerScreen(
    viewModel: ModelManagerViewModel = hiltViewModel(
        checkNotNull<ViewModelStoreOwner>(
            LocalViewModelStoreOwner.current
        ) {
                "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
            }, null
    )
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTier by viewModel.selectedTier.collectAsState()
    val downloadFilter by viewModel.downloadFilter.collectAsState()
    val selectedLicense by viewModel.selectedLicense.collectAsState()
    val selectedUseCase by viewModel.selectedUseCase.collectAsState()
    val selectedBackend by viewModel.selectedBackend.collectAsState()
    val filteredModels by viewModel.filteredModels.collectAsState()
    val downloadedIds by viewModel.downloadedModelIds.collectAsState()
    val downloads by viewModel.downloads.collectAsState()
    val loadingModelId by viewModel.loadingModelId.collectAsState()
    val loadedModelId by viewModel.loadedModelId.collectAsState()
    val error by viewModel.error.collectAsState()
    val caps by viewModel.deviceCapabilities.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Repository") },
            actions = {
                IconButton(onClick = { viewModel.refreshDeviceCapabilities() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground
            )
        )

        AnimatedVisibility(
            visible = caps.totalRamGb > 0,
            enter = expandVertically(animationSpec = tween(300)),
            exit = shrinkVertically(animationSpec = tween(300))
        ) {
            DeviceBanner(caps = caps)
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search models...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }

            item {
                StyledCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.Tune,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Filters",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        FilterSection("Tier") {
                            listOf(null to "All", 1 to "T1", 2 to "T2", 3 to "T3").forEach { (tier, label) ->
                                FilterChip(
                                    selected = selectedTier == tier,
                                    onClick = { viewModel.setSelectedTier(tier) },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NeonCyan.copy(alpha = 0.2f)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }

                        FilterSection("Status") {
                            DownloadFilter.entries.forEach { filter ->
                                val label = when (filter) {
                                    DownloadFilter.ALL -> "All"
                                    DownloadFilter.DOWNLOADED -> "Downloaded"
                                    DownloadFilter.NOT_DOWNLOADED -> "Not Downloaded"
                                }
                                FilterChip(
                                    selected = downloadFilter == filter,
                                    onClick = { viewModel.setDownloadFilter(filter) },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NeonCyan.copy(alpha = 0.2f)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }

                        CollapsibleSection(
                            icon = Icons.Outlined.Tune,
                            title = "More Filters",
                            subtitle = "License, Use Case, Backend"
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                FilterSection("License") {
                                    LicenseFilter.entries.forEach { license ->
                                        val label = when (license) {
                                            LicenseFilter.ALL -> "Any License"
                                            LicenseFilter.APACHE_2_0 -> "Apache 2.0"
                                            LicenseFilter.MIT -> "MIT"
                                        }
                                        FilterChip(
                                            selected = selectedLicense == license,
                                            onClick = { viewModel.setSelectedLicense(license) },
                                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = NeonCyan.copy(alpha = 0.2f)
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    }
                                }

                                FilterSection("Use Case") {
                                    UseCaseFilter.entries.forEach { useCase ->
                                        val label = when (useCase) {
                                            UseCaseFilter.ALL -> "Any Use"
                                            UseCaseFilter.CHAT -> "Chat"
                                            UseCaseFilter.CODE -> "Code"
                                            UseCaseFilter.REASONING -> "Reasoning"
                                            UseCaseFilter.VISION -> "Vision"
                                            UseCaseFilter.GENERAL -> "General"
                                        }
                                        FilterChip(
                                            selected = selectedUseCase == useCase,
                                            onClick = { viewModel.setSelectedUseCase(useCase) },
                                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = NeonCyan.copy(alpha = 0.2f)
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    }
                                }

                                FilterSection("Backend") {
                                    listOf(null to "All", BackendType.LITERT to "LiteRT", BackendType.LLAMA_CPP to "GGUF").forEach { (backend, label) ->
                                        FilterChip(
                                            selected = selectedBackend == backend,
                                            onClick = { viewModel.setSelectedBackend(backend) },
                                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = NeonCyan.copy(alpha = 0.2f)
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "${filteredModels.size} models \u00B7 ${downloadedIds.size} downloaded \u00B7 ${"%.1f".format(caps.availableRamGb)} GB free RAM",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (error != null) {
                item {
                    Snackbar {
                        Text(error!!)
                    }
                }
            }

            if (filteredModels.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No models found",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) "Try a different search" else "No models match the selected filters",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredModels, key = { it.id }) { model ->
                    val isDownloaded = downloadedIds.contains(model.id)
                    val downloadState = downloads[model.id]
                    val isLoading = loadingModelId == model.id
                    val isLoaded = loadedModelId == model.id
                    val canRun = viewModel.canRunModel(model.id)
                    val canDl = viewModel.canDownloadModel(model.id)
                    val incompatReason = viewModel.getIncompatibilityReason(model.id)

                    ModelCard(
                        model = model,
                        isDownloaded = isDownloaded,
                        downloadState = downloadState,
                        isLoading = isLoading,
                        isLoaded = isLoaded,
                        canRun = canRun,
                        canDownload = canDl,
                        incompatibilityReason = incompatReason,
                        onDownload = { viewModel.triggerDownload(model.id) },
                        onLoad = { viewModel.loadModelToRam(model.id) },
                        onUnload = { viewModel.unloadModel() },
                        onDelete = { viewModel.deleteModel(model.id) },
                        onRetry = { viewModel.retryDownload(model.id) },
                        onDismissError = { viewModel.clearDownloadError(model.id) },
                        onCancel = { viewModel.cancelDownload(model.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceBanner(caps: DeviceCapabilities) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DeviceStat("RAM", "${"%.1f".format(caps.availableRamGb)} GB", NeonCyan)
            DeviceStat("Storage", "${"%.1f".format(caps.availableStorageGb)} GB", SuccessGreen)
            DeviceStat("Battery", "${caps.batteryLevel}%", if (caps.batteryLevel > 20) SuccessGreen else WarningAmber)
        }
    }
}

@Composable
private fun DeviceStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterSection(title: String, content: @Composable FlowRowScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            content = content
        )
    }
}

@Composable
private fun ModelCard(
    model: ModelInfo,
    isDownloaded: Boolean,
    downloadState: ModelDownloadState?,
    isLoading: Boolean,
    isLoaded: Boolean,
    canRun: Boolean,
    canDownload: Boolean,
    incompatibilityReason: String?,
    onDownload: () -> Unit,
    onLoad: () -> Unit,
    onUnload: () -> Unit,
    onDelete: () -> Unit,
    onRetry: () -> Unit,
    onDismissError: () -> Unit,
    onCancel: () -> Unit
) {
    val isLiteRT = model.backendType == BackendType.LITERT

    StyledCard(
        gradient = if (isLoaded) Brush.horizontalGradient(
            listOf(NeonCyan.copy(alpha = 0.05f), MaterialTheme.colorScheme.surface)
        ) else null
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = model.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        InfoChip("${"%.1f".format(model.sizeMb / 1024.0)} GB")
                        InfoChip(model.params)
                        InfoChip("T${model.tier}")
                        InfoChip("${model.contextWindow} ctx")
                        BackendChip(isLiteRT = isLiteRT)
                        if (model.license == "apache-2.0" || model.license == "mit") {
                            InfoChip(
                                text = if (model.license == "apache-2.0") "Apache 2.0" else "MIT",
                            )
                        }
                    }
                }
                if (isLoaded) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SuccessGreen.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "ACTIVE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = model.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MetaText("Min RAM", "${"%.1f".format(model.minRamGb)} GB")
                MetaText("Model size", "${"%.1f".format(model.sizeMb / 1024.0)} GB")
            }

            if (incompatibilityReason != null && !isDownloaded) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = WarningAmber.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = WarningAmber,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = incompatibilityReason,
                            style = MaterialTheme.typography.labelSmall,
                            color = WarningAmber
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedVisibility(visible = downloadState != null && downloadState.isDownloading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DownloadProgressBar(
                        progress = downloadState?.progressFraction ?: 0f,
                        speed = downloadState?.downloadSpeedMbps ?: 0.0,
                        percentage = downloadState?.progressPercentage ?: 0.0,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Cancel,
                            contentDescription = "Cancel Download",
                            tint = ErrorRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            AnimatedVisibility(visible = downloadState != null && downloadState.isError) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = downloadState?.error ?: "Download failed",
                        style = MaterialTheme.typography.bodySmall,
                        color = ErrorRed,
                        modifier = Modifier.weight(1f)
                    )
                    Row {
                        TextButton(onClick = onDismissError) { Text("Dismiss", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        TextButton(onClick = onRetry) { Text("Retry") }
                    }
                }
            }

            if (downloadState == null || !downloadState.isDownloading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                if (isLoaded) {
                    FilledTonalButton(onClick = onUnload, colors = ButtonDefaults.filledTonalButtonColors(containerColor = WarningAmber.copy(alpha = 0.2f))) {
                        Text("UNLOAD", color = WarningAmber)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(onClick = onDelete, colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)) {
                        Text("DELETE")
                    }
                } else if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Loading...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else if (isDownloaded) {
                    FilledTonalButton(
                        onClick = onLoad,
                        enabled = canRun,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (canRun) NeonCyan.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(if (canRun) "LOAD TO RAM" else "INSUFFICIENT RAM")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(onClick = onDelete, colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)) {
                        Text("DELETE")
                    }
                } else {
                    val buttonLabel = if (!canDownload) {
                        when {
                            incompatibilityReason?.contains("RAM", ignoreCase = true) == true -> "NEEDS MORE RAM"
                            incompatibilityReason?.contains("storage", ignoreCase = true) == true -> "NO SPACE"
                            incompatibilityReason?.contains("battery", ignoreCase = true) == true -> "LOW BATTERY"
                            else -> "UNAVAILABLE"
                        }
                    } else "GET MODEL"
                    FilledTonalButton(
                        onClick = onDownload,
                        enabled = canDownload
                    ) {
                        Icon(
                            Icons.Outlined.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(buttonLabel)
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun InfoChip(text: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MetaText(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = NeonCyan
        )
    }
}

@Composable
private fun BackendChip(isLiteRT: Boolean) {
    val (label, containerColor, contentColor) = if (isLiteRT) {
        Triple("LiteRT", NeonCyan.copy(alpha = 0.15f), NeonCyan)
    } else {
        Triple("llama.cpp", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = containerColor,
        tonalElevation = if (isLiteRT) 0.dp else 2.dp
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            color = contentColor
        )
    }
}

@Composable
private fun DownloadProgressBar(
    progress: Float,
    speed: Double,
    percentage: Double,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = NeonCyan,
            trackColor = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${"%.1f".format(percentage)}%",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = NeonCyan
            )
            Text(
                text = "${"%.1f".format(speed)} Mbps",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
