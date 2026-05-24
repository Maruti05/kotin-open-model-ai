package com.vedica.labs.ind.app.chat.openmodels.domain.util

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.vedica.labs.ind.app.chat.openmodels.data.model.DiagnosticsInfo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HardwareChecker @Inject constructor() {

    fun getDiagnostics(context: Context): DiagnosticsInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val totalRamGb = memoryInfo.totalMem.toDouble() / (1024 * 1024 * 1024)
        val availableRamGb = memoryInfo.availMem.toDouble() / (1024 * 1024 * 1024)
        val cores = Runtime.getRuntime().availableProcessors()

        val hasVulkan = if (Build.VERSION.SDK_INT >= 24) {
            context.packageManager.hasSystemFeature("android.hardware.vulkan")
        } else false

        val hasNnapi = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1

        return DiagnosticsInfo(
            totalRamGb = (totalRamGb * 100).toInt() / 100.0,
            availableRamGb = (availableRamGb * 100).toInt() / 100.0,
            cores = cores,
            hasVulkan = hasVulkan,
            hasNnapi = hasNnapi
        )
    }

    fun getThreadRecommendation(modelId: String, cores: Int): Int {
        val lower = modelId.lowercase()
        return when {
            lower.contains("7b") || lower.contains("mistral") -> (cores - 2).coerceAtLeast(2)
            lower.contains("3b") || lower.contains("llama_3_3") -> (cores - 1).coerceAtLeast(2)
            else -> cores.coerceAtMost(4)
        }
    }
}
