package com.vedica.labs.ind.app.chat.openmodels.ui.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.vedica.labs.ind.app.chat.openmodels.data.model.BenchmarkResult
import com.vedica.labs.ind.app.chat.openmodels.data.model.DiagnosticsInfo
import com.vedica.labs.ind.app.chat.openmodels.ui.components.EmptyState
import com.vedica.labs.ind.app.chat.openmodels.ui.components.StatusBadge
import com.vedica.labs.ind.app.chat.openmodels.ui.components.StyledCard
import com.vedica.labs.ind.app.chat.openmodels.ui.theme.ErrorRed
import com.vedica.labs.ind.app.chat.openmodels.ui.theme.InfoBlue
import com.vedica.labs.ind.app.chat.openmodels.ui.theme.NeonCyan
import com.vedica.labs.ind.app.chat.openmodels.ui.theme.SuccessGreen
import com.vedica.labs.ind.app.chat.openmodels.ui.theme.VibrantIndigo
import com.vedica.labs.ind.app.chat.openmodels.ui.theme.WarningAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(
        checkNotNull<ViewModelStoreOwner>(
            LocalViewModelStoreOwner.current
        ) {
                "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
            }, null
    )
) {
    val diagnostics by viewModel.diagnostics.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val isBenchmarking by viewModel.isBenchmarking.collectAsState()
    val latestBenchmark by viewModel.latestBenchmark.collectAsState()
    val recentBenchmarks by viewModel.recentBenchmarks.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Telemetry") },
            actions = {
                IconButton(onClick = { viewModel.refreshDiagnostics() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground
            )
        )

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            error != null && diagnostics == null -> {
                EmptyState(
                    icon = Icons.Outlined.ErrorOutline,
                    title = "Diagnostics Unavailable",
                    subtitle = error,
                    actionLabel = "Retry",
                    onAction = { viewModel.refreshDiagnostics() }
                )
            }
            diagnostics != null -> {
                DashboardContent(
                    diagnostics = diagnostics!!,
                    latestBenchmark = latestBenchmark,
                    recentBenchmarks = recentBenchmarks,
                    isBenchmarking = isBenchmarking,
                    onRefresh = { viewModel.refreshDiagnostics() },
                    onRunBenchmark = { viewModel.runBenchmark() }
                )
            }
        }
    }
}

