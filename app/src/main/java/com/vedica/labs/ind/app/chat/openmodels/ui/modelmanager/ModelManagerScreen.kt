package com.vedica.labs.ind.app.chat.openmodels.ui.modelmanager

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.FlowRow
import androidx.hilt.navigation.compose.hiltViewModel
import com.vedica.labs.ind.app.chat.openmodels.data.model.ModelDownloadState
import com.vedica.labs.ind.app.chat.openmodels.data.model.ModelCatalog
import com.vedica.labs.ind.app.chat.openmodels.ui.components.EmptyState
import com.vedica.labs.ind.app.chat.openmodels.ui.components.StyledCard
import com.vedica.labs.ind.app.chat.openmodels.ui.theme.*

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ModelManagerScreen(
    viewModel: ModelManagerViewModel = hiltViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTier by viewModel.selectedTier.collectAsState()
    val downloadFilter by viewModel.downloadFilter.collectAsState()
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

        // Device capability banner
        AnimatedVisibility(
            visible = caps.totalRamGb > 0,
            enter = expandVertically(animationSpec = tween(300)),
            exit = shrinkVertically(animationSpec = tween(300))
        ) {
            DeviceBanner(caps = caps)
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
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

        Spacer(modifier = Modifier.height(8.dp))

        // Tier filters + Download filter dropdown — adaptive FlowRow
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
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

            // Download filter dropdown
            var expanded by remember { mutableStateOf(false) }
            Box {
                FilterChip(
                    selected = false,
                    onClick = { expanded = true },
                    label = {
                        Text(
                            text = when (downloadFilter) {
                                DownloadFilter.ALL -> "All"
                                DownloadFilter.DOWNLOADED -> "Downloaded"
                                DownloadFilter.NOT_DOWNLOADED -> "Not Downloaded"
                            },
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    trailingIcon = {
                        Icon(
                            Icons.Outlined.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    shape = RoundedCornerShape(8.dp)
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DownloadFilter.entries.forEach { filter ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = when (filter) {
                                        DownloadFilter.ALL -> "All Models"
                                        DownloadFilter.DOWNLOADED -> "Downloaded"
                                        DownloadFilter.NOT_DOWNLOADED -> "Not Downloaded"
                                    },
                                    fontWeight = if (filter == downloadFilter) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                viewModel.setDownloadFilter(filter)
                                expanded = false
                            },
                            leadingIcon = if (filter == downloadFilter) {
                                { Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            } else null
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Model count
        Text(
            text = "${filteredModels.size} models \u00B7 ${downloadedIds.size} downloaded \u00B7 ${"%.1f".format(caps.availableRamGb)} GB free RAM",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        if (error != null) {
            Snackbar(modifier = Modifier.padding(16.dp)) {
                Text(error!!)
            }
        }

        if (filteredModels.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.CloudDownload,
                title = "No models found",
                subtitle = if (searchQuery.isNotEmpty()) "Try a different search" else "No models match the selected filters"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredModels, key = { it["id"] as String }) { model ->
                    val modelId = model["id"] as String
                    val isDownloaded = downloadedIds.contains(modelId)
                    val downloadState = downloads[modelId]
                    val isLoading = loadingModelId == modelId
                    val isLoaded = loadedModelId == modelId
                    val canRun = viewModel.canRunModel(modelId)
                    val canDl = viewModel.canDownloadModel(modelId)
                    val incompatReason = viewModel.getIncompatibilityReason(modelId)

                    ModelCard(
                        model = model,
                        isDownloaded = isDownloaded,
                        downloadState = downloadState,
                        isLoading = isLoading,
                        isLoaded = isLoaded,
                        canRun = canRun,
                        canDownload = canDl,
                        incompatibilityReason = incompatReason,
                        onDownload = { viewModel.triggerDownload(modelId) },
                        onLoad = { viewModel.loadModelToRam(modelId) },
                        onUnload = { viewModel.unloadModel() },
                        onDelete = { viewModel.deleteModel(modelId) },
                        onRetry = { viewModel.retryDownload(modelId) },
                        onDismissError = { viewModel.clearDownloadError(modelId) }
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

@Composable
private fun ModelCard(
    model: Map<String, Any>,
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
    onDismissError: () -> Unit
) {
    val modelId = model["id"] as String
    val name = model["name"] as String
    val params = model["params"] as String
    val sizeMb = (model["sizeMb"] as Number).toDouble()
    val tier = (model["tier"] as Number).toInt()
    val minRam = (model["minRamGb"] as Number).toDouble()
    val description = model["description"] as String
    val contextWindow = (model["contextWindow"] as Number).toInt()

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
                        text = name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        InfoChip("${"%.1f".format(sizeMb / 1024.0)} GB")
                        InfoChip(params)
                        InfoChip("T$tier")
                        InfoChip("${contextWindow} ctx")
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
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MetaText("Min RAM", "${"%.1f".format(minRam)} GB")
                MetaText("Model size", "${"%.1f".format(sizeMb / 1024.0)} GB")
            }

            // Incompatibility warning
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

            // Download progress
            AnimatedVisibility(visible = downloadState != null && downloadState.isDownloading) {
                DownloadProgressBar(
                    progress = downloadState?.progressFraction ?: 0f,
                    speed = downloadState?.downloadSpeedMbps ?: 0.0,
                    percentage = downloadState?.progressPercentage ?: 0.0
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Error state
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

            // Actions
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
private fun DownloadProgressBar(progress: Float, speed: Double, percentage: Double) {
    Column {
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
