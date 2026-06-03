package com.vedica.labs.ind.app.chat.openmodels.domain.util

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.vedica.labs.ind.app.chat.openmodels.data.model.DiagnosticsInfo
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HardwareChecker @Inject constructor() {

    fun getDiagnostics(context: Context): DiagnosticsInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val totalRamGb = BigDecimal(memoryInfo.totalMem.toDouble() / (1024 * 1024 * 1024))
            .setScale(2, RoundingMode.HALF_UP).toDouble()
        val availableRamGb = BigDecimal(memoryInfo.availMem.toDouble() / (1024 * 1024 * 1024))
            .setScale(2, RoundingMode.HALF_UP).toDouble()
        val cores = Runtime.getRuntime().availableProcessors()

        val hasVulkan = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL)
        } else {
            context.packageManager.hasSystemFeature("android.hardware.vulkan")
        }

        val hasNnapi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.packageManager.hasSystemFeature("android.hardware.nnapi")
        } else {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1
        }

        val (totalStorage, availableStorage) = getStorageInfo()

        val androidVersion = Build.VERSION.RELEASE ?: ""
        val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"

        return DiagnosticsInfo(
            totalRamGb = totalRamGb,
            availableRamGb = availableRamGb,
            cores = cores,
            hasVulkan = hasVulkan,
            hasNnapi = hasNnapi,
            totalStorageGb = totalStorage,
            availableStorageGb = availableStorage,
            androidVersion = androidVersion,
            deviceName = deviceName
        )
    }

    private fun getStorageInfo(): Pair<Double, Double> {
        return try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val total = stat.totalBytes.toDouble() / (1024 * 1024 * 1024)
            val available = stat.availableBytes.toDouble() / (1024 * 1024 * 1024)
            val totalRounded = BigDecimal(total).setScale(2, RoundingMode.HALF_UP).toDouble()
            val availableRounded = BigDecimal(available).setScale(2, RoundingMode.HALF_UP).toDouble()
            Pair(totalRounded, availableRounded)
        } catch (_: Exception) {
            Pair(0.0, 0.0)
        }
    }

}