@Composable
private fun DashboardContent(
    diagnostics: DiagnosticsInfo,
    latestBenchmark: BenchmarkResult?,
    recentBenchmarks: List<BenchmarkResult>,
    isBenchmarking: Boolean,
    onRefresh: () -> Unit,
    onRunBenchmark: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Health Score
        item(key = "health") {
            AnimatedHealthScoreCard(score = diagnostics.healthScore)
        }

        // Stats Grid
        item(key = "stats1") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnimatedStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Memory,
                    label = "RAM Usage",
                    value = "${"%.1f".format(diagnostics.usedRamGb)} GB",
                    subtitle = "of ${"%.1f".format(diagnostics.totalRamGb)} GB",
                    gradient = Brush.linearGradient(listOf(NeonCyan.copy(alpha = 0.1f), Color.Transparent))
                )
                AnimatedStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Settings,
                    label = "CPU Cores",
                    value = "${diagnostics.cores}",
                    subtitle = "Logical cores",
                    gradient = Brush.linearGradient(listOf(VibrantIndigo.copy(alpha = 0.1f), Color.Transparent))
                )
            }
        }

        item(key = "stats2") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnimatedStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Speed,
                    label = "Performance Tier",
                    value = "T${diagnostics.deviceTier}",
                    subtitle = diagnostics.tierLabel.split("\u2014").lastOrNull()?.trim() ?: "",
                    gradient = Brush.linearGradient(listOf(InfoBlue.copy(alpha = 0.1f), Color.Transparent))
                )
                AnimatedStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Storage,
                    label = "Storage",
                    value = "${"%.1f".format(diagnostics.usedStorageGb)} GB",
                    subtitle = "of ${"%.1f".format(diagnostics.totalStorageGb)} GB",
                    gradient = Brush.linearGradient(listOf(SuccessGreen.copy(alpha = 0.1f), Color.Transparent))
                )
            }
        }

        // Device Info
        item(key = "device") {
            StyledCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DeviceInfoBadge(
                        icon = Icons.Outlined.Info,
                        label = "Android",
                        value = diagnostics.androidVersion
                    )
                    DeviceInfoBadge(
                        icon = Icons.Outlined.Smartphone,
                        label = "Device",
                        value = diagnostics.deviceName
                    )
                }
            }
        }

        // Acceleration
        item(key = "accel") {
            StyledCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AccelerationBadge(
                        label = "Vulkan",
                        supported = diagnostics.hasVulkan
                    )
                    AccelerationBadge(
                        label = "NNAPI",
                        supported = diagnostics.hasNnapi
                    )
                }
            }
        }

        // Resource utilization with animated progress
        item(key = "tier") {
            StyledCard {
                Column {
                    Text(
                        text = diagnostics.tierLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val ramProgress by animateFloatAsState(
                        targetValue = diagnostics.usedRamPercent,
                        animationSpec = tween(1000)
                    )
                    val ramColor = when {
                        diagnostics.usedRamPercent < 0.6f -> SuccessGreen
                        diagnostics.usedRamPercent < 0.85f -> WarningAmber
                        else -> ErrorRed
                    }
                    Text(
                        text = "RAM",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { ramProgress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = ramColor,
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${"%.1f".format(diagnostics.usedRamGb)} GB / ${"%.1f".format(diagnostics.totalRamGb)} GB",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${"%.0f".format(diagnostics.usedRamPercent * 100)}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = ramColor
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(12.dp))

                    val storageProgress by animateFloatAsState(
                        targetValue = diagnostics.usedStoragePercent,
                        animationSpec = tween(1000)
                    )
                    val storageColor = when {
                        diagnostics.usedStoragePercent < 0.6f -> SuccessGreen
                        diagnostics.usedStoragePercent < 0.85f -> WarningAmber
                        else -> ErrorRed
                    }
                    Text(
                        text = "Storage",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { storageProgress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = storageColor,
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${"%.1f".format(diagnostics.usedStorageGb)} GB / ${"%.1f".format(diagnostics.totalStorageGb)} GB",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${"%.0f".format(diagnostics.usedStoragePercent * 100)}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = storageColor
                        )
                    }
                }
            }
        }

        // Benchmark
        item(key = "benchmark") {
            BenchmarkCard(
                latestBenchmark = latestBenchmark,
                isBenchmarking = isBenchmarking,
                onRunBenchmark = onRunBenchmark
            )
        }

        // Benchmark History
        if (recentBenchmarks.isNotEmpty()) {
            item(key = "history_header") {
                Text(
                    text = "Benchmark History",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            items(recentBenchmarks.take(4), key = { it.id }) { benchmark ->
                BenchmarkHistoryItem(benchmark)
            }
        }
    }
}

@Composable
private fun AnimatedHealthScoreCard(score: Int) {
    val animatedScore by animateFloatAsState(
        targetValue = score.toFloat(),
        animationSpec = tween(1200)
    )
    val scoreColor = when {
        score >= 70 -> SuccessGreen
        score >= 40 -> WarningAmber
        else -> ErrorRed
    }

    StyledCard(
        gradient = Brush.linearGradient(
            listOf(scoreColor.copy(alpha = 0.05f), Color.Transparent)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Health Score",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Overall device readiness",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                val tierLabel = when {
                    score >= 70 -> "Excellent"
                    score >= 40 -> "Fair"
                    else -> "Limited"
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = scoreColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = tierLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { animatedScore / 100f },
                    modifier = Modifier.fillMaxSize(),
                    color = scoreColor,
                    trackColor = scoreColor.copy(alpha = 0.15f),
                    strokeWidth = 6.dp
                )
                Text(
                    text = "${animatedScore.toInt()}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = scoreColor
                )
            }
        }
    }
}

@Composable
private fun AnimatedStatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    subtitle: String,
    gradient: Brush? = null
) {
    StyledCard(modifier = modifier, gradient = gradient) {
        Column {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AccelerationBadge(label: String, supported: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = if (supported) Icons.Outlined.CheckCircle else Icons.Outlined.Cancel,
            contentDescription = null,
            tint = if (supported) SuccessGreen else ErrorRed,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        StatusBadge(
            text = if (supported) "Supported" else "Unsupported",
            isActive = supported
        )
    }
}

@Composable
private fun DeviceInfoBadge(
    icon: ImageVector,
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = NeonCyan,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun BenchmarkCard(
    latestBenchmark: BenchmarkResult?,
    isBenchmarking: Boolean,
    onRunBenchmark: () -> Unit
) {
    StyledCard {
        Column {
            Text(
                text = "On-Device Benchmark",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (isBenchmarking) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Running...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                FilledTonalButton(onClick = onRunBenchmark) {
                    Text("RUN BENCHMARK")
                }
            }
            if (latestBenchmark != null) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(12.dp))
                BenchmarkResultRow(latestBenchmark)
            }
        }
    }
}

@Composable
private fun BenchmarkResultRow(result: BenchmarkResult) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BenchmarkMetric("Tokens/sec", "${"%.1f".format(result.tokensPerSecond)}")
        BenchmarkMetric("Prompt Eval", "${result.promptEvalLatencyMs}ms")
        BenchmarkMetric("Generation", "${result.totalGenerationLatencyMs}ms")
        BenchmarkMetric("RAM Used", "${"%.0f".format(result.ramUsedMb)}MB")
    }
}

@Composable
private fun BenchmarkMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = NeonCyan
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BenchmarkHistoryItem(result: BenchmarkResult) {
    StyledCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = result.modelName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${"%.1f".format(result.tokensPerSecond)} tok/s",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = NeonCyan
                )
            }
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "${result.totalGenerationLatencyMs}ms",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
