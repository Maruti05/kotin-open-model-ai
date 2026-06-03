package com.vedica.labs.ind.app.chat.openmodels.data.model

data class DiagnosticsInfo(
    val totalRamGb: Double,
    val availableRamGb: Double,
    val cores: Int,
    val hasVulkan: Boolean,
    val hasNnapi: Boolean,
    val totalStorageGb: Double = 0.0,
    val availableStorageGb: Double = 0.0,
    val androidVersion: String = "",
    val deviceName: String = ""
) {
    val usedRamGb: Double get() = totalRamGb - availableRamGb
    val usedRamPercent: Float get() = if (totalRamGb > 0) (usedRamGb / totalRamGb).toFloat().coerceIn(0f, 1f) else 0f

    val usedStorageGb: Double get() = totalStorageGb - availableStorageGb
    val usedStoragePercent: Float get() = if (totalStorageGb > 0) (usedStorageGb / totalStorageGb).toFloat().coerceIn(0f, 1f) else 0f

    val deviceTier: Int get() = when {
        totalRamGb >= 8.0 -> 1
        totalRamGb >= 4.0 -> 2
        else -> 3
    }

    val tierLabel: String get() = when (deviceTier) {
        1 -> "T1 \u2014 High-End"
        2 -> "T2 \u2014 Mid-Range"
        3 -> "T3 \u2014 Entry Level"
        else -> "Unknown"
    }

    val healthScore: Int get() {
        val ramScore = (1f - usedRamPercent) * 30
        val storageScore = (1f - usedStoragePercent) * 15
        val tierScore = when (deviceTier) {
            1 -> 25; 2 -> 15; else -> 5
        }
        val cpuScore = (cores.toFloat() / 12f) * 15
        val accelScore = if (hasVulkan || hasNnapi) 15 else 0
        return (ramScore + storageScore + tierScore + cpuScore + accelScore).toInt().coerceIn(0, 100)
    }
}
