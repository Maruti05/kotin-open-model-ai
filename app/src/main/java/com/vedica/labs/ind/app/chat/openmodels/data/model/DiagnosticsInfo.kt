package com.vedica.labs.ind.app.chat.openmodels.data.model

data class DiagnosticsInfo(
    val totalRamGb: Double,
    val availableRamGb: Double,
    val cores: Int,
    val hasVulkan: Boolean,
    val hasNnapi: Boolean
) {
    val usedRamGb: Double get() = totalRamGb - availableRamGb
    val usedRamPercent: Float get() = if (totalRamGb > 0) (usedRamGb / totalRamGb).toFloat().coerceIn(0f, 1f) else 0f

    val deviceTier: Int get() = when {
        totalRamGb >= 8.0 -> 1
        totalRamGb >= 4.0 -> 2
        else -> 3
    }

    val tierLabel: String get() = when (deviceTier) {
        1 -> "T1 — High-End"
        2 -> "T2 — Mid-Range"
        3 -> "T3 — Entry Level"
        else -> "Unknown"
    }

    val healthScore: Int get() {
        val ramScore = (1f - usedRamPercent) * 35
        val tierScore = when (deviceTier) {
            1 -> 30; 2 -> 20; else -> 10
        }
        val cpuScore = (cores.toFloat() / 12f) * 20
        val accelScore = if (hasVulkan || hasNnapi) 15 else 0
        return (ramScore + tierScore + cpuScore + accelScore).toInt().coerceIn(0, 100)
    }
}
